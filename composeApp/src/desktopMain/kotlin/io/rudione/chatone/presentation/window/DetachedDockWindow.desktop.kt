package io.rudione.chatone.presentation.window

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
actual fun DetachedDockWindow(
    windowId: String,
    title: String,
    onClose: () -> Unit,
    content: @Composable () -> Unit
) {
    ChatoneDetachedWindow(
        windowId = windowId,
        title = title,
        defaultWidth = 460.dp,
        defaultHeight = 720.dp,
        minWidth = MIN_TOOL_WINDOW_WIDTH,
        minHeight = MIN_TOOL_WINDOW_HEIGHT,
        onCloseRequest = onClose
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            content()
        }
    }
}
