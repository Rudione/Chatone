package io.rudione.chatone.data.remote.emote

import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.rudione.chatone.data.remote.dto.SevenTvEmoteSet
import io.rudione.chatone.data.remote.dto.SevenTvUserResponse
import io.rudione.chatone.domain.model.GenericEmote

data class SevenTvChannelResult(
    val emotes: List<GenericEmote>,
    val emoteSetId: String?,
    val emoteSetName: String? = null,
    val sevenTvUserId: String? = null
)

class SevenTvApiClient(private val httpClient: HttpClient) {
    private val baseUrl = "https://7tv.io/v3"

    companion object {
        private const val TAG = "7TV"
    }

    suspend fun getChannelEmotes(userId: String): List<GenericEmote> {
        return getChannelEmotesWithSetId(userId).emotes
    }

    suspend fun getChannelEmotesWithSetId(userId: String): SevenTvChannelResult {
        return try {
            val response = httpClient.get("$baseUrl/users/twitch/$userId").body<SevenTvUserResponse>()
            val setId = response.emoteSet?.id ?: response.emoteSetId
            val inlineEmotes = response.emoteSet?.emotes?.mapNotNull { SevenTvEmoteMapper.fromSetEntry(it) } ?: emptyList()

            val emotes = if (inlineEmotes.isNotEmpty() || setId.isNullOrBlank()) {
                inlineEmotes
            } else {
                getEmoteSet(setId)
            }
            SevenTvChannelResult(
                emotes = emotes,
                emoteSetId = setId,
                emoteSetName = response.emoteSet?.name?.takeIf { it.isNotBlank() },
                sevenTvUserId = response.user?.id?.takeIf { it.isNotBlank() }
            )
        } catch (e: Exception) {
            Napier.e("Failed to get 7TV channel emotes: ${e.message}", tag = TAG)
            SevenTvChannelResult(emotes = emptyList(), emoteSetId = null)
        }
    }

    suspend fun getGlobalEmotes(): List<GenericEmote> {
        return try {
            val response = httpClient.get("$baseUrl/emote-sets/global").body<SevenTvEmoteSet>()
            response.emotes.mapNotNull { SevenTvEmoteMapper.fromSetEntry(it) }
        } catch (e: Exception) {
            Napier.e("Failed to get 7TV global emotes: ${e.message}", tag = TAG)
            emptyList()
        }
    }

    suspend fun getEmoteSet(emoteSetId: String): List<GenericEmote> {
        return try {
            httpClient.get("$baseUrl/emote-sets/$emoteSetId")
                .body<SevenTvEmoteSet>()
                .emotes.mapNotNull { SevenTvEmoteMapper.fromSetEntry(it) }
        } catch (e: Exception) {
            Napier.w("Failed to fetch 7TV emote set $emoteSetId: ${e.message}", tag = TAG)
            emptyList()
        }
    }
}
