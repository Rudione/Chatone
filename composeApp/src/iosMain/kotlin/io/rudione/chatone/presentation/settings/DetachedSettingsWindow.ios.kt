package io.rudione.chatone.presentation.settings

import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.rudione.chatone.presentation.nativecontext.SuspendNativeMessageMenus

@Composable
actual fun DetachedSettingsWindow(
    onClose: () -> Unit,
    onThemeChanged: (Boolean) -> Unit
) {
    SuspendNativeMessageMenus()
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            usePlatformInsets = false,
            dismissOnClickOutside = false
        )
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            SettingsScreen(onNavigateBack = onClose, onThemeChanged = onThemeChanged, isDetached = true)
        }
    }
}
