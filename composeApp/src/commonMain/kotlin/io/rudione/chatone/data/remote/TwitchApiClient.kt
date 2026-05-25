package io.rudione.chatone.data.remote

import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.delete
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.rudione.chatone.data.remote.dto.*
import io.rudione.chatone.util.Result
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.decodeFromString

class TwitchApiClient(
    private val httpClient: HttpClient,
    private val clientId: String
) {
    private val baseUrl = "https://api.twitch.tv/helix"
    private val authUrl = "https://id.twitch.tv/oauth2"

    companion object {
        private const val TAG = "TwitchApiClient"
    }

    suspend fun getAccessToken(code: String, clientSecret: String, redirectUri: String): Result<TokenResponse> {
        return try {
            val response = httpClient.post("$authUrl/token") {
                parameter("client_id", clientId)
                parameter("client_secret", clientSecret)
                parameter("code", code)
                parameter("grant_type", "authorization_code")
                parameter("redirect_uri", redirectUri)
            }.body<TokenResponse>()
            Napier.d("Access token received successfully", tag = TAG)
            Result.Success(response)
        } catch (e: Exception) {
            Napier.e("Failed to get access token: ${e.message}", e, tag = TAG)
            Result.Error(e)
        }
    }

    suspend fun refreshAccessToken(refreshToken: String, clientSecret: String): Result<TokenResponse> {
        return try {
            val response = httpClient.post("$authUrl/token") {
                parameter("client_id", clientId)
                parameter("client_secret", clientSecret)
                parameter("grant_type", "refresh_token")
                parameter("refresh_token", refreshToken)
            }.body<TokenResponse>()
            Napier.d("Token refreshed successfully", tag = TAG)
            Result.Success(response)
        } catch (e: Exception) {
            Napier.e("Failed to refresh token: ${e.message}", e, tag = TAG)
            Result.Error(e)
        }
    }

    suspend fun validateToken(accessToken: String): Result<ValidateTokenResponse> {
        return try {
            val response = httpClient.get("https://id.twitch.tv/oauth2/validate") {
                header(HttpHeaders.Authorization, "OAuth $accessToken")
            }

            if (response.status.isSuccess()) {
                Result.Success(response.body())
            } else {
                Result.Error(Exception("Token validation failed: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun revokeToken(accessToken: String): Result<Unit> {
        return try {
            httpClient.post("https://id.twitch.tv/oauth2/revoke") {
                setBody(
                    FormDataContent(
                        Parameters.build {
                            append("client_id", clientId)
                            append("token", accessToken)
                        }
                    )
                )
                contentType(ContentType.Application.FormUrlEncoded)
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun getUsers(accessToken: String, logins: List<String> = emptyList(), ids: List<String> = emptyList()): Result<UsersResponse> {
        return try {
            val response = httpClient.get("$baseUrl/users") {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                header("Client-ID", clientId)
                header(HttpHeaders.Accept, "application/json")

                if (ids.isNotEmpty()) {
                    ids.take(100).forEach { id ->
                        url { parameters.append("id", id) }
                    }
                }
                if (logins.isNotEmpty()) {
                    logins.take(100).forEach { login ->
                        url { parameters.append("login", login) }
                    }
                }
            }

            if (response.status.isSuccess()) {
                Result.Success(response.body())
            } else {
                val errorBody = response.bodyAsText()
                Napier.e("Failed to get users: ${response.status} — $errorBody", tag = TAG)
                Result.Error(Exception("Failed to get users: ${response.status}"))
            }
        } catch (e: Exception) {
            Napier.e("Exception in getUsers: ${e.message}", e, tag = TAG)
            Result.Error(e)
        }
    }


    suspend fun searchChannels(accessToken: String, query: String, first: Int = 20): Result<SearchChannelsResponse> {
        return try {
            val response = httpClient.get("$baseUrl/search/channels") {
                header("Authorization", "Bearer $accessToken")
                header("Client-Id", clientId)
                parameter("query", query)
                parameter("first", first)
                parameter("live_only", false)
            }.body<SearchChannelsResponse>()
            Napier.d("Found ${response.data.size} channels for query: $query", tag = TAG)
            Result.Success(response)
        } catch (e: Exception) {
            Napier.e("Failed to search channels: ${e.message}", e, tag = TAG)
            Result.Error(e)
        }
    }

    suspend fun getStreams(accessToken: String, userIds: List<String> = emptyList(), userLogins: List<String> = emptyList(), first: Int = 100): Result<ChannelsResponse> {
        val validLogins = userLogins.filter { it.isNotBlank() && it.all { c -> c.code in 32..126 } }
        if (validLogins.isEmpty() && userIds.isEmpty()) return Result.Success(ChannelsResponse(data = emptyList()))
        return try {
            val response = httpClient.get("$baseUrl/streams") {
                header("Authorization", "Bearer $accessToken")
                header("Client-Id", clientId)
                userIds.forEach { parameter("user_id", it) }
                validLogins.forEach { parameter("user_login", it) }
                parameter("first", first)
            }.body<ChannelsResponse>()
            Napier.d("Got ${response.data.size} streams", tag = TAG)
            Result.Success(response)
        } catch (e: Exception) {
            Napier.e("Failed to get streams: ${e.message}", e, tag = TAG)
            Result.Error(e)
        }
    }

    suspend fun getGlobalBadges(accessToken: String): Result<BadgesResponse> {
        return try {
            val response = httpClient.get("$baseUrl/chat/badges/global") {
                header("Authorization", "Bearer $accessToken")
                header("Client-Id", clientId)
            }.body<BadgesResponse>()
            Result.Success(response)
        } catch (e: Exception) {
            Napier.e("Failed to get global badges: ${e.message}", e, tag = TAG)
            Result.Error(e)
        }
    }

    suspend fun getChannelBadges(broadcasterId: String, accessToken: String): Result<BadgesResponse> {
        return try {
            val response = httpClient.get("$baseUrl/chat/badges") {
                header("Authorization", "Bearer $accessToken")
                header("Client-Id", clientId)
                parameter("broadcaster_id", broadcasterId)
            }.body<BadgesResponse>()
            Result.Success(response)
        } catch (e: Exception) {
            Napier.e("Failed to get channel badges: ${e.message}", e, tag = TAG)
            Result.Error(e)
        }
    }


    suspend fun banUser(
        accessToken: String,
        broadcasterId: String,
        moderatorId: String,
        userId: String,
        duration: Int? = null,
        reason: String? = null
    ): Result<Unit> {
        return try {
            val dataBody = buildJsonObject {
                put("user_id", JsonPrimitive(userId))

                if (duration != null && duration > 0) {
                    put("duration", JsonPrimitive(duration))
                }
                put("reason", JsonPrimitive(reason ?: ""))
            }
            val body = buildJsonObject { put("data", dataBody) }
            httpClient.post("$baseUrl/moderation/bans") {
                header("Authorization", "Bearer $accessToken")
                header("Client-Id", clientId)
                parameter("broadcaster_id", broadcasterId)
                parameter("moderator_id", moderatorId)
                contentType(ContentType.Application.Json)
                setBody(body)
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Napier.e("Failed to ban user: ${e.message}", e, tag = TAG)
            Result.Error(e)
        }
    }

    suspend fun unbanUser(accessToken: String, broadcasterId: String, moderatorId: String, userId: String): Result<Unit> {
        return try {
            httpClient.delete("$baseUrl/moderation/bans") {
                header("Authorization", "Bearer $accessToken")
                header("Client-Id", clientId)
                parameter("broadcaster_id", broadcasterId)
                parameter("moderator_id", moderatorId)
                parameter("user_id", userId)
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Napier.e("Failed to unban user: ${e.message}", e, tag = TAG)
            Result.Error(e)
        }
    }

    suspend fun deleteMessage(accessToken: String, broadcasterId: String, moderatorId: String, messageId: String): Result<Unit> {
        return try {
            httpClient.delete("$baseUrl/moderation/chat") {
                header("Authorization", "Bearer $accessToken")
                header("Client-Id", clientId)
                parameter("broadcaster_id", broadcasterId)
                parameter("moderator_id", moderatorId)
                parameter("message_id", messageId)
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Napier.e("Failed to delete message: ${e.message}", e, tag = TAG)
            Result.Error(e)
        }
    }

    suspend fun clearChat(accessToken: String, broadcasterId: String, moderatorId: String): Result<Unit> {
        return try {
            httpClient.delete("$baseUrl/moderation/chat") {
                header("Authorization", "Bearer $accessToken")
                header("Client-Id", clientId)
                parameter("broadcaster_id", broadcasterId)
                parameter("moderator_id", moderatorId)
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Napier.e("Failed to clear chat: ${e.message}", e, tag = TAG)
            Result.Error(e)
        }
    }


    suspend fun updateChatSettings(
        accessToken: String,
        broadcasterId: String,
        moderatorId: String,
        settings: Map<String, Any>
    ): Result<Unit> {
        return try {
            val body = buildJsonObject {
                settings.forEach { (key, value) ->
                    when (value) {
                        is Boolean -> put(key, JsonPrimitive(value))
                        is Int -> put(key, JsonPrimitive(value))
                        is Long -> put(key, JsonPrimitive(value))
                        is Double -> put(key, JsonPrimitive(value))
                        is String -> put(key, JsonPrimitive(value))
                        else -> put(key, JsonPrimitive(value.toString()))
                    }
                }
            }
            httpClient.patch("$baseUrl/chat/settings") {
                header("Authorization", "Bearer $accessToken")
                header("Client-Id", clientId)
                parameter("broadcaster_id", broadcasterId)
                parameter("moderator_id", moderatorId)
                contentType(ContentType.Application.Json)
                setBody(body)
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Napier.e("Failed to update chat settings: ${e.message}", e, tag = TAG)
            Result.Error(e)
        }
    }

    suspend fun sendWhisper(accessToken: String, fromUserId: String, toUserId: String, message: String): Result<Unit> {
        return try {
            httpClient.post("$baseUrl/whispers") {
                header("Authorization", "Bearer $accessToken")
                header("Client-Id", clientId)
                parameter("from_user_id", fromUserId)
                parameter("to_user_id", toUserId)
                contentType(ContentType.Application.Json)
                setBody(buildJsonObject { put("message", JsonPrimitive(message)) })
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Napier.e("Failed to send whisper: ${e.message}", e, tag = TAG)
            Result.Error(e)
        }
    }


    suspend fun sendChatMessage(
        accessToken: String,
        broadcasterId: String,
        senderId: String,
        message: String,
        replyParentMessageId: String? = null
    ): Result<SendChatMessageData> {
        return try {
            val response = httpClient.post("$baseUrl/chat/messages") {
                header("Authorization", "Bearer $accessToken")
                header("Client-Id", clientId)
                contentType(ContentType.Application.Json)
                setBody(buildJsonObject {
                    put("broadcaster_id", JsonPrimitive(broadcasterId))
                    put("sender_id", JsonPrimitive(senderId))
                    put("message", JsonPrimitive(message))
                    if (replyParentMessageId != null) {
                        put("reply_parent_message_id", JsonPrimitive(replyParentMessageId))
                    }
                })
            }
            if (!response.status.isSuccess()) {
                val errText = response.bodyAsText()
                Napier.e("sendChatMessage HTTP ${response.status}: $errText", tag = TAG)
                return Result.Error(Exception("HTTP ${response.status}"))
            }
            val rawText = response.bodyAsText()
            Napier.v("sendChatMessage response: $rawText", tag = TAG)

            val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
            val body = json.decodeFromString<SendChatMessageResponse>(rawText)
            val data = body.data.firstOrNull()
            if (data != null && data.isSent) {
                Napier.d("sendChatMessage success, message_id=${data.messageId}", tag = TAG)
                Result.Success(data)
            } else {
                val reason = data?.dropReason?.message ?: "Message not sent / not in response"
                Napier.e("sendChatMessage not sent: $reason", tag = TAG)
                Result.Error(Exception(reason))
            }
        } catch (e: Exception) {
            Napier.e("Failed to send chat message via Helix: ${e.message}", e, tag = TAG)
            Result.Error(e)
        }
    }

    suspend fun addModerator(accessToken: String, broadcasterId: String, userId: String): Result<Unit> {
        return try {
            httpClient.post("$baseUrl/moderation/moderators") {
                header("Authorization", "Bearer $accessToken")
                header("Client-Id", clientId)
                parameter("broadcaster_id", broadcasterId)
                parameter("user_id", userId)
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Napier.e("Failed to add moderator: ${e.message}", e, tag = TAG)
            Result.Error(e)
        }
    }

    suspend fun removeModerator(accessToken: String, broadcasterId: String, userId: String): Result<Unit> {
        return try {
            httpClient.delete("$baseUrl/moderation/moderators") {
                header("Authorization", "Bearer $accessToken")
                header("Client-Id", clientId)
                parameter("broadcaster_id", broadcasterId)
                parameter("user_id", userId)
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Napier.e("Failed to remove moderator: ${e.message}", e, tag = TAG)
            Result.Error(e)
        }
    }

    suspend fun addVip(accessToken: String, broadcasterId: String, userId: String): Result<Unit> {
        return try {
            httpClient.post("$baseUrl/channels/vips") {
                header("Authorization", "Bearer $accessToken")
                header("Client-Id", clientId)
                parameter("broadcaster_id", broadcasterId)
                parameter("user_id", userId)
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Napier.e("Failed to add VIP: ${e.message}", e, tag = TAG)
            Result.Error(e)
        }
    }

    suspend fun removeVip(accessToken: String, broadcasterId: String, userId: String): Result<Unit> {
        return try {
            httpClient.delete("$baseUrl/channels/vips") {
                header("Authorization", "Bearer $accessToken")
                header("Client-Id", clientId)
                parameter("broadcaster_id", broadcasterId)
                parameter("user_id", userId)
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Napier.e("Failed to remove VIP: ${e.message}", e, tag = TAG)
            Result.Error(e)
        }
    }

    suspend fun getChatSettings(accessToken: String, broadcasterId: String, moderatorId: String? = null): Result<ChatSettingsResponse> {
        return try {
            val response = httpClient.get("$baseUrl/chat/settings") {
                header("Authorization", "Bearer $accessToken")
                header("Client-Id", clientId)
                parameter("broadcaster_id", broadcasterId)
                moderatorId?.let { parameter("moderator_id", it) }
            }.body<ChatSettingsResponse>()
            Napier.d("Got chat settings for broadcaster: $broadcasterId", tag = TAG)
            Result.Success(response)
        } catch (e: Exception) {
            Napier.e("Failed to get chat settings: ${e.message}", e, tag = TAG)
            Result.Error(e)
        }
    }

    suspend fun sendAnnouncement(accessToken: String, broadcasterId: String, moderatorId: String, message: String, color: String = "primary"): Result<Unit> {
        return try {
            httpClient.post("$baseUrl/chat/announcements") {
                header("Authorization", "Bearer $accessToken")
                header("Client-Id", clientId)
                parameter("broadcaster_id", broadcasterId)
                parameter("moderator_id", moderatorId)
                contentType(ContentType.Application.Json)
                setBody(buildJsonObject {
                    put("message", JsonPrimitive(message))
                    put("color", JsonPrimitive(color))
                })
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Napier.e("Failed to send announcement: ${e.message}", e, tag = TAG)
            Result.Error(e)
        }
    }

    suspend fun startRaid(accessToken: String, fromBroadcasterId: String, toBroadcasterId: String): Result<Unit> {
        return try {
            httpClient.post("$baseUrl/raids") {
                header("Authorization", "Bearer $accessToken")
                header("Client-Id", clientId)
                parameter("from_broadcaster_id", fromBroadcasterId)
                parameter("to_broadcaster_id", toBroadcasterId)
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Napier.e("Failed to start raid: ${e.message}", e, tag = TAG)
            Result.Error(e)
        }
    }

    suspend fun cancelRaid(accessToken: String, broadcasterId: String): Result<Unit> {
        return try {
            httpClient.delete("$baseUrl/raids") {
                header("Authorization", "Bearer $accessToken")
                header("Client-Id", clientId)
                parameter("broadcaster_id", broadcasterId)
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Napier.e("Failed to cancel raid: ${e.message}", e, tag = TAG)
            Result.Error(e)
        }
    }

    suspend fun createPoll(
        accessToken: String,
        broadcasterId: String,
        title: String,
        choices: List<String>,
        durationSeconds: Int,
        channelPointsVotingEnabled: Boolean = false,
        channelPointsPerVote: Int = 0
    ): Result<PollData> {
        return try {
            val body = buildJsonObject {
                put("broadcaster_id", JsonPrimitive(broadcasterId))
                put("title", JsonPrimitive(title))
                put("choices", buildJsonArray {
                    choices.forEach { c -> add(buildJsonObject { put("title", JsonPrimitive(c)) }) }
                })
                put("duration", JsonPrimitive(durationSeconds))
                if (channelPointsVotingEnabled) {
                    put("channel_points_voting_enabled", JsonPrimitive(true))
                    put("channel_points_per_vote", JsonPrimitive(channelPointsPerVote))
                }
            }
            val resp = httpClient.post("$baseUrl/polls") {
                header("Authorization", "Bearer $accessToken")
                header("Client-Id", clientId)
                contentType(ContentType.Application.Json)
                setBody(body)
            }.body<io.rudione.chatone.data.remote.dto.PollsResponse>()
            resp.data.firstOrNull()?.let { Result.Success(it) }
                ?: Result.Error(Exception("Empty poll response"))
        } catch (e: Exception) {
            Napier.e("createPoll failed: ${e.message}", e, tag = TAG)
            Result.Error(e)
        }
    }

    suspend fun endPoll(
        accessToken: String,
        broadcasterId: String,
        pollId: String,
        status: String
    ): Result<Unit> {
        return try {
            val body = buildJsonObject {
                put("broadcaster_id", JsonPrimitive(broadcasterId))
                put("id", JsonPrimitive(pollId))
                put("status", JsonPrimitive(status))
            }
            httpClient.patch("$baseUrl/polls") {
                header("Authorization", "Bearer $accessToken")
                header("Client-Id", clientId)
                contentType(ContentType.Application.Json)
                setBody(body)
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Napier.e("endPoll failed: ${e.message}", e, tag = TAG)
            Result.Error(e)
        }
    }

    suspend fun getActivePoll(
        accessToken: String,
        broadcasterId: String
    ): Result<io.rudione.chatone.data.remote.dto.PollData?> {
        return try {
            val resp = httpClient.get("$baseUrl/polls") {
                header("Authorization", "Bearer $accessToken")
                header("Client-Id", clientId)
                parameter("broadcaster_id", broadcasterId)
                parameter("first", 1)
            }.body<PollsResponse>()
            Result.Success(resp.data.firstOrNull { it.status == "ACTIVE" })
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun createPrediction(
        accessToken: String,
        broadcasterId: String,
        title: String,
        outcomes: List<String>,
        predictionWindowSeconds: Int
    ): Result<PredictionData> {
        return try {
            val body = buildJsonObject {
                put("broadcaster_id", JsonPrimitive(broadcasterId))
                put("title", JsonPrimitive(title))
                put("outcomes", buildJsonArray {
                    outcomes.forEach { o -> add(buildJsonObject { put("title", JsonPrimitive(o)) }) }
                })
                put("prediction_window", JsonPrimitive(predictionWindowSeconds))
            }
            val resp = httpClient.post("$baseUrl/predictions") {
                header("Authorization", "Bearer $accessToken")
                header("Client-Id", clientId)
                contentType(ContentType.Application.Json)
                setBody(body)
            }.body<io.rudione.chatone.data.remote.dto.PredictionsResponse>()
            resp.data.firstOrNull()?.let { Result.Success(it) }
                ?: Result.Error(Exception("Empty prediction response"))
        } catch (e: Exception) {
            Napier.e("createPrediction failed: ${e.message}", e, tag = TAG)
            Result.Error(e)
        }
    }

    suspend fun endPrediction(
        accessToken: String,
        broadcasterId: String,
        predictionId: String,
        status: String,
        winningOutcomeId: String? = null
    ): Result<Unit> {
        return try {
            val body = buildJsonObject {
                put("broadcaster_id", JsonPrimitive(broadcasterId))
                put("id", JsonPrimitive(predictionId))
                put("status", JsonPrimitive(status))
                if (winningOutcomeId != null) put("winning_outcome_id", JsonPrimitive(winningOutcomeId))
            }
            httpClient.patch("$baseUrl/predictions") {
                header("Authorization", "Bearer $accessToken")
                header("Client-Id", clientId)
                contentType(ContentType.Application.Json)
                setBody(body)
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Napier.e("endPrediction failed: ${e.message}", e, tag = TAG)
            Result.Error(e)
        }
    }

    suspend fun getActivePrediction(
        accessToken: String,
        broadcasterId: String
    ): Result<PredictionData?> {
        return try {
            val resp = httpClient.get("$baseUrl/predictions") {
                header("Authorization", "Bearer $accessToken")
                header("Client-Id", clientId)
                parameter("broadcaster_id", broadcasterId)
                parameter("first", 5)
            }.body<PredictionsResponse>()
            Result.Success(resp.data.firstOrNull { it.status == "ACTIVE" || it.status == "LOCKED" })
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun sendShoutout(accessToken: String, fromBroadcasterId: String, toBroadcasterId: String, moderatorId: String): Result<Unit> {
        return try {
            httpClient.post("$baseUrl/chat/shoutouts") {
                header("Authorization", "Bearer $accessToken")
                header("Client-Id", clientId)
                parameter("from_broadcaster_id", fromBroadcasterId)
                parameter("to_broadcaster_id", toBroadcasterId)
                parameter("moderator_id", moderatorId)
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Napier.e("Failed to send shoutout: ${e.message}", e, tag = TAG)
            Result.Error(e)
        }
    }

    suspend fun createEventSubSubscription(
        accessToken: String,
        type: String,
        version: String,
        condition: Map<String, String>,
        sessionId: String
    ): Result<Unit> {
        return try {
            val body = buildJsonObject {
                put("type", JsonPrimitive(type))
                put("version", JsonPrimitive(version))
                put("condition", buildJsonObject {
                    condition.forEach { (k, v) -> put(k, JsonPrimitive(v)) }
                })
                put("transport", buildJsonObject {
                    put("method", JsonPrimitive("websocket"))
                    put("session_id", JsonPrimitive(sessionId))
                })
            }
            httpClient.post("$baseUrl/eventsub/subscriptions") {
                header("Authorization", "Bearer $accessToken")
                header("Client-Id", clientId)
                contentType(ContentType.Application.Json)
                setBody(body)
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Napier.e("Failed to create EventSub subscription [$type]: ${e.message}", e, tag = TAG)
            Result.Error(e)
        }
    }

    suspend fun getAutoModSettings(
        accessToken: String,
        broadcasterId: String,
        moderatorId: String
    ): Result<JsonObject> {
        return try {
            val response = httpClient.get("$baseUrl/moderation/automod/settings") {
                header("Authorization", "Bearer $accessToken")
                header("Client-Id", clientId)
                parameter("broadcaster_id", broadcasterId)
                parameter("moderator_id", moderatorId)
            }.body<JsonObject>()
            Result.Success(response)
        } catch (e: Exception) {
            Napier.e("Failed to get AutoMod settings: ${e.message}", e, tag = TAG)
            Result.Error(e)
        }
    }

    suspend fun updateAutoModSettings(
        accessToken: String,
        broadcasterId: String,
        moderatorId: String,
        levels: Map<String, Int>
    ): Result<Unit> {
        return try {
            httpClient.put("$baseUrl/moderation/automod/settings") {
                header("Authorization", "Bearer $accessToken")
                header("Client-Id", clientId)
                parameter("broadcaster_id", broadcasterId)
                parameter("moderator_id", moderatorId)
                contentType(ContentType.Application.Json)
                setBody(buildJsonObject {
                    levels.forEach { (k, v) -> put(k, JsonPrimitive(v)) }
                })
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Napier.e("Failed to update AutoMod settings: ${e.message}", e, tag = TAG)
            Result.Error(e)
        }
    }

    @kotlinx.serialization.Serializable
    private data class ChatColorEntry(
        @kotlinx.serialization.SerialName("user_id") val userId: String,
        @kotlinx.serialization.SerialName("user_login") val userLogin: String = "",
        @kotlinx.serialization.SerialName("color") val color: String = ""
    )

    @kotlinx.serialization.Serializable
    private data class ChatColorResponse(val data: List<ChatColorEntry> = emptyList())

    /**
     * Returns the user's current chat color as a hex string (e.g. "#38D7FF"), or null if Twitch
     * hasn't assigned one yet. Used to pre-populate the local-echo colour when sending a message
     * before the IRC USERSTATE tag has arrived in a freshly-joined channel.
     */
    suspend fun getUserChatColor(accessToken: String, userId: String): Result<String?> {
        return try {
            val resp = httpClient.get("$baseUrl/chat/color") {
                header("Authorization", "Bearer $accessToken")
                header("Client-Id", clientId)
                parameter("user_id", userId)
            }.body<ChatColorResponse>()
            Result.Success(resp.data.firstOrNull()?.color?.takeIf { it.isNotBlank() })
        } catch (e: Exception) {
            Napier.w("Failed to fetch chat color for $userId: ${e.message}", tag = TAG)
            Result.Error(e)
        }
    }

    suspend fun updateUserChatColor(accessToken: String, userId: String, color: String): Result<Unit> {
        return try {
            httpClient.put("$baseUrl/chat/color") {
                header("Authorization", "Bearer $accessToken")
                header("Client-Id", clientId)
                parameter("user_id", userId)
                parameter("color", color)
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Napier.e("Failed to update chat color: ${e.message}", e, tag = TAG)
            Result.Error(e)
        }
    }

    suspend fun warnUser(
        accessToken: String,
        broadcasterId: String,
        moderatorId: String,
        userId: String,
        reason: String
    ): Result<Unit> {
        return try {
            httpClient.post("$baseUrl/moderation/warnings") {
                header("Authorization", "Bearer $accessToken")
                header("Client-Id", clientId)
                contentType(ContentType.Application.Json)
                parameter("broadcaster_id", broadcasterId)
                parameter("moderator_id", moderatorId)
                setBody(buildJsonObject {
                    put("data", buildJsonObject {
                        put("user_id", userId)
                        put("reason", reason)
                    })
                })
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Napier.e("Failed to warn user: ${e.message}", e, tag = TAG)
            Result.Error(e)
        }
    }

    suspend fun getChannelFollower(accessToken: String, broadcasterId: String, userId: String): Result<FollowersResponse> {
        return try {
            val response = httpClient.get("$baseUrl/channels/followers") {
                header("Authorization", "Bearer $accessToken")
                header("Client-Id", clientId)
                parameter("broadcaster_id", broadcasterId)
                parameter("user_id", userId)
            }.body<FollowersResponse>()
            Result.Success(response)
        } catch (e: Exception) {
            Napier.e("Failed to get follower info: ${e.message}", e, tag = TAG)
            Result.Error(e)
        }
    }


    suspend fun getModeratedChannels(
        accessToken: String,
        userId: String
    ): Result<Set<String>> {
        return try {
            val ids = mutableSetOf<String>()
            var cursor: String? = null
            do {
                val response = httpClient.get("$baseUrl/moderation/channels") {
                    header("Authorization", "Bearer $accessToken")
                    header("Client-Id", clientId)
                    parameter("user_id", userId)
                    parameter("first", 100)
                    cursor?.let { parameter("after", it) }
                }
                when (response.status) {
                    HttpStatusCode.OK -> {
                        val body = response.body<ModeratedChannelsResponse>()
                        body.data.forEach { ids.add(it.broadcasterId) }
                        cursor = body.pagination?.cursor?.takeIf { it.isNotEmpty() }
                    }
                    HttpStatusCode.Unauthorized, HttpStatusCode.Forbidden -> {
                        Napier.d("getModeratedChannels: missing scope or unauthorized (${response.status})", tag = TAG)
                        return Result.Error(Exception("Missing scope user:read:moderated_channels"))
                    }
                    else -> {
                        Napier.w("getModeratedChannels unexpected status ${response.status}", tag = TAG)
                        return Result.Error(Exception("HTTP ${response.status.value}"))
                    }
                }
            } while (cursor != null)
            Result.Success(ids)
        } catch (e: Exception) {
            Napier.e("Failed to get moderated channels: ${e.message}", e, tag = TAG)
            Result.Error(e)
        }
    }

    suspend fun getChannelModerators(
        accessToken: String,
        broadcasterId: String,
        userId: String
    ): Result<ModeratorsResponse> {
        return try {
            val response = httpClient.get("https://api.twitch.tv/helix/moderation/moderators") {
                header("Authorization", "Bearer $accessToken")
                header("Client-Id", clientId)
                parameter("broadcaster_id", broadcasterId)
                parameter("user_id", userId)
            }

            when (response.status) {
                HttpStatusCode.OK -> {
                    Result.Success(response.body<ModeratorsResponse>())
                }
                HttpStatusCode.Unauthorized, HttpStatusCode.Forbidden -> {
                    Napier.d("No permission to check moderators (401/403)", tag = TAG)
                    Result.Success(ModeratorsResponse(emptyList()))
                }
                HttpStatusCode.TooManyRequests -> {
                    Napier.w("Rate limited on moderators endpoint", tag = TAG)
                    Result.Error(Exception("Rate limited"))
                }
                else -> {
                    Napier.w("Unexpected status ${response.status} on moderators endpoint", tag = TAG)
                    Result.Error(Exception("HTTP ${response.status.value}"))
                }
            }
        } catch (e: kotlinx.serialization.SerializationException) {
            Napier.e("Failed to parse moderators response: ${e.message}", e, tag = TAG)
            Result.Success(ModeratorsResponse(emptyList()))
        } catch (e: Exception) {
            Napier.e("Failed to get moderators: ${e.message}", e, tag = TAG)
            Result.Error(e)
        }
    }


    suspend fun getChatters(
        accessToken: String,
        broadcasterId: String,
        moderatorId: String,
        first: Int = 100,
        after: String? = null
    ): Result<ChattersResponse> {
        return try {
            val response = httpClient.get("$baseUrl/chat/chatters") {
                header("Authorization", "Bearer $accessToken")
                header("Client-Id", clientId)
                parameter("broadcaster_id", broadcasterId)
                parameter("moderator_id", moderatorId)
                parameter("first", first)
                after?.let { parameter("after", it) }
            }.body<ChattersResponse>()
            Result.Success(response)
        } catch (e: Exception) {
            Napier.e("Failed to get chatters: ${e.message}", e, tag = TAG)
            Result.Error(e)
        }
    }


    suspend fun blockUser(accessToken: String, targetUserId: String): Result<Unit> {
        return try {
            httpClient.put("$baseUrl/users/blocks") {
                header("Authorization", "Bearer $accessToken")
                header("Client-Id", clientId)
                parameter("target_user_id", targetUserId)
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Napier.e("Failed to block user: ${e.message}", e, tag = TAG)
            Result.Error(e)
        }
    }


    suspend fun unblockUser(accessToken: String, targetUserId: String): Result<Unit> {
        return try {
            httpClient.delete("$baseUrl/users/blocks") {
                header("Authorization", "Bearer $accessToken")
                header("Client-Id", clientId)
                parameter("target_user_id", targetUserId)
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Napier.e("Failed to unblock user: ${e.message}", e, tag = TAG)
            Result.Error(e)
        }
    }


    suspend fun getBlockedUsers(accessToken: String, broadcasterId: String, first: Int = 100): Result<BlockedUsersResponse> {
        return try {
            val response = httpClient.get("$baseUrl/users/blocks") {
                header("Authorization", "Bearer $accessToken")
                header("Client-Id", clientId)
                parameter("broadcaster_id", broadcasterId)
                parameter("first", first)
            }.body<BlockedUsersResponse>()
            Result.Success(response)
        } catch (e: Exception) {
            Napier.e("Failed to get blocked users: ${e.message}", e, tag = TAG)
            Result.Error(e)
        }
    }


    suspend fun manageAutoModMessage(
        accessToken: String,
        userId: String,
        msgId: String,
        action: String
    ): Result<Unit> {
        return try {
            httpClient.post("$baseUrl/moderation/automod/message") {
                header("Authorization", "Bearer $accessToken")
                header("Client-Id", clientId)
                contentType(io.ktor.http.ContentType.Application.Json)
                setBody(kotlinx.serialization.json.buildJsonObject {
                    put("user_id", kotlinx.serialization.json.JsonPrimitive(userId))
                    put("msg_id", kotlinx.serialization.json.JsonPrimitive(msgId))
                    put("action", kotlinx.serialization.json.JsonPrimitive(action))
                })
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Napier.e("Failed to manage automod message: ${e.message}", e, tag = TAG)
            Result.Error(e)
        }
    }

    suspend fun getUserEmotes(accessToken: String, userId: String, broadcasterId: String? = null): Result<UserEmotesResponse> {
        return try {
            val response = httpClient.get("$baseUrl/chat/emotes/user") {
                header("Authorization", "Bearer $accessToken")
                header("Client-Id", clientId)
                parameter("user_id", userId)
                if (broadcasterId != null) parameter("broadcaster_id", broadcasterId)
            }.body<UserEmotesResponse>()
            Result.Success(response)
        } catch (e: Exception) {
            Napier.w("Failed to get user emotes: ${e.message}", tag = TAG)
            Result.Error(e)
        }
    }
}