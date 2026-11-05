package io.rudione.chatone.presentation.chat

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

@Immutable
data class ChatSurfaceStyle(
    val transparentBackground: Boolean = false,
    val showHeader: Boolean = true,
    val showWatchButton: Boolean = true,
    val scrollableHeader: Boolean = false,
    val showInput: Boolean = true,
    val showScrollbar: Boolean = true,
    val showPinnedMessage: Boolean = true,
    val showEventBanners: Boolean = true,
    val messageAlpha: Float = 1f,
    val messageBackdropAlpha: Float = 0f,
    val claimsFocus: Boolean = true
)

val LocalChatSurfaceStyle = compositionLocalOf { ChatSurfaceStyle() }

sealed interface ChatRemoteCommand {
    data class ScrollBy(val pixels: Float) : ChatRemoteCommand
    data object Latest : ChatRemoteCommand
}

@Stable
class ChatRemoteScroll {
    private val _commands = MutableSharedFlow<ChatRemoteCommand>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val commands: SharedFlow<ChatRemoteCommand> = _commands.asSharedFlow()

    fun scrollBy(pixels: Float) {
        _commands.tryEmit(ChatRemoteCommand.ScrollBy(pixels))
    }

    fun toLatest() {
        _commands.tryEmit(ChatRemoteCommand.Latest)
    }
}

val LocalChatRemoteScroll = staticCompositionLocalOf<ChatRemoteScroll?> { null }
