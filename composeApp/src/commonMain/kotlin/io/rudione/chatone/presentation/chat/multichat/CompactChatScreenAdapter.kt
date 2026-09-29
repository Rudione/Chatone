package io.rudione.chatone.presentation.chat.multichat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Stable
data class CompactChatLayout(
    val showBadges: Boolean,
    val showTimestamps: Boolean,
    val showHeader: Boolean,
    val showModButtons: Boolean,
    val emoteScale: Float,
    val fontScale: Float,
    val inputHeight: Dp,
    val avatarSize: Dp
) {
    companion object {

        fun forSize(panels: Int): CompactChatLayout = when {
            panels <= 1 -> CompactChatLayout(
                showBadges = true, showTimestamps = true, showHeader = true,
                showModButtons = true, emoteScale = 1f, fontScale = 1f,
                inputHeight = 56.dp, avatarSize = 28.dp
            )
            panels <= 3 -> CompactChatLayout(
                showBadges = true, showTimestamps = true, showHeader = true,
                showModButtons = true, emoteScale = 1f, fontScale = 0.95f,
                inputHeight = 48.dp, avatarSize = 24.dp
            )
            panels <= 5 -> CompactChatLayout(
                showBadges = true, showTimestamps = false, showHeader = false,
                showModButtons = false, emoteScale = 0.9f, fontScale = 0.9f,
                inputHeight = 40.dp, avatarSize = 20.dp
            )
            else -> CompactChatLayout(
                showBadges = false, showTimestamps = false, showHeader = false,
                showModButtons = false, emoteScale = 0.8f, fontScale = 0.85f,
                inputHeight = 36.dp, avatarSize = 18.dp
            )
        }
    }
}

@Composable
fun rememberCompactChatLayout(panelManager: ChatPanelManager): CompactChatLayout {
    val count = rememberPanelCount(panelManager)
    return CompactChatLayout.forSize(count)
}
