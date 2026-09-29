package io.rudione.chatone.presentation.window

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import java.awt.Cursor
import java.awt.Frame
import java.awt.MouseInfo
import java.awt.Point
import java.awt.Window
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionAdapter

internal val LocalDraggableWindow = compositionLocalOf<Window?> { null }

private val MoveCursor = PointerIcon(Cursor(Cursor.MOVE_CURSOR))

@Composable
actual fun Modifier.windowDragArea(): Modifier {
    val window = LocalDraggableWindow.current ?: return this
    val dragger = remember(window) { WindowDragger(window) }
    DisposableEffect(dragger) { onDispose { dragger.stop() } }

    return this
        .pointerHoverIcon(MoveCursor)
        .pointerInput(dragger) {
            awaitEachGesture {
                awaitFirstDown()
                dragger.start()
            }
        }
}

private class WindowDragger(private val window: Window) {

    private var pointerAtStart: Point? = null
    private var windowAtStart: Point? = null

    private val motionListener = object : MouseMotionAdapter() {
        override fun mouseDragged(event: MouseEvent) = follow()
    }

    private val buttonListener = object : MouseAdapter() {
        override fun mouseReleased(event: MouseEvent) = stop()
    }

    fun start() {
        stop()
        if (isMaximized()) return
        pointerAtStart = MouseInfo.getPointerInfo()?.location ?: return
        windowAtStart = window.location
        window.addMouseMotionListener(motionListener)
        window.addMouseListener(buttonListener)
    }

    fun stop() {
        pointerAtStart = null
        windowAtStart = null
        window.removeMouseMotionListener(motionListener)
        window.removeMouseListener(buttonListener)
    }

    private fun follow() {
        val grab = pointerAtStart ?: return
        val origin = windowAtStart ?: return
        val pointer = MouseInfo.getPointerInfo()?.location ?: return
        window.setLocation(
            origin.x + (pointer.x - grab.x),
            origin.y + (pointer.y - grab.y)
        )
    }

    private fun isMaximized(): Boolean {
        val frame = window as? Frame ?: return false
        return frame.extendedState and Frame.MAXIMIZED_BOTH != 0
    }
}
