package io.rudione.chatone.presentation.chat.multichat

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import io.rudione.chatone.domain.model.ChatPanel


@Composable
fun MultiChatScope(
    panelManager: ChatPanelManager,
    primaryChannelLogin: String,
    sharedBackground: @Composable () -> Unit,
    enableDragAndDrop: Boolean = true,
    enablePersistence: Boolean = false,
    panelPersistence: PanelPersistence? = null,
    onAnyChannelOpened: (String) -> Unit = {},
    panelContent: @Composable (channelLogin: String, isCompact: Boolean, modifier: Modifier) -> Unit,
    modifier: Modifier = Modifier
) {
    val panels by panelManager.panels.collectAsState()


    if (enablePersistence && panelPersistence != null) {
        LaunchedEffect(Unit) { panelPersistence.load(panelManager) }
        LaunchedEffect(panels.size) { panelPersistence.save(panelManager) }
    }

    val container: @Composable () -> Unit = {
        MultiChatContainer(
            panelManager = panelManager,
            defaultChannelLogin = primaryChannelLogin,
            background = sharedBackground,
            panelContent = panelContent,
            modifier = Modifier.fillMaxSize()
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (enableDragAndDrop) {
            PanelDropZone(
                panelManager = panelManager,
                onChannelOpened = onAnyChannelOpened,
                modifier = Modifier.fillMaxSize()
            ) {
                container()
            }
        } else {
            container()
        }
    }
}
