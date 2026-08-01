package io.rudione.chatone.data.remote

import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.readText
import io.rudione.chatone.domain.model.IrcEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class TwitchEventSubClient(
    private val httpClient: HttpClient,
    private val apiClient: TwitchApiClient,
    private val scope: CoroutineScope
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
        connectionJob?.cancel()
        connectionJob = scope.launch {
            var backoff = 2_000L
            var nextUrl = url
            while (isActive && currentToken.isNotEmpty()) {
                try {
                    Napier.d("Connecting to EventSub ($nextUrl)...", tag = TAG)
                    httpClient.webSocket(nextUrl) {
                        session = this
                        backoff = 2_000L
                        for (frame in incoming) {
                            if (frame is Frame.Text) {
                                val reconnectUrl = handleMessage(frame.readText())
                                if (reconnectUrl != null) {
                                    nextUrl = reconnectUrl
                                    return@webSocket
                                }
                            }
                        }
                    }
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Napier.e("EventSub error: ${e.message}", e, tag = TAG)
                }
                session = null
                if (isActive && currentToken.isNotEmpty()) {
                    Napier.d("EventSub reconnecting in ${backoff}ms", tag = TAG)
                    delay(backoff)
                    backoff = (backoff * 2).coerceAtMost(120_000L)
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
                        "automod.message.hold" -> handleAutoModHold(event)
                        "automod.message.update" -> handleAutoModUpdate(event)
                    }
                }
            }
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
        apiClient.createEventSubSubscription(
            accessToken = currentToken,
            type = "automod.message.hold",
            version = "2",
            condition = condition,
            sessionId = sessionId
        )
        apiClient.createEventSubSubscription(
            accessToken = currentToken,
            type = "automod.message.update",
            version = "2",
            condition = condition,
            sessionId = sessionId
        )
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

    private suspend fun handleAutoModHold(event: JsonObject) {
        val msgId = event.str("message_id") ?: return
        val userId = event.str("user_id") ?: ""
        val login = event.str("user_login") ?: ""
        val displayName = event.str("user_name") ?: login
        val messageObj = event["message"]?.jsonObject
        val text = messageObj?.str("text")
            ?: messageObj?.get("fragments")?.jsonArray
                ?.joinToString("") { it.jsonObject.str("text") ?: "" }
            ?: ""
        val category = event["automod"]?.jsonObject?.str("category")
            ?: event["category"]?.jsonPrimitive?.content
        val level = event["automod"]?.jsonObject?.str("level")?.toIntOrNull()
            ?: event["level"]?.jsonPrimitive?.content?.toIntOrNull()

        _events.emit(
            IrcEvent.AutoModHeld(
                channel = currentChannelId,
                msgId = msgId,
                userId = userId,
                username = login,
                displayName = displayName,
                message = text,
                color = null,
                reasonCategory = category,
                reasonLevel = level
            )
        )
    }

    private suspend fun handleAutoModUpdate(event: JsonObject) {
        val msgId = event.str("message_id") ?: return
        val resolvedBy = event.str("moderator_user_login") ?: "a moderator"
        val status = when (event.str("status")?.lowercase()) {
            "approved" -> "ALLOWED"
            "denied" -> "DENIED"
            else -> return
        }
        _events.emit(
            IrcEvent.AutoModResolved(
                channel = currentChannelId,
                msgId = msgId,
                resolvedBy = resolvedBy,
                action = status
            )
        )
    }
}
