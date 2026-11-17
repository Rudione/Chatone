package io.rudione.chatone.data.remote.emote

import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.readText
import io.rudione.chatone.util.network.NetworkMonitor
import io.rudione.chatone.util.network.recoverSocket
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import kotlin.time.Duration.Companion.milliseconds

class SevenTvEventApi(
    private val httpClient: HttpClient,
    private val scope: CoroutineScope,
    private val networkMonitor: NetworkMonitor = NetworkMonitor(scope)
) : SevenTvEventSource {

    private val parser = SevenTvEventParser()
    private var session: WebSocketSession? = null
    private var connectionJob: Job? = null
    private val lifecycleMutex = Mutex()
    private val subscriptions = SevenTvSubscriptionBook()

    private val _dispatches = MutableSharedFlow<List<SevenTvLiveEvent>>(
        extraBufferCapacity = 256,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    override val dispatches: SharedFlow<List<SevenTvLiveEvent>> = _dispatches.asSharedFlow()

    suspend fun connect() = lifecycleMutex.withLock {
        if (connectionJob?.isActive == true) return@withLock
        var initialSession = if (networkMonitor.status.value.available == false) null else try {
            withTimeout(CONNECT_TIMEOUT_MS.milliseconds) { httpClient.webSocketSession(EVENT_API_URL) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Napier.w("7TV connection unavailable: ${e::class.simpleName}", tag = TAG)
            null
        }
        connectionJob = scope.launch {
            networkMonitor.recoverSocket(onFailure = {
                Napier.w("7TV connection interrupted: ${it.message ?: it::class.simpleName}", tag = TAG)
            }) {
                val ws = initialSession
                    ?: withTimeout(CONNECT_TIMEOUT_MS.milliseconds) { httpClient.webSocketSession(EVENT_API_URL) }
                initialSession = null
                session = ws
                try {
                    runSession(ws)
                } finally {
                    subscriptions.endSession()
                    if (session === ws) session = null
                    ws.cancel()
                }
            }
        }.also { job -> job.invokeOnCompletion { initialSession?.cancel() } }
    }

    private suspend fun runSession(ws: WebSocketSession) {
        var silenceLimitMs = HELLO_TIMEOUT_MS
        while (true) {
            currentCoroutineContext().ensureActive()
            val received = withTimeoutOrNull(silenceLimitMs.milliseconds) { ws.incoming.receiveCatching() }
                ?: throw SevenTvSilenceTimeout(silenceLimitMs)
            val frame = received.getOrNull() ?: throw SevenTvSocketClosed()
            if (frame !is Frame.Text) continue
            when (val parsed = parser.parse(frame.readText())) {
                is SevenTvFrame.Hello -> {
                    silenceLimitMs = parsed.heartbeatIntervalMs * MISSED_HEARTBEATS
                    val commands = subscriptions.startSession(parsed.subscriptionLimit)
                    Napier.d("7TV EventAPI hello, heartbeat ${parsed.heartbeatIntervalMs}ms, ${commands.size} subscriptions", tag = TAG)
                    commands.forEach { ws.send(Frame.Text(it)) }
                }
                is SevenTvFrame.Dispatch -> if (parsed.events.isNotEmpty()) _dispatches.tryEmit(parsed.events)
                SevenTvFrame.Reconnect -> {
                    Napier.d("7TV asked to reconnect", tag = TAG)
                    return
                }
                is SevenTvFrame.EndOfStream -> throw SevenTvEndOfStream(parsed.code, parsed.reason)
                is SevenTvFrame.Failure -> Napier.w("7TV error: ${parsed.reason}", tag = TAG)
                SevenTvFrame.Heartbeat, SevenTvFrame.Ignored -> Unit
                null -> Napier.w("Unreadable 7TV event", tag = TAG)
            }
        }
    }

    override fun subscribeToEmoteSet(emoteSetId: String) =
        send(subscriptions.add(SevenTvSubscriptionBook.Kind.CHANNEL_SET, emoteSetId))

    override fun subscribeToPersonalSet(emoteSetId: String) =
        send(subscriptions.add(SevenTvSubscriptionBook.Kind.PERSONAL_SET, emoteSetId))

    override fun subscribeToUser(sevenTvUserId: String) =
        send(subscriptions.add(SevenTvSubscriptionBook.Kind.USER, sevenTvUserId))

    override fun subscribeToTwitchChannel(twitchChannelId: String) =
        send(subscriptions.add(SevenTvSubscriptionBook.Kind.TWITCH_CHANNEL, twitchChannelId))

    private fun send(commands: List<String>) {
        if (commands.isEmpty()) return
        val ws = session ?: return
        scope.launch {
            try {
                commands.forEach { ws.send(Frame.Text(it)) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Napier.w("7TV subscribe will be retried after reconnect: ${e::class.simpleName}", tag = TAG)
            }
        }
    }

    suspend fun disconnect() = lifecycleMutex.withLock {
        connectionJob?.cancelAndJoin()
        connectionJob = null
        session?.cancel()
        session = null
        subscriptions.clear()
    }

    private companion object {
        const val TAG = "7TV-EventAPI"
        const val EVENT_API_URL = "wss://events.7tv.io/v3"
        const val CONNECT_TIMEOUT_MS = 15_000L
        const val HELLO_TIMEOUT_MS = 15_000L
        const val MISSED_HEARTBEATS = 3
    }
}

internal object SevenTvCommands {
    private const val OP_SUBSCRIBE = 35
    private val CHANNEL_EVENTS = listOf("entitlement.create", "entitlement.delete", "cosmetic.create")
    private val ID = Regex("^[A-Za-z0-9]{1,64}$")

    fun isValidId(id: String): Boolean = ID.matches(id)

    fun emoteSet(emoteSetId: String): String = byObjectId("emote_set.update", emoteSetId)

    fun user(sevenTvUserId: String): String = byObjectId("user.update", sevenTvUserId)

    fun twitchChannel(twitchChannelId: String): List<String> = CHANNEL_EVENTS.map { type ->
        subscribe(type) {
            put("ctx", "channel")
            put("platform", "TWITCH")
            put("id", twitchChannelId)
        }
    }

    private fun byObjectId(type: String, objectId: String): String = subscribe(type) { put("object_id", objectId) }

    private fun subscribe(
        type: String,
        condition: JsonObjectBuilder.() -> Unit
    ): String = buildJsonObject {
        put("op", OP_SUBSCRIBE)
        putJsonObject("d") {
            put("type", type)
            putJsonObject("condition", condition)
        }
    }.toString()
}

private class SevenTvSilenceTimeout(limitMs: Long) : Exception("no 7TV frames for ${limitMs}ms")

private class SevenTvSocketClosed : Exception("7TV socket closed")

private class SevenTvEndOfStream(code: Int?, reason: String?) : Exception("7TV ended the stream: $code $reason")
