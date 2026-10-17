package io.rudione.chatone.presentation.chat.rendering

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged

@Composable
fun SelectionCursorBox(
    modifier: Modifier = Modifier,
    cursorOverride: () -> PointerIcon? = { null },
    content: @Composable BoxScope.() -> Unit
) {
    var isSelecting by remember { mutableStateOf(false) }
    val override = cursorOverride()
    Box(
        modifier = modifier
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val pressed = event.changes.any { it.pressed }
                        isSelecting = when {
                            !pressed -> false
                            cursorOverride() != null -> false
                            event.changes.any { it.pressed && it.positionChanged() } -> true
                            else -> isSelecting
                        }
                    }
                }
            }
            .pointerHoverIcon(
                icon = override ?: if (isSelecting) PointerIcon.Text else PointerIcon.Default,
                overrideDescendants = true
            ),
        content = content
    )
}
