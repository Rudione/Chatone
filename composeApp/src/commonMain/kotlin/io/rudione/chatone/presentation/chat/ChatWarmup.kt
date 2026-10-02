package io.rudione.chatone.presentation.chat

import androidx.compose.runtime.Immutable

@Immutable
data class ChatWarmup(
    val globalEmotes: Boolean = false,
    val globalBadges: Boolean = false,
    val channelAssets: Boolean = false,
    val history: Boolean = false
) {
    val assetsReady: Boolean
        get() = globalEmotes && globalBadges && channelAssets

    val isComplete: Boolean
        get() = assetsReady && history

    companion object {
        val Complete = ChatWarmup(globalEmotes = true, globalBadges = true, channelAssets = true, history = true)
    }
}
