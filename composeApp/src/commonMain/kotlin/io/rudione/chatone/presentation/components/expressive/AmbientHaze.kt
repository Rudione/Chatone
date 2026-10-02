package io.rudione.chatone.presentation.components.expressive

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

@Composable
fun rememberAmbientPhase(animated: Boolean, periodMillis: Long = HAZE_PERIOD_MILLIS): State<Float> {
    val phase = remember { mutableFloatStateOf(0f) }
    if (animated) {
        LaunchedEffect(periodMillis) {
            while (true) {
                val now = withFrameMillis { it }
                phase.floatValue = (now % periodMillis).toFloat() / periodMillis
                delay(HAZE_FRAME_MILLIS)
            }
        }
    }
    return phase
}

@Composable
fun Modifier.ambientHaze(
    tint: Color = MaterialTheme.colorScheme.primary,
    accent: Color = MaterialTheme.colorScheme.tertiary,
    base: Color = MaterialTheme.colorScheme.background,
    strength: Float = 1f,
    animated: Boolean = true
): Modifier {
    val glow by animateColorAsState(tint, tween(HAZE_COLOR_MILLIS), label = "hazeTint")
    val side by animateColorAsState(accent, tween(HAZE_COLOR_MILLIS), label = "hazeAccent")
    val phase by rememberAmbientPhase(animated)
    return drawBehind {
        if (base.alpha > 0f) drawRect(base)
        val turn = phase * 2f * PI.toFloat()
        val span = max(size.width, size.height)
        val bottom = size.height
        drawRect(
            Brush.radialGradient(
                colors = listOf(
                    glow.copy(alpha = 0.56f * strength),
                    glow.copy(alpha = 0.22f * strength),
                    Color.Transparent
                ),
                center = Offset(
                    size.width * (0.5f + 0.12f * sin(turn)),
                    bottom * (1.08f + 0.03f * cos(turn))
                ),
                radius = span * 0.78f
            )
        )
        drawRect(
            Brush.radialGradient(
                colors = listOf(side.copy(alpha = 0.20f * strength), Color.Transparent),
                center = Offset(size.width * (0.18f + 0.1f * cos(turn)), bottom * 0.92f),
                radius = span * 0.5f
            )
        )
        drawRect(
            Brush.radialGradient(
                colors = listOf(glow.copy(alpha = 0.10f * strength), Color.Transparent),
                center = Offset(
                    size.width * (0.86f - 0.08f * sin(turn)),
                    bottom * (0.78f + 0.04f * sin(turn))
                ),
                radius = span * 0.42f
            )
        )
        drawRect(
            Brush.verticalGradient(
                0f to base.copy(alpha = 0.55f * base.alpha),
                0.45f to Color.Transparent
            )
        )
    }
}

private const val HAZE_PERIOD_MILLIS = 18_000L
private const val HAZE_FRAME_MILLIS = 42L
private const val HAZE_COLOR_MILLIS = 700
