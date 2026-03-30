package io.rudione.chatone.data.remote.emote

import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.rudione.chatone.data.remote.dto.*
import io.rudione.chatone.domain.model.SevenTvCosmetics
import io.rudione.chatone.domain.model.SevenTvUserCosmetic
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class SevenTvCosmeticsClient(private val httpClient: HttpClient) {
    companion object {
        private const val TAG = "7TV-Cosmetics"
        private const val BASE_URL = "https://7tv.io/v3"
    }

    private val userCosmeticsCache = mutableMapOf<String, SevenTvUserCosmetic>()
    private val cacheMutex = Mutex()

    suspend fun getUserCosmetics(twitchUserId: String): SevenTvUserCosmetic? {
        cacheMutex.withLock {
            userCosmeticsCache[twitchUserId]?.let { return it }
        }

        return try {
            val response = httpClient.get("$BASE_URL/users/twitch/$twitchUserId")
                .body<SevenTvUserConnection>()

            // user может быть null если у юзера нет аккаунта 7TV
            val user = response.user ?: run {
                // Кэшируем пустой результат чтобы не делать повторные запросы
                val empty = SevenTvUserCosmetic(sevenTvId = "", paint = null, badge = null, nameColor = null)
                cacheMutex.withLock { userCosmeticsCache[twitchUserId] = empty }
                return null
            }

            val style = user.style

            // Бейдж: сначала берём из style.badge если есть,
            // иначе пробуем загрузить по badge_id
            val badge = style.badge?.toCosmetic()
                ?: style.badgeId?.let { badgeId ->
                    loadBadgeById(badgeId)
                }

            val cosmetic = SevenTvUserCosmetic(
                sevenTvId = user.id,
                paint = style.paint?.toCosmetic(),
                badge = badge,
                nameColor = style.color
            )

            cacheMutex.withLock {
                userCosmeticsCache[twitchUserId] = cosmetic
            }

            if (cosmetic.paint != null || cosmetic.badge != null) cosmetic else null
        } catch (e: Exception) {
            Napier.w("Failed to get 7TV cosmetics for user $twitchUserId: ${e.message}", tag = TAG)
            null
        }
    }

    private suspend fun loadBadgeById(badgeId: String): SevenTvCosmetics.Badge? {
        return try {
            val badge = httpClient.get("$BASE_URL/cosmetics/$badgeId").body<SevenTvBadge>()
            badge.toCosmetic()
        } catch (_: Exception) {
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
                SevenTvCosmetics.PaintStop(at = stop.at.toFloat(), color = stop.color)
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