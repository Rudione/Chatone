package io.rudione.chatone.presentation.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.rudione.chatone.presentation.components.SystemBarsAppearance
import io.rudione.chatone.presentation.stream.FloatingStreamPlayer
import io.rudione.chatone.presentation.stream.LocalFloatingStreamPlayer

@Composable
actual fun DetachedSettingsWindow(
    onClose: () -> Unit,
    onThemeChanged: (Boolean) -> Unit
) {
    if (LocalFloatingStreamPlayer.current?.isPictureInPicture == true) return
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            decorFitsSystemWindows = false
        )
    ) {
        SystemBarsAppearance(lightBackground = MaterialTheme.colorScheme.background.luminance() > 0.5f)
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            SettingsScreen(
                onNavigateBack = onClose,
                onThemeChanged = onThemeChanged,
                isWideScreen = false,
                isDetached = true
            )
            FloatingStreamPlayer(onExpand = onClose)
        }
    }
}
