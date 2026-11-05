package io.rudione.chatone.data.remote

import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.http.parameters
import io.rudione.chatone.domain.points.WatchStreak
import io.rudione.chatone.domain.points.WatchStreakReward
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import kotlin.io.encoding.Base64

data class WatchBroadcast(
    val channelId: String,
    val broadcastId: String,
    val gameId: String?,
    val gameName: String?
)

data class MinuteWatched(
    val userId: String,
    val channelLogin: String,
    val broadcast: WatchBroadcast
)

class TwitchWatchClient(
    private val httpClient: HttpClient,
    private val transport: ViewerGqlTransport
) {
    private val spadeLock = Mutex()
    private var spadeUrl: String? = null

    suspend fun streak(channelLogin: String, token: String): WatchStreak? {
        val variables = buildJsonObject { put("login", channelLogin.lowercase()) }
        val reply = transport.signedIn(token, STREAK_QUERY, variables) as? GqlReply.Data ?: return null
        val channel = reply.data.obj("user")?.obj("channel") ?: return null
        val rewards = channel.obj("communityPointsSettings")?.obj("earning")?.arr("watchStreakPoints").orEmpty()
            .mapNotNull { element ->
                val reward = element as? JsonObject ?: return@mapNotNull null
                val length = reward.int("streakLength") ?: return@mapNotNull null
                val points = reward.int("points") ?: return@mapNotNull null
                WatchStreakReward(length, points)
            }
        val current = channel.obj("self")?.arr("viewerMilestones").orEmpty()
            .mapNotNull { it as? JsonObject }
            .firstOrNull { it.str("category")?.contains(STREAK_CATEGORY, ignoreCase = true) == true }
            ?.int("value") ?: 0
        return WatchStreak(current = current, rewards = rewards)
    }

    suspend fun broadcast(channelLogin: String): WatchBroadcast? {
        val variables = buildJsonObject { put("login", channelLogin.lowercase()) }
        val reply = transport.anonymous(BROADCAST_QUERY, variables) as? GqlReply.Data ?: return null
        val user = reply.data.obj("user") ?: return null
        val stream = user.obj("stream") ?: return null
        val game = stream.obj("game")
        return WatchBroadcast(
            channelId = user.str("id") ?: return null,
            broadcastId = stream.str("id") ?: return null,
            gameId = game?.str("id"),
            gameName = game?.str("displayName")
        )
    }

    suspend fun reportMinute(event: MinuteWatched): Boolean = try {
        val payload = buildJsonArray {
            add(buildJsonObject {
                put("event", MINUTE_WATCHED_EVENT)
                putJsonObject("properties") {
                    put("channel_id", event.broadcast.channelId)
                    put("broadcast_id", event.broadcast.broadcastId)
                    put("player", PLAYER)
                    put("user_id", event.userId)
                    put("live", true)
                    put("channel", event.channelLogin.lowercase())
                    event.broadcast.gameName?.let { put("game", it) }
                    event.broadcast.gameId?.let { put("game_id", it) }
                }
            })
        }
        val encoded = Base64.encode(payload.toString().encodeToByteArray())
        val response = httpClient.submitForm(url = resolveSpadeUrl(), formParameters = parameters { append("data", encoded) }) {
            header("User-Agent", TwitchFirstPartyClient.WEB.userAgent)
        }
        response.status.isSuccess().also { ok ->
            if (!ok) Napier.w("minute-watched HTTP ${response.status.value}", tag = TAG)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Napier.w("minute-watched failed: ${e::class.simpleName}", tag = TAG)
        false
    }

    private suspend fun resolveSpadeUrl(): String = spadeLock.withLock {
        spadeUrl ?: (lookupSpadeUrl() ?: DEFAULT_SPADE_URL).also { spadeUrl = it }
    }

    private suspend fun lookupSpadeUrl(): String? = try {
        val page = httpClient.get(TWITCH_HOME) { header("User-Agent", TwitchFirstPartyClient.WEB.userAgent) }
            .takeIf { it.status.isSuccess() }?.bodyAsText()
        val settingsUrl = page?.let { SETTINGS_SCRIPT.find(it)?.value }
        val settings = settingsUrl?.let { url ->
            httpClient.get(url).takeIf { it.status.isSuccess() }?.bodyAsText()
        }
        settings?.let { SPADE_URL.find(it)?.groupValues?.get(1) }?.takeIf { it.startsWith("https://") }
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        null
    }

    private fun JsonArray?.orEmpty() = this ?: JsonArray(emptyList())

    private companion object {
        const val TAG = "TwitchWatch"
        const val STREAK_CATEGORY = "STREAK"
        const val MINUTE_WATCHED_EVENT = "minute-watched"
        const val PLAYER = "site"
        const val TWITCH_HOME = "https://www.twitch.tv/"
        const val DEFAULT_SPADE_URL = "https://spade.twitch.tv/track"
        val SETTINGS_SCRIPT = Regex("https://(?:static\\.twitchcdn\\.net|assets\\.twitch\\.tv)/config/settings\\.[a-f0-9]+\\.js")
        val SPADE_URL = Regex("\"spade_url\":\"([^\"]+)\"")
        const val STREAK_QUERY =
            "query ChatoneWatchStreak(\$login: String!) { user(login: \$login) { id channel { " +
                    "self { viewerMilestones { category value } } " +
                    "communityPointsSettings { earning { watchStreakPoints { streakLength points } } } } } }"
        const val BROADCAST_QUERY =
            "query ChatoneWatchBroadcast(\$login: String!) { user(login: \$login) { id stream { id game { id displayName } } } }"
    }
}
