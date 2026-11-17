package io.rudione.chatone.presentation.components.motion

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.rudione.chatone.presentation.components.ControlMotion
import io.rudione.chatone.presentation.components.LocalControlMotion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

@Stable
class IconInteraction(val source: InteractionSource) {
    var animatesItself by mutableStateOf(false)
}

val LocalIconInteraction = staticCompositionLocalOf<IconInteraction?> { null }

@Stable
internal class MotionIconState {
    val hover = Animatable(0f)
    val pulse = Animatable(0f)

    suspend fun play(motion: IconMotion) {
        pulse.snapTo(0f)
        pulse.animateTo(1f, tween(motion.durationMs, easing = LinearEasing))
        pulse.snapTo(0f)
    }

    suspend fun track(source: InteractionSource, motion: () -> IconMotion?) = coroutineScope {
        var hovers = 0
        var presses = 0
        source.interactions.collect { interaction ->
            when (interaction) {
                is HoverInteraction.Enter -> {
                    hovers++
                    motion()?.takeIf { it.pulseOnHover }?.let { launchPlay(it) }
                }
                is HoverInteraction.Exit -> hovers = (hovers - 1).coerceAtLeast(0)
                is PressInteraction.Press -> presses++
                is PressInteraction.Release -> {
                    presses = (presses - 1).coerceAtLeast(0)
                    motion()?.takeIf { it.pulseOnPress }?.let { launchPlay(it) }
                }
                is PressInteraction.Cancel -> presses = (presses - 1).coerceAtLeast(0)
            }
            val target = if (hovers > 0 || presses > 0) 1f else 0f
            if (hover.targetValue != target) {
                launch { hover.animateTo(target, HoverSpring) }
            }
        }
    }

    private fun CoroutineScope.launchPlay(motion: IconMotion) {
        launch { play(motion) }
    }

    private companion object {
        val HoverSpring = spring<Float>(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow)
    }
}

@Composable
fun MotionIcon(
    imageVector: ImageVector,
    motion: IconMotion,
    contentDescription: String?,
    tint: Color,
    modifier: Modifier = Modifier,
    pulse: Int = 0,
    interactionSource: InteractionSource? = null
) {
    val interaction = LocalIconInteraction.current
    val source = interactionSource ?: interaction?.source
    val still = LocalControlMotion.current == ControlMotion.Reduced
    val state = remember { MotionIconState() }
    val glyph = remember(imageVector) { GlyphCache.layers(imageVector) }
    val canvas = remember { GlyphCanvas() }
    val currentMotion by rememberUpdatedState(motion)
    val currentTint by rememberUpdatedState(tint)

    if (interaction != null && interactionSource == null) {
        DisposableEffect(interaction) {
            interaction.animatesItself = true
            onDispose { interaction.animatesItself = false }
        }
    }
    if (!still) {
        if (source != null) {
            LaunchedEffect(source) { state.track(source) { currentMotion } }
        }
        val seen = remember { intArrayOf(pulse) }
        LaunchedEffect(pulse) {
            val fresh = pulse > seen[0]
            seen[0] = pulse
            if (fresh) state.play(currentMotion)
        }
    }

    val semantics = if (contentDescription != null) {
        Modifier.semantics {
            this.contentDescription = contentDescription
            role = Role.Image
        }
    } else Modifier
    Spacer(
        modifier
            .then(semantics)
            .size(24.dp)
            .drawBehind {
                drawMotion(canvas, glyph, currentMotion, currentTint, state.hover.value, state.pulse.value)
            }
    )
}
