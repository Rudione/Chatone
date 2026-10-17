package io.rudione.chatone.data.repository

import com.russhwolf.settings.Settings
import io.github.aakira.napier.Napier
import io.rudione.chatone.data.remote.gif.GiphyApiClient
import io.rudione.chatone.domain.model.ChannelGifConfig
import io.rudione.chatone.domain.model.ChannelGifConfigSource
import io.rudione.chatone.domain.model.GifAccess
import io.rudione.chatone.domain.model.GifSearchConfig
import io.rudione.chatone.domain.model.GifSearchItem
import io.rudione.chatone.domain.model.GifSearchPage
import io.rudione.chatone.util.Result
import io.rudione.chatone.util.security.getSecret
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlin.time.Clock

class GifRepository(
    private val giphyApi: GiphyApiClient,
    private val settings: Settings,
    private val channelConfigs: ChannelGifConfigSource,
    private val clock: Clock = Clock.System,
    private val personalKey: () -> String = { settings.getSecret(KEY_API_KEY).trim() }
) {
    companion object {
        private const val TAG = "GifRepository"
        private const val KEY_FAVORITES = "favorite_gifs"
        private const val KEY_API_KEY = "giphy_api_key"
        private const val MAX_FAVORITES = 200
        private const val CACHE_LIMIT = 24
        private const val CHANNEL_CONFIG_TTL_MS = 10 * 60_000L
        private const val MAX_CACHED_CHANNELS = 32
        const val PERSONAL_KEY_RATING = "pg-13"
    }

    private class CachedChannelConfig(val config: ChannelGifConfig, val loadedAtMs: Long)

    private data class PageKey(val config: GifSearchConfig, val query: String, val offset: Int)

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private val _favorites = MutableStateFlow(loadFavorites())
    val favorites: StateFlow<List<GifSearchItem>> = _favorites

    private val pageCache = LinkedHashMap<PageKey, GifSearchPage>()
    private val channelCache = LinkedHashMap<String, CachedChannelConfig>()
    private val channelLock = Mutex()

    suspend fun access(channelId: String, token: String): GifAccess {
        val channel = channelConfig(channelId, token)
        val ownKey = personalKey()
        return when {
            channel != null && channel.enabled && channel.apiKey.isNotEmpty() ->
                GifAccess.Ready(GifSearchConfig(channel.apiKey, channel.rating), fromChannel = true)

            channel != null && !channel.enabled -> GifAccess.Disabled

            ownKey.isNotEmpty() ->
                GifAccess.Ready(GifSearchConfig(ownKey, PERSONAL_KEY_RATING), fromChannel = false)

            else -> GifAccess.NeedsKey
        }
    }

    suspend fun load(config: GifSearchConfig, query: String, offset: Int): Result<GifSearchPage> {
        val trimmed = query.trim()
        val key = PageKey(config, trimmed, offset)
        pageCache[key]?.let { return Result.Success(it) }

        val result = if (trimmed.isEmpty()) {
            giphyApi.trending(config, offset)
        } else {
            giphyApi.search(config, trimmed, offset)
        }
        if (result is Result.Success) {
            pageCache[key] = result.data
            while (pageCache.size > CACHE_LIMIT) {
                pageCache.remove(pageCache.keys.first())
            }
        }
        return result
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

    private suspend fun channelConfig(channelId: String, token: String): ChannelGifConfig? {
        if (channelId.isBlank() || token.isBlank()) return null
        val now = clock.now().toEpochMilliseconds()
        channelLock.withLock {
            channelCache[channelId]?.takeIf { now - it.loadedAtMs < CHANNEL_CONFIG_TTL_MS }?.let { return it.config }
        }
        val loaded = when (val result = channelConfigs.channelGifConfig(channelId, token)) {
            is Result.Success -> result.data
            is Result.Error -> {
                Napier.w("GIF picker config unavailable: ${result.exception::class.simpleName}", tag = TAG)
                return null
            }
            else -> return null
        }
        channelLock.withLock {
            channelCache.remove(channelId)
            channelCache[channelId] = CachedChannelConfig(loaded, now)
            while (channelCache.size > MAX_CACHED_CHANNELS) channelCache.remove(channelCache.keys.first())
        }
        return loaded
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
