package io.rudione.chatone.presentation.window

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

enum class WindowGrip(val moves: Boolean, val left: Boolean, val right: Boolean, val bottom: Boolean) {
    MOVE(moves = true, left = false, right = false, bottom = false),
    LEFT(moves = false, left = true, right = false, bottom = false),
    RIGHT(moves = false, left = false, right = true, bottom = false),
    BOTTOM(moves = false, left = false, right = false, bottom = true),
    BOTTOM_LEFT(moves = false, left = true, right = false, bottom = true),
    BOTTOM_RIGHT(moves = false, left = false, right = true, bottom = true)
}

@Composable
expect fun Modifier.windowGrip(grip: WindowGrip, onActiveChange: (Boolean) -> Unit = {}): Modifier
