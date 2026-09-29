package io.rudione.chatone.presentation.chat.multichat

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import io.rudione.chatone.domain.model.ChatPanel
import io.rudione.chatone.domain.model.PanelLayoutMode
import io.rudione.chatone.domain.model.layoutMode

@Composable
fun MultiChatContainer(
    panelManager: ChatPanelManager,
    defaultChannelLogin: String,
    background: @Composable () -> Unit,
    panelContent: @Composable (channelLogin: String, isCompact: Boolean, modifier: Modifier) -> Unit,
    modifier: Modifier = Modifier
) {
    val panels by panelManager.panels.collectAsState()

    Box(modifier = modifier.fillMaxSize()) {
        background()

        if (panels.isEmpty()) {
            panelContent(defaultChannelLogin, false, Modifier.fillMaxSize())
        } else {
            val isCompact = panels.layoutMode() == PanelLayoutMode.COMPACT

            if (panels.size == 1) {
                panelContent(panels[0].channelLogin, false, Modifier.fillMaxSize())
            } else {
                MultiChatHost(
                    panelManager = panelManager,
                    panelContent = { panel: ChatPanel, compact: Boolean, mod: Modifier ->
                        panelContent(panel.channelLogin, compact || isCompact, mod)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
