package io.rudione.chatone.data.remote.emote

import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.rudione.chatone.data.remote.dto.SevenTvEmote
import io.rudione.chatone.data.remote.dto.SevenTvEmoteSet
import io.rudione.chatone.data.remote.dto.SevenTvUserResponse
import io.rudione.chatone.domain.model.EmoteProvider
import io.rudione.chatone.domain.model.GenericEmote

data class SevenTvChannelResult(
    val emotes: List<GenericEmote>,
    val emoteSetId: String?
)

class SevenTvApiClient(private val httpClient: HttpClient) {
    private val baseUrl = "https://7tv.io/v3"

    companion object {
        private const val TAG = "7TV"
        private const val ZERO_WIDTH_FLAG = 1 shl 8
    }

    suspend fun getChannelEmotes(userId: String): List<GenericEmote> {
        return getChannelEmotesWithSetId(userId).emotes
    }

    suspend fun getChannelEmotesWithSetId(userId: String): SevenTvChannelResult {
        return try {
            val response = httpClient.get("$baseUrl/users/twitch/$userId").body<SevenTvUserResponse>()
            SevenTvChannelResult(
                emotes = response.emoteSet?.emotes?.mapNotNull { it.toGenericEmote() } ?: emptyList(),
                emoteSetId = response.emoteSet?.id
            )
        } catch (e: Exception) {
            Napier.e("Failed to get 7TV channel emotes: ${e.message}", tag = TAG)
            SevenTvChannelResult(emotes = emptyList(), emoteSetId = null)
        }
    }

    suspend fun getGlobalEmotes(): List<GenericEmote> {
        return try {
            val response = httpClient.get("$baseUrl/emote-sets/global").body<SevenTvEmoteSet>()
            response.emotes.mapNotNull { it.toGenericEmote() }
        } catch (e: Exception) {
            Napier.e("Failed to get 7TV global emotes: ${e.message}", tag = TAG)
            emptyList()
        }
    }

    private fun SevenTvEmote.toGenericEmote(): GenericEmote? {
        val emoteData = data ?: return null
        val host = emoteData.host
        val baseUrl = "https:${host.url}"
        val webpFiles = host.files.filter { it.format == "WEBP" }
        if (webpFiles.isEmpty()) return null

        val file1x = webpFiles.find { it.name.contains("1x") }
        val file2x = webpFiles.find { it.name.contains("2x") }
        val file3x = webpFiles.find { it.name.contains("4x") }
            ?: webpFiles.find { it.name.contains("3x") }

        val url1x = file1x?.let { "$baseUrl/${it.name}" }
        val url2x = file2x?.let { "$baseUrl/${it.name}" }
        val url3x = file3x?.let { "$baseUrl/${it.name}" }

        val sizeFile = file2x ?: file1x ?: webpFiles.first()

        return GenericEmote(
            id = id,
            code = name,
            url1x = url1x ?: "$baseUrl/${webpFiles.first().name}",
            url2x = url2x ?: url1x ?: "$baseUrl/${webpFiles.first().name}",
            url3x = url3x ?: url2x ?: url1x ?: "$baseUrl/${webpFiles.first().name}",
            provider = EmoteProvider.SEVEN_TV,
            isZeroWidth = (flags and ZERO_WIDTH_FLAG) != 0 || (emoteData.flags and ZERO_WIDTH_FLAG) != 0,
            width = sizeFile.width,
            height = sizeFile.height,
            originalName = emoteData.name,
            authorName = emoteData.owner?.displayName?.ifEmpty { emoteData.owner.username } ?: ""
        )
    }
}