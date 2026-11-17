package io.rudione.chatone.presentation.profile

import io.rudione.chatone.base.UIEffect

sealed interface ChannelProfileEffect : UIEffect {
    data object FollowBlocked : ChannelProfileEffect
    data object FollowFailed : ChannelProfileEffect
}
