package io.rudione.chatone.util.platform

import androidx.compose.runtime.Composable

private val TouchInput = InputCapabilities(hardwareKeyboard = false, pointer = false)

@Composable
actual fun rememberInputCapabilities(): InputCapabilities = TouchInput
