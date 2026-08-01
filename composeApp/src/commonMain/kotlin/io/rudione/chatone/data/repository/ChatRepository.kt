package io.rudione.chatone.data.repository

import io.github.aakira.napier.Napier
import io.rudione.chatone.data.local.ChatoneDatabase
import io.rudione.chatone.data.remote.TwitchApiClient
import io.rudione.chatone.data.remote.TwitchIrcClient
import io.rudione.chatone.domain.model.Channel
import io.rudione.chatone.domain.model.ChatMessage
import io.rudione.chatone.domain.model.IrcEvent
import io.rudione.chatone.domain.model.TwitchAccount
import io.rudione.chatone.util.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlin.time.Clock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

interface ChatRepository {
    val messages: SharedFlow<ChatMessage>
    val events: SharedFlow<IrcEvent>
    val connectionState: StateFlow<TwitchIrcClient.ConnectionState>

    suspend fun connect(account: TwitchAccount)
    suspend fun connectAnonymous()
    suspend fun disconnect()
    suspend fun joinChannel(channelName: String)
    suspend fun partChannel(channelName: String)
    suspend fun sendMessage(channelName: String, message: String)
    suspend fun searchChannels(query: String, accessToken: String): Result<List<Channel>>
    suspend fun getChannelInfo(channelLogin: String, accessToken: String): Result<Channel?>
    suspend fun saveChannel(channel: Channel)
    suspend fun getChannels(): Flow<List<Channel>>
    suspend fun getFavoriteChannels(): Flow<List<Channel>>
    suspend fun updateFavorite(channelId: String, isFavorite: Boolean)
    suspend fun saveMessage(message: ChatMessage)
    suspend fun saveMessages(messages: List<ChatMessage>)
    suspend fun getLocalHistoryForChannel(channelId: String, limit: Long = 200): List<ChatMessage>
    suspend fun getMessagesForChannel(channelId: String, limit: Long): Flow<List<ChatMessage>>
    suspend fun getLocalHistoryForUser(channelId: String, userId: String, limit: Long = 500): List<ChatMessage>
}

