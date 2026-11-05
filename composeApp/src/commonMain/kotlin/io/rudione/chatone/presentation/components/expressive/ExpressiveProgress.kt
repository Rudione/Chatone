package io.rudione.chatone.presentation.components.expressive

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

private class MorphShape(val lobes: Int, val depth: Float)

private val MorphSequence = listOf(
    MorphShape(lobes = 8, depth = 0.17f),
    MorphShape(lobes = 1, depth = 0f),
    MorphShape(lobes = 9, depth = 0.08f),
    MorphShape(lobes = 5, depth = 0.12f),
    MorphShape(lobes = 1, depth = 0f),
    MorphShape(lobes = 4, depth = 0.16f),
    MorphShape(lobes = 12, depth = 0.05f)
)

private const val MORPH_MS = 650
private const val SPIN_MS = 4_800
private const val OUTLINE_STEPS = 144
private const val TWO_PI = (2 * PI).toFloat()

@Composable
fun MorphingLoadingIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    color: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer
) {
    val transition = rememberInfiniteTransition(label = "morphingLoader")
    val morph = transition.animateFloat(
        initialValue = 0f,
        targetValue = MorphSequence.size.toFloat(),
        animationSpec = infiniteRepeatable(tween(MORPH_MS * MorphSequence.size, easing = LinearEasing)),
        label = "morph"
    )
    val spin = transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(SPIN_MS, easing = LinearEasing)),
        label = "spin"
    )
    val path = remember { Path() }
    Box(
        modifier
            .size(size)
            .semantics { progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate }
            .drawWithCache {
                onDrawBehind {
                    if (containerColor.alpha > 0f) drawCircle(containerColor)
                    val position = morph.value
                    val index = floor(position).toInt()
                    val local = FastOutSlowInEasing.transform(position - index)
                    val from = MorphSequence[index % MorphSequence.size]
                    val to = MorphSequence[(index + 1) % MorphSequence.size]
                    val radius = this.size.minDimension * if (containerColor.alpha > 0f) 0.3f else 0.42f
                    val centre = center
                    path.reset()
                    for (step in 0..OUTLINE_STEPS) {
                        val angle = TWO_PI * step / OUTLINE_STEPS
                        val bumpFrom = from.depth * cos(from.lobes * angle)
                        val bumpTo = to.depth * cos(to.lobes * angle)
                        val r = radius * (1f + bumpFrom + (bumpTo - bumpFrom) * local)
                        val x = centre.x + r * cos(angle)
                        val y = centre.y + r * sin(angle)
                        if (step == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    path.close()
                    rotate(spin.value + local * 90f, centre) { drawPath(path, color) }
                }
            }
    )
}

@Composable
fun WavyLinearProgress(
    modifier: Modifier = Modifier,
    progress: (() -> Float)? = null,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    strokeWidth: Dp = 4.dp,
    amplitude: Dp = 3.dp,
    wavelength: Dp = 22.dp
) {
    val transition = rememberInfiniteTransition(label = "wavyProgress")
    val phase = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1_000, easing = LinearEasing)),
        label = "wavePhase"
    )
    val sweep = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1_600, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "waveSweep"
    )
    val wave = remember { Path() }
    Box(
        modifier
            .fillMaxWidth()
            .height(amplitude * 2 + strokeWidth)
            .semantics {
                progressBarRangeInfo = progress?.let { ProgressBarRangeInfo(it().coerceIn(0f, 1f), 0f..1f) }
                    ?: ProgressBarRangeInfo.Indeterminate
            }
            .drawWithCache {
                val stroke = strokeWidth.toPx()
                val amp = amplitude.toPx()
                val length = wavelength.toPx()
                val gap = stroke * 1.5f
                val mid = size.height / 2f
                onDrawBehind {
                    val (start, end) = progress?.let { 0f to it().coerceIn(0f, 1f) * size.width } ?: run {
                        val head = sweep.value
                        val tail = (head - 0.35f).coerceAtLeast(0f)
                        tail * size.width to (head * 1.2f).coerceAtMost(1f) * size.width
                    }
                    if (start > stroke) {
                        drawLine(trackColor, Offset(stroke / 2, mid), Offset((start - gap).coerceAtLeast(stroke / 2), mid), stroke, StrokeCap.Round)
                    }
                    if (end < size.width - stroke) {
                        drawLine(trackColor, Offset(end + gap, mid), Offset(size.width - stroke / 2, mid), stroke, StrokeCap.Round)
                    }
                    if (end - start > 1f) {
                        wave.reset()
                        val shift = phase.value * length
                        var x = start
                        wave.moveTo(x, mid + amp * sin((x + shift) / length * TWO_PI))
                        while (x < end) {
                            x = (x + 2f).coerceAtMost(end)
                            wave.lineTo(x, mid + amp * sin((x + shift) / length * TWO_PI))
                        }
                        drawPath(wave, color, style = Stroke(width = stroke, cap = StrokeCap.Round))
                    }
                }
            }
    )
}
