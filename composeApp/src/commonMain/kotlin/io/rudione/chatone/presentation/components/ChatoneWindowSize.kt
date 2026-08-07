package io.rudione.chatone.presentation.components

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class ChatoneWindowSize { Compact, Expanded, Large }

object ChatoneBreakpoints {
    val expanded: Dp = 640.dp
    val large: Dp = 1000.dp

    val minPane: Dp = 200.dp
    val minChatPane: Dp = 400.dp
    val railWidth: Dp = 40.dp
    val dockWidth: Dp = 380.dp
    val dockWidthWide: Dp = 440.dp

    fun of(width: Dp): ChatoneWindowSize = when {
        width >= large -> ChatoneWindowSize.Large
        width >= expanded -> ChatoneWindowSize.Expanded
        else -> ChatoneWindowSize.Compact
    }
}

val LocalWindowSize = staticCompositionLocalOf { ChatoneWindowSize.Expanded }

enum class DockPanel { None, Settings, Automod, Assistant, Moderation, Emotes, Points }

@Stable
class DockHost {
    var panel by mutableStateOf(DockPanel.None)
        private set
    var channelLogin by mutableStateOf<String?>(null)
        private set
    var content by mutableStateOf<(@androidx.compose.runtime.Composable () -> Unit)?>(null)
        private set
    var torn by mutableStateOf(DockPanel.None)
        private set
    var tornContent by mutableStateOf<(@androidx.compose.runtime.Composable () -> Unit)?>(null)
        private set

    fun tearOut() {
        if (panel == DockPanel.None) return
        torn = panel
        tornContent = content
        panel = DockPanel.None
        content = null
    }

    fun closeTorn() {
        torn = DockPanel.None
        tornContent = null
    }

    fun openWith(target: DockPanel, body: @androidx.compose.runtime.Composable () -> Unit) {
        panel = target
        content = body
    }

    fun open(target: DockPanel, login: String? = null) {
        panel = target
        channelLogin = login
    }

    fun close() {
        panel = DockPanel.None
        content = null
    }

    fun closeIf(target: DockPanel) {
        if (panel == target) close()
    }
}

val LocalDockHost = staticCompositionLocalOf<DockHost?> { null }

fun DockHost?.evictedFrom(target: DockPanel, wasDocked: Boolean): Boolean =
    this != null && wasDocked && panel != target && torn != target

object DockState {
    var large by mutableStateOf(false)
}
