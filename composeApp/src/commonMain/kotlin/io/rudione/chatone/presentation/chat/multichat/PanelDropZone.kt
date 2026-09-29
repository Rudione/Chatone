package io.rudione.chatone.presentation.chat.multichat

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier

@Composable
fun PanelDropZone(
    panelManager: ChatPanelManager,
    modifier: Modifier = Modifier,
    onChannelOpened: (String) -> Unit = {},
    content: @Composable () -> Unit
) {
    val state = rememberDragDropState()

    LaunchedEffect(state.lastDroppedChannel) {
        state.consume()?.let { channel ->
            val panelId = panelManager.openPanel(channel)
            if (panelId != null) onChannelOpened(channel)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .channelDropTarget(
                onChannelDropped = { state.receive(it) },
                onDragEnter = { state.enter() },
                onDragExit = { state.exit() }
            )
    ) {
        content()
        PanelDropOverlay(
            isActive = state.isDragOver,
            modifier = Modifier.fillMaxSize()
        )
    }
}
