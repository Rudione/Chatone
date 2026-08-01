package io.rudione.chatone.presentation.chat.multichat

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import org.koin.compose.koinInject

@Composable
fun PreviewMultiChatIntegration(
    primaryChannel: String,
    background: @Composable () -> Unit,
    chatContent: @Composable (channelLogin: String, isCompact: Boolean, modifier: Modifier) -> Unit
) {
    val panelManager: ChatPanelManager = koinInject()
    val persistence: PanelPersistence = koinInject()
    val lifecycleSync: PanelLifecycleSync = koinInject()

    LaunchedEffect(panelManager) {
        lifecycleSync.attach(panelManager)
    }

    MultiChatScope(
        panelManager = panelManager,
        primaryChannelLogin = primaryChannel,
        sharedBackground = background,
        enableDragAndDrop = true,
        enablePersistence = true,
        panelPersistence = persistence,
        panelContent = chatContent,
        modifier = Modifier.fillMaxSize()
    )
}
