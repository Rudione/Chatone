package io.rudione.chatone.data.remote.gif

import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.rudione.chatone.data.remote.dto.GiphyGif
import io.rudione.chatone.data.remote.dto.GiphyResponse
import io.rudione.chatone.domain.model.GifSearchConfig
import io.rudione.chatone.domain.model.GifSearchItem
import io.rudione.chatone.domain.model.GifSearchPage
import io.rudione.chatone.util.Result
import io.rudione.chatone.util.media.GiphyMedia
import kotlinx.coroutines.CancellationException

class GiphyApiClient(private val httpClient: HttpClient) {

    companion object {
        private const val TAG = "Giphy"
        private const val BASE_URL = "https://api.giphy.com/v1/gifs"
        const val PAGE_SIZE = 30
        const val SIGNUP_URL = "https://developers.giphy.com/dashboard/"
    }

    suspend fun trending(config: GifSearchConfig, offset: Int = 0): Result<GifSearchPage> =
        request(config, "$BASE_URL/trending", query = null, offset = offset)

    suspend fun search(config: GifSearchConfig, query: String, offset: Int = 0): Result<GifSearchPage> =
        request(config, "$BASE_URL/search", query = query, offset = offset)

    private suspend fun request(
        config: GifSearchConfig,
        url: String,
        query: String?,
        offset: Int
    ): Result<GifSearchPage> {
        if (config.apiKey.isBlank()) return Result.Error(GiphyKeyMissingException())
        return try {
            val response: GiphyResponse = httpClient.get(url) {
                parameter("api_key", config.apiKey)
                parameter("limit", PAGE_SIZE)
                parameter("offset", offset)
                parameter("rating", config.rating)
                parameter("bundle", "messaging_non_clips")
                if (query != null) parameter("q", query)
            }.body()

            val status = response.meta?.status ?: 200
            if (status !in 200..299) {
                val message = response.meta?.msg.orEmpty().ifBlank { "HTTP $status" }
                Napier.w("Giphy request failed: $message", tag = TAG)
                return Result.Error(GiphyRequestException(status, message))
            }

            val items = response.data.mapNotNull { it.toSearchItem() }
            Result.Success(
                GifSearchPage(
                    items = items,
                    nextOffset = if (items.size < PAGE_SIZE) null else offset + response.data.size
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Napier.w("Giphy request error: ${e::class.simpleName}", tag = TAG)
            Result.Error(e)
        }
    }

    private fun GiphyGif.toSearchItem(): GifSearchItem? {
        val send = images.original?.url?.takeIf { it.isNotBlank() } ?: return null
        val previewImage = listOfNotNull(
            images.fixedHeightSmall,
            images.fixedWidthSmall,
            images.fixedHeight,
            images.fixedWidth,
            images.fixedHeightDownsampled
        ).firstOrNull { it.url.isNotBlank() }
        val preview = previewImage?.let { GiphyMedia.preferWebp(it.url, it.webp) } ?: send
        if (id.isBlank()) return null
        return GifSearchItem(
            id = id,
            title = title.ifBlank { id },
            previewUrl = preview,
            sendUrl = send,
            width = images.original?.width?.toIntOrNull() ?: 0,
            height = images.original?.height?.toIntOrNull() ?: 0
        )
    }
}

class GiphyKeyMissingException : Exception("GIPHY API key is not configured")

class GiphyRequestException(val status: Int, override val message: String) : Exception(message)
