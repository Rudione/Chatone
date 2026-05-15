package io.rudione.chatone.data.remote

import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.close
import io.ktor.websocket.readText
import io.rudione.chatone.domain.model.ChatMessage
import io.rudione.chatone.domain.model.IrcEvent
import io.rudione.chatone.util.IrcMessage
import io.rudione.chatone.util.IrcMessageParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

class TwitchIrcClient(
    private val httpClient: HttpClient,
    private val scope: CoroutineScope
) {
    private val wssUrl = "wss://irc-ws.chat.twitch.tv:443"

    private var session: WebSocketSession? = null
    private var pingJob: Job? = null
    private var reconnectJob: Job? = null
    private var connectionJob: Job? = null

    private val _messages = MutableSharedFlow<ChatMessage>(
        extraBufferCapacity = 1024,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val messages: SharedFlow<ChatMessage> = _messages.asSharedFlow()

    private val _events = MutableSharedFlow<IrcEvent>(
        extraBufferCapacity = 512,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events: SharedFlow<IrcEvent> = _events.asSharedFlow()

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val joinedChannels = mutableSetOf<String>()
    private var currentUsername: String = ""
    private var currentToken: String = ""
    private var isAnonymous: Boolean = false

    private var reconnectAttempts = 0
    private val maxReconnectAttempts = 5
    private val baseReconnectDelay = 1000L
    private val maxJitter = 250L

    companion object {
        private const val TAG = "TwitchIrcClient"
        private const val JOIN_CHUNK_SIZE = 5
        private const val JOIN_CHUNK_DELAY = 600L
        private const val PING_INTERVAL = 300_000L
    }

    sealed class ConnectionState {
        object Disconnected : ConnectionState()
        object Connecting : ConnectionState()
        object Connected : ConnectionState()
        data class Error(val message: String) : ConnectionState()
    }

    fun connect(username: String, oauthToken: String) {
        if (_connectionState.value is ConnectionState.Connected) {
            Napier.d("Already connected", tag = TAG)
            return
        }

        currentUsername = username.lowercase()
        currentToken = oauthToken
        isAnonymous = username.startsWith("justinfan") || oauthToken.isBlank()

        connectionJob?.cancel()
        connectionJob = scope.launch {
            connectInternal()
        }
    }

    private suspend fun connectInternal() {
        _connectionState.value = ConnectionState.Connecting

        try {
            httpClient.webSocket(wssUrl) {
                session = this
                Napier.d("WebSocket connected", tag = TAG)


                send(Frame.Text("CAP REQ :twitch.tv/tags twitch.tv/commands twitch.tv/membership"))

                if (isAnonymous) {
                    send(Frame.Text("PASS NaM"))
                    send(Frame.Text("NICK justinfan${Random.nextInt(10000, 99999)}"))
                } else {
                    send(Frame.Text("PASS oauth:$currentToken"))
                    send(Frame.Text("NICK $currentUsername"))
                }

                _connectionState.value = ConnectionState.Connected
                reconnectAttempts = 0

                startPingJob()


                if (joinedChannels.isNotEmpty()) {
                    val channels = joinedChannels.toList()
                    rejoinChannels(channels)
                }


                for (frame in incoming) {
                    if (frame is Frame.Text) {
                        val text = frame.readText()
                        handleIncomingMessage(text)
                    }
                }
            }
        } catch (e: Exception) {
            Napier.e("WebSocket error: ${e.message}", e, tag = TAG)
            _connectionState.value = ConnectionState.Error(e.message ?: "Unknown error")
            handleReconnect()
        } finally {
            session = null
            pingJob?.cancelAndJoin()
            if (_connectionState.value !is ConnectionState.Error) {
                _connectionState.value = ConnectionState.Disconnected
            }
        }
    }

    private suspend fun handleIncomingMessage(text: String) {
        text.split("\r\n").filter { it.isNotBlank() }.forEach { line ->
            Napier.v("IRC: $line", tag = TAG)


            if (line.startsWith("PING")) {
                val server = line.substringAfter("PING ")
                session?.send(Frame.Text("PONG $server"))
                return@forEach
            }

            val ircMsg = IrcMessage.parse(line) ?: return@forEach

            when (ircMsg.command) {
                "PRIVMSG" -> {
                    try {
                        val message = IrcMessageParser.parsePrivMsg(ircMsg)
                        _messages.emit(message)
                        _events.emit(IrcEvent.PrivMsg(message))
                    } catch (e: Exception) {
                        Napier.e("Failed to parse PRIVMSG: ${e.message}", e, tag = TAG)
                    }
                }

                "NOTICE" -> {
                    val channel = ircMsg.channel
                    val msg = ircMsg.trailing
                    val msgId = ircMsg.tags["msg-id"]
                    _events.emit(IrcEvent.Notice(channel, msg, msgId))
                    Napier.d("NOTICE #$channel: $msg", tag = TAG)
                }

                "USERNOTICE" -> {
                    val channel = ircMsg.channel
                    val systemMsg = ircMsg.tags["system-msg"]?.replace("\\s", " ") ?: ""
                    val userMsg = if (ircMsg.params.size > 1) {
                        try {
                            IrcMessageParser.parsePrivMsg(ircMsg)
                        } catch (e: Exception) { null }
                    } else null
                    val msgId = ircMsg.tags["msg-id"]
                    _events.emit(IrcEvent.UserNotice(channel, systemMsg, userMsg, msgId, ircMsg.tags))
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
                            displayName = tags["display-name"] ?: currentUsername,
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
                            displayName = tags["display-name"] ?: currentUsername,
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
                    session?.close()
                    handleReconnect()
                }

                "JOIN" -> {
                    val channel = ircMsg.channel
                    _events.emit(IrcEvent.Connected(channel))
                    Napier.d("Joined #$channel", tag = TAG)
                }

                "PART" -> {
                    Napier.d("Left #${ircMsg.channel}", tag = TAG)
                }

                "001", "002", "003", "004", "353", "366", "372", "375", "376", "CAP" -> {

                }

                else -> {
                    Napier.v("Unhandled IRC command: ${ircMsg.command}", tag = TAG)
                }
            }
        }
    }

    private suspend fun rejoinChannels(channels: List<String>) {
        channels.chunked(JOIN_CHUNK_SIZE).forEachIndexed { index, chunk ->
            if (index > 0) delay(JOIN_CHUNK_DELAY)
            chunk.forEach { channel ->
                session?.send(Frame.Text("JOIN #$channel"))
            }
        }
    }

    suspend fun joinChannel(channelName: String) {
        val channel = channelName.lowercase().removePrefix("#")
        if (joinedChannels.contains(channel)) {
            Napier.d("Already in channel: $channel", tag = TAG)
            return
        }

        session?.send(Frame.Text("JOIN #$channel"))
        joinedChannels.add(channel)
        Napier.d("Joining channel: $channel", tag = TAG)
    }

    suspend fun partChannel(channelName: String) {
        val channel = channelName.lowercase().removePrefix("#")
        session?.send(Frame.Text("PART #$channel"))
        joinedChannels.remove(channel)
        Napier.d("Leaving channel: $channel", tag = TAG)
    }

    suspend fun sendMessage(channelName: String, message: String) {
        if (isAnonymous) {
            _events.emit(IrcEvent.Error("Cannot send messages as guest"))
            return
        }
        val channel = channelName.lowercase().removePrefix("#")
        session?.send(Frame.Text("PRIVMSG #$channel :$message"))
        Napier.d("Sent message to #$channel: $message", tag = TAG)
    }

    suspend fun sendRawCommand(command: String) {
        if (isAnonymous) return
        session?.send(Frame.Text(command))
        Napier.v("Sent raw IRC command: $command", tag = TAG)
    }

    suspend fun disconnect() {
        reconnectJob?.cancelAndJoin()
        connectionJob?.cancelAndJoin()
        pingJob?.cancelAndJoin()
        session?.close()
        session = null
        joinedChannels.clear()
        _connectionState.value = ConnectionState.Disconnected
        Napier.d("Disconnected", tag = TAG)
    }

    private fun startPingJob() {
        pingJob?.cancel()
        pingJob = scope.launch {
            while (isActive && session != null) {
                delay(PING_INTERVAL - Random.nextLong(0, maxJitter))
                try {
                    session?.send(Frame.Text("PING :tmi.twitch.tv"))
                } catch (e: Exception) {
                    Napier.e("Failed to send ping: ${e.message}", e, tag = TAG)
                    break
                }
            }
        }
    }

    private fun handleReconnect() {
        if (reconnectAttempts >= maxReconnectAttempts) {
            Napier.e("Max reconnect attempts reached", tag = TAG)
            _connectionState.value = ConnectionState.Error("Max reconnect attempts reached")
            return
        }

        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            reconnectAttempts++
            val delay = baseReconnectDelay * (1L shl (reconnectAttempts - 1)) + Random.nextLong(0, maxJitter)
            Napier.d("Reconnecting in ${delay}ms (attempt $reconnectAttempts/$maxReconnectAttempts)", tag = TAG)
            delay(delay)

            if (currentUsername.isNotEmpty()) {
                connectInternal()
            }
        }
    }
}