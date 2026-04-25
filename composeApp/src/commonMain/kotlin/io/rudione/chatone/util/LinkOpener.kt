package io.rudione.chatone.util

import androidx.compose.runtime.Composable
import io.rudione.chatone.presentation.settings.SettingsState

expect fun openUrl(url: String, mode: SettingsState.LinkOpenMode)
