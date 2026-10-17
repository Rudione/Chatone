package io.rudione.chatone.presentation.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
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

fun Modifier.onUnclaimedSecondaryClick(onClick: (Offset) -> Unit): Modifier =
    this then UnclaimedSecondaryClickElement(onClick)

private data class UnclaimedSecondaryClickElement(
    val onClick: (Offset) -> Unit
) : ModifierNodeElement<UnclaimedSecondaryClickNode>() {
    override fun create(): UnclaimedSecondaryClickNode = UnclaimedSecondaryClickNode(onClick)

    override fun update(node: UnclaimedSecondaryClickNode) {
        node.onClick = onClick
    }
}

private class UnclaimedSecondaryClickNode(var onClick: (Offset) -> Unit) : DelegatingNode() {
    init {
        delegate(
            SuspendingPointerInputModifierNode {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        if (event.type != PointerEventType.Press || !event.buttons.isSecondaryPressed) continue
                        val change = event.changes.firstOrNull() ?: continue
                        if (event.changes.any { it.isConsumed }) continue
                        change.consume()
                        onClick(change.position)
                    }
                }
            }
        )
    }
}
