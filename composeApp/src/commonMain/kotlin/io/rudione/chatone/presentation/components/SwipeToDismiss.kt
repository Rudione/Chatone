package io.rudione.chatone.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.SuspendingPointerInputModifierNode
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

private const val DISMISS_FRACTION = 0.35f
private val FlingVelocity = 900.dp
private val FlingMinTravel = 48.dp

fun Modifier.swipeToDismiss(direction: SwipeDirection, onDismiss: () -> Unit): Modifier =
    this then SwipeToDismissElement(direction, onDismiss)

internal object SwipeDismissDecision {
    fun shouldDismiss(
        travel: Float,
        width: Float,
        velocity: Float,
        flingVelocity: Float,
        flingMinTravel: Float
    ): Boolean = width > 0f && (
        travel >= width * DISMISS_FRACTION ||
            (velocity >= flingVelocity && travel >= flingMinTravel)
        )
}

private data class SwipeToDismissElement(
    val direction: SwipeDirection,
    val onDismiss: () -> Unit
) : ModifierNodeElement<SwipeToDismissNode>() {
    override fun create(): SwipeToDismissNode = SwipeToDismissNode(direction, onDismiss)

    override fun update(node: SwipeToDismissNode) {
        node.direction = direction
        node.onDismiss = onDismiss
    }
}

private class SwipeToDismissNode(
    var direction: SwipeDirection,
    var onDismiss: () -> Unit
) : DelegatingNode(), LayoutModifierNode {

    private val offset = Animatable(0f)
    private var width = 0

    init {
        delegate(SuspendingPointerInputModifierNode { detectDismissSwipe() })
    }

    override fun onAttach() {
        if (offset.value != 0f) coroutineScope.launch { offset.snapTo(0f) }
    }

    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        val placeable = measurable.measure(constraints)
        width = placeable.width
        return layout(placeable.width, placeable.height) {
            placeable.placeWithLayer(0, 0) { translationX = offset.value * direction.sign }
        }
    }

    private suspend fun PointerInputScope.detectDismissSwipe() {
        val slop = viewConfiguration.touchSlop
        val holdTimeout = viewConfiguration.longPressTimeoutMillis
        val flingPx = FlingVelocity.toPx()
        val flingTravelPx = FlingMinTravel.toPx()
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            if (down.type == PointerType.Mouse) return@awaitEachGesture
            val sign = direction.sign
            val tracker = VelocityTracker().apply { addPointerInputChange(down) }
            var travel = Offset.Zero
            var claimed = false
            var dragged = 0f
            while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull { it.id == down.id }
                if (change == null || event.changes.count { it.pressed } > 1) {
                    if (claimed) settleBack()
                    break
                }
                if (!change.pressed) {
                    if (claimed) release(dragged, tracker.calculateVelocity().x * sign, flingPx, flingTravelPx)
                    break
                }
                if (!claimed && change.isConsumed) break
                travel += change.positionChange()
                tracker.addPointerInputChange(change)
                if (!claimed) {
                    val heldTooLong = change.uptimeMillis - down.uptimeMillis > holdTimeout
                    when (SwipeIntent.decide(travel, sign, slop, heldTooLong)) {
                        SwipeIntent.Decision.Reject -> break
                        SwipeIntent.Decision.Undecided -> continue
                        SwipeIntent.Decision.Claim -> {
                            claimed = true
                            dragged = offset.value
                        }
                    }
                }
                change.consume()
                dragged = (dragged + change.positionChange().x * sign).coerceAtLeast(0f)
                val target = dragged
                coroutineScope.launch { offset.snapTo(target) }
            }
        }
    }

    private fun release(travel: Float, velocity: Float, flingPx: Float, flingTravelPx: Float) {
        val dismiss = SwipeDismissDecision.shouldDismiss(
            travel = travel,
            width = width.toFloat(),
            velocity = velocity,
            flingVelocity = flingPx,
            flingMinTravel = flingTravelPx
        )
        if (dismiss) onDismiss() else settleBack()
    }

    private fun settleBack() {
        coroutineScope.launch {
            offset.animateTo(0f, spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow))
        }
    }
}
