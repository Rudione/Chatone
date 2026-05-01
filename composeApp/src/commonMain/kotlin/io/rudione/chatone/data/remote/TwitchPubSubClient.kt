package io.rudione.chatone.data.remote

import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.close
import io.ktor.websocket.readText
import io.rudione.chatone.domain.model.IrcEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class TwitchPubSubClient(
    private val httpClient: HttpClient,
    private val scope: CoroutineScope
) {
    private val TAG = "TwitchPubSub"
    private val PUBSUB_URL = "wss://pubsub-edge.twitch.tv/v1"
    private val json = Json { ignoreUnknownKeys = true }

    private val _events = MutableSharedFlow<IrcEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<IrcEvent> = _events

    private var connectionJob: Job? = null
    private var session: WebSocketSession? = null
    private var pingJob: Job? = null

    private var currentToken: String = ""
    private var currentUserId: String = ""
    private var currentChannelId: String = ""

    fun connect(accessToken: String, userId: String, channelId: String) {
        if (accessToken.isEmpty() || userId.isEmpty() || channelId.isEmpty()) return
        if (accessToken == currentToken && userId == currentUserId && channelId == currentChannelId) return

        currentToken = accessToken
        currentUserId = userId
        currentChannelId = channelId
        reconnect()
    }

    fun disconnect() {
        pingJob?.cancel()
        connectionJob?.cancel()
        session = null
        currentToken = ""
        currentUserId = ""
        currentChannelId = ""
    }

    private fun reconnect() {
        pingJob?.cancel()
        connectionJob?.cancel()
        connectionJob = scope.launch {
            var backoff = 2_000L
            while (isActive && currentToken.isNotEmpty()) {
                try {
                    Napier.d("Connecting to PubSub...", tag = TAG)
                    httpClient.webSocket(PUBSUB_URL) {
                        session = this
                        val topic = "automod-queue.$currentUserId.$currentChannelId"
                        val listenMsg = """{"type":"LISTEN","data":{"topics":["$topic"],"auth_token":"$currentToken"}}"""
                        send(Frame.Text(listenMsg))
                        Napier.d("Subscribed to PubSub topic: $topic", tag = TAG)

                        pingJob = scope.launch {
                            while (isActive) {
                                delay(60_000L)
                                try {
                                    send(Frame.Text("{\"type\":\"PING\"}"))
                                } catch (_: Exception) {}
                            }
                        }

                        backoff = 2_000L
                        for (frame in incoming) {
                            if (frame is Frame.Text) {
                                handleMessage(frame.readText())
                            }
                        }
                    }
                } catch (e: Exception) {
                    Napier.e("PubSub error: ${e.message}", e, tag = TAG)
                }
                session = null
                pingJob?.cancel()
                if (isActive && currentToken.isNotEmpty()) {
                    Napier.d("PubSub reconnecting in ${backoff}ms", tag = TAG)
                    delay(backoff)
                    backoff = (backoff * 2).coerceAtMost(120_000L)
                }
            }
        }
    }

    private fun handleMessage(raw: String) {
        try {
            val obj = json.parseToJsonElement(raw).jsonObject
            val type = obj["type"]?.jsonPrimitive?.content ?: return

            when (type) {
                "PONG" -> Napier.v("PubSub PONG", tag = TAG)
                "RECONNECT" -> {
                    Napier.d("PubSub server requested reconnect", tag = TAG)
                    reconnect()
                }
                "RESPONSE" -> {
                    val error = obj["error"]?.jsonPrimitive?.content
                    if (!error.isNullOrEmpty()) {
                        Napier.e("PubSub LISTEN error: $error", tag = TAG)
                    }
                }
                "MESSAGE" -> {
                    val dataObj = obj["data"]?.jsonObject ?: return
                    val topic = dataObj["topic"]?.jsonPrimitive?.content ?: return
                    val messageStr = dataObj["message"]?.jsonPrimitive?.content ?: return

                    if (topic.startsWith("automod-queue.")) {
                        handleAutoModMessage(messageStr)
                    }
                }
            }
        } catch (e: Exception) {
            Napier.e("PubSub parse error: ${e.message}", tag = TAG)
        }
    }

    private fun handleAutoModMessage(messageStr: String) {
        try {
            val msgObj = json.parseToJsonElement(messageStr).jsonObject
            val msgType = msgObj["type"]?.jsonPrimitive?.content ?: return

            if (msgType != "automod_caught_message") return

            val data = msgObj["data"]?.jsonObject ?: return
            val msgId = data["message_id"]?.jsonPrimitive?.content ?: return
            val contentObj = data["message"]?.jsonObject
            val text = contentObj?.get("text")?.jsonPrimitive?.content
                ?: contentObj?.get("content")?.jsonPrimitive?.content
                ?: ""

            val senderObj = data["sender"]?.jsonObject
            val userId = senderObj?.get("user_id")?.jsonPrimitive?.content ?: ""
            val login = senderObj?.get("login")?.jsonPrimitive?.content ?: ""
            val displayName = senderObj?.get("display_name")?.jsonPrimitive?.content ?: login
            val color = senderObj?.get("chat_color")?.jsonPrimitive?.content

            val channel = currentChannelId

            Napier.d("AutoMod held: msgId=$msgId user=$login text=$text", tag = TAG)

            scope.launch {
                _events.emit(
                    IrcEvent.AutoModHeld(
                        channel = channel,
                        msgId = msgId,
                        userId = userId,
                        username = login,
                        displayName = displayName,
                        message = text,
                        color = color
                    )
                )
            }
        } catch (e: Exception) {
            Napier.e("AutoMod message parse error: ${e.message}", e, tag = TAG)
        }
    }
}
