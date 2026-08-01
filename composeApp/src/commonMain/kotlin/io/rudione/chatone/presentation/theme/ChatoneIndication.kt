package io.rudione.chatone.presentation.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch

private val CompactTargetMaxSize: Dp = 64.dp
private const val PRESSED_SCALE = 0.90f
private const val HOVER_SCALE = 1.04f
private const val HOVER_OVERLAY_ALPHA = 0.07f
private const val PRESS_OVERLAY_ALPHA = 0.12f
private const val FOCUS_OVERLAY_ALPHA = 0.10f

object ChatoneIndication : IndicationNodeFactory {

    override fun create(interactionSource: InteractionSource): DelegatableNode =
        ChatoneIndicationNode(interactionSource)

    override fun hashCode(): Int = -0x2f1a7b31

    override fun equals(other: Any?): Boolean = other === this
}

private class ChatoneIndicationNode(
    private val interactionSource: InteractionSource
) : Modifier.Node(), DrawModifierNode {

    private val scale = Animatable(1f)
    private val overlay = Animatable(0f)

    private var pressed = false
    private var hovered = false
    private var focused = false

    override fun onAttach() {
        coroutineScope.launch {
            interactionSource.interactions.collect { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> pressed = true
                    is PressInteraction.Release -> pressed = false
                    is PressInteraction.Cancel -> pressed = false
                    is HoverInteraction.Enter -> hovered = true
                    is HoverInteraction.Exit -> hovered = false
                    is FocusInteraction.Focus -> focused = true
                    is FocusInteraction.Unfocus -> focused = false
                    else -> return@collect
                }
                animateToTargets()
            }
        }
    }

    private fun animateToTargets() {
        val targetOverlay = when {
            pressed -> PRESS_OVERLAY_ALPHA
            hovered -> HOVER_OVERLAY_ALPHA
            focused -> FOCUS_OVERLAY_ALPHA
            else -> 0f
        }
        val targetScale = when {
            pressed -> PRESSED_SCALE
            hovered -> HOVER_SCALE
            else -> 1f
        }
        coroutineScope.launch {
            overlay.animateTo(targetOverlay, tween(durationMillis = if (pressed) 90 else 160))
        }
        coroutineScope.launch {
            scale.animateTo(
                targetScale,
                spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
            )
        }
    }

    private fun ContentDrawScope.isCompactTarget(): Boolean {
        val limit = CompactTargetMaxSize.toPx()
        return size.width <= limit && size.height <= limit
    }

    override fun ContentDrawScope.draw() {
        val overlayAlpha = overlay.value
        val compact = isCompactTarget()
        val currentScale = if (compact) scale.value else 1f

        if (currentScale == 1f) {
            drawContent()
        } else {
            scale(currentScale, currentScale) { this@draw.drawContent() }
        }

        if (overlayAlpha > 0f) {
            drawRect(color = Color.White.copy(alpha = overlayAlpha), size = Size(size.width, size.height))
        }
    }
}
