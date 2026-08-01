package io.rudione.chatone.presentation.chat.multichat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

@Composable
fun PanelEventDispatcher(
    bus: PanelEventBus,
    panelManager: ChatPanelManager,
    onScrollToBottomRequested: (panelId: String) -> Unit = {},
    onFocusRequested: (panelId: String) -> Unit = {}
) {
    LaunchedEffect(bus) {
        bus.events.collect { event ->
            when (event) {
                is PanelEventBus.Event.ScrollToBottom -> onScrollToBottomRequested(event.panelId)
                is PanelEventBus.Event.FocusPanel -> {
                    panelManager.setActive(event.panelId)
                    onFocusRequested(event.panelId)
                }
                is PanelEventBus.Event.PanelOpenedByDrag -> {

                    panelManager.openPanel(event.channelLogin)
                }
                else -> {}
            }
        }
    }
}
