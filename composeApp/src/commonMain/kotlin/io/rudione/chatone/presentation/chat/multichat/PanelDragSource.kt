package io.rudione.chatone.presentation.chat.multichat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier


expect fun Modifier.channelDragSource(channelLogin: String): Modifier


expect fun Modifier.channelDropTarget(
    onChannelDropped: (String) -> Unit,
    onDragEnter: () -> Unit = {},
    onDragExit: () -> Unit = {}
): Modifier
