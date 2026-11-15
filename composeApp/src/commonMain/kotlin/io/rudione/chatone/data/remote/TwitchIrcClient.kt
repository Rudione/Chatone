package io.rudione.chatone.data.remote

import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocketSession
import io.rudione.chatone.domain.model.ChatMessage
import io.rudione.chatone.domain.model.IrcEvent
import io.rudione.chatone.util.chat.IrcMessage
import io.rudione.chatone.util.chat.IrcMessageParser
import io.rudione.chatone.util.log.HotPathLog
import io.rudione.chatone.util.network.NetworkMonitor
import io.rudione.chatone.util.network.isNetworkFailure
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.onTimeout
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.atomicfu.atomic
import kotlin.random.Random
import kotlin.time.Clock
import kotlinx.coroutines.ExperimentalCoroutinesApi

class TwitchIrcClient internal constructor(
    private val scope: CoroutineScope,
    private val networkMonitor: NetworkMonitor,
    private val openSocket: suspend () -> IrcSocket,
    private val now: () -> Long = { Clock.System.now().toEpochMilliseconds() },
    private val jitter: () -> Long = { Random.nextLong(0, 500) },
    private val joinRateLimiter: IrcJoinRateLimiter = IrcJoinRateLimiter(now)
) {
    constructor(httpClient: HttpClient, scope: CoroutineScope, networkMonitor: NetworkMonitor = NetworkMonitor(scope)) :
        this(scope, networkMonitor, { KtorIrcSocket(httpClient.webSocketSession("wss://irc-ws.chat.twitch.tv:443")) }, joinRateLimiter = sharedJoinRateLimiter)

    private data class PendingPing(val token: String, val response: CompletableDeferred<Unit>)

    private data class Credentials(val username: String, val token: String) {
        val anonymous: Boolean get() = username.startsWith("justinfan") || token.isBlank()
    }

    private val lifecycleMutex = Mutex()
    private val lifecycleVersion = atomic(0L)
    private val channelLock = SynchronizedObject()
    private val joinedChannels = mutableSetOf<String>()
    private val reconnectRequests = Channel<Unit>(Channel.CONFLATED)
    private val activeSocket = MutableStateFlow<IrcSocket?>(null)
    private val confirmedChannels = mutableSetOf<String>()
    private var priorityChannel: String? = null
    private val joinRequests = Channel<Unit>(Channel.CONFLATED)
    private var connectionJob: Job? = null
    private val credentials = MutableStateFlow<Credentials?>(null)

    private val _messages = MutableSharedFlow<ChatMessage>(extraBufferCapacity = 1024, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val messages: SharedFlow<ChatMessage> = _messages.asSharedFlow()
    private val _events = MutableSharedFlow<IrcEvent>(extraBufferCapacity = 512, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val events: SharedFlow<IrcEvent> = _events.asSharedFlow()
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    enum class FailureReason { NETWORK, TIMEOUT, AUTHENTICATION, SERVER }

    sealed class ConnectionState {
        data object Disconnected : ConnectionState()
        data object Connecting : ConnectionState()
        data object Connected : ConnectionState()
        data object WaitingForNetwork : ConnectionState()
        data class Reconnecting(val reason: FailureReason, val attempt: Int) : ConnectionState()
        data class Error(val message: String, val reason: FailureReason = FailureReason.SERVER) : ConnectionState()
    }

    fun connect(username: String, oauthToken: String) {
        val requested = Credentials(username.lowercase(), oauthToken.removePrefix("oauth:"))
        val version = lifecycleVersion.incrementAndGet()
        scope.launch {
            lifecycleMutex.withLock {
                if (version != lifecycleVersion.value) return@withLock
                if (credentials.value == requested && connectionJob?.isActive == true) return@withLock
                connectionJob?.cancelAndJoin()
                if (version != lifecycleVersion.value) return@withLock
                credentials.value = requested
                connectionJob = scope.launch { connectionLoop(requested) }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun connectionLoop(account: Credentials) = coroutineScope {
        val networkObserver = launch {
            var previous = networkMonitor.status.value
            networkMonitor.status.collect { current ->
                if (current != previous && (previous.available != null || current.available == false || current.wakeVersion != previous.wakeVersion)) {
                    reconnectRequests.trySend(Unit)
                }
                previous = current
            }
        }
        var attempt = 0
        try {
            while (isActive) {
                if (networkMonitor.status.value.available == false) {
                    _connectionState.value = ConnectionState.WaitingForNetwork
                    networkMonitor.status.first { it.available != false }
                    attempt = 0
                }
                while (reconnectRequests.tryReceive().isSuccess) { }
                _connectionState.value = ConnectionState.Connecting
                var connectedAt: Long? = null
                try {
                    supervisorScope {
                        val sessionJob = async { runSession(account) { connectedAt = now() } }
                        try {
                            select<Unit> {
                                sessionJob.onAwait { }
                                reconnectRequests.onReceive { }
                            }
                        } finally {
                            sessionJob.cancelAndJoin()
                        }
                    }
                    attempt = 0
                    continue
                } catch (e: IrcAuthenticationException) {
                    _connectionState.value = ConnectionState.Error("Authentication failed", FailureReason.AUTHENTICATION)
                    reconnectRequests.receive()
                    attempt = 0
                    continue
                } catch (e: TimeoutCancellationException) {
                    _connectionState.value = ConnectionState.Reconnecting(FailureReason.TIMEOUT, attempt + 1)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    val reason = when {
                        e is IrcResponseTimeoutException -> FailureReason.TIMEOUT
                        isNetworkFailure(e) -> FailureReason.NETWORK
                        else -> FailureReason.SERVER
                    }
                    _connectionState.value = ConnectionState.Reconnecting(reason, attempt + 1)
                    Napier.w("IRC connection interrupted: ${e::class.simpleName}", tag = TAG)
                }
                if (connectedAt?.let { now() - it >= 30_000 } == true) attempt = 0
                val waitMs = (1_000L shl attempt.coerceAtMost(5)).coerceAtMost(30_000) + jitter()
                attempt = (attempt + 1).coerceAtMost(6)
                if (networkMonitor.status.value.available != false) select<Unit> {
                    onTimeout(waitMs) { }
                    reconnectRequests.onReceive { attempt = 0 }
                }
            }
        } finally {
            networkObserver.cancel()
            activeSocket.value = null
            _connectionState.value = ConnectionState.Disconnected
        }
    }

    private suspend fun runSession(account: Credentials, onConnected: () -> Unit) {
        val socket = withTimeout(15_000) { openSocket() }
        synchronized(channelLock) { confirmedChannels.clear() }
        try {
            coroutineScope {
                val authenticated = CompletableDeferred<Unit>()
                val pendingPing = MutableStateFlow<PendingPing?>(null)
                val verifiedAt = atomic(now())
                val username = if (account.anonymous) "justinfan${Random.nextInt(10000, 99999)}" else account.username
                launch {
                    while (isActive) {
                        val text = socket.receive() ?: throw IrcConnectionClosedException()
                        handleIncomingMessage(text, socket, username, authenticated) { token ->
                            pendingPing.value?.takeIf { it.token == token }?.let {
                                verifiedAt.value = now()
                                it.response.complete(Unit)
                            }
                        }
                    }
                }
                socket.send("CAP REQ :twitch.tv/tags twitch.tv/commands twitch.tv/membership")
                socket.send(if (account.anonymous) "PASS NaM" else "PASS oauth:${account.token}")
                socket.send("NICK $username")
                withTimeout(15_000) { authenticated.await() }
                verifiedAt.value = now()
                activeSocket.value = socket
                _connectionState.value = ConnectionState.Connected
                onConnected()
                launch { rejoinChannels(socket, account.username) }
                launch {
                    while (isActive) {
                        delay(30_000)
                        if (now() - verifiedAt.value > 60_000) throw IrcResponseTimeoutException()
                        val ping = PendingPing("chatone-${now()}", CompletableDeferred())
                        pendingPing.value = ping
                        socket.send("PING :${ping.token}")
                        withTimeoutOrNull(10_000) { ping.response.await(); true } ?: throw IrcResponseTimeoutException()
                        pendingPing.value = null
                    }
                }
                awaitCancellation()
            }
        } finally {
            activeSocket.compareAndSet(socket, null)
            synchronized(channelLock) { confirmedChannels.clear() }
            socket.cancel()
        }
    }

    private suspend fun handleIncomingMessage(text: String, socket: IrcSocket, username: String, authenticated: CompletableDeferred<Unit>, onPong: (String) -> Unit) {
        text.split("\r\n").filter { it.isNotBlank() }.forEach { line ->
            HotPathLog.verbose(TAG) { "IRC: $line" }

            if (line.startsWith("PING")) {
                val server = line.substringAfter("PING ")
                socket.send("PONG $server")
                return@forEach
            }

            val ircMsg = IrcMessage.parse(line) ?: return@forEach

            when (ircMsg.command) {
                "001" -> authenticated.complete(Unit)
                "PONG" -> onPong(ircMsg.trailing)
                "464" -> throw IrcAuthenticationException()

                "PRIVMSG" -> {
                    try {
                        val message = IrcMessageParser.parsePrivMsg(ircMsg)
                        _messages.emit(message)
                        _events.emit(IrcEvent.PrivMsg(message))
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Napier.e("Failed to parse PRIVMSG: ${e.message}", e, tag = TAG)
                    }
                }

                "NOTICE" -> {
                    val channel = ircMsg.channel
                    val msg = ircMsg.trailing
                    val msgId = ircMsg.tags["msg-id"]
                    if (msg.contains("authentication failed", ignoreCase = true) ||
                        msg.contains("improperly formatted auth", ignoreCase = true) ||
                        msg.contains("login unsuccessful", ignoreCase = true)) throw IrcAuthenticationException()
                    _events.emit(IrcEvent.Notice(channel, msg, msgId))
                    Napier.d("NOTICE #$channel: $msg", tag = TAG)
                }

                "USERNOTICE" -> {
                    val channel = ircMsg.channel
                    val systemMsg = ircMsg.tags["system-msg"]?.replace("\\s", " ") ?: ""
                    val userMsg = if (ircMsg.params.size > 1) {
                        try {
                            IrcMessageParser.parsePrivMsg(ircMsg)
                        } catch (e: Exception) {
                            null
                        }
                    } else null
                    val msgId = ircMsg.tags["msg-id"]
                    _events.emit(
                        IrcEvent.UserNotice(
                            channel,
                            systemMsg,
                            userMsg,
                            msgId,
                            ircMsg.tags
                        )
                    )
                    Napier.d("USERNOTICE #$channel: $systemMsg", tag = TAG)
                }

                "CLEARCHAT" -> {
                    val channel = ircMsg.channel
                    val targetUser = if (ircMsg.params.size > 1) ircMsg.trailing else null
                    val banDuration = ircMsg.tags["ban-duration"]?.toIntOrNull()
                    _events.emit(IrcEvent.ClearChat(channel, targetUser, banDuration, ircMsg.tags))
                    Napier.d("CLEARCHAT #$channel: ${targetUser ?: "all"}", tag = TAG)
                }

                "CLEARMSG" -> {
                    val channel = ircMsg.channel
                    val targetMsgId = ircMsg.tags["target-msg-id"] ?: ""
                    val login = ircMsg.tags["login"] ?: ""
                    val msg = ircMsg.trailing
                    _events.emit(IrcEvent.ClearMsg(channel, targetMsgId, msg, login))
                    Napier.d("CLEARMSG #$channel: $targetMsgId", tag = TAG)
                }

                "ROOMSTATE" -> {
                    val channel = ircMsg.channel
                    synchronized(channelLock) { confirmedChannels.add(channel) }
                    val tags = ircMsg.tags
                    _events.emit(
                        IrcEvent.RoomState(
                            channel = channel,
                            emoteOnly = tags["emote-only"]?.let { it == "1" },
                            followersOnly = tags["followers-only"]?.toIntOrNull(),
                            slowMode = tags["slow"]?.toIntOrNull(),
                            subsOnly = tags["subs-only"]?.let { it == "1" },
                            r9k = tags["r9k"]?.let { it == "1" },
                            roomId = tags["room-id"]
                        )
                    )
                }

                "USERSTATE" -> {
                    val channel = ircMsg.channel
                    val tags = ircMsg.tags
                    _events.emit(
                        IrcEvent.UserState(
                            channel = channel,
                            badgeInfo = tags["badge-info"] ?: "",
                            badges = tags["badges"] ?: "",
                            color = tags["color"]?.takeIf { it.isNotEmpty() },
                            displayName = tags["display-name"] ?: username,
                            emoteSets = tags["emote-sets"]?.split(",") ?: emptyList(),
                            isMod = tags["mod"] == "1"
                        )
                    )
                }

                "GLOBALUSERSTATE" -> {
                    val tags = ircMsg.tags
                    _events.emit(
                        IrcEvent.GlobalUserState(
                            badgeInfo = tags["badge-info"] ?: "",
                            badges = tags["badges"] ?: "",
                            color = tags["color"]?.takeIf { it.isNotEmpty() },
                            displayName = tags["display-name"] ?: username,
                            emoteSets = tags["emote-sets"]?.split(",") ?: emptyList(),
                            userId = tags["user-id"] ?: ""
                        )
                    )
                }

                "WHISPER" -> {
                    val tags = ircMsg.tags
                    _events.emit(
                        IrcEvent.Whisper(
                            fromUser = ircMsg.nick,
                            displayName = tags["display-name"] ?: ircMsg.nick,
                            message = ircMsg.trailing,
                            color = tags["color"]?.takeIf { it.isNotEmpty() },
                            badges = tags["badges"] ?: "",
                            userId = tags["user-id"] ?: ""
                        )
                    )
                }

                "RECONNECT" -> {
                    Napier.d("Server requested RECONNECT", tag = TAG)
                    _events.emit(IrcEvent.Reconnect())
                    throw IrcServerReconnectException()
                }

                "JOIN" -> {
                    val channel = ircMsg.channel
                    val who = ircMsg.nick
                    if (who.isNotEmpty() && !who.equals(username, ignoreCase = true)) {
                        _events.emit(IrcEvent.UserJoin(channel, who))
                    } else {
                        synchronized(channelLock) { confirmedChannels.add(channel) }
                        _events.emit(IrcEvent.Connected(channel))
                        Napier.d("Joined #$channel", tag = TAG)
                    }
                }

                "PART" -> {
                    val who = ircMsg.nick
                    if (who.isNotEmpty() && !who.equals(username, ignoreCase = true)) {
                        _events.emit(IrcEvent.UserPart(ircMsg.channel, who))
                        HotPathLog.verbose(TAG) { "$who left #${ircMsg.channel}" }
                    } else {
                        Napier.d("Left #${ircMsg.channel}", tag = TAG)
                    }
                }

                "002", "003", "004", "353", "366", "372", "375", "376", "CAP" -> {

                }

                else -> {
                    Napier.v("Unhandled IRC command: ${ircMsg.command}", tag = TAG)
                }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun rejoinChannels(socket: IrcSocket, login: String) {
        val attempts = mutableMapOf<String, Long>()
        fun nextChannel(): String? = synchronized(channelLock) {
            fun pending(channel: String) = channel in joinedChannels && channel !in confirmedChannels &&
                (attempts[channel]?.let { now() - it >= 15_000L } != false)
            priorityChannel?.takeIf(::pending)
                ?: joinedChannels.firstOrNull { it !in confirmedChannels && it !in attempts }
                ?: joinedChannels.firstOrNull(::pending)
        }
        while (currentCoroutineContext().isActive) {
            if (nextChannel() == null) {
                select<Unit> {
                    joinRequests.onReceive { }
                    onTimeout(1_000L) { }
                }
            } else {
                joinRateLimiter.awaitTurn(login)
                val channel = nextChannel() ?: continue
                socket.send("JOIN #$channel")
                attempts[channel] = now()
            }
        }
    }

    suspend fun joinChannel(channelName: String) {
        val channel = channelName.lowercase().removePrefix("#")
        if (!CHANNEL_PATTERN.matches(channel)) return
        synchronized(channelLock) {
            joinedChannels.add(channel)
            priorityChannel = channel
        }
        joinRequests.trySend(Unit)
    }

    suspend fun partChannel(channelName: String) {
        val channel = channelName.lowercase().removePrefix("#")
        if (!synchronized(channelLock) { confirmedChannels.remove(channel); joinedChannels.remove(channel) }) return
        activeSocket.value?.let { send(it, "PART #$channel") }
    }

    suspend fun sendMessage(channelName: String, message: String) {
        if (credentials.value?.anonymous != false) {
            _events.emit(IrcEvent.Error("Cannot send messages as guest"))
            return
        }
        val channel = stripIrcFraming(channelName).lowercase().removePrefix("#")
        val body = stripIrcFraming(message)
        if (!CHANNEL_PATTERN.matches(channel) || body.isEmpty()) return
        send(requireNotNull(activeSocket.value) { "Chat is disconnected" }, "PRIVMSG #$channel :$body")
    }

    suspend fun sendRawCommand(command: String) {
        if (credentials.value?.anonymous != false) return
        val safeCommand = stripIrcFraming(command)
        if (safeCommand.isEmpty()) return
        send(requireNotNull(activeSocket.value) { "Chat is disconnected" }, safeCommand)
    }

    private suspend fun send(socket: IrcSocket, command: String) {
        try {
            socket.send(command)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            reconnectRequests.trySend(Unit)
            throw e
        }
    }

    private fun stripIrcFraming(value: String): String = value.filterNot { it == '\r' || it == '\n' || it == '\u0000' }

    fun reconnect() {
        reconnectRequests.trySend(Unit)
    }

    fun reconnectIfFailed() {
        if (_connectionState.value !is ConnectionState.Connected) reconnect()
    }

    suspend fun disconnect() {
        val version = lifecycleVersion.incrementAndGet()
        lifecycleMutex.withLock {
            if (version != lifecycleVersion.value) return@withLock
            credentials.value = null
            connectionJob?.cancelAndJoin()
            connectionJob = null
            synchronized(channelLock) { joinedChannels.clear(); confirmedChannels.clear(); priorityChannel = null }
            _connectionState.value = ConnectionState.Disconnected
        }
    }

    private companion object {
        const val TAG = "TwitchIrcClient"
        val CHANNEL_PATTERN = Regex("^[a-z0-9_]{1,25}$")
        val sharedJoinRateLimiter = IrcJoinRateLimiter()
    }
}

private class IrcAuthenticationException : Exception()
private class IrcServerReconnectException : Exception()
private class IrcConnectionClosedException : Exception()

private class IrcResponseTimeoutException : Exception()
