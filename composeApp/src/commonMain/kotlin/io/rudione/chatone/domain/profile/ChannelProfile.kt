package io.rudione.chatone.domain.profile

import androidx.compose.runtime.Immutable

@Immutable
data class ChannelProfile(
    val id: String,
    val login: String,
    val displayName: String,
    val description: String,
    val avatarUrl: String,
    val bannerUrl: String,
    val offlineImageUrl: String,
    val accentHex: String?,
    val chatColorHex: String?,
    val createdAtMs: Long?,
    val followers: Int?,
    val isPartner: Boolean,
    val isAffiliate: Boolean,
    val isStaff: Boolean,
    val teamName: String?,
    val language: String?,
    val links: List<ChannelLink>,
    val panels: List<ChannelPanel>,
    val live: LiveBroadcast?,
    val lastBroadcast: PastBroadcast?,
    val pastBroadcastCount: Int
) {
    val isLive: Boolean get() = live != null
}

@Immutable
data class ChannelLink(val title: String, val url: String)

@Immutable
data class ChannelPanel(val title: String, val description: String, val imageUrl: String, val linkUrl: String)

@Immutable
data class LiveBroadcast(
    val broadcastId: String?,
    val title: String,
    val viewers: Int,
    val startedAtMs: Long?,
    val category: String,
    val categoryArtUrl: String,
    val previewUrl: String,
    val tags: List<String>
)

@Immutable
data class PastBroadcast(val startedAtMs: Long, val title: String, val category: String)

enum class VideoKind { ARCHIVE, HIGHLIGHT, UPLOAD }

enum class VideoOrder { NEWEST, POPULAR }

enum class ClipPeriod { DAY, WEEK, MONTH, ALL }

enum class ClipOrder { TOP, TRENDING }

@Immutable
data class VideoQuery(val kind: VideoKind? = null, val order: VideoOrder = VideoOrder.NEWEST) {
    val isDefault: Boolean get() = this == VideoQuery()
}

@Immutable
data class ClipQuery(
    val period: ClipPeriod = ClipPeriod.ALL,
    val order: ClipOrder = ClipOrder.TOP,
    val featuredOnly: Boolean = false
) {
    val isDefault: Boolean get() = this == ClipQuery()
}

@Immutable
data class ChannelVideo(
    val id: String,
    val title: String,
    val lengthSeconds: Int,
    val views: Int,
    val createdAtMs: Long?,
    val thumbnailUrl: String,
    val category: String,
    val categoryArtUrl: String,
    val kind: VideoKind
) {
    val url: String get() = "https://www.twitch.tv/videos/$id"
}

@Immutable
data class ChannelClip(
    val id: String,
    val slug: String,
    val title: String,
    val views: Int,
    val createdAtMs: Long?,
    val durationSeconds: Int,
    val thumbnailUrl: String,
    val curator: String,
    val category: String,
    val url: String,
    val isFeatured: Boolean
)

@Immutable
data class MediaPage<T>(val items: List<T>, val nextCursor: String?) {
    val hasMore: Boolean get() = nextCursor != null
}

@Immutable
data class CategoryShare(val name: String, val artUrl: String, val streams: Int)

interface ChannelProfileSource {
    suspend fun profile(login: String): ChannelProfile?
    suspend fun videos(login: String, query: VideoQuery, cursor: String?): MediaPage<ChannelVideo>?
    suspend fun clips(login: String, query: ClipQuery, cursor: String?): MediaPage<ChannelClip>?
}
