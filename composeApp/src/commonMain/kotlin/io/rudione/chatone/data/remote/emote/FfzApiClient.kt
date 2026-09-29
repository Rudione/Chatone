package io.rudione.chatone.data.remote.emote

import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.rudione.chatone.data.remote.dto.FfzEmote
import io.rudione.chatone.data.remote.dto.FfzGlobalResponse
import io.rudione.chatone.data.remote.dto.FfzRoomResponse
import io.rudione.chatone.domain.model.EmoteProvider
import io.rudione.chatone.domain.model.GenericEmote

class FfzApiClient(private val httpClient: HttpClient) {
    private val baseUrl = "https://api.frankerfacez.com/v1"

    companion object {
        private const val TAG = "FFZ"
    }

    suspend fun getChannelEmotes(userId: String): List<GenericEmote> {
        return try {
            val response = httpClient.get("$baseUrl/room/id/$userId").body<FfzRoomResponse>()
            response.sets.values.flatMap { set ->
                set.emoticons.map { it.toGenericEmote() }
            }
        } catch (e: Exception) {
            Napier.e("Failed to get FFZ channel emotes: ${e.message}", tag = TAG)
            emptyList()
        }
    }

    suspend fun getGlobalEmotes(): List<GenericEmote> {
        return try {
            val response = httpClient.get("$baseUrl/set/global").body<FfzGlobalResponse>()
            response.sets.values.flatMap { set ->
                set.emoticons.map { it.toGenericEmote() }
            }
        } catch (e: Exception) {
            Napier.e("Failed to get FFZ global emotes: ${e.message}", tag = TAG)
            emptyList()
        }
    }

    private fun FfzEmote.toGenericEmote(): GenericEmote {
        val animatedUrls = animated
        val urls = if (!animatedUrls.isNullOrEmpty()) animatedUrls else this.urls
        return GenericEmote(
            id = id.toString(),
            code = name,
            url1x = urls["1"] ?: urls.values.firstOrNull() ?: "",
            url2x = urls["2"] ?: urls["1"] ?: urls.values.firstOrNull() ?: "",
            url3x = urls["4"] ?: urls["2"] ?: urls.values.firstOrNull() ?: "",
            provider = EmoteProvider.FFZ
        )
    }
}
