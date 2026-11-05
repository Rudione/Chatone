package io.rudione.chatone.data.remote

import io.ktor.http.encodeURLParameter
import io.rudione.chatone.domain.media.MediaCatalog
import io.rudione.chatone.domain.media.MediaDetails
import io.rudione.chatone.domain.media.MediaKind
import io.rudione.chatone.domain.media.MediaPerson
import io.rudione.chatone.domain.media.MediaSource
import io.rudione.chatone.domain.media.ReplayBadge
import io.rudione.chatone.domain.media.ReplayEmote
import io.rudione.chatone.domain.media.ReplayLine
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.time.Instant

class TwitchMediaClient(private val transport: ViewerGqlTransport) : MediaCatalog {

    override suspend fun clip(slug: String): MediaDetails? {
        if (!SLUG.matches(slug)) return null
        val reply = transport.anonymous(CLIP_QUERY, buildJsonObject { put("slug", slug) }) as? GqlReply.Data ?: return null
        val clip = reply.data.obj("clip") ?: return null
        val sources = clipSources(clip)
        val broadcaster = clip.obj("broadcaster")?.person() ?: return null
        return MediaDetails(
            kind = MediaKind.CLIP,
            id = slug,
            title = clip.str("title").orEmpty(),
            views = clip.int("viewCount") ?: 0,
            createdAtMs = clip.str("createdAt").epochMs(),
            durationSeconds = clip.int("durationSeconds") ?: 0,
            thumbnailUrl = clip.str("thumbnailURL").https(),
            pageUrl = clip.str("url").https().ifEmpty { "https://clips.twitch.tv/$slug" },
            broadcaster = broadcaster,
            curator = clip.obj("curator")?.person(),
            category = clip.obj("game")?.str("displayName").orEmpty(),
            replayVideoId = clip.obj("video")?.str("id"),
            replayOffsetSeconds = clip.int("videoOffsetSeconds") ?: 0,
            sources = sources
        )
    }

    override suspend fun video(id: String): MediaDetails? {
        if (!VIDEO_ID.matches(id)) return null
        val reply = transport.anonymous(VIDEO_QUERY, buildJsonObject { put("id", id) }) as? GqlReply.Data ?: return null
        val video = reply.data.obj("video") ?: return null
        val token = video.obj("playbackAccessToken")
        val signature = token?.str("signature") ?: return null
        val value = token.str("value") ?: return null
        val playlist = "https://usher.ttvnw.net/vod/$id.m3u8?allow_source=true&player=twitchweb" +
                "&playlist_include_framerate=true&sig=${signature.encodeURLParameter()}&token=${value.encodeURLParameter()}"
        return MediaDetails(
            kind = MediaKind.VIDEO,
            id = id,
            title = video.str("title").orEmpty(),
            views = video.int("viewCount") ?: 0,
            createdAtMs = video.str("createdAt").epochMs(),
            durationSeconds = video.int("lengthSeconds") ?: 0,
            thumbnailUrl = video.str("previewThumbnailURL").https(),
            pageUrl = "https://www.twitch.tv/videos/$id",
            broadcaster = video.obj("owner")?.person() ?: return null,
            curator = null,
            category = video.obj("game")?.str("displayName").orEmpty(),
            replayVideoId = id,
            replayOffsetSeconds = 0,
            sources = listOf(MediaSource(label = "", url = playlist, isStream = true))
        )
    }

    override suspend fun replay(videoId: String, fromSeconds: Int): List<ReplayLine>? {
        if (!VIDEO_ID.matches(videoId)) return null
        val variables = buildJsonObject {
            put("id", videoId)
            put("offset", fromSeconds.coerceAtLeast(0))
        }
        val reply = transport.anonymous(REPLAY_QUERY, variables) as? GqlReply.Data ?: return null
        val comments = reply.data.obj("video")?.obj("comments") ?: return emptyList()
        return comments.arr("edges").objects().mapNotNull { edge ->
            val node = edge.obj("node") ?: return@mapNotNull null
            val commenter = node.obj("commenter")
            val message = node.obj("message") ?: return@mapNotNull null
            val body = replayBody(message) ?: return@mapNotNull null
            val login = commenter?.str("login").orEmpty()
            val offset = node.int("contentOffsetSeconds") ?: 0
            ReplayLine(
                id = node.str("id")?.takeIf { it.isNotBlank() } ?: "$offset:$login:${body.first.hashCode()}",
                offsetSeconds = offset,
                userId = commenter?.str("id").orEmpty(),
                login = login,
                displayName = commenter?.str("displayName")?.takeIf { it.isNotBlank() } ?: login,
                colorHex = message.str("userColor")?.takeIf { it.startsWith("#") },
                text = body.first,
                badges = message.arr("userBadges").objects().mapNotNull { badge ->
                    val setId = badge.str("setID")?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                    ReplayBadge(setId, badge.str("version").orEmpty())
                },
                emotes = body.second
            )
        }
    }

