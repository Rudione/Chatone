package io.rudione.chatone.presentation.chat.multichat

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun ChannelDragHandle(
    channelLogin: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier.channelDragSource(channelLogin)) {
        content()
    }
}
