package io.rudione.chatone.presentation.chat.multichat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

@Composable
fun rememberIsPanelActive(panelManager: ChatPanelManager, panelId: String): Boolean {
    val activeId by panelManager.activePanelId.collectAsState()
    return activeId == panelId
}

@Composable
fun rememberShouldAutoScroll(
    panelManager: ChatPanelManager,
    panelId: String
): Boolean {

    val active = rememberIsPanelActive(panelManager, panelId)
    return active
}
