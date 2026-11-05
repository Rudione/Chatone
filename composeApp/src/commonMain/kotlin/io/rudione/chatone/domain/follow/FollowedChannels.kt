package io.rudione.chatone.domain.follow

import androidx.compose.runtime.Immutable

@Immutable
data class FollowedChannel(
    val id: String,
    val login: String,
    val displayName: String,
    val avatarUrl: String,
    val followedAtMs: Long?,
    val liveViewers: Int?,
    val liveCategory: String?
) {
    val isLive: Boolean get() = liveViewers != null
}

sealed interface FollowedChannelsResult {
    data class Loaded(val channels: List<FollowedChannel>) : FollowedChannelsResult
    data object SignedOut : FollowedChannelsResult
    data object Failed : FollowedChannelsResult
}

interface FollowedChannelsSource {
    suspend fun followedChannels(forceRefresh: Boolean = false): FollowedChannelsResult
}

object FollowedChannelOrder {
    fun sort(channels: List<FollowedChannel>): List<FollowedChannel> = channels.sortedWith(
        compareByDescending<FollowedChannel> { it.isLive }
            .thenByDescending { it.liveViewers ?: 0 }
            .thenByDescending { it.followedAtMs ?: 0L }
            .thenBy { it.login }
    )

    fun filter(channels: List<FollowedChannel>, query: String): List<FollowedChannel> {
        val needle = query.trim().removePrefix("#").lowercase()
        if (needle.isEmpty()) return channels
        return channels.filter {
            it.login.contains(needle) || it.displayName.lowercase().contains(needle) ||
                    it.liveCategory?.lowercase()?.contains(needle) == true
        }
    }
}
