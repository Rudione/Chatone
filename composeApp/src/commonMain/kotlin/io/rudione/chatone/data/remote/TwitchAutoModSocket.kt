package io.rudione.chatone.data.remote

import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocket
import io.rudione.chatone.util.network.NetworkMonitor
import io.rudione.chatone.util.network.recoverSocket
import kotlinx.atomicfu.atomic
import kotlin.time.Clock
import io.ktor.websocket.Frame
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
import kotlinx.coroutines.flow.collectLatest
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

class TwitchAutoModSocket(
    private val httpClient: HttpClient,
    private val apiClient: TwitchApiClient,
    private val scope: CoroutineScope,
    private val networkMonitor: NetworkMonitor = NetworkMonitor(scope)
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
        token = ""
        moderatorId = ""
        wanted.value = emptySet()
    }

    private fun start() {
        val previous = job
        previous?.cancel()
        job = scope.launch {
            previous?.join()
            forgetSubscriptions()
            var url = DEFAULT_URL
            networkMonitor.recoverSocket(onFailure = { Napier.w("AutoMod connection interrupted: ${it::class.simpleName}", tag = TAG) }) {
                if (url == DEFAULT_URL) wanted.first { it.isNotEmpty() }
                val connectingUrl = url
                url = DEFAULT_URL
                val next = runSession(connectingUrl, connectingUrl != DEFAULT_URL)
                if (next != null) url = next else forgetSubscriptions()
            }
        }
    }

    private suspend fun forgetSubscriptions() = subscribedLock.withLock { subscribed.clear() }

    private suspend fun runSession(url: String, resumed: Boolean): String? {
        var reconnectUrl: String? = null
        try {
            httpClient.webSocket(url) {
                val lastFrame = atomic(Clock.System.now().toEpochMilliseconds())
                val keepaliveSeconds = atomic(DEFAULT_KEEPALIVE_SECONDS)
                val watchdog = launch {
                    while (isActive) {
                        delay(WATCHDOG_TICK_MS)
                        if (Clock.System.now().toEpochMilliseconds() - lastFrame.value > (keepaliveSeconds.value + KEEPALIVE_GRACE_SECONDS) * 1_000) {
                            Napier.w("AutoMod EventSub went silent, reconnecting", tag = TAG)
                            throw AutoModHeartbeatTimeout()
                        }
                    }
                }
                var subscriber: Job? = null
                for (frame in incoming) {
                    if (frame !is Frame.Text) continue
                    lastFrame.value = Clock.System.now().toEpochMilliseconds()
                    val root = runCatching { json.parseToJsonElement(frame.readText()).jsonObject }.getOrNull() ?: continue
                    val metadata = root["metadata"] as? JsonObject ?: continue
                    val payload = root["payload"] as? JsonObject
                    when (metadata.text("message_type")) {
                        "session_welcome" -> {
                            val session = payload?.get("session") as? JsonObject
                            keepaliveSeconds.value = (session?.get("keepalive_timeout_seconds") as? JsonPrimitive)
                                ?.contentOrNull?.toLongOrNull() ?: DEFAULT_KEEPALIVE_SECONDS
                            val sessionId = session?.text("id") ?: continue
                            if (!resumed) forgetSubscriptions()
                            subscriber?.cancel()
                            subscriber = launch {
                                wanted.collectLatest { channels ->
                                    var backoff = LIMIT_RETRY_START_MS
                                    while (!subscribeMissing(sessionId, channels)) {
                                        delay(backoff)
                                        backoff = (backoff * 2).coerceAtMost(LIMIT_RETRY_MAX_MS)
                                    }
                                }
                            }
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

    private suspend fun subscribeMissing(sessionId: String, channels: Set<String>): Boolean {
        for (channelId in channels) {
            if (subscribedLock.withLock { channelId in subscribed }) continue
            val condition = mapOf("broadcaster_user_id" to channelId, "moderator_user_id" to moderatorId)
            val held = apiClient.createEventSubSubscription(token, AutoModEventParser.HOLD, AutoModEventParser.VERSION, condition, sessionId)
            if (held.isTransportLimit()) {
                Napier.w("AutoMod EventSub: websocket transport limit reached for this account, retrying later", tag = TAG)
                return false
            }
            val updated = apiClient.createEventSubSubscription(token, AutoModEventParser.UPDATE, AutoModEventParser.VERSION, condition, sessionId)
            if (held is Result.Success && updated is Result.Success) subscribedLock.withLock { subscribed += channelId }
            else Napier.w("AutoMod subscription failed for $channelId", tag = TAG)
        }
        return true
    }

    private fun Result<Unit>.isTransportLimit(): Boolean =
        ((this as? Result.Error)?.exception as? HelixApiException)?.status == HTTP_TOO_MANY

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
        const val LIMIT_RETRY_START_MS = 60_000L
        const val LIMIT_RETRY_MAX_MS = 600_000L
        const val HTTP_TOO_MANY = 429
        const val TAG = "AutoModSocket"
        const val DEFAULT_URL = "wss://eventsub.wss.twitch.tv/ws?keepalive_timeout_seconds=30"
        const val DEFAULT_KEEPALIVE_SECONDS = 30L
        const val KEEPALIVE_GRACE_SECONDS = 10L
        const val WATCHDOG_TICK_MS = 5_000L
    }
}

private class AutoModHeartbeatTimeout : Exception()