    private fun replayBody(message: JsonObject): Pair<String, List<ReplayEmote>>? {
        val text = StringBuilder()
        val emotes = mutableListOf<ReplayEmote>()
        for (fragment in message.arr("fragments").objects()) {
            val piece = fragment.str("text").orEmpty()
            if (piece.isEmpty()) continue
            val room = MAX_LINE - text.length
            if (room <= 0) break
            val emoteId = fragment.obj("emote")?.str("emoteID")?.takeIf { it.isNotBlank() }
            if (emoteId != null && piece.length <= room) {
                emotes += ReplayEmote(emoteId, text.length, text.length + piece.length - 1)
            }
            text.append(piece.take(room))
        }
        val body = text.trimEnd().toString()
        if (body.isBlank()) return null
        return body to emotes.filter { it.end < body.length }
    }

    internal fun clipSources(clip: JsonObject, qualities: String = VIDEO_QUALITIES): List<MediaSource> {
        val token = clip.obj("playbackAccessToken")
        val signature = token?.str("signature").orEmpty()
        val value = token?.str("value").orEmpty()
        return clip.arr(qualities).objects().mapNotNull { quality ->
            val url = quality.str("sourceURL")?.takeIf { it.startsWith("https://") } ?: return@mapNotNull null
            val label = quality.str("quality").orEmpty()
            val signed = if (signature.isNotEmpty()) {
                val separator = if ('?' in url) '&' else '?'
                "$url${separator}sig=${signature.encodeURLParameter()}&token=${value.encodeURLParameter()}"
            } else url
            MediaSource(label = if (label.isNotEmpty()) "${label}p" else label, url = signed, isStream = false)
        }.sortedByDescending { it.label.removeSuffix("p").toIntOrNull() ?: 0 }
    }

    private fun JsonObject.person(): MediaPerson? {
        val login = str("login")?.lowercase() ?: return null
        return MediaPerson(
            id = str("id").orEmpty(),
            login = login,
            displayName = str("displayName")?.takeIf { it.isNotBlank() } ?: login,
            avatarUrl = str("profileImageURL").https()
        )
    }

    private fun String?.https(): String = this?.takeIf { it.startsWith("https://") }.orEmpty()

    private fun String?.epochMs(): Long? = this?.let { runCatching { Instant.parse(it).toEpochMilliseconds() }.getOrNull() }

    internal companion object {
        private const val MAX_LINE = 500
        private val SLUG = Regex("^[A-Za-z0-9_-]{3,100}$")
        private val VIDEO_ID = Regex("^[0-9]{1,20}$")
        private const val PERSON = "id login displayName profileImageURL(width: 70)"
        internal const val VIDEO_QUALITIES = "videoQualities"
        internal const val RAW_VIDEO_QUALITIES = "rawVideoQualities"
        internal const val SOURCES_FIELDS =
            "playbackAccessToken(params: {platform: \"web\", playerBackend: \"mediaplayer\", playerType: \"site\"}) { signature value } " +
                    "videoQualities { quality sourceURL } rawVideoQualities { quality sourceURL }"
        private const val TOKEN =
            "playbackAccessToken(params: {platform: \"web\", playerBackend: \"mediaplayer\", playerType: \"site\"}) { signature value }"
        private const val CLIP_QUERY =
            "query ChatoneClip(\$slug: ID!) { clip(slug: \$slug) { id slug title viewCount createdAt durationSeconds " +
                    "videoOffsetSeconds url thumbnailURL(width: 480, height: 272) curator { $PERSON } broadcaster { $PERSON } " +
                    "game { displayName } video { id } $TOKEN videoQualities { quality sourceURL } } }"
        private const val VIDEO_QUERY =
            "query ChatoneVideo(\$id: ID!) { video(id: \$id) { id title lengthSeconds viewCount createdAt " +
                    "previewThumbnailURL(width: 640, height: 360) owner { $PERSON } game { displayName } $TOKEN } }"
        private const val REPLAY_QUERY =
            "query ChatoneReplay(\$id: ID!, \$offset: Int) { video(id: \$id) { comments(contentOffsetSeconds: \$offset) { " +
                    "edges { node { id contentOffsetSeconds commenter { id login displayName } " +
                    "message { userColor userBadges { setID version } fragments { text emote { emoteID } } } } } } } }"
    }
}
