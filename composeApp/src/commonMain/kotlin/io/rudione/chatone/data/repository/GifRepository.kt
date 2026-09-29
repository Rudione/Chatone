package io.rudione.chatone.data.repository

import com.russhwolf.settings.Settings
import io.github.aakira.napier.Napier
import io.rudione.chatone.data.remote.gif.GiphyApiClient
import io.rudione.chatone.domain.model.GifSearchItem
import io.rudione.chatone.domain.model.GifSearchPage
import io.rudione.chatone.util.Result
import io.rudione.chatone.util.security.getSecret
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.Json

class GifRepository(
    private val giphyApi: GiphyApiClient,
    private val settings: Settings
) {
    companion object {
        private const val TAG = "GifRepository"
        private const val KEY_FAVORITES = "favorite_gifs"
        private const val KEY_API_KEY = "giphy_api_key"
        private const val MAX_FAVORITES = 200
        private const val CACHE_LIMIT = 24
    }

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private val _favorites = MutableStateFlow(loadFavorites())
    val favorites: StateFlow<List<GifSearchItem>> = _favorites

    private val pageCache = LinkedHashMap<String, GifSearchPage>()

    fun apiKey(): String = settings.getSecret(KEY_API_KEY).trim()

    fun hasApiKey(): Boolean = apiKey().isNotEmpty()

    suspend fun load(query: String, offset: Int): Result<GifSearchPage> {
        val trimmed = query.trim()
        val key = "$trimmed@$offset"
        pageCache[key]?.let { return Result.Success(it) }

        val result = if (trimmed.isEmpty()) {
            giphyApi.trending(apiKey(), offset)
        } else {
            giphyApi.search(apiKey(), trimmed, offset)
        }
        if (result is Result.Success) {
            pageCache[key] = result.data
            while (pageCache.size > CACHE_LIMIT) {
                pageCache.remove(pageCache.keys.first())
            }
        }
        return result
    }

    fun invalidateCache() {
        pageCache.clear()
    }

    fun isFavorite(id: String): Boolean = _favorites.value.any { it.id == id }

    fun toggleFavorite(item: GifSearchItem) {
        val current = _favorites.value
        val updated = if (current.any { it.id == item.id }) {
            current.filterNot { it.id == item.id }
        } else {
            (listOf(item) + current).take(MAX_FAVORITES)
        }
        _favorites.value = updated
        persistFavorites(updated)
    }

    private fun loadFavorites(): List<GifSearchItem> {
        val raw = settings.getStringOrNull(KEY_FAVORITES) ?: return emptyList()
        return try {
            json.decodeFromString<List<GifSearchItem>>(raw)
        } catch (e: Exception) {
            Napier.w("Failed to read favorite GIFs: ${e.message}", tag = TAG)
            emptyList()
        }
    }

    private fun persistFavorites(items: List<GifSearchItem>) {
        try {
            settings.putString(KEY_FAVORITES, json.encodeToString(items))
        } catch (e: Exception) {
            Napier.w("Failed to persist favorite GIFs: ${e.message}", tag = TAG)
        }
    }
}