class ChatRepositoryImpl(
    private val ircClient: TwitchIrcClient,
    private val apiClient: TwitchApiClient,
    private val database: ChatoneDatabase
) : ChatRepository {

    companion object {
        private const val TAG = "ChatRepository"
    }

    override val messages: SharedFlow<ChatMessage> = ircClient.messages
    override val events: SharedFlow<IrcEvent> = ircClient.events
    override val connectionState: StateFlow<TwitchIrcClient.ConnectionState> = ircClient.connectionState

    override suspend fun connect(account: TwitchAccount) {
        Napier.d("Connecting IRC for user: ${account.login}", tag = TAG)
        ircClient.connect(account.login, account.accessToken)
    }

    override suspend fun connectAnonymous() {
        Napier.d("Connecting IRC anonymously", tag = TAG)
        ircClient.connect("justinfan12345", "")
    }

    override suspend fun disconnect() {
        Napier.d("Disconnecting IRC", tag = TAG)
        ircClient.disconnect()
    }

    override suspend fun joinChannel(channelName: String) {
        Napier.d("Joining channel: $channelName", tag = TAG)
        ircClient.joinChannel(channelName)

        val channel = database.channelQueries.getChannelById(channelName).executeAsOneOrNull()
        if (channel != null) {
            database.channelQueries.updateLastJoined(
                lastJoinedAt = Clock.System.now().toEpochMilliseconds(),
                id = channelName
            )
        }
    }

    override suspend fun partChannel(channelName: String) {
        Napier.d("Leaving channel: $channelName", tag = TAG)
        ircClient.partChannel(channelName)
    }

    override suspend fun sendMessage(channelName: String, message: String) {
        Napier.d("Sending message to $channelName: $message", tag = TAG)
        ircClient.sendMessage(channelName, message)
    }

    override suspend fun searchChannels(query: String, accessToken: String): Result<List<Channel>> {
        return try {
            val result = apiClient.searchChannels(accessToken, query)
            if (result is Result.Success) {
                val channels = result.data.data.map { channelData ->
                    Channel(
                        id = channelData.id,
                        login = channelData.broadcasterLogin,
                        displayName = channelData.displayName,
                        profileImageUrl = channelData.thumbnailUrl,
                        description = channelData.title,
                        viewerCount = 0,
                        isLive = channelData.isLive,
                        gameName = channelData.gameName,
                        title = channelData.title
                    )
                }
                Result.Success(channels)
            } else {
                Result.Error(Exception("Failed to search channels"))
            }
        } catch (e: Exception) {
            Napier.e("Failed to search channels: ${e.message}", e, tag = TAG)
            Result.Error(e)
        }
    }

    override suspend fun getChannelInfo(channelLogin: String, accessToken: String): Result<Channel?> {
        return try {
            val userResult = apiClient.getUsers(accessToken, logins = listOf(channelLogin))
            if (userResult !is Result.Success || userResult.data.data.isEmpty()) {
                return Result.Success(null)
            }

            val userData = userResult.data.data.first()

            val streamResult = apiClient.getStreams(accessToken, userLogins = listOf(channelLogin))
            val streamData = if (streamResult is Result.Success && streamResult.data.data.isNotEmpty()) {
                streamResult.data.data.first()
            } else null

            val channel = Channel(
                id = userData.id,
                login = userData.login,
                displayName = userData.displayName,
                profileImageUrl = userData.profileImageUrl,
                description = userData.description,
                viewerCount = streamData?.viewerCount ?: 0,
                isLive = streamData != null,
                gameName = streamData?.gameName ?: "",
                title = streamData?.title ?: ""
            )

            Result.Success(channel)
        } catch (e: Exception) {
            Napier.e("Failed to get channel info: ${e.message}", e, tag = TAG)
            Result.Error(e)
        }
    }

    override suspend fun saveChannel(channel: Channel) {
        database.channelQueries.insertOrUpdateChannel(
            id = channel.id,
            login = channel.login,
            displayName = channel.displayName,
            profileImageUrl = channel.profileImageUrl,
            description = channel.description,
            viewerCount = channel.viewerCount.toLong(),
            isLive = if (channel.isLive) 1 else 0,
            gameName = channel.gameName,
            title = channel.title,
            isFavorite = 0,
            lastJoinedAt = Clock.System.now().toEpochMilliseconds()
        )
        Napier.d("Channel saved: ${channel.login}", tag = TAG)
    }

    override suspend fun getChannels(): Flow<List<Channel>> = flow {
        val channels = database.channelQueries.getAllChannels()
            .executeAsList()
            .map { entity ->
                Channel(
                    id = entity.id,
                    login = entity.login,
                    displayName = entity.displayName,
                    profileImageUrl = entity.profileImageUrl,
                    description = entity.description,
                    viewerCount = entity.viewerCount.toInt(),
                    isLive = entity.isLive == 1L,
                    gameName = entity.gameName,
                    title = entity.title
                )
            }
        emit(channels)
    }

    override suspend fun getFavoriteChannels(): Flow<List<Channel>> = flow {
        val channels = database.channelQueries.getFavoriteChannels()
            .executeAsList()
            .map { entity ->
                Channel(
                    id = entity.id,
                    login = entity.login,
                    displayName = entity.displayName,
                    profileImageUrl = entity.profileImageUrl,
                    description = entity.description,
                    viewerCount = entity.viewerCount.toInt(),
                    isLive = entity.isLive == 1L,
                    gameName = entity.gameName,
                    title = entity.title
                )
            }
        emit(channels)
    }

    override suspend fun updateFavorite(channelId: String, isFavorite: Boolean) {
        database.channelQueries.updateFavorite(
            isFavorite = if (isFavorite) 1 else 0,
            id = channelId
        )
    }

    override suspend fun saveMessage(message: ChatMessage) {
        insertMessage(message)
    }

    override suspend fun saveMessages(messages: List<ChatMessage>) {
        if (messages.isEmpty()) return
        database.messageQueries.transaction {
            messages.forEach { insertMessage(it) }
        }
    }

    override suspend fun getLocalHistoryForChannel(channelId: String, limit: Long): List<ChatMessage> {
        if (channelId.isBlank()) return emptyList()
        return database.messageQueries.getMessagesByChannel(channelId, limit)
            .executeAsList()
            .map { it.toChatMessage() }
            .reversed()
    }

    private fun insertMessage(message: ChatMessage) {
        database.messageQueries.insertMessage(
            id = message.id,
            channelId = message.channelId,
            channelName = message.channelName,
            userId = message.userId,
            username = message.username,
            displayName = message.displayName,
            message = message.message,
            timestamp = message.timestamp,
            color = message.color,
            badges = Json.encodeToString(message.badges),
            emotes = Json.encodeToString(message.emotes),
            isModerator = if (message.isModerator) 1 else 0,
            isSubscriber = if (message.isSubscriber) 1 else 0,
            isVip = if (message.isVip) 1 else 0,
            isBroadcaster = if (message.isBroadcaster) 1 else 0,
            isMention = if (message.isMention) 1 else 0,
            isAction = if (message.isAction) 1 else 0
        )
    }

    override suspend fun getMessagesForChannel(channelId: String, limit: Long): Flow<List<ChatMessage>> = flow {
        val messages = database.messageQueries.getMessagesByChannel(channelId, limit)
            .executeAsList()
            .map { it.toChatMessage() }
        emit(messages.reversed())
    }

    override suspend fun getLocalHistoryForUser(channelId: String, userId: String, limit: Long): List<ChatMessage> {
        return database.messageQueries.getMessagesByChannelAndUser(channelId, userId, limit)
            .executeAsList()
            .map { it.toChatMessage() }
            .reversed()
    }

    private fun io.rudione.chatone.data.local.MessageEntity.toChatMessage(): ChatMessage = ChatMessage(
        id = id,
        channelId = channelId,
        channelName = channelName,
        userId = userId,
        username = username,
        displayName = displayName,
        message = message,
        timestamp = timestamp,
        color = color,
        badges = try {
            Json.decodeFromString(badges)
        } catch (e: Exception) {
            emptyList()
        },
        emotes = try {
            Json.decodeFromString(emotes)
        } catch (e: Exception) {
            emptyList()
        },
        isModerator = isModerator == 1L,
        isSubscriber = isSubscriber == 1L,
        isVip = isVip == 1L,
        isBroadcaster = isBroadcaster == 1L,
        isMention = isMention == 1L,
        isAction = isAction == 1L
    )
}
