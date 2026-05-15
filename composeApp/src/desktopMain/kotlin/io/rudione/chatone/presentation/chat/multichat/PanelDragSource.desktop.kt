package io.rudione.chatone.presentation.chat.multichat

import androidx.compose.ui.Modifier




actual fun Modifier.channelDragSource(channelLogin: String): Modifier = this

actual fun Modifier.channelDropTarget(
    onChannelDropped: (String) -> Unit,
    onDragEnter: () -> Unit,
    onDragExit: () -> Unit
): Modifier = this
