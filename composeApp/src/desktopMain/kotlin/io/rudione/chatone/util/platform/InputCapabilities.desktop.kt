package io.rudione.chatone.util.platform

import androidx.compose.runtime.Composable

private val DesktopInput = InputCapabilities(hardwareKeyboard = true, pointer = true)

@Composable
actual fun rememberInputCapabilities(): InputCapabilities = DesktopInput
