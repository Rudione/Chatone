package io.rudione.chatone.data.remote.emote

import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.close
import io.ktor.websocket.readText
import io.rudione.chatone.data.remote.dto.SevenTvEventMessage
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * 7TV EventAPI v3 WebSocket client for real-time emote set updates.
 * Subscribes to emote_set.update events for channels to get live
 * notifications when emotes are added/removed/renamed.
 */
class SevenTvEventApi(
    private val httpClient: HttpClient,
    private val scope: CoroutineScope
) {
    companion object {
        private const val TAG = "7TV-EventAPI"
        private const val EVENT_API_URL = "wss://events.7tv.io/v3"
        private const val OP_DISPATCH = 0
        private const val OP_HELLO = 1
        private const val OP_HEARTBEAT = 2
        private const val OP_SUBSCRIBE = 35
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private var session: WebSocketSession? = null
    private var heartbeatJob: Job? = null
    private var receiveJob: Job? = null
    private val subscribedSets = mutableSetOf<String>()

    // Events flow
    private val _emoteSetUpdates = MutableSharedFlow<EmoteSetUpdateEvent>(extraBufferCapacity = 64)
    val emoteSetUpdates: SharedFlow<EmoteSetUpdateEvent> = _emoteSetUpdates

    sealed class EmoteSetUpdateEvent {
        data class EmoteAdded(
            val emoteSetId: String,
            val emoteId: String,
            val emoteName: String,
            val actorName: String
        ) : EmoteSetUpdateEvent()

        data class EmoteRemoved(
            val emoteSetId: String,
            val emoteId: String,
            val emoteName: String,
            val actorName: String
        ) : EmoteSetUpdateEvent()

        data class EmoteRenamed(
            val emoteSetId: String,
            val emoteId: String,
            val oldName: String,
            val newName: String,
            val actorName: String
        ) : EmoteSetUpdateEvent()
    }

    suspend fun connect() {
        try {
            session = httpClient.webSocketSession(EVENT_API_URL)
            Napier.d("Connected to 7TV EventAPI", tag = TAG)
            startReceiving()
        } catch (e: Exception) {
            Napier.e("Failed to connect to 7TV EventAPI: ${e.message}", tag = TAG)
        }
    }

    fun subscribeToEmoteSet(emoteSetId: String) {
        if (emoteSetId in subscribedSets) return
        subscribedSets.add(emoteSetId)

        scope.launch {
            val subscribeMsg = """
                {"op":$OP_SUBSCRIBE,"d":{"type":"emote_set.update","condition":{"object_id":"$emoteSetId"}}}
            """.trimIndent()
            try {
                session?.send(Frame.Text(subscribeMsg))
                Napier.d("Subscribed to emote set: $emoteSetId", tag = TAG)
            } catch (e: Exception) {
                Napier.e("Failed to subscribe to emote set: ${e.message}", tag = TAG)
            }
        }
    }

    fun unsubscribeFromEmoteSet(emoteSetId: String) {
        subscribedSets.remove(emoteSetId)
    }

    private fun startReceiving() {
        receiveJob?.cancel()
        receiveJob = scope.launch {
            val ws = session ?: return@launch
            try {
                for (frame in ws.incoming) {
                    if (frame is Frame.Text) {
                        handleMessage(frame.readText())
                    }
                }
            } catch (e: Exception) {
                Napier.e("7TV EventAPI receive error: ${e.message}", tag = TAG)
                reconnect()
            }
        }
    }

    private fun handleMessage(text: String) {
        try {
            val message = json.decodeFromString<SevenTvEventMessage>(text)
            when (message.op) {
                OP_HELLO -> {
                    val interval = message.d?.heartbeatInterval ?: 30000
                    startHeartbeat(interval)
                    Napier.d("7TV EventAPI hello, heartbeat interval: ${interval}ms", tag = TAG)
                    // Re-subscribe to all sets after reconnect
                    subscribedSets.toList().forEach { setId ->
                        scope.launch { subscribeToEmoteSet(setId) }
                    }
                }
                OP_DISPATCH -> handleDispatch(message)
                OP_HEARTBEAT -> { /* Server heartbeat ack */ }
            }
        } catch (e: Exception) {
            Napier.w("Failed to parse 7TV event: ${e.message}", tag = TAG)
        }
    }

    private fun handleDispatch(message: SevenTvEventMessage) {
        val data = message.d ?: return
        val body = data.body ?: return
        val emoteSetId = data.condition["object_id"] ?: body.id
        val actorName = body.actor?.displayName ?: body.actor?.username ?: "Unknown"

        // Handle pushed emotes (added)
        body.pushed.forEach { pushed ->
            val emote = pushed.value ?: return@forEach
            scope.launch {
                _emoteSetUpdates.emit(
                    EmoteSetUpdateEvent.EmoteAdded(
                        emoteSetId = emoteSetId,
                        emoteId = emote.id,
                        emoteName = emote.name,
                        actorName = actorName
                    )
                )
            }
        }

        // Handle pulled emotes (removed)
        body.pulled.forEach { pulled ->
            val emote = pulled.old_value ?: return@forEach
            scope.launch {
                _emoteSetUpdates.emit(
                    EmoteSetUpdateEvent.EmoteRemoved(
                        emoteSetId = emoteSetId,
                        emoteId = emote.id,
                        emoteName = emote.name,
                        actorName = actorName
                    )
                )
            }
        }

        // Handle updated emotes (renamed)
        body.updated.forEach { updated ->
            val oldEmote = updated.old_value
            val newEmote = updated.value
            if (oldEmote != null && newEmote != null && oldEmote.name != newEmote.name) {
                scope.launch {
                    _emoteSetUpdates.emit(
                        EmoteSetUpdateEvent.EmoteRenamed(
                            emoteSetId = emoteSetId,
                            emoteId = newEmote.id,
                            oldName = oldEmote.name,
                            newName = newEmote.name,
                            actorName = actorName
                        )
                    )
                }
            }
        }
    }

    private fun startHeartbeat(intervalMs: Long) {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (isActive) {
                delay(intervalMs)
                try {
                    session?.send(Frame.Text("""{"op":$OP_HEARTBEAT}"""))
                } catch (e: Exception) {
                    Napier.e("7TV heartbeat failed: ${e.message}", tag = TAG)
                    break
                }
            }
        }
    }

    private fun reconnect() {
        scope.launch {
            delay(5000)
            Napier.d("Reconnecting to 7TV EventAPI...", tag = TAG)
            connect()
        }
    }

    suspend fun disconnect() {
        heartbeatJob?.cancel()
        receiveJob?.cancel()
        session?.close()
        session = null
        subscribedSets.clear()
        Napier.d("Disconnected from 7TV EventAPI", tag = TAG)
    }
}
