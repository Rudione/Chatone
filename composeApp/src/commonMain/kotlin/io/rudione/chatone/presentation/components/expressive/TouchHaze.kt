package io.rudione.chatone.presentation.components.expressive

import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.PointerInputModifierNode
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.pow

fun Modifier.touchHaze(tint: Color, strength: Float = 1f): Modifier =
    this then TouchHazeElement(tint, strength)

private data class TouchHazeElement(val tint: Color, val strength: Float) :
    ModifierNodeElement<TouchHazeNode>() {
    override fun create() = TouchHazeNode(tint, strength)

    override fun update(node: TouchHazeNode) {
        node.tint = tint
        node.strength = strength
        node.invalidateDraw()
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "touchHaze"
        properties["tint"] = tint
        properties["strength"] = strength
    }
}

private class Puff(val center: Offset, val bornNanos: Long, val power: Float)

private class TouchHazeNode(var tint: Color, var strength: Float) :
    Modifier.Node(), DrawModifierNode, PointerInputModifierNode {

    private val puffs = ArrayList<Puff>(MAX_PUFFS)
    private var target = Offset.Unspecified
    private var glowAt = Offset.Unspecified
    private var glow = 0f
    private var glowGoal = 0f
    private var lastPuffAt = Offset.Unspecified
    private var nowNanos = 0L
    private var ticker: Job? = null

    override fun onPointerEvent(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        bounds: IntSize
    ) {
        if (pass != PointerEventPass.Initial) return
        val change = pointerEvent.changes.firstOrNull() ?: return
        val position = change.position
        val mouse = change.type == PointerType.Mouse
        when (pointerEvent.type) {
            PointerEventType.Press -> {
                follow(position, PRESSED_GLOW)
                puff(position, PRESS_POWER)
            }

            PointerEventType.Move -> {
                follow(position, if (change.pressed) PRESSED_GLOW else HOVER_GLOW)
                if (change.pressed && travelled(position, bounds) > PUFF_SPACING) puff(
                    position,
                    DRAG_POWER
                )
            }

            PointerEventType.Enter -> if (mouse) follow(position, HOVER_GLOW)
            PointerEventType.Release -> glowGoal = if (mouse) HOVER_GLOW else 0f
            PointerEventType.Exit -> glowGoal = 0f
            else -> return
        }
        invalidateDraw()
        wake()
    }

    override fun onCancelPointerInput() {
        glowGoal = 0f
        wake()
    }

    override fun onDetach() {
        ticker = null
        puffs.clear()
        glow = 0f
    }

    private fun follow(position: Offset, goal: Float) {
        target = position
        if (!glowAt.isSpecified()) glowAt = position
        glowGoal = goal
    }

    private fun puff(position: Offset, power: Float) {
        if (puffs.size == MAX_PUFFS) puffs.removeAt(0)
        val born = if (ticker?.isActive == true) nowNanos else 0L
        puffs += Puff(position, born, power)
        lastPuffAt = position
    }

    private fun travelled(position: Offset, bounds: IntSize): Float {
        if (!lastPuffAt.isSpecified()) return Float.MAX_VALUE
        val span = max(bounds.width, bounds.height).coerceAtLeast(1)
        return (position - lastPuffAt).getDistance() / span
    }

    private fun wake() {
        if (!isAttached || ticker?.isActive == true) return
        ticker = coroutineScope.launch {
            var last = withFrameNanos { it }
            nowNanos = last
            restampPending(last)
            while (true) {
                val frame = withFrameNanos { it }
                val dt = ((frame - last) / NANOS_PER_SECOND).coerceIn(0f, MAX_STEP_SECONDS)
                last = frame
                nowNanos = frame
                step(dt)
                invalidateDraw()
                if (settled()) break
            }
        }
    }

    private fun restampPending(frame: Long) {
        for (i in puffs.indices) {
            val p = puffs[i]
            if (p.bornNanos == 0L) puffs[i] = Puff(p.center, frame, p.power)
        }
    }

    private fun step(dt: Float) {
        puffs.removeAll { age(it) >= 1f }
        if (target.isSpecified() && glowAt.isSpecified()) {
            val follow = 1f - exp(-FOLLOW_RATE * dt)
            glowAt += (target - glowAt) * follow
        }
        val fade = 1f - exp(-GLOW_RATE * dt)
        glow += (glowGoal - glow) * fade
        if (glowGoal == 0f && glow < SETTLE_EPSILON) glow = 0f
    }

    private fun settled(): Boolean {
        if (puffs.isNotEmpty()) return false
        if (kotlin.math.abs(glow - glowGoal) > SETTLE_EPSILON) return false
        if (glowGoal == 0f) return true
        return !target.isSpecified() || (target - glowAt).getDistance() < SETTLE_DISTANCE_PX
    }

    private fun age(puff: Puff): Float =
        if (puff.bornNanos == 0L) 0f
        else ((nowNanos - puff.bornNanos) / (PUFF_LIFETIME_SECONDS * NANOS_PER_SECOND)).coerceIn(
            0f,
            1f
        )

    override fun ContentDrawScope.draw() {
        val span = max(size.width, size.height)
        if (span > 0f && strength > 0f) {
            if (glow > 0f && glowAt.isSpecified()) {
                val radius = span * GLOW_RADIUS
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            tint.copy(alpha = GLOW_ALPHA * glow * strength),
                            Color.Transparent
                        ),
                        center = glowAt,
                        radius = radius
                    ),
                    radius = radius,
                    center = glowAt
                )
            }
            for (puff in puffs) {
                val t = age(puff)
                val eased = 1f - (1f - t).pow(3)
                val radius = span * (PUFF_START + (PUFF_END - PUFF_START) * puff.power * eased)
                val alpha = PUFF_ALPHA * puff.power * strength * (1f - t).pow(1.6f)
                if (alpha <= 0f || radius <= 0f) continue
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            tint.copy(alpha = alpha),
                            tint.copy(alpha = alpha * 0.35f),
                            Color.Transparent
                        ),
                        center = puff.center,
                        radius = radius
                    ),
                    radius = radius,
                    center = puff.center
                )
            }
        }
        drawContent()
    }

    private fun Offset.isSpecified(): Boolean = this != Offset.Unspecified
}

private const val MAX_PUFFS = 8
private const val PRESS_POWER = 1f
private const val DRAG_POWER = 0.55f
private const val PRESSED_GLOW = 1f
private const val HOVER_GLOW = 0.45f
private const val PUFF_SPACING = 0.08f
private const val PUFF_LIFETIME_SECONDS = 1.6f
private const val PUFF_START = 0.05f
private const val PUFF_END = 0.5f
private const val PUFF_ALPHA = 0.42f
private const val GLOW_RADIUS = 0.34f
private const val GLOW_ALPHA = 0.28f
private const val FOLLOW_RATE = 7f
private const val GLOW_RATE = 4.5f
private const val SETTLE_EPSILON = 0.004f
private const val SETTLE_DISTANCE_PX = 0.5f
private const val MAX_STEP_SECONDS = 0.05f
private const val NANOS_PER_SECOND = 1_000_000_000f
