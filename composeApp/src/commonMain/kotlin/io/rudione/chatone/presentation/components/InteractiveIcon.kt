package io.rudione.chatone.presentation.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import io.rudione.chatone.presentation.components.motion.IconInteraction
import io.rudione.chatone.presentation.components.motion.LocalIconInteraction

const val ICON_HOVER_ROTATION = -7f
const val ICON_PRESS_ROTATION = 4f

@Composable
fun Modifier.interactiveIcon(
    interactionSource: InteractionSource,
    hoverRotation: Float = ICON_HOVER_ROTATION,
    pressRotation: Float = ICON_PRESS_ROTATION,
    enabled: Boolean = true
): Modifier {
    if (LocalControlMotion.current != ControlMotion.Default) return this
    val hovered by interactionSource.collectIsHoveredAsState()
    val pressed by interactionSource.collectIsPressedAsState()

    val targetRotation = when {
        !enabled -> 0f
        pressed -> pressRotation
        hovered -> hoverRotation
        else -> 0f
    }

    val rotation by animateFloatAsState(
        targetValue = targetRotation,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "interactiveIconRotation"
    )

    return this.graphicsLayer { rotationZ = rotation }
}

@Composable
fun ChatoneIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: IconButtonColors = IconButtonDefaults.iconButtonColors(),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val click = controlClick(onClick)
    IconButton(
        onClick = click,
        modifier = modifier.nativeControlPress(source),
        enabled = enabled,
        colors = colors,
        interactionSource = source
    ) {
        val iconInteraction = remember(source) { IconInteraction(source) }
        Box(
            modifier = Modifier.interactiveIcon(source, enabled = enabled && !iconInteraction.animatesItself),
            contentAlignment = Alignment.Center
        ) {
            CompositionLocalProvider(LocalIconInteraction provides iconInteraction) {
                content()
            }
        }
    }
}
