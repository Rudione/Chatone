package io.rudione.chatone.presentation.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.russhwolf.settings.Settings
import io.rudione.chatone.presentation.window.ChatoneDetachedWindow
import io.rudione.chatone.presentation.window.MIN_TOOL_WINDOW_HEIGHT
import io.rudione.chatone.presentation.window.MIN_TOOL_WINDOW_WIDTH

private const val KEY_SETTINGS_PINNED = "win_settings_always_on_top"

@Composable
actual fun DetachedSettingsWindow(
    onClose: () -> Unit,
    onThemeChanged: (Boolean) -> Unit
) {
    val settings = remember { Settings() }
    var pinned by remember { mutableStateOf(settings.getBoolean(KEY_SETTINGS_PINNED, false)) }

    ChatoneDetachedWindow(
        windowId = "settings",
        title = "Chatone — Settings",
        defaultWidth = 860.dp,
        defaultHeight = 680.dp,
        minWidth = MIN_TOOL_WINDOW_WIDTH,
        minHeight = MIN_TOOL_WINDOW_HEIGHT,
        alwaysOnTop = pinned,
        showTitleBar = false,
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
                isDetached = true,
                embedded = true,
                isPinned = pinned,
                onTogglePin = {
                    pinned = !pinned
                    settings.putBoolean(KEY_SETTINGS_PINNED, pinned)
                }
            )
        }
    }
}
