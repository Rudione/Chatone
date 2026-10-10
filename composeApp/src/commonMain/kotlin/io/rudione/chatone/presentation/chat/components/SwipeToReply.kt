package io.rudione.chatone.presentation.chat.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.SuspendingPointerInputModifierNode
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

@Immutable
internal class SwipeReplyStyle(
    val icon: Painter,
    val iconTint: Color,
    val container: Color
)

internal fun Modifier.swipeToReply(enabled: Boolean, style: SwipeReplyStyle, onReply: () -> Unit): Modifier =
    if (enabled) this then SwipeToReplyElement(style, onReply) else this

private data class SwipeToReplyElement(
    val style: SwipeReplyStyle,
    val onReply: () -> Unit
) : ModifierNodeElement<SwipeToReplyNode>() {
    override fun create(): SwipeToReplyNode = SwipeToReplyNode(style, onReply)

    override fun update(node: SwipeToReplyNode) {
        node.style = style
        node.onReply = onReply
    }
}

private class SwipeToReplyNode(
    var style: SwipeReplyStyle,
    var onReply: () -> Unit
) : DelegatingNode(), DrawModifierNode, CompositionLocalConsumerModifierNode {

    private var shift by mutableFloatStateOf(0f)
    private var pop by mutableFloatStateOf(1f)
    private var armed = false
    private var settleJob: Job? = null
    private var popJob: Job? = null

    init {
        delegate(SuspendingPointerInputModifierNode { detectReplySwipe() })
    }

    override fun onDetach() {
        settleJob?.cancel()
        popJob?.cancel()
        shift = 0f
        pop = 1f
        armed = false
    }

    override fun ContentDrawScope.draw() {
        val offset = shift
        if (offset == 0f) {
            drawContent()
            return
        }
        translate(left = offset) { this@draw.drawContent() }
        val threshold = TriggerDistance.toPx()
        val revealed = -offset
        val progress = (revealed / threshold).coerceIn(0f, 1f)
        val radius = BubbleRadius.toPx()
        val center = Offset(
            x = size.width - min(revealed, threshold) / 2f,
            y = size.height / 2f
        )
        val bubbleScale = (MIN_BUBBLE_SCALE + (1f - MIN_BUBBLE_SCALE) * progress) * pop
        scale(bubbleScale, pivot = center) {
            drawCircle(
                color = style.container,
                radius = radius,
                center = center,
                alpha = if (armed) 1f else progress * IDLE_BUBBLE_ALPHA
            )
            val iconSize = IconSize.toPx()
            translate(center.x - iconSize / 2f, center.y - iconSize / 2f) {
                with(style.icon) {
                    draw(Size(iconSize, iconSize), alpha = progress, colorFilter = ColorFilter.tint(style.iconTint))
                }
            }
        }
    }

    private suspend fun PointerInputScope.detectReplySwipe() {
        val slop = viewConfiguration.touchSlop
        val threshold = TriggerDistance.toPx()
        val limit = threshold * MAX_STRETCH
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            var travelX = 0f
            var travelY = 0f
            var claimed = false
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                if (event.changes.count { it.pressed } > 1) break
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                if (!change.pressed) break
                if (!claimed && change.isConsumed) return@awaitEachGesture
                val delta = change.positionChange()
                travelX += delta.x
                travelY += delta.y
                if (!claimed) {
                    if (abs(travelX) < slop && abs(travelY) < slop) continue
                    if (travelX > -slop || abs(travelX) < abs(travelY) * DIRECTION_RATIO) return@awaitEachGesture
                    claimed = true
                    settleJob?.cancel()
                }
                change.consume()
                val pulled = max(0f, -(travelX + slop))
                shift = -resisted(pulled, threshold, limit)
                updateArmed(pulled >= threshold)
            }
            if (claimed) release()
        }
    }

    private fun updateArmed(reached: Boolean) {
        if (reached == armed) return
        armed = reached
        if (!reached) return
        currentValueOf(LocalHapticFeedback).performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
        popJob?.cancel()
        popJob = coroutineScope.launch {
            animate(POP_SCALE, 1f, animationSpec = spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMedium)) { value, _ ->
                pop = value
            }
        }
    }

    private fun release() {
        val reply = armed
        armed = false
        if (reply) onReply()
        val from = shift
        settleJob = coroutineScope.launch {
            animate(from, 0f, animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow)) { value, _ ->
                shift = value
            }
        }
    }

    private fun resisted(pulled: Float, threshold: Float, limit: Float): Float =
        if (pulled <= threshold) pulled else min(limit, threshold + (pulled - threshold) * OVERSHOOT_RESISTANCE)

    private companion object {
        val TriggerDistance = 64.dp
        val BubbleRadius = 15.dp
        val IconSize = 18.dp
        const val DIRECTION_RATIO = 1.6f
        const val MAX_STRETCH = 1.6f
        const val OVERSHOOT_RESISTANCE = 0.35f
        const val MIN_BUBBLE_SCALE = 0.45f
        const val IDLE_BUBBLE_ALPHA = 0.55f
        const val POP_SCALE = 1.28f
    }
}
