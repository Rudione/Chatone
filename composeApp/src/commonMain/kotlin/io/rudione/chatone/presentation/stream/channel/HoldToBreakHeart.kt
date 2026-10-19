package io.rudione.chatone.presentation.stream.channel

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.rudione.chatone.icons.lucide.Heart
import io.rudione.chatone.icons.lucide.Lucide
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val HOLD_MS = 2_000
private const val TICK_MS = 140L
private const val BREAK_MS = 560
private const val BREAK_ANGLE = 18f
private val ButtonSize = 96.dp
private val HeartSize = 40.dp
private val RingWidth = 4.dp
private val BreakSpread = 7.dp
private val BreakDrop = 5.dp

@Composable
internal fun HoldToBreakHeart(
    label: String,
    onBroken: () -> Unit,
    modifier: Modifier = Modifier,
    diameter: Dp = ButtonSize
) {
    val colors = MaterialTheme.colorScheme
    val idle = colors.surfaceContainerHighest
    val idleContent = colors.onSurface
    val danger = colors.error
    val onDanger = colors.onError
    val progress = remember { Animatable(0f) }
    val breaking = remember { Animatable(0f) }
    val press = remember { Animatable(1f) }
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val currentOnBroken by rememberUpdatedState(onBroken)
    var broken by remember { mutableStateOf(false) }
    val heart = rememberVectorPainter(Lucide.Heart)

    Spacer(
        modifier
            .size(diameter)
            .graphicsLayer {
                scaleX = press.value
                scaleY = press.value
            }
            .semantics {
                contentDescription = label
                role = Role.Button
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown().consume()
                    if (broken) return@awaitEachGesture
                    val hold: Job = scope.launch {
                        launch { press.animateTo(0.93f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium)) }
                        val ticks = launch {
                            while (isActive) {
                                haptics.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
                                delay(TICK_MS)
                            }
                        }
                        val remaining = ((1f - progress.value) * HOLD_MS).toInt().coerceAtLeast(1)
                        progress.animateTo(1f, tween(remaining, easing = LinearEasing))
                        ticks.cancel()
                        broken = true
                        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                        launch { press.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessMediumLow)) }
                        breaking.animateTo(1f, tween(BREAK_MS, easing = FastOutSlowInEasing))
                        currentOnBroken()
                    }
                    waitForUpOrCancellation()
                    if (!broken) {
                        hold.cancel()
                        scope.launch { press.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMedium)) }
                        scope.launch { progress.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow)) }
                    }
                }
            }
            .drawWithCache {
                val circle = Path().apply { addOval(Rect(Offset.Zero, size)) }
                val heartPx = HeartSize.toPx()
                val ring = RingWidth.toPx()
                val halves = crackHalves(heartPx)
                onDrawBehind {
                    val level = progress.value
                    drawCircle(idle)
                    if (level > 0f) {
                        val top = size.height * (1f - level)
                        clipPath(circle) {
                            drawRect(danger, topLeft = Offset(0f, top), size = Size(size.width, size.height - top))
                        }
                        drawArc(
                            color = danger,
                            startAngle = -90f,
                            sweepAngle = 360f * level,
                            useCenter = false,
                            topLeft = Offset(ring / 2f, ring / 2f),
                            size = Size(size.width - ring, size.height - ring),
                            style = Stroke(ring)
                        )
                    }
                    val tint = ColorFilter.tint(lerp(idleContent, onDanger, level))
                    val origin = Offset((size.width - heartPx) / 2f, (size.height - heartPx) / 2f)
                    drawHeart(heart, heartPx, origin, tint, breaking.value, halves, BreakSpread.toPx(), BreakDrop.toPx())
                }
            }
    )
}

private class CrackHalves(val left: Path, val right: Path)

private fun crackHalves(size: Float): CrackHalves {
    val crack = listOf(
        Offset(0.5f, 0.18f),
        Offset(0.42f, 0.4f),
        Offset(0.57f, 0.58f),
        Offset(0.45f, 0.76f),
        Offset(0.5f, 1f)
    ).map { Offset(it.x * size, it.y * size) }
    fun half(edge: Float) = Path().apply {
        moveTo(edge, 0f)
        lineTo(crack.first().x, 0f)
        crack.forEach { lineTo(it.x, it.y) }
        lineTo(edge, size)
        close()
    }
    return CrackHalves(left = half(0f), right = half(size))
}

private fun DrawScope.drawHeart(
    painter: Painter,
    sizePx: Float,
    origin: Offset,
    tint: ColorFilter,
    breaking: Float,
    halves: CrackHalves,
    spread: Float,
    drop: Float
) {
    val box = Size(sizePx, sizePx)
    if (breaking <= 0f) {
        translate(origin.x, origin.y) { with(painter) { draw(box, colorFilter = tint) } }
        return
    }
    val fade = (1f - ((breaking - 0.55f) / 0.45f).coerceIn(0f, 1f))
    val pivot = Offset(sizePx / 2f, sizePx)
    listOf(halves.left to -1f, halves.right to 1f).forEach { (half, side) ->
        translate(origin.x + side * spread * breaking, origin.y + drop * breaking) {
            rotate(side * BREAK_ANGLE * breaking, pivot) {
                clipPath(half) { with(painter) { draw(box, alpha = fade, colorFilter = tint) } }
            }
        }
    }
}
