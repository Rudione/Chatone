package io.rudione.chatone.presentation.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.rudione.chatone.presentation.theme.ChatoneTheme
import io.rudione.chatone.presentation.window.ChatoneDetachedWindow

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
        onCloseRequest = onClose
    ) {
        ChatoneTheme {
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
}
