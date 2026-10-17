package io.rudione.chatone.presentation.window

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import java.awt.Cursor
import java.awt.MouseInfo
import java.awt.Point
import java.awt.Rectangle
import java.awt.Window

@Composable
actual fun Modifier.windowGrip(grip: WindowGrip, onActiveChange: (Boolean) -> Unit): Modifier {
    val window = LocalDraggableWindow.current ?: return this
    val notify by rememberUpdatedState(onActiveChange)
    return this
        .pointerHoverIcon(grip.cursor)
        .pointerInput(window, grip) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                if (down.type == PointerType.Mouse && !currentEvent.buttons.isPrimaryPressed) return@awaitEachGesture
                val start = MouseInfo.getPointerInfo()?.location ?: return@awaitEachGesture
                val origin = window.bounds
                down.consume()
                notify(true)
                try {
                    while (true) {
                        val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        change.consume()
                        val pointer = MouseInfo.getPointerInfo()?.location ?: continue
                        val target = grip.reshape(origin, start, pointer, window)
                        if (target != window.bounds) window.bounds = target
                    }
                } finally {
                    notify(false)
                }
            }
        }
}

private fun WindowGrip.reshape(origin: Rectangle, start: Point, pointer: Point, window: Window): Rectangle {
    val dx = pointer.x - start.x
    val dy = pointer.y - start.y
    if (moves) return Rectangle(origin.x + dx, origin.y + dy, origin.width, origin.height)
    val minWidth = window.minimumSize?.width?.coerceAtLeast(1) ?: 1
    val minHeight = window.minimumSize?.height?.coerceAtLeast(1) ?: 1
    var x = origin.x
    var width = origin.width
    if (right) width = (origin.width + dx).coerceAtLeast(minWidth)
    if (left) {
        width = (origin.width - dx).coerceAtLeast(minWidth)
        x = origin.x + origin.width - width
    }
    val height = if (bottom) (origin.height + dy).coerceAtLeast(minHeight) else origin.height
    return Rectangle(x, origin.y, width, height)
}

private val GripCursors: Map<WindowGrip, PointerIcon> = WindowGrip.entries.associateWith { grip ->
    PointerIcon(
        Cursor(
            when (grip) {
                WindowGrip.MOVE -> Cursor.MOVE_CURSOR
                WindowGrip.LEFT -> Cursor.W_RESIZE_CURSOR
                WindowGrip.RIGHT -> Cursor.E_RESIZE_CURSOR
                WindowGrip.BOTTOM -> Cursor.S_RESIZE_CURSOR
                WindowGrip.BOTTOM_LEFT -> Cursor.SW_RESIZE_CURSOR
                WindowGrip.BOTTOM_RIGHT -> Cursor.SE_RESIZE_CURSOR
            }
        )
    )
}

private val WindowGrip.cursor: PointerIcon get() = GripCursors.getValue(this)
