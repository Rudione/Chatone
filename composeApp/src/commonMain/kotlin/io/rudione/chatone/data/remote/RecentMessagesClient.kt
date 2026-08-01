package io.rudione.chatone.data.remote

import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.rudione.chatone.domain.model.ChatMessage
import io.rudione.chatone.util.chat.IrcMessage
import io.rudione.chatone.util.chat.IrcMessageParser
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RecentMessagesResponse(
    val messages: List<String> = emptyList(),
    val error: String? = null,
    @SerialName("error_code") val errorCode: String? = null
)

sealed interface RecentMessagesResult {
    data class Success(val messages: List<ChatMessage>) : RecentMessagesResult

    data object Pending : RecentMessagesResult

    data object Unavailable : RecentMessagesResult

    data object Failed : RecentMessagesResult
}

class RecentMessagesClient(
    private val httpClient: HttpClient
) {
    private val baseUrl = "https://recent-messages.robotty.de/api/v2/recent-messages"

    companion object {
        private const val TAG = "RecentMessages"
        private const val REQUEST_TIMEOUT_MS = 8000L
        private const val CONNECT_TIMEOUT_MS = 5000L

        private val PERMANENT_ERROR_CODES = setOf(
            "channel_ignored",
            "channel_not_found",
            "invalid_channel_name"
        )
    }

    suspend fun getRecentMessages(channel: String, limit: Int = 100): RecentMessagesResult {
        val channelName = channel.lowercase().removePrefix("#")
        return try {
            val response = httpClient.get("$baseUrl/$channelName") {
                parameter("limit", limit)
                timeout {
                    requestTimeoutMillis = REQUEST_TIMEOUT_MS
                    connectTimeoutMillis = CONNECT_TIMEOUT_MS
                }
            }.body<RecentMessagesResponse>()

            if (response.error != null) {
                Napier.d(
                    "Recent messages unavailable for $channelName: ${response.error} (${response.errorCode})",
                    tag = TAG
                )
                return when {
                    response.errorCode in PERMANENT_ERROR_CODES -> RecentMessagesResult.Unavailable
                    else -> RecentMessagesResult.Pending
                }
            }

            RecentMessagesResult.Success(
                response.messages.mapNotNull { raw ->
                    try {
                        val ircMsg = IrcMessage.parse(raw) ?: return@mapNotNull null
                        if (ircMsg.command == "PRIVMSG") {
                            IrcMessageParser.parsePrivMsg(ircMsg)
                        } else null
                    } catch (e: Exception) {
                        null
                    }
                }
            )
        } catch (e: Exception) {
            Napier.w("Failed to get recent messages for $channelName: ${e.message}", tag = TAG)
            RecentMessagesResult.Failed
        }
    }
}
