package io.rudione.chatone.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import io.rudione.chatone.presentation.components.motion.LocalIconInteraction
import io.rudione.chatone.presentation.components.motion.MotionIconState

val LocalSidebarExpanded = staticCompositionLocalOf { false }

@Composable
fun AnimatedMenuIcon(
    expanded: Boolean,
    tint: Color,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    val progress = animateFloatAsState(if (expanded) 1f else 0f, tween(220), label = "sidebarMenu")
    val hover = rememberMenuHover()
    Canvas(modifier.semantics { contentDescription?.let { this.contentDescription = it } }) {
        drawMenuGlyph(tint, progress.value, hover?.value ?: 0f)
    }
}

@Composable
private fun rememberMenuHover(): Animatable<Float, AnimationVector1D>? {
    val interaction = LocalIconInteraction.current ?: return null
    if (LocalControlMotion.current != ControlMotion.Default) return null
    val state = remember { MotionIconState() }
    DisposableEffect(interaction) {
        interaction.animatesItself = true
        onDispose { interaction.animatesItself = false }
    }
    LaunchedEffect(interaction.source) { state.track(interaction.source) { null } }
    return state.hover
}

private const val MenuLineStagger = 0.12f

private fun DrawScope.drawMenuGlyph(tint: Color, progress: Float, hover: Float) {
    val stroke = size.minDimension * 0.085f
    val edge = size.minDimension
    val origin = Offset((size.width - edge) / 2, (size.height - edge) / 2)
    val open = hover * (1f - progress)
    val left = origin.x + edge * 0.18f
    val right = origin.x + edge * 0.82f
    val top = origin.y + edge * (0.25f - progress * 0.07f)
    val bottom = origin.y + edge * (0.75f + progress * 0.07f)
    val middle = size.height / 2
    val push = edge * 0.22f
    val drift = edge * 0.04f
    rotate(90f * hover * progress) {
        val first = slide(open, 0)
        drawLine(
            tint,
            Offset(left + push * first, top),
            Offset(right + drift * first, top + (bottom - top) * progress),
            stroke,
            StrokeCap.Round
        )
        val second = slide(open, 1)
        drawLine(
            tint.copy(alpha = tint.alpha * (1f - progress)),
            Offset(left + push * second, middle),
            Offset(right + drift * second, middle),
            stroke,
            StrokeCap.Round
        )
        val third = slide(open, 2)
        drawLine(
            tint,
            Offset(left + push * third, bottom),
            Offset(right + drift * third, bottom - (bottom - top) * progress),
            stroke,
            StrokeCap.Round
        )
        if (open > 0f) {
            val reach = (bottom - top) / 2 * open.coerceAtMost(1.08f)
            drawLine(
                tint.copy(alpha = tint.alpha * open.coerceIn(0f, 1f)),
                Offset(left, middle - reach),
                Offset(left, middle + reach),
                stroke,
                StrokeCap.Round
            )
        }
    }
}

private fun slide(open: Float, line: Int): Float {
    val delay = MenuLineStagger * line
    return ((open - delay) / (1f - delay)).coerceAtLeast(0f)
}

@Composable
fun AnimatedLockIcon(
    locked: Boolean,
    tint: Color,
    modifier: Modifier = Modifier
) {
    val progress by animateFloatAsState(if (locked) 1f else 0f, tween(240), label = "windowLock")
    Canvas(modifier) {
        val stroke = size.minDimension * 0.09f
        val body = Offset(size.width * 0.2f, size.height * 0.46f)
        drawRoundRect(
            tint, body, Size(size.width * 0.6f, size.height * 0.43f),
            CornerRadius(stroke * 1.1f), style = Stroke(stroke)
        )
        val lift = size.height * 0.11f * (1f - progress)
        val shift = size.width * 0.18f * (1f - progress)
        val shackle = Path().apply {
            moveTo(size.width * 0.34f + shift, body.y - lift)
            lineTo(size.width * 0.34f + shift, size.height * 0.29f - lift)
            cubicTo(
                size.width * 0.34f + shift, size.height * 0.06f - lift,
                size.width * 0.66f + shift, size.height * 0.06f - lift,
                size.width * 0.66f + shift, size.height * 0.29f - lift
            )
            lineTo(size.width * 0.66f + shift, body.y - lift)
        }
        drawPath(shackle, tint, style = Stroke(stroke, cap = StrokeCap.Round))
        drawLine(
            tint,
            Offset(size.width * 0.5f, size.height * 0.62f),
            Offset(size.width * 0.5f, size.height * 0.73f),
            stroke,
            StrokeCap.Round
        )
    }
}
