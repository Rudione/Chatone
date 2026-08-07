package io.rudione.chatone.presentation.window

import androidx.compose.runtime.Composable

@Composable
actual fun DetachedDockWindow(
    windowId: String,
    title: String,
    onClose: () -> Unit,
    content: @Composable () -> Unit
) {
}
