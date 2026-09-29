package io.rudione.chatone.util.system

import androidx.compose.ui.Modifier

actual fun Modifier.handleHover(
    onEnter: () -> Unit,
    onExit: () -> Unit
): Modifier = this
