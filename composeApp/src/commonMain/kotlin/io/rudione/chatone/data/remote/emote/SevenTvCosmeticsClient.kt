package io.rudione.chatone.data.remote.emote

import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.rudione.chatone.data.remote.dto.*
import io.rudione.chatone.domain.model.SevenTvCosmetics
import io.rudione.chatone.domain.model.SevenTvUserCosmetic
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.async

class SevenTvCosmeticsClient(
    private val httpClient: HttpClient,
    private val scope: CoroutineScope
) {
    companion object {
        private const val TAG = "7TV-Cosmetics"
        private const val BASE_URL = "https://7tv.io/v3"

        private val GQL_USER_COSMETICS = """
            query GetUserCosmetics(${'$'}id: String!) {
              user(id: ${'$'}id) {
                id
                style {
                  color
                  badge {
                    id
                    name
                    tooltip
                    tag
                    host { url files { name format width height } }
                  }
                  paint {
                    id
                    name
                    function
                    color
                    stops { at color }
                    repeat
                    angle
                    shadows { x_offset y_offset radius color }
                  }
                }
              }
            }
        """.trimIndent()
    }

    private val userCosmeticsCache = mutableMapOf<String, SevenTvUserCosmetic>()
    private val inflight = mutableMapOf<String, Deferred<SevenTvUserCosmetic?>>()

    @OptIn(InternalCoroutinesApi::class)
    private val cacheLock = SynchronizedObject()

    @OptIn(InternalCoroutinesApi::class)
    private fun readCached(twitchUserId: String): SevenTvUserCosmetic? =
        synchronized(cacheLock) { userCosmeticsCache[twitchUserId] }

    @OptIn(InternalCoroutinesApi::class)
    private fun writeCached(twitchUserId: String, value: SevenTvUserCosmetic) {
        synchronized(cacheLock) { userCosmeticsCache[twitchUserId] = value }
    }

    @OptIn(InternalCoroutinesApi::class)
    suspend fun getUserCosmetics(twitchUserId: String): SevenTvUserCosmetic? {
        val request = synchronized(cacheLock) {
            userCosmeticsCache[twitchUserId]?.let { cached ->
                return if (cached.sevenTvId.isEmpty()) null else cached
            }
            inflight[twitchUserId] ?: scope.async { fetchUserCosmetics(twitchUserId) }.also { started ->
                inflight[twitchUserId] = started
                started.invokeOnCompletion {
                    synchronized(cacheLock) {
                        if (inflight[twitchUserId] === started) inflight.remove(twitchUserId)
                    }
                }
            }
        }
        return request.await()
    }

    private suspend fun fetchUserCosmetics(twitchUserId: String): SevenTvUserCosmetic? {
        return try {

            val response = httpClient.get("$BASE_URL/users/twitch/$twitchUserId")
                .body<SevenTvUserResponse>()

            val inlineUser = response.user
            if (inlineUser == null) {
                writeCached(twitchUserId, SevenTvUserCosmetic(sevenTvId = "", paint = null, badge = null, nameColor = null))
                return null
            }

            val sevenTvId = inlineUser.id
            val inlineStyle = inlineUser.style

            val hasBadge = inlineStyle.badge != null || !inlineStyle.badgeId.isNullOrEmpty()
            val hasPaint = inlineStyle.paint != null || !inlineStyle.paintId.isNullOrEmpty()

            if (!hasBadge && !hasPaint && inlineStyle.color == null) {
                writeCached(twitchUserId, SevenTvUserCosmetic(sevenTvId = sevenTvId, paint = null, badge = null, nameColor = null))
                return null
            }

            val inlineBadge = inlineStyle.badge?.toCosmetic()
            val inlinePaint = inlineStyle.paint?.toCosmetic()

            val badgeReady = inlineBadge != null || !hasBadge
            val paintReady = inlinePaint != null || !hasPaint

            val cosmetic = if (badgeReady && paintReady) {
                Napier.d("7TV: inline cosmetics for $twitchUserId badge=${inlineBadge?.name} paint=${inlinePaint?.name}", tag = TAG)
                SevenTvUserCosmetic(sevenTvId = sevenTvId, paint = inlinePaint, badge = inlineBadge, nameColor = inlineStyle.color)
            } else {

                Napier.d("7TV: requesting GQL for $twitchUserId (7tv=$sevenTvId) badge_id=${inlineStyle.badgeId} paint_id=${inlineStyle.paintId}", tag = TAG)
                val gqlResult = fetchViaGql(sevenTvId, inlineStyle.color)

                gqlResult ?: run {

                    val badge = inlineBadge ?: inlineStyle.badgeId?.let { loadBadgeById(it) }
                    val paint = inlinePaint ?: inlineStyle.paintId?.let { loadPaintById(it) }
                    SevenTvUserCosmetic(sevenTvId = sevenTvId, paint = paint, badge = badge, nameColor = inlineStyle.color)
                }
            }

            writeCached(twitchUserId, cosmetic)
            Napier.d("7TV final cosmetics for $twitchUserId: badge=${cosmetic.badge?.name} paint=${cosmetic.paint?.name} color=${cosmetic.nameColor}", tag = TAG)
            return cosmetic.takeIf { it.badge != null || it.paint != null || it.nameColor != null }

        } catch (e: Exception) {
            Napier.w("7TV cosmetics failed for $twitchUserId: ${e.message}", tag = TAG)
            null
        }
    }

    private suspend fun fetchViaGql(sevenTvId: String, fallbackColor: Int?): SevenTvUserCosmetic? {
        return try {
            val resp = httpClient.post("$BASE_URL/gql") {
                contentType(ContentType.Application.Json)
                setBody(SevenTvGqlRequest(query = GQL_USER_COSMETICS, variables = mapOf("id" to sevenTvId)))
            }.body<SevenTvGqlResponse>()

            if (!resp.errors.isNullOrEmpty()) {
                Napier.w("7TV GQL errors for $sevenTvId: ${resp.errors.joinToString { it.message }}", tag = TAG)
            }

            val user = resp.data?.user ?: return null
            val style = user.style
            SevenTvUserCosmetic(
                sevenTvId = sevenTvId,
                paint = style.paint?.toCosmetic(),
                badge = style.badge?.toCosmetic(),
                nameColor = style.color ?: fallbackColor
            )
        } catch (e: Exception) {
            Napier.w("7TV GQL request failed for $sevenTvId: ${e.message}", tag = TAG)
            null
        }
    }

    private suspend fun loadBadgeById(badgeId: String): SevenTvCosmetics.Badge? {
        return try {
            val badge = httpClient.get("$BASE_URL/cosmetics/$badgeId").body<SevenTvBadge>()
            badge.toCosmetic()
        } catch (e: Exception) {
            Napier.w("7TV badge load failed id=$badgeId: ${e.message}", tag = TAG)
            null
        }
    }

    private suspend fun loadPaintById(paintId: String): SevenTvCosmetics.Paint? {
        return try {
            val paint = httpClient.get("$BASE_URL/cosmetics/$paintId").body<SevenTvPaint>()
            paint.toCosmetic()
        } catch (e: Exception) {
            Napier.w("7TV paint load failed id=$paintId: ${e.message}", tag = TAG)
            null
        }
    }

    fun getCachedCosmetics(twitchUserId: String): SevenTvUserCosmetic? =
        readCached(twitchUserId)?.takeIf { it.sevenTvId.isNotEmpty() }

    @OptIn(InternalCoroutinesApi::class)
    fun clearCache() {
        synchronized(cacheLock) { userCosmeticsCache.clear() }
    }

    private fun SevenTvPaint.toCosmetic() = SevenTvCosmetics.Paint(
        id = id, name = name, function = function, color = color,
        stops = stops.map { SevenTvCosmetics.PaintStop(it.at.toFloat(), it.color) },
        repeat = repeat, angle = angle, imageUrl = imageUrl,
        shadows = shadows.map { SevenTvCosmetics.PaintShadow(it.xOffset.toFloat(), it.yOffset.toFloat(), it.radius.toFloat(), it.color) }
    )

    private fun SevenTvBadge.toCosmetic(): SevenTvCosmetics.Badge {
        val rawUrl = host.url
        val baseUrl = when {
            rawUrl.startsWith("https://") || rawUrl.startsWith("http://") -> rawUrl
            rawUrl.startsWith("//") -> "https:$rawUrl"
            rawUrl.isNotBlank() -> "https://$rawUrl"
            else -> "https://cdn.7tv.app"
        }
        val webp = host.files.filter { it.format.uppercase() == "WEBP" }.ifEmpty { host.files }
        fun find(vararg parts: String) = parts.firstNotNullOfOrNull { p -> webp.find { it.name.contains(p, true) }?.let { "$baseUrl/${it.name}" } }
        val u1 = find("1x") ?: webp.firstOrNull()?.let { "$baseUrl/${it.name}" } ?: ""
        val u2 = find("2x") ?: u1
        val u3 = find("3x", "4x") ?: u2

        Napier.d("7TV badge toCosmetic: id=$id name=$name url2x=$u2 files=${host.files.map { "${it.name}(${it.format})" }}", tag = TAG)
        return SevenTvCosmetics.Badge(id = id, name = name, tooltip = tooltip, url1x = u1, url2x = u2, url3x = u3)
    }
}
