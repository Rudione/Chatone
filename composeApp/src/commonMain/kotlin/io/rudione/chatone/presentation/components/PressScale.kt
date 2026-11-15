package io.rudione.chatone.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.launch

@Composable
fun Modifier.pressScale(interaction: MutableInteractionSource, pressedScale: Float = 0.96f): Modifier {
    val motion = LocalControlMotion.current
    if (motion == ControlMotion.Reduced) return this
    val targetScale = if (motion == ControlMotion.Ios) pressedScale.coerceAtLeast(0.96f) else pressedScale
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) targetScale else 1f,
        animationSpec = spring(dampingRatio = if (motion == ControlMotion.Ios) Spring.DampingRatioNoBouncy else Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "pressScale"
    )
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

private const val TACTILE_DIP_MS = 70
private const val TACTILE_SETTLED_EPSILON = 0.015f

@Composable
fun Modifier.tactilePress(interactionSource: InteractionSource, pressedScale: Float = 0.9f): Modifier {
    val motion = LocalControlMotion.current
    if (motion == ControlMotion.Reduced) return this
    val targetScale = if (motion == ControlMotion.Ios) pressedScale.coerceAtLeast(0.96f) else pressedScale
    val scale = remember { Animatable(1f) }
    LaunchedEffect(interactionSource, targetScale, motion) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> launch {
                    scale.animateTo(targetScale, spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessHigh))
                }
                is PressInteraction.Release, is PressInteraction.Cancel -> launch {
                    if ((interaction is PressInteraction.Release || motion == ControlMotion.Default) && scale.value > targetScale + TACTILE_SETTLED_EPSILON) {
                        scale.animateTo(targetScale, tween(TACTILE_DIP_MS))
                    }
                    scale.animateTo(1f, spring(dampingRatio = if (motion == ControlMotion.Ios) Spring.DampingRatioNoBouncy else Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow))
                }
            }
        }
    }
    return graphicsLayer {
        scaleX = scale.value
        scaleY = scale.value
    }
}
