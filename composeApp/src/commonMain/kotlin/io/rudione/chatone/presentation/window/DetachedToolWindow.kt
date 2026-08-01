package io.rudione.chatone.presentation.window

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
expect fun DetachedToolWindow(
    windowId: String,
    title: String,
    defaultWidth: Dp = 420.dp,
    defaultHeight: Dp = 360.dp,
    onClose: () -> Unit,
    content: @Composable () -> Unit
)
