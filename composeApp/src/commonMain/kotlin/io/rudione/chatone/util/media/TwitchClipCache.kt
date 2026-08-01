package io.rudione.chatone.util.media

import io.rudione.chatone.data.remote.TwitchApiClient
import io.rudione.chatone.util.Result
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class TwitchClipInfo(
    val title: String,
    val thumbnailUrl: String,
    val broadcasterName: String,
    val creatorName: String,
    val gameName: String?,
    val viewCount: Int,
    val durationSeconds: Double,
    val createdAt: String
)

object TwitchClipCache {
    private val CLIP_URL_REGEX = Regex(
        """clips\.twitch\.tv/(?:embed\?clip=)?([A-Za-z0-9_-]+)|twitch\.tv/[^/\s]+/clip/([A-Za-z0-9_-]+)""",
        RegexOption.IGNORE_CASE
    )

    private val cache = mutableMapOf<String, TwitchClipInfo>()
    private val gameNameCache = mutableMapOf<String, String>()
    private val mutex = Mutex()

    fun extractSlug(url: String): String? {
        val match = CLIP_URL_REGEX.find(url) ?: return null
        return match.groupValues[1].ifBlank { match.groupValues[2] }.takeIf { it.isNotBlank() }
    }

    fun cached(slug: String): TwitchClipInfo? = cache[slug]

    suspend fun fetch(apiClient: TwitchApiClient, accessToken: String, slug: String): TwitchClipInfo? {
        cache[slug]?.let { return it }
        if (accessToken.isBlank()) return null
        return mutex.withLock {
            cache[slug]?.let { return it }
            val result = apiClient.getClipsBySlugs(accessToken, listOf(slug))
            val clip = (result as? Result.Success)?.data?.data?.firstOrNull() ?: return null

            val gameName = clip.gameId.takeIf { it.isNotBlank() }?.let { gameId ->
                gameNameCache[gameId] ?: run {
                    val gamesResult = apiClient.getGames(accessToken, listOf(gameId))
                    (gamesResult as? Result.Success)?.data?.data?.firstOrNull()?.name
                        ?.also { gameNameCache[gameId] = it }
                }
            }

            val info = TwitchClipInfo(
                title = clip.title,
                thumbnailUrl = clip.thumbnailUrl,
                broadcasterName = clip.broadcasterName,
                creatorName = clip.creatorName,
                gameName = gameName,
                viewCount = clip.viewCount,
                durationSeconds = clip.duration,
                createdAt = clip.createdAt
            )
            cache[slug] = info
            info
        }
    }
}
