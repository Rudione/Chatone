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

        private val EMOTE_FORMAT_PREFERENCE = listOf("WEBP", "AVIF", "GIF", "PNG")
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

    
    suspend fun getEmoteSet(emoteSetId: String): List<GenericEmote> {
        return try {
            httpClient.get("$baseUrl/emote-sets/$emoteSetId")
                .body<SevenTvEmoteSet>()
                .emotes.mapNotNull { it.toGenericEmote() }
        } catch (e: Exception) {
            Napier.w("Failed to fetch 7TV emote set $emoteSetId: ${e.message}", tag = TAG)
            emptyList()
        }
    }

    suspend fun getEmoteById(emoteId: String): GenericEmote? {
        return try {
            val emote = httpClient.get("$baseUrl/emotes/$emoteId").body<SevenTvEmote>()
            emote.toGenericEmote()
        } catch (e: Exception) {
            Napier.w("Failed to fetch 7TV emote $emoteId: ${e.message}", tag = TAG)
            null
        }
    }

    private fun SevenTvEmote.toGenericEmote(): GenericEmote? {
        val emoteData = data ?: return null
        val host = emoteData.host
        val baseUrl = "https:${host.url}"

        // Prefer WEBP (decodes on every platform). Fall back to AVIF/GIF/PNG so emotes that
        // 7TV only ships in a newer format (common for newer/personal emotes) are still shown
        // instead of being silently dropped — this is why some users' emotes were invisible
        // here while Chatterino, which also reads AVIF, showed them.
        val files = EMOTE_FORMAT_PREFERENCE
            .firstNotNullOfOrNull { fmt -> host.files.filter { it.format == fmt }.takeIf { it.isNotEmpty() } }
            ?: return null

        val file1x = files.find { it.name.contains("1x") }
        val file2x = files.find { it.name.contains("2x") }
        val file3x = files.find { it.name.contains("4x") }
            ?: files.find { it.name.contains("3x") }

        val url1x = file1x?.let { "$baseUrl/${it.name}" }
        val url2x = file2x?.let { "$baseUrl/${it.name}" }
        val url3x = file3x?.let { "$baseUrl/${it.name}" }

        val sizeFile = file2x ?: file1x ?: files.first()

        return GenericEmote(
            id = id,
            code = name,
            url1x = url1x ?: "$baseUrl/${files.first().name}",
            url2x = url2x ?: url1x ?: "$baseUrl/${files.first().name}",
            url3x = url3x ?: url2x ?: url1x ?: "$baseUrl/${files.first().name}",
            provider = EmoteProvider.SEVEN_TV,
            isZeroWidth = (flags and ZERO_WIDTH_FLAG) != 0 || (emoteData.flags and ZERO_WIDTH_FLAG) != 0,
            width = sizeFile.width,
            height = sizeFile.height,
            originalName = emoteData.name,
            authorName = emoteData.owner?.displayName?.ifEmpty { emoteData.owner.username } ?: ""
        )
    }
}