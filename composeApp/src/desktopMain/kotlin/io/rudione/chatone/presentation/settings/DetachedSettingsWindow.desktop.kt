package io.rudione.chatone.presentation.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import io.rudione.chatone.presentation.theme.ChatoneTheme

@Composable
actual fun DetachedSettingsWindow(
    onClose: () -> Unit,
    onThemeChanged: (Boolean) -> Unit
) {
    val windowState = rememberWindowState(
        width = 860.dp,
        height = 680.dp,
        position = WindowPosition.PlatformDefault
    )

    Window(
        onCloseRequest = onClose,
        title = "Chatone — Settings",
        state = windowState,
        resizable = true
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
