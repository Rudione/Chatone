package io.rudione.chatone.data.remote

import io.rudione.chatone.domain.profile.ChannelClip
import io.rudione.chatone.domain.profile.ChannelLink
import io.rudione.chatone.domain.profile.ChannelPanel
import io.rudione.chatone.domain.profile.ChannelProfile
import io.rudione.chatone.domain.profile.ChannelProfileSource
import io.rudione.chatone.domain.profile.ChannelVideo
import io.rudione.chatone.domain.profile.ClipOrder
import io.rudione.chatone.domain.profile.ClipPeriod
import io.rudione.chatone.domain.profile.ClipQuery
import io.rudione.chatone.domain.profile.LiveBroadcast
import io.rudione.chatone.domain.profile.MediaPage
import io.rudione.chatone.domain.profile.PastBroadcast
import io.rudione.chatone.domain.profile.VideoKind
import io.rudione.chatone.domain.profile.VideoOrder
import io.rudione.chatone.domain.profile.VideoQuery
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import kotlin.time.Instant

class TwitchChannelProfileClient(private val transport: ViewerGqlTransport) : ChannelProfileSource {

    override suspend fun profile(login: String): ChannelProfile? {
        val normalized = login.normalizedLogin() ?: return null
        val reply = transport.anonymous(PROFILE_QUERY, buildJsonObject { put("login", normalized) })
        val user = (reply as? GqlReply.Data)?.data?.obj("user") ?: return null
        val id = user.str("id")?.takeIf { it.isNotBlank() } ?: return null
        val roles = user.obj("roles")
        val settings = user.obj("broadcastSettings")
        return ChannelProfile(
            id = id,
            login = user.str("login")?.lowercase() ?: normalized,
            displayName = user.str("displayName")?.takeIf { it.isNotBlank() } ?: normalized,
            description = user.str("description").orEmpty().trim().take(MAX_BIO),
            avatarUrl = user.str("profileImageURL").https(),
            bannerUrl = user.str("bannerImageURL").https(),
            offlineImageUrl = user.str("offlineImageURL").https(),
            accentHex = user.str("primaryColorHex")?.takeIf { HEX.matches(it) },
            chatColorHex = user.str("chatColor")?.removePrefix("#")?.takeIf { HEX.matches(it) },
            createdAtMs = user.str("createdAt").epochMs(),
            followers = user.obj("followers")?.int("totalCount"),
            isPartner = roles?.bool("isPartner") == true,
            isAffiliate = roles?.bool("isAffiliate") == true,
            isStaff = roles?.bool("isStaff") == true,
            teamName = user.obj("primaryTeam")?.let { it.str("displayName") ?: it.str("name") }?.takeIf { it.isNotBlank() },
            language = settings?.str("language")?.takeIf { it.isNotBlank() },
            links = user.obj("channel")?.arr("socialMedias")?.objects().orEmpty().mapNotNull { link ->
                val url = link.str("url")?.takeIf { it.startsWith("https://") || it.startsWith("http://") }
                    ?: return@mapNotNull null
                ChannelLink(title = link.str("title")?.takeIf { it.isNotBlank() } ?: link.str("name").orEmpty(), url = url)
            },
            panels = user.arr("panels").objects().mapNotNull { panel ->
                val title = panel.str("title").orEmpty().trim()
                val description = panel.str("description").orEmpty().trim().take(MAX_PANEL_TEXT)
                val image = panel.str("imageURL").https()
                if (title.isEmpty() && description.isEmpty() && image.isEmpty()) return@mapNotNull null
                ChannelPanel(title, description, image, panel.str("linkURL").https())
            },
            live = user.obj("stream")?.let { stream ->
                LiveBroadcast(
                    broadcastId = stream.str("id")?.takeIf { ID.matches(it) },
                    title = stream.str("title").orEmpty(),
                    viewers = stream.int("viewersCount") ?: 0,
                    startedAtMs = stream.str("createdAt").epochMs(),
                    category = stream.obj("game")?.str("displayName").orEmpty(),
                    categoryArtUrl = stream.obj("game")?.str("boxArtURL").https(),
                    previewUrl = stream.str("previewImageURL").https(),
                    tags = stream.arr("freeformTags").objects().mapNotNull { it.str("name") }
                )
            },
            lastBroadcast = user.obj("lastBroadcast")?.let { last ->
                val started = last.str("startedAt").epochMs() ?: return@let null
                PastBroadcast(started, last.str("title").orEmpty(), last.obj("game")?.str("displayName").orEmpty())
            },
            pastBroadcastCount = user.obj("videos")?.int("totalCount") ?: 0
        )
    }

