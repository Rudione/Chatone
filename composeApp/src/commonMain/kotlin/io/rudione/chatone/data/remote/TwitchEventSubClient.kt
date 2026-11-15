package io.rudione.chatone.data.remote

import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.rudione.chatone.util.network.NetworkMonitor
import io.rudione.chatone.util.network.recoverSocket
import kotlinx.coroutines.withTimeoutOrNull
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.readText
import io.rudione.chatone.domain.model.IrcEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.coroutines.CancellationException

class TwitchEventSubClient(
    private val httpClient: HttpClient,
    private val apiClient: TwitchApiClient,
    private val scope: CoroutineScope,
    private val networkMonitor: NetworkMonitor = NetworkMonitor(scope)
) {
    private val TAG = "EventSub"
    private val DEFAULT_URL = "wss://eventsub.wss.twitch.tv/ws"
    private val json = Json { ignoreUnknownKeys = true }

    private val _events = MutableSharedFlow<IrcEvent>(
        extraBufferCapacity = 512,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events: SharedFlow<IrcEvent> = _events

    private var connectionJob: Job? = null
    private var session: WebSocketSession? = null

    private var currentToken: String = ""
    private var currentUserId: String = ""
    private var currentChannelId: String = ""

    fun connect(accessToken: String, userId: String, channelId: String) {
        if (accessToken.isEmpty() || userId.isEmpty() || channelId.isEmpty()) return
        if (accessToken == currentToken && userId == currentUserId && channelId == currentChannelId) return

        currentToken = accessToken
        currentUserId = userId
        currentChannelId = channelId
        reconnect(DEFAULT_URL)
    }

    fun disconnect() {
        connectionJob?.cancel()
        session = null
        currentToken = ""
        currentUserId = ""
        currentChannelId = ""
    }

    private fun reconnect(url: String) {
        val previous = connectionJob
        previous?.cancel()
        connectionJob = scope.launch {
            previous?.join()
            var nextUrl = url
            networkMonitor.recoverSocket(onFailure = {
                nextUrl = DEFAULT_URL
                Napier.w("EventSub connection interrupted: ${it::class.simpleName}", tag = TAG)
            }) {
                try {
                    val connectUrl = nextUrl
                    nextUrl = DEFAULT_URL
                    Napier.d("Connecting to EventSub", tag = TAG)
                    httpClient.webSocket(connectUrl) {
                        session = this
                        var silenceLimitMs = 15_000L
                        while (isActive) {
                            val frame = withTimeoutOrNull(silenceLimitMs) { incoming.receiveCatching().getOrNull() }
                                ?: throw EventSubHeartbeatTimeout()
                            if (frame is Frame.Text) {
                                val raw = frame.readText()
                                val sessionData = runCatching {
                                    json.parseToJsonElement(raw).jsonObject["payload"]?.jsonObject?.get("session")?.jsonObject
                                }.getOrNull()
                                sessionData?.get("keepalive_timeout_seconds")?.jsonPrimitive?.content?.toLongOrNull()?.let {
                                    silenceLimitMs = (it.coerceIn(10, 600) + 10) * 1_000
                                }
                                val reconnectUrl = handleMessage(raw)
                                if (reconnectUrl != null) {
                                    nextUrl = reconnectUrl
                                    return@webSocket
                                }
                            }
                        }
                    }
                } finally {
                    session = null
                }
            }
        }
    }

    private suspend fun handleMessage(raw: String): String? {
        try {
            val obj = json.parseToJsonElement(raw).jsonObject
            val metadata = obj["metadata"]?.jsonObject ?: return null
            val payload = obj["payload"]?.jsonObject
            when (metadata["message_type"]?.jsonPrimitive?.content) {
                "session_welcome" -> {
                    val sessionId = payload?.get("session")?.jsonObject
                        ?.get("id")?.jsonPrimitive?.content ?: return null
                    Napier.d("EventSub session welcome: $sessionId", tag = TAG)
                    subscribeAll(sessionId)
                }

                "session_keepalive" -> {}

                "session_reconnect" -> {
                    val newUrl = payload?.get("session")?.jsonObject
                        ?.get("reconnect_url")?.jsonPrimitive?.content
                    Napier.d("EventSub session reconnect requested", tag = TAG)
                    return newUrl
                }

                "revocation" -> {
                    val type = payload?.get("subscription")?.jsonObject
                        ?.get("type")?.jsonPrimitive?.content
                    Napier.w("EventSub subscription revoked: $type", tag = TAG)
                }

                "notification" -> {
                    val subType = metadata["subscription_type"]?.jsonPrimitive?.content
                    val event = payload?.get("event")?.jsonObject ?: return null
                    when (subType) {
                        "channel.moderate" -> handleModerate(event)
                        RewardRedemptionParser.EVENTSUB_REDEMPTION_ADD ->
                            RewardRedemptionParser.fromEventSub(event)?.let { _events.emit(IrcEvent.RewardRedeemed(it)) }
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Napier.e("EventSub parse error: ${e.message}", e, tag = TAG)
        }
        return null
    }

    private suspend fun subscribeAll(sessionId: String) {
        val condition = mapOf(
            "broadcaster_user_id" to currentChannelId,
            "moderator_user_id" to currentUserId
        )
        apiClient.createEventSubSubscription(
            accessToken = currentToken,
            type = "channel.moderate",
            version = "2",
            condition = condition,
            sessionId = sessionId
        )
        if (currentUserId == currentChannelId) {
            apiClient.createEventSubSubscription(
                accessToken = currentToken,
                type = RewardRedemptionParser.EVENTSUB_REDEMPTION_ADD,
                version = "1",
                condition = mapOf("broadcaster_user_id" to currentChannelId),
                sessionId = sessionId
            )
        }
    }

    private fun JsonObject.str(key: String): String? = this[key]?.jsonPrimitive?.content

    private suspend fun handleModerate(event: JsonObject) {
        val rawAction = event.str("action") ?: return
        val moderator = event.str("moderator_user_login") ?: return
        val moderatorUserId = event.str("moderator_user_id")

        val detail = event[rawAction]?.jsonObject

        val action = when (rawAction.lowercase()) {
            "ban" -> IrcEvent.ModeratorAction.ACTION_BAN
            "timeout" -> IrcEvent.ModeratorAction.ACTION_TIMEOUT
            "unban" -> IrcEvent.ModeratorAction.ACTION_UNBAN
            "untimeout" -> IrcEvent.ModeratorAction.ACTION_UNTIMEOUT
            "delete" -> IrcEvent.ModeratorAction.ACTION_DELETE
            "clear" -> IrcEvent.ModeratorAction.ACTION_CLEAR
            "mod" -> IrcEvent.ModeratorAction.ACTION_MOD
            "unmod" -> IrcEvent.ModeratorAction.ACTION_UNMOD
            "vip" -> IrcEvent.ModeratorAction.ACTION_VIP
            "unvip" -> IrcEvent.ModeratorAction.ACTION_UNVIP
            "warn" -> "warn"
            else -> rawAction.lowercase()
        }

        val target = detail?.str("user_login")
        val targetUserId = detail?.str("user_id")
        val targetMessageId = detail?.str("message_id")
        val reason = detail?.str("reason")

        val duration = if (action == IrcEvent.ModeratorAction.ACTION_TIMEOUT) {
            detail?.str("expires_at")?.let { expires ->
                runCatching {
                    val secs = (Instant.parse(expires) - Clock.System.now()).inWholeSeconds
                    secs.coerceAtLeast(1).toInt()
                }.getOrNull()
            }
        } else null

        Napier.d("EventSub moderate: $action by $moderator on ${target ?: "-"}", tag = TAG)

        _events.emit(
            IrcEvent.ModeratorAction(
                channel = currentChannelId,
                action = action,
                moderator = moderator,
                moderatorUserId = moderatorUserId,
                target = target,
                targetUserId = targetUserId,
                duration = duration,
                reason = reason,
                targetMessageId = targetMessageId
            )
        )
    }
}

private class EventSubHeartbeatTimeout : Exception()
