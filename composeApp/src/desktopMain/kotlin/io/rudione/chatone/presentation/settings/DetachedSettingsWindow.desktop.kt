package io.rudione.chatone.presentation.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.rudione.chatone.presentation.window.ChatoneDetachedWindow
import io.rudione.chatone.presentation.window.MIN_TOOL_WINDOW_HEIGHT
import io.rudione.chatone.presentation.window.MIN_TOOL_WINDOW_WIDTH

@Composable
actual fun DetachedSettingsWindow(
    onClose: () -> Unit,
    onThemeChanged: (Boolean) -> Unit
) {
    ChatoneDetachedWindow(
        windowId = "settings",
        title = "Chatone — Settings",
        defaultWidth = 860.dp,
        defaultHeight = 680.dp,
        minWidth = MIN_TOOL_WINDOW_WIDTH,
        minHeight = MIN_TOOL_WINDOW_HEIGHT,
        onCloseRequest = onClose
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            SettingsScreen(
                onNavigateBack = onClose,
                onThemeChanged = onThemeChanged,
                isWideScreen = true,
                isDetached = true
            )
        }
    }
}
