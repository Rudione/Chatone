package io.rudione.chatone.presentation.profile

import io.rudione.chatone.base.UiEvent
import io.rudione.chatone.domain.profile.ClipQuery
import io.rudione.chatone.domain.profile.VideoQuery

sealed interface ChannelProfileEvent : UiEvent {
    data object Retry : ChannelProfileEvent
    data class SelectTab(val tab: ProfileTab) : ChannelProfileEvent
    data object LoadMoreClips : ChannelProfileEvent
    data object LoadMoreVideos : ChannelProfileEvent
    data class ApplyClipQuery(val query: ClipQuery) : ChannelProfileEvent
    data class ApplyVideoQuery(val query: VideoQuery) : ChannelProfileEvent
    data class SetFollowing(val follow: Boolean) : ChannelProfileEvent
}
