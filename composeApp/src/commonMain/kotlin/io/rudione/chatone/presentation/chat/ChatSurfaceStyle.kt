package io.rudione.chatone.presentation.chat

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf

@Immutable
data class ChatSurfaceStyle(
    val transparentBackground: Boolean = false,
    val showHeader: Boolean = true,
    val showWatchButton: Boolean = true,
    val scrollableHeader: Boolean = false,
    val showInput: Boolean = true,
    val showScrollbar: Boolean = true,
    val showPinnedMessage: Boolean = true,
    val messageAlpha: Float = 1f
)

val LocalChatSurfaceStyle = compositionLocalOf { ChatSurfaceStyle() }
