package io.rudione.chatone.presentation.chat.multichat

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier


@Composable
fun PanelSwapZone(
    panelManager: ChatPanelManager,
    panelIndex: Int,
    onSwap: (fromIndex: Int, toIndex: Int) -> Unit = { from, to ->
        panelManager.reorder(from, to)
    },
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    androidx.compose.foundation.layout.Box(
        modifier = modifier.channelDropTarget(
            onChannelDropped = { channel ->


                val panels = panelManager.panels.value
                val fromIdx = panels.indexOfFirst { it.channelLogin.equals(channel, ignoreCase = true) }
                if (fromIdx >= 0 && fromIdx != panelIndex) {
                    onSwap(fromIdx, panelIndex)
                } else {
                    panelManager.openPanel(channel)
                }
            }
        )
    ) {
        content()
    }
}
