package io.rudione.chatone.data.remote

import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import io.rudione.chatone.domain.model.IrcEvent
import io.rudione.chatone.util.Result
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlin.time.TimeSource

class TwitchAutoModSocket(
    private val httpClient: HttpClient,
    private val apiClient: TwitchApiClient,
    private val scope: CoroutineScope
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val _events = MutableSharedFlow<IrcEvent>(extraBufferCapacity = 256, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val events: SharedFlow<IrcEvent> = _events.asSharedFlow()

    private val wanted = MutableStateFlow<Set<String>>(emptySet())
    private val subscribed = mutableSetOf<String>()
    private val subscribedLock = Mutex()
    private var token = ""
    private var moderatorId = ""
    private var job: Job? = null

    fun connect(accessToken: String, userId: String, channelIds: Set<String>) {
        if (accessToken.isBlank() || userId.isBlank()) return
        val sameSession = accessToken == token && userId == moderatorId && job?.isActive == true
        wanted.value = wanted.value + channelIds
        if (sameSession) return
        token = accessToken
        moderatorId = userId
        wanted.value = channelIds
        start()
    }

    fun watch(channelId: String) {
        if (channelId.isNotBlank() && channelId !in wanted.value) wanted.value = wanted.value + channelId
    }

    fun disconnect() {
        job?.cancel()
        job = null
        token = ""
        moderatorId = ""
        wanted.value = emptySet()
    }

    private fun start() {
        job?.cancel()
        job = scope.launch {
            forgetSubscriptions()
            var url = DEFAULT_URL
            var backoffMs = INITIAL_BACKOFF_MS
            while (isActive && token.isNotEmpty()) {
                if (url == DEFAULT_URL) wanted.first { it.isNotEmpty() }
                val resumed = url != DEFAULT_URL
                val next = runSession(url, resumed)
                if (next != null) {
                    url = next
                    backoffMs = INITIAL_BACKOFF_MS
                } else {
                    url = DEFAULT_URL
                    forgetSubscriptions()
                    delay(backoffMs)
                    backoffMs = (backoffMs * 2).coerceAtMost(MAX_BACKOFF_MS)
                }
            }
        }
    }

    private suspend fun forgetSubscriptions() = subscribedLock.withLock { subscribed.clear() }

    private suspend fun runSession(url: String, resumed: Boolean): String? {
        var reconnectUrl: String? = null
        try {
            httpClient.webSocket(url) {
                var lastFrame = TimeSource.Monotonic.markNow()
                var keepaliveSeconds = DEFAULT_KEEPALIVE_SECONDS
                val watchdog = launch {
                    while (isActive) {
                        delay(WATCHDOG_TICK_MS)
                        if (lastFrame.elapsedNow().inWholeSeconds > keepaliveSeconds + KEEPALIVE_GRACE_SECONDS) {
                            Napier.w("AutoMod EventSub went silent, reconnecting", tag = TAG)
                            close()
                            return@launch
                        }
                    }
                }
                var subscriber: Job? = null
                for (frame in incoming) {
                    if (frame !is Frame.Text) continue
                    lastFrame = TimeSource.Monotonic.markNow()
                    val root = runCatching { json.parseToJsonElement(frame.readText()).jsonObject }.getOrNull() ?: continue
                    val metadata = root["metadata"] as? JsonObject ?: continue
                    val payload = root["payload"] as? JsonObject
                    when (metadata.text("message_type")) {
                        "session_welcome" -> {
                            val session = payload?.get("session") as? JsonObject
                            keepaliveSeconds = (session?.get("keepalive_timeout_seconds") as? JsonPrimitive)
                                ?.contentOrNull?.toLongOrNull() ?: DEFAULT_KEEPALIVE_SECONDS
                            val sessionId = session?.text("id") ?: continue
                            if (!resumed) forgetSubscriptions()
                            subscriber?.cancel()
                            subscriber = launch { wanted.collect { channels -> subscribeMissing(sessionId, channels) } }
                        }
                        "session_reconnect" -> {
                            reconnectUrl = ((payload?.get("session") as? JsonObject)?.text("reconnect_url"))
                            break
                        }
                        "revocation" -> {
                            val condition = (payload?.get("subscription") as? JsonObject)?.get("condition") as? JsonObject
                            condition?.text("broadcaster_user_id")?.let { id -> subscribedLock.withLock { subscribed.remove(id) } }
                        }
                        "notification" -> handleNotification(metadata.text("subscription_type"), payload)
                    }
                }
                watchdog.cancel()
                subscriber?.cancel()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Napier.w("AutoMod EventSub error: ${e.message}", tag = TAG)
        }
        return reconnectUrl
    }

    private suspend fun subscribeMissing(sessionId: String, channels: Set<String>) {
        for (channelId in channels) {
            if (subscribedLock.withLock { channelId in subscribed }) continue
            val condition = mapOf("broadcaster_user_id" to channelId, "moderator_user_id" to moderatorId)
            val held = apiClient.createEventSubSubscription(token, AutoModEventParser.HOLD, AutoModEventParser.VERSION, condition, sessionId)
            val updated = apiClient.createEventSubSubscription(token, AutoModEventParser.UPDATE, AutoModEventParser.VERSION, condition, sessionId)
            if (held is Result.Success && updated is Result.Success) subscribedLock.withLock { subscribed += channelId }
            else Napier.w("AutoMod subscription failed for $channelId", tag = TAG)
        }
    }

    private suspend fun handleNotification(type: String?, payload: JsonObject?) {
        val event = payload?.get("event") as? JsonObject ?: return
        val parsed = when (type) {
            AutoModEventParser.HOLD -> AutoModEventParser.hold(event)
            AutoModEventParser.UPDATE -> AutoModEventParser.update(event)
            else -> null
        } ?: return
        _events.emit(parsed)
    }

    private fun JsonObject.text(key: String): String? = (this[key] as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }

    private companion object {
        const val TAG = "AutoModSocket"
        const val DEFAULT_URL = "wss://eventsub.wss.twitch.tv/ws?keepalive_timeout_seconds=30"
        const val DEFAULT_KEEPALIVE_SECONDS = 30L
        const val KEEPALIVE_GRACE_SECONDS = 10L
        const val WATCHDOG_TICK_MS = 5_000L
        const val INITIAL_BACKOFF_MS = 2_000L
        const val MAX_BACKOFF_MS = 60_000L
    }
}
