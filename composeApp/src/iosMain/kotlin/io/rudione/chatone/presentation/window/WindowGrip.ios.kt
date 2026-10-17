package io.rudione.chatone.presentation.window

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun Modifier.windowGrip(grip: WindowGrip, onActiveChange: (Boolean) -> Unit): Modifier = this
