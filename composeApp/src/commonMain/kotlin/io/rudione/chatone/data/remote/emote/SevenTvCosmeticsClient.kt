package io.rudione.chatone.data.remote.emote

import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.rudione.chatone.data.remote.dto.*
import io.rudione.chatone.domain.model.SevenTvCosmetics
import io.rudione.chatone.domain.model.SevenTvUserCosmetic
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Client for 7TV cosmetics API - loads paints and badges for users.
 * Caches cosmetics by user ID for efficient lookups during message rendering.
 */
class SevenTvCosmeticsClient(private val httpClient: HttpClient) {
    companion object {
        private const val TAG = "7TV-Cosmetics"
        private const val BASE_URL = "https://7tv.io/v3"
    }

    // Cache: twitchUserId -> SevenTvUserCosmetic
    private val userCosmeticsCache = mutableMapOf<String, SevenTvUserCosmetic>()
    private val cacheMutex = Mutex()

    // Batch lookup: we look up users by their Twitch IDs
    suspend fun getUserCosmetics(twitchUserId: String): SevenTvUserCosmetic? {
        cacheMutex.withLock {
            userCosmeticsCache[twitchUserId]?.let { return it }
        }

        return try {
            val response = httpClient.get("$BASE_URL/users/twitch/$twitchUserId")
                .body<SevenTvUserConnection>()
            val user = response.user ?: return null
            val style = user.style

            val cosmetic = SevenTvUserCosmetic(
                sevenTvId = user.id,
                paint = style.paint?.toCosmetic(),
                badge = style.badge?.toCosmetic(),
                nameColor = style.color
            )

            cacheMutex.withLock {
                userCosmeticsCache[twitchUserId] = cosmetic
            }

            cosmetic
        } catch (e: Exception) {
            Napier.w("Failed to get 7TV cosmetics for user $twitchUserId: ${e.message}", tag = TAG)
            null
        }
    }

    fun getCachedCosmetics(twitchUserId: String): SevenTvUserCosmetic? {
        return userCosmeticsCache[twitchUserId]
    }

    fun clearCache() {
        userCosmeticsCache.clear()
    }

    private fun SevenTvPaint.toCosmetic(): SevenTvCosmetics.Paint {
        return SevenTvCosmetics.Paint(
            id = id,
            name = name,
            function = function,
            color = color,
            stops = stops.map { stop ->
                SevenTvCosmetics.PaintStop(
                    at = stop.at.toFloat(),
                    color = stop.color
                )
            },
            repeat = repeat,
            angle = angle,
            imageUrl = imageUrl,
            shadows = shadows.map { shadow ->
                SevenTvCosmetics.PaintShadow(
                    xOffset = shadow.xOffset.toFloat(),
                    yOffset = shadow.yOffset.toFloat(),
                    radius = shadow.radius.toFloat(),
                    color = shadow.color
                )
            }
        )
    }

    private fun SevenTvBadge.toCosmetic(): SevenTvCosmetics.Badge {
        val baseUrl = "https:${host.url}"
        val webpFiles = host.files.filter { it.format == "WEBP" }
        val url1x = webpFiles.find { it.name.contains("1x") }?.let { "$baseUrl/${it.name}" }
        val url2x = webpFiles.find { it.name.contains("2x") }?.let { "$baseUrl/${it.name}" }
        val url3x = (webpFiles.find { it.name.contains("3x") } ?: webpFiles.find { it.name.contains("4x") })
            ?.let { "$baseUrl/${it.name}" }

        return SevenTvCosmetics.Badge(
            id = id,
            name = name,
            tooltip = tooltip,
            url1x = url1x ?: url2x ?: "",
            url2x = url2x ?: url1x ?: "",
            url3x = url3x ?: url2x ?: url1x ?: ""
        )
    }
}
