package io.rudione.chatone.data.repository

import io.rudione.chatone.data.remote.TwitchApiClient
import io.rudione.chatone.data.remote.TwitchAutoModSocket
import io.rudione.chatone.domain.model.IrcEvent
import io.rudione.chatone.domain.moderation.AutoModQueue
import io.rudione.chatone.util.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Clock

class AutoModHub(
    private val socket: TwitchAutoModSocket,
    private val apiClient: TwitchApiClient,
    private val sessions: ViewerSessionProvider,
    private val scope: CoroutineScope,
    private val now: () -> Long = { Clock.System.now().toEpochMilliseconds() }
) {
    private val queue = AutoModQueue()
    private val queueLock = Mutex()
    private val startLock = Mutex()
    private var runningFor: Pair<String, String>? = null

    private val _pending = MutableStateFlow<Map<String, List<IrcEvent.AutoModHeld>>>(emptyMap())
    val pending: StateFlow<Map<String, List<IrcEvent.AutoModHeld>>> = _pending.asStateFlow()

    val events: SharedFlow<IrcEvent> = socket.events

    init {
        scope.launch { socket.events.collect(::track) }
        scope.launch {
            sessions.activeAccountId.drop(1).collect {
                stop()
                start()
            }
        }
    }

    fun ensureRunning() {
        scope.launch { start() }
    }

    fun watch(channelId: String) {
        socket.watch(channelId)
    }

    fun pendingFor(channelId: String): List<IrcEvent.AutoModHeld> = _pending.value[channelId].orEmpty()

    suspend fun isResolved(msgId: String): Boolean = queueLock.withLock { queue.isResolved(msgId) }

    suspend fun resolve(msgId: String, allow: Boolean): Result<Unit> {
        val session = sessions.current() ?: return Result.Error(IllegalStateException("No account"))
        return apiClient.manageAutoModMessage(session.helixToken, session.userId, msgId, if (allow) ALLOW else DENY)
    }

    private suspend fun start() = startLock.withLock {
        val session = sessions.current() ?: return@withLock
        val identity = session.userId to session.helixToken
        if (runningFor == identity) return@withLock
        runningFor = identity
        val moderated = (apiClient.getModeratedChannels(session.helixToken, session.userId) as? Result.Success)?.data.orEmpty()
        socket.connect(session.helixToken, session.userId, moderated + session.userId)
    }

    private suspend fun stop() {
        startLock.withLock { runningFor = null }
        socket.disconnect()
        queueLock.withLock { queue.clear() }
        _pending.value = emptyMap()
    }

    private suspend fun track(event: IrcEvent) {
        queueLock.withLock {
            when (event) {
                is IrcEvent.AutoModHeld -> queue.onHeld(event)
                is IrcEvent.AutoModResolved -> queue.onResolved(event)
                else -> return
            }
            _pending.value = queue.snapshot(now())
        }
    }

    private companion object {
        const val ALLOW = "ALLOW"
        const val DENY = "DENY"
    }
}
