package io.rudione.chatone.presentation.chat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import io.rudione.chatone.presentation.window.isWindowsOs
import io.rudione.chatone.util.system.WindowsTitleBar
import java.awt.MouseInfo
import java.awt.Point
import java.awt.Window

internal val LocalProfileWindow = compositionLocalOf<Window?> { null }

@Composable
actual fun Modifier.profileWindowDrag(): Modifier {
    val window = LocalProfileWindow.current ?: return this

    if (isWindowsOs) {
        return this.pointerInput(window) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    if (event.type == PointerEventType.Press) {
                        runCatching { WindowsTitleBar.beginWindowDrag(window) }
                    }
                }
            }
        }
    }

    return this.pointerInput(window) {
        awaitPointerEventScope {
            while (true) {
                val down = awaitPointerEvent()
                if (down.type != PointerEventType.Press) continue

                val grabScreen = MouseInfo.getPointerInfo()?.location ?: continue
                val origin = Point(window.location)

                while (true) {
                    val move = awaitPointerEvent()
                    if (move.type == PointerEventType.Release) break
                    if (move.changes.none { it.pressed }) break
                    val current = MouseInfo.getPointerInfo()?.location ?: break
                    window.setLocation(
                        origin.x + (current.x - grabScreen.x),
                        origin.y + (current.y - grabScreen.y)
                    )
                }
            }
        }
    }
}
