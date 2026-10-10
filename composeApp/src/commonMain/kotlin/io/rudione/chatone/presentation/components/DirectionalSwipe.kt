package io.rudione.chatone.presentation.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.SuspendingPointerInputModifierNode
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.unit.dp
import kotlin.math.abs

enum class SwipeDirection(internal val sign: Float) { Left(-1f), Right(1f) }

private val TriggerDistance = 56.dp
private val FlingVelocity = 700.dp
private const val DIRECTION_RATIO = 1.3f

fun Modifier.directionalSwipe(direction: SwipeDirection, onSwipe: () -> Unit): Modifier =
    this then DirectionalSwipeElement(direction, onSwipe)

internal object SwipeIntent {
    enum class Decision { Undecided, Claim, Reject }

    fun decide(travel: Offset, sign: Float, slop: Float, heldTooLong: Boolean): Decision {
        if (heldTooLong) return Decision.Reject
        if (abs(travel.x) < slop && abs(travel.y) < slop) return Decision.Undecided
        val forward = travel.x * sign
        return if (forward >= slop && abs(travel.x) >= abs(travel.y) * DIRECTION_RATIO) Decision.Claim else Decision.Reject
    }
}

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
        val slop = viewConfiguration.touchSlop
        val holdTimeout = viewConfiguration.longPressTimeoutMillis
        val triggerPx = TriggerDistance.toPx()
        val flingPx = FlingVelocity.toPx()
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            if (down.type == PointerType.Mouse) return@awaitEachGesture
            val sign = direction.sign
            val tracker = VelocityTracker().apply { addPointerInputChange(down) }
            var travel = Offset.Zero
            var claimed = false
            var fired = false
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                if (event.changes.count { it.pressed } > 1) break
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                if (!claimed && change.isConsumed) break
                if (!change.pressed) {
                    if (claimed && !fired && tracker.calculateVelocity().x * sign >= flingPx) onSwipe()
                    break
                }
                travel += change.positionChange()
                tracker.addPointerInputChange(change)
                if (!claimed) {
                    val heldTooLong = change.uptimeMillis - down.uptimeMillis > holdTimeout
                    when (SwipeIntent.decide(travel, sign, slop, heldTooLong)) {
                        SwipeIntent.Decision.Reject -> break
                        SwipeIntent.Decision.Undecided -> {
                            if (claimedBelow(down.id)) break
                            continue
                        }
                        SwipeIntent.Decision.Claim -> claimed = true
                    }
                }
                change.consume()
                if (!fired && travel.x * sign >= triggerPx) {
                    fired = true
                    onSwipe()
                }
            }
        }
    }

    private suspend fun AwaitPointerEventScope.claimedBelow(pointerId: PointerId): Boolean =
        awaitPointerEvent(PointerEventPass.Final).changes.any { it.id == pointerId && it.isConsumed }
}