    override suspend fun videos(login: String, query: VideoQuery, cursor: String?): MediaPage<ChannelVideo>? {
        val normalized = login.normalizedLogin() ?: return null
        val variables = buildJsonObject {
            put("login", normalized)
            put("first", PAGE_SIZE)
            put("sort", if (query.order == VideoOrder.POPULAR) "VIEWS" else "TIME")
            val kind = query.kind
            if (kind == null) put("type", JsonNull) else put("type", kind.name)
            if (cursor.isNullOrBlank()) put("after", JsonNull) else put("after", cursor)
        }
        val reply = transport.anonymous(VIDEOS_QUERY, variables) as? GqlReply.Data ?: return null
        val connection = reply.data.obj("user")?.obj("videos") ?: return MediaPage(emptyList(), null)
        return connection.page { node -> node.toVideo() }
    }

    override suspend fun clips(login: String, query: ClipQuery, cursor: String?): MediaPage<ChannelClip>? {
        val normalized = login.normalizedLogin() ?: return null
        val variables = buildJsonObject {
            put("login", normalized)
            put("first", PAGE_SIZE)
            if (cursor.isNullOrBlank()) put("after", JsonNull) else put("after", cursor)
            putJsonObject("criteria") {
                put("period", query.period.gqlName)
                put("sort", if (query.order == ClipOrder.TRENDING) "TRENDING" else "VIEWS_DESC")
                if (query.featuredOnly) put("isFeatured", true)
            }
        }
        val reply = transport.anonymous(CLIPS_QUERY, variables) as? GqlReply.Data ?: return null
        val connection = reply.data.obj("user")?.obj("clips") ?: return MediaPage(emptyList(), null)
        return connection.page { node -> node.toClip() }
    }

    private fun <T> JsonObject.page(map: (JsonObject) -> T?): MediaPage<T> {
        val edges = arr("edges").objects()
        val items = edges.mapNotNull { edge -> edge.obj("node")?.let(map) }
        val hasNext = obj("pageInfo")?.bool("hasNextPage") == true
        val cursor = edges.lastOrNull()?.str("cursor")?.takeIf { hasNext && it.isNotBlank() }
        return MediaPage(items, cursor)
    }

    private fun JsonObject.toVideo(): ChannelVideo? {
        val id = str("id")?.takeIf { ID.matches(it) } ?: return null
        val game = obj("game")
        return ChannelVideo(
            id = id,
            title = str("title").orEmpty().take(MAX_TITLE),
            lengthSeconds = int("lengthSeconds") ?: 0,
            views = int("viewCount") ?: 0,
            createdAtMs = (str("publishedAt") ?: str("createdAt")).epochMs(),
            thumbnailUrl = str("previewThumbnailURL").https().takeUnless { it.contains("/_404/") }.orEmpty(),
            category = game?.str("displayName").orEmpty(),
            categoryArtUrl = game?.str("boxArtURL").https(),
            kind = when (str("broadcastType")) {
                "HIGHLIGHT" -> VideoKind.HIGHLIGHT
                "UPLOAD" -> VideoKind.UPLOAD
                else -> VideoKind.ARCHIVE
            }
        )
    }

