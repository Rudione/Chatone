package io.rudione.chatone.presentation.settings

import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Composable

@Composable
actual fun DetachedSettingsWindow(
    onClose: () -> Unit,
    onThemeChanged: (Boolean) -> Unit
) {
    SideEffect(Unit) { onClose() }
}
