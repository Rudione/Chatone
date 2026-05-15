package io.rudione.chatone.presentation.settings

import androidx.compose.runtime.Composable

@Composable
expect fun DetachedSettingsWindow(
    onClose: () -> Unit,
    onThemeChanged: (Boolean) -> Unit
)
