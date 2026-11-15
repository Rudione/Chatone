package io.rudione.chatone.data.remote.emote

import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.websocket.Frame
import io.rudione.chatone.util.network.NetworkMonitor
import io.rudione.chatone.util.network.recoverSocket
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.MutableStateFlow
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.readText
import io.rudione.chatone.data.remote.dto.SevenTvEventBody
import io.rudione.chatone.data.remote.dto.SevenTvEventMessage
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CancellationException

class SevenTvEventApi(
    private val httpClient: HttpClient,
    private val scope: CoroutineScope,
    private val networkMonitor: NetworkMonitor = NetworkMonitor(scope)
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
    private var connectionJob: Job? = null
    private val lifecycleMutex = Mutex()
    private val heartbeatInterval = MutableStateFlow(30_000L)
    private val subscribedSets = SevenTvSubscriptions()
    private val subscribedTwitchChannels = SevenTvSubscriptions()

    private val _emoteSetUpdates = MutableSharedFlow<EmoteSetUpdateEvent>(
        extraBufferCapacity = 256,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
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

        data class PersonalEmoteSetGranted(
            val twitchUserId: String,
            val twitchUsername: String,
            val emoteSetId: String
        ) : EmoteSetUpdateEvent()

        data class PersonalEmoteSetRevoked(
            val twitchUserId: String,
            val emoteSetId: String
        ) : EmoteSetUpdateEvent()
    }

    suspend fun connect() = lifecycleMutex.withLock {
        if (connectionJob?.isActive == true) return@withLock
        var initialSession = if (networkMonitor.status.value.available == false) null else try {
            withTimeout(15_000.milliseconds) { httpClient.webSocketSession(EVENT_API_URL) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Napier.w("7TV connection unavailable: ${e::class.simpleName}", tag = TAG)
            null
        }
        connectionJob = scope.launch {
            networkMonitor.recoverSocket(onFailure = {
                Napier.w(
                    "7TV connection interrupted: ${it::class.simpleName}",
                    tag = TAG
                )
            }) {
                val ws = initialSession
                    ?: withTimeout(15_000.milliseconds) { httpClient.webSocketSession(EVENT_API_URL) }
                initialSession = null
                session = ws
                heartbeatInterval.value = 30_000
                try {
                    var silenceLimit = 15_000L
                    while (isActive) {
                        val frame = withTimeoutOrNull(silenceLimit.milliseconds) {
                            ws.incoming.receiveCatching().getOrNull()
                        }
                            ?: throw SevenTvHeartbeatTimeout()
                        if (frame is Frame.Text) {
                            handleMessage(frame.readText())
                            silenceLimit = heartbeatInterval.value * 2 + 10_000
                        }
                    }
                } finally {
                    heartbeatJob?.cancelAndJoin()
                    ws.cancel()
                    session = null
                }
            }
        }.also { job -> job.invokeOnCompletion { initialSession?.cancel() } }
    }

    fun subscribeToEmoteSet(emoteSetId: String) {
        if (emoteSetId.isBlank() || !subscribedSets.add(emoteSetId)) return
        sendEmoteSetSubscription(emoteSetId)
    }

    private fun sendEmoteSetSubscription(emoteSetId: String) {
        scope.launch {
            val subscribeMsg = """
                {"op":$OP_SUBSCRIBE,"d":{"type":"emote_set.update","condition":{"object_id":"$emoteSetId"}}}
            """.trimIndent()
            try {
                session?.send(Frame.Text(subscribeMsg))
                Napier.d("Subscribed to emote set: $emoteSetId", tag = TAG)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Napier.e("Failed to subscribe to emote set: ${e.message}", tag = TAG)
            }
        }
    }


    fun subscribeToTwitchChannel(twitchChannelId: String) {
        if (twitchChannelId.isBlank() || !subscribedTwitchChannels.add(twitchChannelId)) return
        sendTwitchChannelSubscriptions(twitchChannelId)
    }

    private fun sendTwitchChannelSubscriptions(twitchChannelId: String) {
        scope.launch {
            val types = listOf("entitlement.create", "entitlement.delete", "cosmetic.create")
            for (type in types) {
                val msg = """
                    {"op":$OP_SUBSCRIBE,"d":{"type":"$type","condition":{"ctx":"channel","platform":"TWITCH","id":"$twitchChannelId"}}}
                """.trimIndent()
                try {
                    session?.send(Frame.Text(msg))
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Napier.e(
                        "Failed to subscribe $type for channel $twitchChannelId: ${e.message}",
                        tag = TAG
                    )
                }
            }
            Napier.d("Subscribed to channel entitlements: $twitchChannelId", tag = TAG)
        }
    }

    private fun handleMessage(text: String) {
        try {
            val message = json.decodeFromString<SevenTvEventMessage>(text)
            when (message.op) {
                OP_HELLO -> {
                    val interval = (message.d?.heartbeatInterval ?: 30000).coerceIn(1_000, 300_000)
                    heartbeatInterval.value = interval
                    startHeartbeat(interval)
                    Napier.d("7TV EventAPI hello, heartbeat interval: ${interval}ms", tag = TAG)

                    subscribedSets.snapshot().forEach(::sendEmoteSetSubscription)
                    subscribedTwitchChannels.snapshot().forEach(::sendTwitchChannelSubscriptions)
                }

                OP_DISPATCH -> handleDispatch(message)
                OP_HEARTBEAT -> {}
            }
        } catch (e: Exception) {
            Napier.w("Failed to parse 7TV event: ${e.message}", tag = TAG)
        }
    }

    private fun handleDispatch(message: SevenTvEventMessage) {
        val data = message.d ?: return
        val body = data.body ?: return

        when (data.type) {
            "entitlement.create" -> {
                handleEntitlement(body, granted = true)
                return
            }

            "entitlement.delete" -> {
                handleEntitlement(body, granted = false)
                return
            }

            "cosmetic.create" -> {
                return
            }
        }

        val emoteSetId = data.condition["object_id"] ?: body.id
        val actorName = body.actor?.displayName ?: body.actor?.username ?: "Unknown"

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

    private fun handleEntitlement(body: SevenTvEventBody, granted: Boolean) {
        val obj = body.obj ?: return
        if (!obj.kind.equals("EMOTE_SET", ignoreCase = true)) return
        val emoteSetId = obj.refId.ifBlank { return }
        val twitchConnections = obj.user?.connections.orEmpty()
            .filter { it.platform.equals("TWITCH", ignoreCase = true) }
        if (twitchConnections.isEmpty()) return

        for (conn in twitchConnections) {
            val twitchId = conn.id.ifBlank { continue }
            scope.launch {
                _emoteSetUpdates.emit(
                    if (granted)
                        EmoteSetUpdateEvent.PersonalEmoteSetGranted(
                            twitchUserId = twitchId,
                            twitchUsername = conn.username,
                            emoteSetId = emoteSetId
                        )
                    else
                        EmoteSetUpdateEvent.PersonalEmoteSetRevoked(
                            twitchUserId = twitchId,
                            emoteSetId = emoteSetId
                        )
                )
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
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Napier.w("7TV heartbeat failed: ${e::class.simpleName}", tag = TAG)
                    session?.cancel()
                    break
                }
            }
        }
    }

    suspend fun disconnect() = lifecycleMutex.withLock {
        connectionJob?.cancelAndJoin()
        connectionJob = null
        heartbeatJob?.cancelAndJoin()
        session?.cancel()
        session = null
        subscribedSets.clear()
        subscribedTwitchChannels.clear()
    }
}

private class SevenTvHeartbeatTimeout : Exception()
