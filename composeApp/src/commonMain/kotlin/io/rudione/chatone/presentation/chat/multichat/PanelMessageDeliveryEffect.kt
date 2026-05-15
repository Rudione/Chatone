package io.rudione.chatone.presentation.chat.multichat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect


@Composable
fun PanelMessageDeliveryEffect(
    dispatcher: PanelMessageDispatcher,
    panelId: String,
    onMessage: suspend (channelLogin: String, text: String) -> Unit
) {
    LaunchedEffect(dispatcher, panelId) {
        dispatcher.outgoing.collect { msg ->
            if (msg.panelId == panelId) onMessage(msg.channelLogin, msg.text)
        }
    }
}
