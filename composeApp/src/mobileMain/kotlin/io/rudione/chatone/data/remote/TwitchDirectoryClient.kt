package io.rudione.chatone.data.remote

import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.content.TextContent
import io.ktor.http.isSuccess
import io.rudione.chatone.domain.browse.BrowseCategory
import io.rudione.chatone.domain.browse.BrowseChannel
import io.rudione.chatone.domain.browse.BrowsePage
import io.rudione.chatone.domain.browse.BrowseSearchResult
import io.rudione.chatone.domain.browse.BrowseStream
import io.rudione.chatone.domain.browse.StreamDirectory
import io.rudione.chatone.domain.browse.StreamFilter
import io.rudione.chatone.domain.browse.StreamSort
import io.rudione.chatone.util.settings.AppConfig
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import kotlin.time.Clock
import kotlin.time.Instant

class TwitchDirectoryClient(
    private val httpClient: HttpClient,
    private val clock: Clock = Clock.System
) : StreamDirectory {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun topCategories(cursor: String?): BrowsePage<BrowseCategory>? {
        val data = query(TOP_CATEGORIES_QUERY) {
            put("first", CATEGORY_PAGE_SIZE)
            putCursor(cursor)
        } ?: return null
        val connection = data.obj("games") ?: return null
        return connection.page { node -> node.toCategory() }
    }

    override suspend fun categoryStreams(
        categoryId: String,
        filter: StreamFilter,
        cursor: String?
    ): BrowsePage<BrowseStream>? {
        if (!ID_PATTERN.matches(categoryId)) return null
        val data = query(CATEGORY_STREAMS_QUERY) {
            put("id", categoryId)
            put("first", STREAM_PAGE_SIZE)
            putCursor(cursor)
            putJsonObject("options") {
                put("sort", filter.sort.gqlName)
                filter.language?.takeIf { it in StreamFilter.LANGUAGES }?.let { language ->
                    putJsonArray("broadcasterLanguages") { add(language) }
                }
                filter.tag?.trim()?.take(MAX_TAG_LENGTH)?.takeIf { it.isNotEmpty() }?.let { tag ->
                    putJsonArray("freeformTags") { add(tag) }
                }
            }
        } ?: return null
        val connection = data.obj("game")?.obj("streams") ?: return BrowsePage(emptyList(), null)
        return connection.page { node -> node.toStream() }
    }

    override suspend fun topStreams(filter: StreamFilter, cursor: String?): BrowsePage<BrowseStream>? {
        val data = query(TOP_STREAMS_QUERY) {
            put("first", STREAM_PAGE_SIZE)
            putCursor(cursor)
            putJsonObject("options") {
                put("sort", filter.sort.gqlName)
            }
        } ?: return null
        val connection = data.obj("streams") ?: return BrowsePage(emptyList(), null)
        return connection.page { node -> node.toStream() }
    }

    override suspend fun followedStreams(
        userId: String,
        accessToken: String,
        cursor: String?
    ): BrowsePage<BrowseStream>? {
        if (!ID_PATTERN.matches(userId) || accessToken.isBlank()) return null
        return try {
            val response = httpClient.get(FOLLOWED_STREAMS_ENDPOINT) {
                header("Authorization", "Bearer $accessToken")
                header("Client-Id", AppConfig.TWITCH_CLIENT_ID)
                parameter("user_id", userId)
                parameter("first", STREAM_PAGE_SIZE)
                if (!cursor.isNullOrBlank()) parameter("after", cursor)
            }
            if (!response.status.isSuccess()) {
                Napier.w("Followed streams request failed: HTTP ${response.status.value}", tag = TAG)
                return null
            }
            val root = json.parseToJsonElement(response.bodyAsText()) as? JsonObject ?: return null
            val items = (root["data"] as? JsonArray).orEmpty()
                .mapNotNull { (it as? JsonObject)?.toHelixStream() }
            val next = root.obj("pagination")?.string("cursor")?.takeIf { it.isNotEmpty() }
            BrowsePage(items, next)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Napier.w("Followed streams request failed: ${e::class.simpleName}", tag = TAG)
            null
        }
    }

    override suspend fun search(query: String): BrowseSearchResult? {
        val trimmed = query.trim().take(MAX_QUERY_LENGTH)
        if (trimmed.isEmpty()) return BrowseSearchResult(emptyList(), emptyList())
        val data = query(SEARCH_QUERY) {
            put("query", trimmed)
            put("first", SEARCH_PAGE_SIZE)
        } ?: return null
        return BrowseSearchResult(
            categories = data.obj("searchCategories")?.nodes()?.mapNotNull { it.toCategory() }.orEmpty(),
            channels = data.obj("searchUsers")?.nodes()?.mapNotNull { it.toChannel() }.orEmpty()
        )
    }

    private suspend fun query(text: String, variables: JsonObjectBuilder.() -> Unit): JsonObject? = try {
        val payload = buildJsonObject {
            put("query", text)
            putJsonObject("variables", variables)
        }
        val response = httpClient.post(GQL_ENDPOINT) {
            header("Client-Id", DIRECTORY_CLIENT.clientId)
            header("User-Agent", DIRECTORY_CLIENT.userAgent)
            DIRECTORY_CLIENT.origin?.let { header("Origin", it) }
            DIRECTORY_CLIENT.referer?.let { header("Referer", it) }
            setBody(TextContent(payload.toString(), ContentType.Application.Json))
        }
        if (!response.status.isSuccess()) {
            Napier.w("Directory request failed: HTTP ${response.status.value}", tag = TAG)
            null
        } else {
            (json.parseToJsonElement(response.bodyAsText()) as? JsonObject)?.obj("data")
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Napier.w("Directory request failed: ${e::class.simpleName}", tag = TAG)
        null
    }

    private fun <T> JsonObject.page(map: (JsonObject) -> T?): BrowsePage<T> {
        val edges = (this["edges"] as? JsonArray).orEmpty().mapNotNull { it as? JsonObject }
        val items = edges.mapNotNull { edge -> edge.obj("node")?.let(map) }
        val hasNext = (obj("pageInfo")?.get("hasNextPage") as? JsonPrimitive)?.booleanOrNull == true
        val cursor = edges.lastOrNull()?.string("cursor")?.takeIf { hasNext && it.isNotEmpty() }
        return BrowsePage(items, cursor)
    }

    private fun JsonObject.nodes(): List<JsonObject> =
        (this["edges"] as? JsonArray).orEmpty().mapNotNull { (it as? JsonObject)?.obj("node") }

    private fun JsonObject.toCategory(): BrowseCategory? {
        val id = string("id")?.takeIf { ID_PATTERN.matches(it) } ?: return null
        val name = (string("displayName") ?: string("name"))?.takeIf { it.isNotBlank() } ?: return null
        return BrowseCategory(
            id = id,
            name = name.take(MAX_TEXT_LENGTH),
            boxArtUrl = string("boxArtURL").httpsOrEmpty(),
            viewers = int("viewersCount") ?: 0,
            broadcasters = int("broadcastersCount") ?: 0
        )
    }

    private fun JsonObject.toStream(): BrowseStream? {
        val broadcaster = obj("broadcaster") ?: return null
        val login = broadcaster.string("login")?.lowercase()?.takeIf { LOGIN_PATTERN.matches(it) } ?: return null
        val tags = (this["freeformTags"] as? JsonArray).orEmpty()
            .mapNotNull { (it as? JsonObject)?.string("name")?.trim()?.take(MAX_TAG_LENGTH) }
            .filter { it.isNotEmpty() }
        return BrowseStream(
            id = string("id").orEmpty(),
            login = login,
            displayName = broadcaster.string("displayName")?.takeIf { it.isNotBlank() } ?: login,
            avatarUrl = broadcaster.string("profileImageURL").httpsOrEmpty(),
            title = string("title").orEmpty().take(MAX_TEXT_LENGTH),
            viewers = int("viewersCount") ?: 0,
            startedAtMs = string("createdAt")?.let { runCatching { Instant.parse(it).toEpochMilliseconds() }.getOrNull() },
            previewUrl = string("previewImageURL").httpsOrEmpty().withPreviewBucket(),
            language = string("language").orEmpty().uppercase(),
            tags = tags,
            isMature = (this["isMature"] as? JsonPrimitive)?.booleanOrNull == true
        )
    }

    private fun JsonObject.toHelixStream(): BrowseStream? {
        val login = string("user_login")?.lowercase()?.takeIf { LOGIN_PATTERN.matches(it) } ?: return null
        val tags = (this["tags"] as? JsonArray).orEmpty()
            .mapNotNull { (it as? JsonPrimitive)?.contentOrNull?.trim()?.take(MAX_TAG_LENGTH) }
            .filter { it.isNotEmpty() }
        return BrowseStream(
            id = string("id").orEmpty(),
            login = login,
            displayName = string("user_name")?.takeIf { it.isNotBlank() } ?: login,
            avatarUrl = "",
            title = string("title").orEmpty().take(MAX_TEXT_LENGTH),
            viewers = int("viewer_count") ?: 0,
            startedAtMs = string("started_at")?.let { runCatching { Instant.parse(it).toEpochMilliseconds() }.getOrNull() },
            previewUrl = string("thumbnail_url").helixThumb(),
            language = string("language").orEmpty().uppercase(),
            tags = tags,
            isMature = (this["is_mature"] as? JsonPrimitive)?.booleanOrNull == true
        )
    }

    private fun String?.helixThumb(): String {
        val url = this?.takeIf { it.startsWith("https://") } ?: return ""
        return url.replace("{width}", "440").replace("{height}", "248").withPreviewBucket()
    }

    private fun JsonObject.toChannel(): BrowseChannel? {
        val login = string("login")?.lowercase()?.takeIf { LOGIN_PATTERN.matches(it) } ?: return null
        val stream = obj("stream")
        return BrowseChannel(
            login = login,
            displayName = string("displayName")?.takeIf { it.isNotBlank() } ?: login,
            avatarUrl = string("profileImageURL").httpsOrEmpty(),
            liveViewers = stream?.let { it.int("viewersCount") ?: 0 },
            liveCategory = stream?.obj("game")?.string("displayName")?.take(MAX_TEXT_LENGTH)
        )
    }

    private fun String.withPreviewBucket(): String {
        if (isEmpty()) return this
        val bucket = clock.now().toEpochMilliseconds() / PREVIEW_REFRESH_MS
        return "$this?b=$bucket"
    }

    private fun String?.httpsOrEmpty(): String = this?.takeIf { it.startsWith("https://") }.orEmpty()

    private fun JsonObjectBuilder.putCursor(cursor: String?) {
        if (cursor.isNullOrBlank()) put("after", JsonNull) else put("after", cursor)
    }

    private fun JsonObject.obj(key: String): JsonObject? = this[key] as? JsonObject

    private fun JsonObject.string(key: String): String? = (this[key] as? JsonPrimitive)?.contentOrNull

    private fun JsonObject.int(key: String): Int? = (this[key] as? JsonPrimitive)?.intOrNull

    private val StreamSort.gqlName: String
        get() = when (this) {
            StreamSort.VIEWERS_HIGH -> "VIEWER_COUNT"
            StreamSort.VIEWERS_LOW -> "VIEWER_COUNT_ASC"
            StreamSort.RECENT -> "RECENT"
        }

    private companion object {
        const val TAG = "TwitchDirectory"
        val DIRECTORY_CLIENT = TwitchFirstPartyClient.TV
        const val GQL_ENDPOINT = "https://gql.twitch.tv/gql"
        const val FOLLOWED_STREAMS_ENDPOINT = "https://api.twitch.tv/helix/streams/followed"
        const val CATEGORY_PAGE_SIZE = 30
        const val STREAM_PAGE_SIZE = 24
        const val SEARCH_PAGE_SIZE = 10
        const val MAX_QUERY_LENGTH = 100
        const val MAX_TAG_LENGTH = 25
        const val MAX_TEXT_LENGTH = 200
        const val PREVIEW_REFRESH_MS = 300_000L
        val LOGIN_PATTERN = Regex("^[a-z0-9_]{1,25}$")
        val ID_PATTERN = Regex("^[0-9A-Za-z_]{1,32}$")

        const val TOP_CATEGORIES_QUERY =
            "query ChatoneTopCategories(\$first: Int, \$after: Cursor) { " +
                    "games(first: \$first, after: \$after, options: {sort: VIEWER_COUNT}) { " +
                    "edges { cursor node { id name displayName viewersCount broadcastersCount " +
                    "boxArtURL(width: 285, height: 380) } } pageInfo { hasNextPage } } }"

        const val TOP_STREAMS_QUERY =
            "query ChatoneTopStreams(\$first: Int, \$after: Cursor, \$options: StreamOptions) { " +
                    "streams(first: \$first, after: \$after, options: \$options) { " +
                    "edges { cursor node { id title viewersCount createdAt language isMature " +
                    "previewImageURL(width: 440, height: 248) freeformTags { name } " +
                    "broadcaster { login displayName profileImageURL(width: 70) } } } " +
                    "pageInfo { hasNextPage } } }"

        const val CATEGORY_STREAMS_QUERY =
            "query ChatoneCategoryStreams(\$id: ID!, \$first: Int, \$after: Cursor, \$options: GameStreamOptions) { " +
                    "game(id: \$id) { streams(first: \$first, after: \$after, options: \$options) { " +
                    "edges { cursor node { id title viewersCount createdAt language isMature " +
                    "previewImageURL(width: 440, height: 248) freeformTags { name } " +
                    "broadcaster { login displayName profileImageURL(width: 70) } } } " +
                    "pageInfo { hasNextPage } } } }"

        const val SEARCH_QUERY =
            "query ChatoneBrowseSearch(\$query: String!, \$first: Int) { " +
                    "searchCategories(query: \$query, first: \$first) { edges { node { id name displayName " +
                    "viewersCount broadcastersCount boxArtURL(width: 285, height: 380) } } } " +
                    "searchUsers(userQuery: \$query, first: \$first) { edges { node { login displayName " +
                    "profileImageURL(width: 70) stream { viewersCount game { displayName } } } } } }"
    }
}
