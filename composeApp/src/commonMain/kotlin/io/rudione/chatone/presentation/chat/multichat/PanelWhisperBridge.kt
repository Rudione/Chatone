package io.rudione.chatone.presentation.chat.multichat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect


@Composable
fun PanelWhisperBridge(
    bus: PanelEventBus,
    onWhisper: (fromChannel: String, text: String) -> Unit
) {
    LaunchedEffect(bus) {
        bus.events.collect { event ->
            if (event is PanelEventBus.Event.MentionAcrossPanels) {
                onWhisper(event.fromChannel, event.text)
            }
        }
    }
}
