package io.rudione.chatone.data.remote

import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.rudione.chatone.domain.follow.FollowedChannel
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlin.time.Instant

data class FollowedChannelsPage(val channels: List<FollowedChannel>, val nextCursor: String?)

class TwitchFollowsClient(
    private val httpClient: HttpClient,
    private val transport: ViewerGqlTransport,
    private val helixClientId: String
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun viewerFollows(gqlToken: String, cursor: String?): FollowedChannelsPage? {
        val variables = buildJsonObject {
            put("first", PAGE_SIZE)
            if (cursor.isNullOrBlank()) put("after", JsonNull) else put("after", cursor)
        }
        val reply = transport.signedIn(gqlToken, VIEWER_FOLLOWS_QUERY, variables) as? GqlReply.Data ?: return null
        val follows = reply.data.obj("currentUser")?.obj("follows") ?: return null
        val edges = follows.arr("edges").objects()
        val channels = edges.mapNotNull { edge ->
            val node = edge.obj("node") ?: return@mapNotNull null
            node.toFollowedChannel(followedAt = edge.str("followedAt"))
        }
        val hasNext = follows.obj("pageInfo")?.bool("hasNextPage") == true
        return FollowedChannelsPage(channels, edges.lastOrNull()?.str("cursor")?.takeIf { hasNext && it.isNotBlank() })
    }

    suspend fun helixFollows(userId: String, helixToken: String, cursor: String?): FollowedChannelsPage? = try {
        val response = httpClient.get(HELIX_FOLLOWED_ENDPOINT) {
            header("Authorization", "Bearer $helixToken")
            header("Client-Id", helixClientId)
            parameter("user_id", userId)
            parameter("first", PAGE_SIZE)
            if (!cursor.isNullOrBlank()) parameter("after", cursor)
        }
        if (!response.status.isSuccess()) {
            Napier.w("Followed channels HTTP ${response.status.value}", tag = TAG)
            null
        } else {
            val root = json.parseToJsonElement(response.bodyAsText()) as? JsonObject
            val stubs = root?.arr("data")?.objects().orEmpty().mapNotNull { item ->
                val login = item.str("broadcaster_login")?.lowercase()?.takeIf { LOGIN.matches(it) }
                    ?: return@mapNotNull null
                FollowedChannel(
                    id = item.str("broadcaster_id").orEmpty(),
                    login = login,
                    displayName = item.str("broadcaster_name")?.takeIf { it.isNotBlank() } ?: login,
                    avatarUrl = "",
                    followedAtMs = item.str("followed_at").epochMs(),
                    liveViewers = null,
                    liveCategory = null
                )
            }
            FollowedChannelsPage(enrich(stubs), root?.obj("pagination")?.str("cursor")?.takeIf { it.isNotBlank() })
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Napier.w("Followed channels failed: ${e::class.simpleName}", tag = TAG)
        null
    }

    private suspend fun enrich(stubs: List<FollowedChannel>): List<FollowedChannel> {
        if (stubs.isEmpty()) return stubs
        val variables = buildJsonObject { putJsonArray("logins") { stubs.forEach { add(it.login) } } }
        val reply = transport.anonymous(USERS_QUERY, variables) as? GqlReply.Data ?: return stubs
        val details = reply.data.arr("users").objects().mapNotNull { user ->
            user.toFollowedChannel(followedAt = null)
        }.associateBy { it.login }
        return stubs.map { stub ->
            details[stub.login]?.let { it.copy(id = stub.id.ifEmpty { it.id }, followedAtMs = stub.followedAtMs) } ?: stub
        }
    }

    private fun JsonObject.toFollowedChannel(followedAt: String?): FollowedChannel? {
        val login = str("login")?.lowercase()?.takeIf { LOGIN.matches(it) } ?: return null
        val stream = obj("stream")
        return FollowedChannel(
            id = str("id").orEmpty(),
            login = login,
            displayName = str("displayName")?.takeIf { it.isNotBlank() } ?: login,
            avatarUrl = str("profileImageURL")?.takeIf { it.startsWith("https://") }.orEmpty(),
            followedAtMs = followedAt.epochMs(),
            liveViewers = stream?.let { it.int("viewersCount") ?: 0 },
            liveCategory = stream?.obj("game")?.str("displayName")
        )
    }

    private fun String?.epochMs(): Long? =
        this?.let { runCatching { Instant.parse(it).toEpochMilliseconds() }.getOrNull() }

    private companion object {
        const val TAG = "TwitchFollows"
        const val PAGE_SIZE = 100
        const val HELIX_FOLLOWED_ENDPOINT = "https://api.twitch.tv/helix/channels/followed"
        val LOGIN = Regex("^[a-z0-9_]{1,25}$")
        const val CHANNEL_FIELDS =
            "id login displayName profileImageURL(width: 70) stream { viewersCount game { displayName } }"
        const val VIEWER_FOLLOWS_QUERY =
            "query ChatoneViewerFollows(\$first: Int, \$after: Cursor) { currentUser { " +
                    "follows(first: \$first, after: \$after, order: DESC) { " +
                    "edges { cursor followedAt node { $CHANNEL_FIELDS } } pageInfo { hasNextPage } } } }"
        const val USERS_QUERY =
            "query ChatoneFollowedUsers(\$logins: [String!]) { users(logins: \$logins) { $CHANNEL_FIELDS } }"
    }
}
