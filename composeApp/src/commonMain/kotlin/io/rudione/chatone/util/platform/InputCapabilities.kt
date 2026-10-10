package io.rudione.chatone.util.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable

@Immutable
data class InputCapabilities(val hardwareKeyboard: Boolean, val pointer: Boolean)

@Composable
expect fun rememberInputCapabilities(): InputCapabilities
