package io.rudione.chatone.presentation.chat.multichat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import io.rudione.chatone.presentation.chat.ChatViewModel
import org.koin.compose.koinInject


@Composable
fun rememberPanelTrackingBundle(
    panelManager: ChatPanelManager,
    panelId: String,
    bus: PanelEventBus,
    mentionTracker: PanelMentionTracker
): PanelTrackingBundle {
    return remember(panelId) {
        PanelTrackingBundle(
            panelManager = panelManager,
            panelId = panelId,
            bus = bus,
            mentionTracker = mentionTracker
        )
    }
}

class PanelTrackingBundle(
    val panelManager: ChatPanelManager,
    val panelId: String,
    val bus: PanelEventBus,
    val mentionTracker: PanelMentionTracker
) {
    fun markActive() {
        panelManager.setActive(panelId)
        mentionTracker.clear(panelId)
    }

    fun close() {
        panelManager.closePanel(panelId)
        mentionTracker.clear(panelId)
    }

    fun mentionCount(): Int = mentionTracker.count(panelId)
}
