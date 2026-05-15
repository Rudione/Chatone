package io.rudione.chatone.presentation.chat.multichat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue


@Composable
fun rememberOpenPanelChannels(panelManager: ChatPanelManager): List<String> {
    val panels by panelManager.panels.collectAsState()
    return panels.map { it.channelLogin }
}


@Composable
fun rememberPanelCount(panelManager: ChatPanelManager): Int {
    val panels by panelManager.panels.collectAsState()
    return panels.size
}
