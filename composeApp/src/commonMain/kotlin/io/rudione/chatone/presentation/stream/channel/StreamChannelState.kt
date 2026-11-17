package io.rudione.chatone.presentation.stream.channel

import androidx.compose.runtime.Immutable
import io.rudione.chatone.base.UiState
import io.rudione.chatone.domain.channel.ChannelSnapshot
import io.rudione.chatone.domain.follow.FollowFailure

enum class FollowStatus { UNKNOWN, FOLLOWING, NOT_FOLLOWING, SIGNED_OUT }

@Immutable
data class StreamChannelState(
    val channel: ChannelSnapshot = ChannelSnapshot(login = ""),
    val follow: FollowStatus = FollowStatus.UNKNOWN,
    val followBusy: Boolean = false,
    val failure: FollowFailure? = null
) : UiState {
    val canToggleFollow: Boolean
        get() = channel.channelId.isNotBlank() && follow != FollowStatus.SIGNED_OUT
}
