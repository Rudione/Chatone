package io.rudione.chatone.domain.browse

import androidx.compose.runtime.Immutable

@Immutable
data class BrowseCategory(
    val id: String,
    val name: String,
    val boxArtUrl: String,
    val viewers: Int,
    val broadcasters: Int
)

@Immutable
data class BrowseStream(
    val id: String,
    val login: String,
    val displayName: String,
    val avatarUrl: String,
    val title: String,
    val viewers: Int,
    val startedAtMs: Long?,
    val previewUrl: String,
    val language: String,
    val tags: List<String>,
    val isMature: Boolean = false
)

@Immutable
data class BrowseChannel(
    val login: String,
    val displayName: String,
    val avatarUrl: String,
    val liveViewers: Int?,
    val liveCategory: String?
) {
    val isLive: Boolean get() = liveViewers != null
}

@Immutable
data class BrowsePage<T>(val items: List<T>, val nextCursor: String?) {
    val hasMore: Boolean get() = nextCursor != null
}

@Immutable
data class BrowseSearchResult(
    val categories: List<BrowseCategory>,
    val channels: List<BrowseChannel>
)

enum class StreamSort { VIEWERS_HIGH, VIEWERS_LOW, RECENT }

@Immutable
data class StreamFilter(
    val sort: StreamSort = StreamSort.VIEWERS_HIGH,
    val language: String? = null,
    val tag: String? = null,
    val minViewers: Int = 0,
    val hideMature: Boolean = false
) {
    val isDefault: Boolean get() = this == StreamFilter()

    val activeCount: Int
        get() = listOf(
            sort != StreamSort.VIEWERS_HIGH,
            language != null,
            tag != null,
            minViewers > 0,
            hideMature
        ).count { it }

    fun accepts(stream: BrowseStream): Boolean =
        stream.viewers >= minViewers && !(hideMature && stream.isMature)

    fun isExhaustedBy(stream: BrowseStream): Boolean =
        sort == StreamSort.VIEWERS_HIGH && minViewers > 0 && stream.viewers < minViewers

    companion object {
        val LANGUAGES: List<String> = listOf("RU", "EN", "UK", "ES", "DE", "FR", "PT", "KO", "JA")
        val MIN_VIEWER_STEPS: List<Int> = listOf(0, 10, 50, 100, 1_000, 10_000)
    }
}

data class FollowedAccount(val userId: String, val token: String)

interface FollowedAccountProvider {
    val activeAccountId: kotlinx.coroutines.flow.StateFlow<String>
    suspend fun account(activeId: String): FollowedAccount?
}

interface StreamDirectory {
    suspend fun topCategories(cursor: String?): BrowsePage<BrowseCategory>?

    suspend fun categoryStreams(categoryId: String, filter: StreamFilter, cursor: String?): BrowsePage<BrowseStream>?

    suspend fun topStreams(filter: StreamFilter, cursor: String?): BrowsePage<BrowseStream>?

    suspend fun followedStreams(userId: String, accessToken: String, cursor: String?): BrowsePage<BrowseStream>?

    suspend fun search(query: String): BrowseSearchResult?
}
