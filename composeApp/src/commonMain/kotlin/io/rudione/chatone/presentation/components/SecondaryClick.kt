package io.rudione.chatone.presentation.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.SuspendingPointerInputModifierNode
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.ModifierNodeElement

fun Modifier.onSecondaryClick(onClick: () -> Unit): Modifier = this then SecondaryClickElement(onClick)

private data class SecondaryClickElement(val onClick: () -> Unit) : ModifierNodeElement<SecondaryClickNode>() {
    override fun create(): SecondaryClickNode = SecondaryClickNode(onClick)

    override fun update(node: SecondaryClickNode) {
        node.onClick = onClick
    }
}

private class SecondaryClickNode(var onClick: () -> Unit) : DelegatingNode() {
    init {
        delegate(
            SuspendingPointerInputModifierNode {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) {
                            event.changes.forEach { it.consume() }
                            onClick()
                        }
                    }
                }
            }
        )
    }
}
