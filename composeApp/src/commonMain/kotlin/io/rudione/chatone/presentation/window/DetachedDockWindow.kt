package io.rudione.chatone.presentation.window

import androidx.compose.runtime.Composable

@Composable
expect fun DetachedDockWindow(
    windowId: String,
    title: String,
    onClose: () -> Unit,
    content: @Composable () -> Unit
)
