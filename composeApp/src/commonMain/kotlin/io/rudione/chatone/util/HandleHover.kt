package io.rudione.chatone.util

import androidx.compose.ui.Modifier

expect fun Modifier.handleHover(
    onEnter: () -> Unit,
    onExit: () -> Unit
): Modifier