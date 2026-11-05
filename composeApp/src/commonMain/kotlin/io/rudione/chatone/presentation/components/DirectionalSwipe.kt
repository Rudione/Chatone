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

interface SwipeDragHandler {
    fun onSwipeDrag(deltaPx: Float)
    fun onSwipeRelease(velocityPx: Float)
}

private val TriggerDistance = 56.dp
private val FlingVelocity = 700.dp
private const val DIRECTION_RATIO = 1.3f

fun Modifier.directionalSwipe(direction: SwipeDirection, onSwipe: () -> Unit): Modifier =
    this then DirectionalSwipeElement(direction, onSwipe)

fun Modifier.directionalDrag(direction: SwipeDirection, handler: SwipeDragHandler): Modifier =
    this then DirectionalDragElement(direction, handler)

internal object SwipeIntent {
    enum class Decision { Undecided, Claim, Reject }

    fun decide(travel: Offset, sign: Float, slop: Float, heldTooLong: Boolean): Decision {
        if (heldTooLong) return Decision.Reject
        if (abs(travel.x) < slop && abs(travel.y) < slop) return Decision.Undecided
        val forward = travel.x * sign
        return if (forward >= slop && abs(travel.x) >= abs(travel.y) * DIRECTION_RATIO) Decision.Claim else Decision.Reject
    }
}

private interface SwipeSink {
    fun onClaim(travelX: Float)
    fun onMove(deltaX: Float)
    fun onEnd(velocityX: Float)
    fun onCancel()
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
) : DelegatingNode(), SwipeSink {
    private var travel = 0f
    private var fired = false
    private var triggerPx = 0f
    private var flingPx = 0f

    init {
        delegate(SuspendingPointerInputModifierNode {
            triggerPx = TriggerDistance.toPx()
            flingPx = FlingVelocity.toPx()
            detectClaimedSwipes({ direction.sign }, this@DirectionalSwipeNode)
        })
    }

    override fun onClaim(travelX: Float) {
        travel = travelX
        fired = false
        checkTrigger()
    }

    override fun onMove(deltaX: Float) {
        travel += deltaX
        checkTrigger()
    }

    override fun onEnd(velocityX: Float) {
        if (!fired && velocityX * direction.sign >= flingPx) onSwipe()
    }

    override fun onCancel() = Unit

    private fun checkTrigger() {
        if (!fired && travel * direction.sign >= triggerPx) {
            fired = true
            onSwipe()
        }
    }
}

private data class DirectionalDragElement(
    val direction: SwipeDirection,
    val handler: SwipeDragHandler
) : ModifierNodeElement<DirectionalDragNode>() {
    override fun create(): DirectionalDragNode = DirectionalDragNode(direction, handler)

    override fun update(node: DirectionalDragNode) {
        node.direction = direction
        node.handler = handler
    }
}

private class DirectionalDragNode(
    var direction: SwipeDirection,
    var handler: SwipeDragHandler
) : DelegatingNode(), SwipeSink {
    init {
        delegate(SuspendingPointerInputModifierNode { detectClaimedSwipes({ direction.sign }, this@DirectionalDragNode) })
    }

    override fun onClaim(travelX: Float) = handler.onSwipeDrag(travelX)

    override fun onMove(deltaX: Float) = handler.onSwipeDrag(deltaX)

    override fun onEnd(velocityX: Float) = handler.onSwipeRelease(velocityX)

    override fun onCancel() = handler.onSwipeRelease(0f)
}

private suspend fun PointerInputScope.detectClaimedSwipes(sign: () -> Float, sink: SwipeSink) {
    val slop = viewConfiguration.touchSlop
    val holdTimeout = viewConfiguration.longPressTimeoutMillis
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        if (down.type == PointerType.Mouse) return@awaitEachGesture
        val tracker = VelocityTracker().apply { addPointerInputChange(down) }
        var travel = Offset.Zero
        var claimed = false
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            val change = event.changes.firstOrNull { it.id == down.id }
            if (change == null || event.changes.count { it.pressed } > 1 || (!claimed && change.isConsumed)) {
                if (claimed) sink.onCancel()
                break
            }
            if (!change.pressed) {
                if (claimed) sink.onEnd(tracker.calculateVelocity().x)
                break
            }
            val delta = change.positionChange()
            travel += delta
            tracker.addPointerInputChange(change)
            if (!claimed) {
                val heldTooLong = change.uptimeMillis - down.uptimeMillis > holdTimeout
                when (SwipeIntent.decide(travel, sign(), slop, heldTooLong)) {
                    SwipeIntent.Decision.Reject -> break
                    SwipeIntent.Decision.Undecided -> {
                        if (claimedBelow(down.id)) break
                        continue
                    }
                    SwipeIntent.Decision.Claim -> {
                        claimed = true
                        change.consume()
                        sink.onClaim(travel.x)
                        continue
                    }
                }
            }
            change.consume()
            sink.onMove(delta.x)
        }
    }
}

private suspend fun AwaitPointerEventScope.claimedBelow(pointerId: PointerId): Boolean =
    awaitPointerEvent(PointerEventPass.Final).changes.any { it.id == pointerId && it.isConsumed }
