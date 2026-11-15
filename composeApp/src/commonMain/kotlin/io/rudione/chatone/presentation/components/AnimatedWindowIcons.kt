package io.rudione.chatone.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

val LocalSidebarExpanded = staticCompositionLocalOf { false }

@Composable
fun AnimatedMenuIcon(
    expanded: Boolean,
    tint: Color,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    val progress by animateFloatAsState(if (expanded) 1f else 0f, tween(220), label = "sidebarMenu")
    Canvas(modifier.semantics { contentDescription?.let { this.contentDescription = it } }) {
        val stroke = size.minDimension * 0.085f
        val edge = size.minDimension
        val origin = Offset((size.width - edge) / 2, (size.height - edge) / 2)
        val left = origin.x + edge * 0.18f
        val right = origin.x + edge * 0.82f
        val top = origin.y + edge * (0.25f - progress * 0.07f)
        val bottom = origin.y + edge * (0.75f + progress * 0.07f)
        drawLine(
            tint,
            Offset(left, top),
            Offset(right, top + (bottom - top) * progress),
            stroke,
            StrokeCap.Round
        )
        drawLine(
            tint.copy(alpha = tint.alpha * (1f - progress)),
            Offset(left, size.height / 2),
            Offset(right, size.height / 2),
            stroke,
            StrokeCap.Round
        )
        drawLine(
            tint,
            Offset(left, bottom),
            Offset(right, bottom - (bottom - top) * progress),
            stroke,
            StrokeCap.Round
        )
    }
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
