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
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.Parameters
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.rudione.chatone.data.remote.dto.*
import io.rudione.chatone.util.Result
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

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
                // ▼▼▼ Правильное кодирование form-urlencoded ▼▼▼
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

    suspend fun getUsers(accessToken: String, logins: List<String> = emptyList()): Result<UsersResponse> {
        return try {
            val response = httpClient.get("$baseUrl/users") {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                header("Client-ID", clientId)
                header(HttpHeaders.Accept, "application/json")

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

    /**
     * Ban or timeout a user.
     * FIX: duration=null (permanent ban) must be omitted from body, not sent as null.
     */
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
                // Only include duration for timeouts — permanent bans must NOT have duration field
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

    /**
     * Update chat settings. Uses buildJsonObject so Boolean/Int serialize correctly.
     */
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
}