package io.rudione.chatone.data.remote

import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.rudione.chatone.domain.model.ChatMessage
import io.rudione.chatone.util.IrcMessage
import io.rudione.chatone.util.IrcMessageParser
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RecentMessagesResponse(
    val messages: List<String>,
    val error: String? = null,
    @SerialName("error_code") val errorCode: String? = null
)

class RecentMessagesClient(
    private val httpClient: HttpClient
) {
    private val baseUrl = "https://recent-messages.robotty.de/api/v2/recent-messages"

    companion object {
        private const val TAG = "RecentMessages"
    }

    suspend fun getRecentMessages(channel: String, limit: Int = 100): List<ChatMessage> {
        return try {
            val channelName = channel.lowercase().removePrefix("#")
            val response = httpClient.get("$baseUrl/$channelName") {
                parameter("limit", limit)
            }.body<RecentMessagesResponse>()

            if (response.error != null) {
                Napier.w("Recent messages error for $channelName: ${response.error}", tag = TAG)
                return emptyList()
            }

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
        } catch (e: Exception) {
            Napier.e("Failed to get recent messages: ${e.message}", e, tag = TAG)
            emptyList()
        }
    }
}
