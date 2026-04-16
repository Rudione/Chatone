package io.rudione.chatone.util

import androidx.compose.ui.Modifier

actual fun Modifier.handleHover(
    onEnter: () -> Unit,
    onExit: () -> Unit
): Modifier = this