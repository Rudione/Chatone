package io.rudione.chatone.presentation.chat.rendering

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged

@OptIn(ExperimentalComposeUiApi::class)
fun Modifier.selectionCursor(): Modifier = composed {
    var isSelecting by remember { mutableStateOf(false) }

    this
        .pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val pressed = event.changes.any { it.pressed }
                    isSelecting = when {
                        !pressed -> false
                        event.changes.any { it.pressed && it.positionChanged() } -> true
                        else -> isSelecting
                    }
                }
            }
        }
        .pointerHoverIcon(
            icon = if (isSelecting) PointerIcon.Text else PointerIcon.Default,
            overrideDescendants = true
        )
}
