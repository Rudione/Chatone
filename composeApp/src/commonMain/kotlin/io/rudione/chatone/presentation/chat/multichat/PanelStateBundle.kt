package io.rudione.chatone.presentation.chat.multichat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import io.rudione.chatone.domain.model.ChatPanel
import io.rudione.chatone.domain.model.PanelLayoutMode
import io.rudione.chatone.domain.model.layoutMode


data class PanelStateBundle(
    val panels: List<ChatPanel>,
    val activePanelId: String?,
    val activeChannel: String?,
    val mode: PanelLayoutMode,
    val isMultiChat: Boolean,
    val isCompact: Boolean,
    val canOpenMore: Boolean
)

@Composable
fun rememberPanelStateBundle(panelManager: ChatPanelManager): PanelStateBundle {
    val panels by panelManager.panels.collectAsState()
    val activeId by panelManager.activePanelId.collectAsState()
    val activeChannel = panels.firstOrNull { it.panelId == activeId }?.channelLogin
    val mode = panels.layoutMode()
    return PanelStateBundle(
        panels = panels,
        activePanelId = activeId,
        activeChannel = activeChannel,
        mode = mode,
        isMultiChat = panels.size > 1,
        isCompact = mode == PanelLayoutMode.COMPACT,
        canOpenMore = panels.size < ChatPanel.MAX_PANELS
    )
}
