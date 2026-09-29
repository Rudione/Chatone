package io.rudione.chatone.presentation.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.SuspendingPointerInputModifierNode
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.max

enum class SwipeDirection(internal val sign: Float) { Left(-1f), Right(1f) }

private val ClaimDistance = 16.dp
private val TriggerDistance = 64.dp
private val FlingVelocity = 900.dp
private const val DIRECTION_RATIO = 2f

fun Modifier.directionalSwipe(direction: SwipeDirection, onSwipe: () -> Unit): Modifier =
    this then DirectionalSwipeElement(direction, onSwipe)

private data class DirectionalSwipeElement(
    val direction: SwipeDirection,
    val onSwipe: () -> Unit
) : ModifierNodeElement<DirectionalSwipeNode>() {
    override fun create(): DirectionalSwipeNode = DirectionalSwipeNode(direction, onSwipe)

    override fun update(node: DirectionalSwipeNode) {
        node.direction = direction
        node.onSwipe = onSwipe
    }
}

private class DirectionalSwipeNode(
    var direction: SwipeDirection,
    var onSwipe: () -> Unit
) : DelegatingNode() {
    init {
        delegate(SuspendingPointerInputModifierNode { detectDirectionalSwipe() })
    }

    private suspend fun PointerInputScope.detectDirectionalSwipe() {
        val claimPx = max(viewConfiguration.touchSlop, ClaimDistance.toPx())
        val triggerPx = TriggerDistance.toPx()
        val flingPx = FlingVelocity.toPx()
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            val sign = direction.sign
            val tracker = VelocityTracker().apply { addPointerInputChange(down) }
            var travel = Offset.Zero
            var claimed = false
            var fired = false
            while (true) {
                val event = awaitPointerEvent()
                if (event.changes.count { it.pressed } > 1) break
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                if (!claimed && change.isConsumed) break
                if (!change.pressed) {
                    if (claimed && !fired && tracker.calculateVelocity().x * sign >= flingPx) onSwipe()
                    break
                }
                travel += change.positionChange()
                tracker.addPointerInputChange(change)
                val forward = travel.x * sign
                if (!claimed) {
                    if (abs(travel.x) < claimPx && abs(travel.y) < claimPx) continue
                    if (forward < claimPx || abs(travel.x) < abs(travel.y) * DIRECTION_RATIO) break
                    claimed = true
                }
                change.consume()
                if (!fired && forward >= triggerPx) {
                    fired = true
                    onSwipe()
                }
            }
        }
    }
}
