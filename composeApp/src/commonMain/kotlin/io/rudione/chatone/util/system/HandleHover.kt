package io.rudione.chatone.util.system

import androidx.compose.ui.Modifier

expect fun Modifier.handleHover(
    onEnter: () -> Unit,
    onExit: () -> Unit
): Modifier
