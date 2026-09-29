package io.rudione.chatone.presentation.chat.multichat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.focus.FocusRequester

@Composable
fun PanelFocusEffect(
    panelManager: ChatPanelManager,
    panelId: String,
    focusRequester: FocusRequester
) {
    val activeId by panelManager.activePanelId.collectAsState()
    LaunchedEffect(activeId) {
        if (activeId == panelId) {
            try { focusRequester.requestFocus() } catch (_: Exception) {}
        }
    }
}