    private fun JsonObject.toClip(): ChannelClip? {
        val slug = str("slug")?.takeIf { it.isNotBlank() } ?: return null
        return ChannelClip(
            id = str("id").orEmpty(),
            slug = slug,
            title = str("title").orEmpty().take(MAX_TITLE),
            views = int("viewCount") ?: 0,
            createdAtMs = str("createdAt").epochMs(),
            durationSeconds = int("durationSeconds") ?: 0,
            thumbnailUrl = str("thumbnailURL").https(),
            curator = obj("curator")?.str("displayName").orEmpty(),
            category = obj("game")?.str("displayName").orEmpty(),
            url = str("url").https().ifEmpty { "https://clips.twitch.tv/$slug" },
            isFeatured = bool("isFeatured")
        )
    }

    private val ClipPeriod.gqlName: String
        get() = when (this) {
            ClipPeriod.DAY -> "LAST_DAY"
            ClipPeriod.WEEK -> "LAST_WEEK"
            ClipPeriod.MONTH -> "LAST_MONTH"
            ClipPeriod.ALL -> "ALL_TIME"
        }

    private fun String.normalizedLogin(): String? = trim().removePrefix("#").lowercase().takeIf { LOGIN.matches(it) }

    private fun String?.https(): String = this?.takeIf { it.startsWith("https://") }.orEmpty()

    private fun String?.epochMs(): Long? =
        this?.let { runCatching { Instant.parse(it).toEpochMilliseconds() }.getOrNull() }

    private companion object {
        const val PAGE_SIZE = 30
        const val MAX_BIO = 600
        const val MAX_TITLE = 200
        const val MAX_PANEL_TEXT = 2_000
        val LOGIN = Regex("^[a-z0-9_]{1,25}$")
        val ID = Regex("^[0-9]{1,20}$")
        val HEX = Regex("^[0-9A-Fa-f]{6}$")

        const val PROFILE_QUERY =
            "query ChatoneChannelProfile(\$login: String!) { user(login: \$login) { " +
                    "id login displayName description createdAt bannerImageURL offlineImageURL primaryColorHex " +
                    "chatColor profileImageURL(width: 300) followers { totalCount } " +
                    "roles { isPartner isAffiliate isStaff } primaryTeam { name displayName } " +
                    "channel { socialMedias { name title url } } broadcastSettings { language } " +
                    "stream { id title viewersCount createdAt previewImageURL(width: 640, height: 360) " +
                    "freeformTags { name } game { displayName boxArtURL(width: 144, height: 192) } } " +
                    "lastBroadcast { startedAt title game { displayName } } " +
                    "videos(first: 1, type: ARCHIVE) { totalCount } " +
                    "panels { ... on DefaultPanel { title description imageURL linkURL } } } }"

        const val VIDEOS_QUERY =
            "query ChatoneChannelVideos(\$login: String!, \$first: Int, \$after: Cursor, " +
                    "\$type: BroadcastType, \$sort: VideoSort) { user(login: \$login) { " +
                    "videos(first: \$first, after: \$after, type: \$type, sort: \$sort) { edges { cursor node { " +
                    "id title lengthSeconds viewCount createdAt publishedAt broadcastType " +
                    "previewThumbnailURL(width: 640, height: 360) " +
                    "game { displayName boxArtURL(width: 144, height: 192) } } } pageInfo { hasNextPage } } } }"

        const val CLIPS_QUERY =
            "query ChatoneChannelClips(\$login: String!, \$first: Int, \$after: Cursor, " +
                    "\$criteria: UserClipsInput) { user(login: \$login) { " +
                    "clips(first: \$first, after: \$after, criteria: \$criteria) { edges { cursor node { " +
                    "id slug title viewCount createdAt durationSeconds isFeatured url " +
                    "thumbnailURL(width: 480, height: 272) curator { displayName } game { displayName } } } " +
                    "pageInfo { hasNextPage } } } }"
    }
}
