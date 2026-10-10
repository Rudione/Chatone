package io.rudione.chatone.util.system

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.PointerInputModifierNode
import androidx.compose.ui.unit.IntSize

actual fun Modifier.handleHover(
    onEnter: () -> Unit,
    onExit: () -> Unit
): Modifier = this then MouseHoverElement(onEnter, onExit)

private data class MouseHoverElement(
    val onEnter: () -> Unit,
    val onExit: () -> Unit
) : ModifierNodeElement<MouseHoverNode>() {
    override fun create(): MouseHoverNode = MouseHoverNode(onEnter, onExit)

    override fun update(node: MouseHoverNode) {
        node.onEnter = onEnter
        node.onExit = onExit
    }
}

private class MouseHoverNode(
    var onEnter: () -> Unit,
    var onExit: () -> Unit
) : Modifier.Node(), PointerInputModifierNode {
    private var hovered = false

    override fun onPointerEvent(pointerEvent: PointerEvent, pass: PointerEventPass, bounds: IntSize) {
        if (pass != PointerEventPass.Main) return
        when (pointerEvent.type) {
            PointerEventType.Enter -> if (!hovered && pointerEvent.changes.any { it.type == PointerType.Mouse }) {
                hovered = true
                onEnter()
            }
            PointerEventType.Exit -> if (hovered) {
                hovered = false
                onExit()
            }
        }
    }

    override fun onCancelPointerInput() {
        if (!hovered) return
        hovered = false
        onExit()
    }
}
