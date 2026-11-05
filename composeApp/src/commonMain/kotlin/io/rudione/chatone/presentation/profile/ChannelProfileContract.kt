package io.rudione.chatone.presentation.profile

import androidx.compose.runtime.Immutable
import io.rudione.chatone.base.UIEffect
import io.rudione.chatone.base.UiEvent
import io.rudione.chatone.base.UiState
import io.rudione.chatone.data.remote.AccountBadge
import io.rudione.chatone.data.remote.NameHistoryEntry
import io.rudione.chatone.domain.profile.ChannelClip
import io.rudione.chatone.domain.profile.ChannelProfile
import io.rudione.chatone.domain.profile.ChannelVideo
import io.rudione.chatone.domain.profile.ClipQuery
import io.rudione.chatone.domain.profile.StreamCalendar
import io.rudione.chatone.domain.profile.VideoQuery

enum class ProfileTab { HOME, ABOUT, CLIPS, VIDEOS, DOSSIER, ROLES }

enum class ProfileLoad { LOADING, READY, NOT_FOUND, FAILED }

enum class FollowStatus { UNKNOWN, FOLLOWING, NOT_FOLLOWING, BUSY, UNAVAILABLE }

@Immutable
data class MediaFeed<T>(
    val items: List<T> = emptyList(),
    val cursor: String? = null,
    val loading: Boolean = false,
    val failed: Boolean = false,
    val loaded: Boolean = false
) {
    val canLoadMore: Boolean get() = loaded && cursor != null && !loading && !failed
}

@Immutable
data class ChannelProfileState(
    val login: String = "",
    val load: ProfileLoad = ProfileLoad.LOADING,
    val profile: ChannelProfile? = null,
    val isOwn: Boolean = false,
    val tab: ProfileTab = ProfileTab.HOME,
    val recentVideos: List<ChannelVideo> = emptyList(),
    val topClips: List<ChannelClip> = emptyList(),
    val calendar: StreamCalendar? = null,
    val previousNames: List<NameHistoryEntry> = emptyList(),
    val accountBadges: List<AccountBadge> = emptyList(),
    val follow: FollowStatus = FollowStatus.UNKNOWN,
    val clipQuery: ClipQuery = ClipQuery(),
    val clips: MediaFeed<ChannelClip> = MediaFeed(),
    val videoQuery: VideoQuery = VideoQuery(),
    val videos: MediaFeed<ChannelVideo> = MediaFeed()
) : UiState

sealed interface ChannelProfileEvent : UiEvent {
    data object Retry : ChannelProfileEvent
    data class SelectTab(val tab: ProfileTab) : ChannelProfileEvent
    data object LoadMoreClips : ChannelProfileEvent
    data object LoadMoreVideos : ChannelProfileEvent
    data class ApplyClipQuery(val query: ClipQuery) : ChannelProfileEvent
    data class ApplyVideoQuery(val query: VideoQuery) : ChannelProfileEvent
    data class SetFollowing(val follow: Boolean) : ChannelProfileEvent
}

sealed interface ChannelProfileEffect : UIEffect {
    data object FollowBlocked : ChannelProfileEffect
    data object FollowFailed : ChannelProfileEffect
}
