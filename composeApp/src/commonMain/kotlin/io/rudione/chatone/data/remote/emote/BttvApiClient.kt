package io.rudione.chatone.data.remote.emote

import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.rudione.chatone.data.remote.dto.BttvEmote
import io.rudione.chatone.data.remote.dto.BttvGlobalEmote
import io.rudione.chatone.data.remote.dto.BttvUserResponse
import io.rudione.chatone.domain.model.EmoteProvider
import io.rudione.chatone.domain.model.GenericEmote

class BttvApiClient(private val httpClient: HttpClient) {
    private val baseUrl = "https://api.betterttv.net/3/cached"
    private val cdnUrl = "https://cdn.betterttv.net/emote"

    companion object {
        private const val TAG = "BTTV"
    }

    suspend fun getChannelEmotes(userId: String): List<GenericEmote> {
        return try {
            val response = httpClient.get("$baseUrl/users/twitch/$userId").body<BttvUserResponse>()
            (response.channelEmotes + response.sharedEmotes).map { it.toGenericEmote() }
        } catch (e: Exception) {
            Napier.e("Failed to get BTTV channel emotes: ${e.message}", tag = TAG)
            emptyList()
        }
    }

    suspend fun getGlobalEmotes(): List<GenericEmote> {
        return try {
            val response = httpClient.get("$baseUrl/emotes/global").body<List<BttvGlobalEmote>>()
            response.map { emote ->
                GenericEmote(
                    id = emote.id,
                    code = emote.code,
                    url1x = "$cdnUrl/${emote.id}/1x",
                    url2x = "$cdnUrl/${emote.id}/2x",
                    url3x = "$cdnUrl/${emote.id}/3x",
                    provider = EmoteProvider.BTTV
                )
            }
        } catch (e: Exception) {
            Napier.e("Failed to get BTTV global emotes: ${e.message}", tag = TAG)
            emptyList()
        }
    }

    private fun BttvEmote.toGenericEmote(): GenericEmote {
        return GenericEmote(
            id = id,
            code = code,
            url1x = "$cdnUrl/$id/1x",
            url2x = "$cdnUrl/$id/2x",
            url3x = "$cdnUrl/$id/3x",
            provider = EmoteProvider.BTTV
        )
    }
}
