package io.rudione.chatone.presentation.automod

import androidx.compose.runtime.getValue
import io.rudione.chatone.presentation.components.layoutHeight
import io.rudione.chatone.presentation.components.layoutWidth
import androidx.compose.ui.draw.drawBehind
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import io.rudione.chatone.presentation.components.interactiveIcon
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal val DividerHitWidth = 12.dp

@Composable
internal fun AutomodActionButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    buttonSize: Dp = 32.dp
) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val pressed by interactionSource.collectIsPressedAsState()
    val container by animateColorAsState(
        MaterialTheme.colorScheme.primary.copy(
            alpha = when {
                pressed -> 0.36f
                hovered -> 0.28f
                else -> 0.18f
            }
        ),
        tween(140),
        label = "actionButtonContainer"
    )
    Box(
        modifier = modifier
            .size(buttonSize)
            .clip(RoundedCornerShape(9.dp))
            .drawBehind { drawRect(container) }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .pointerHoverIcon(PointerIcon.Hand),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(buttonSize * 0.55f)
                .interactiveIcon(interactionSource)
        )
    }
}

@Composable
internal fun AutomodPaneDivider(
    onDelta: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val dragged by interactionSource.collectIsDraggedAsState()
    val active = hovered || dragged
    val currentOnDelta by rememberUpdatedState(onDelta)
    val lineColor by animateColorAsState(
        if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
        else MaterialTheme.colorScheme.outlineVariant,
        tween(160),
        label = "dividerColor"
    )
    val lineWidth by animateDpAsState(
        if (active) 2.dp else 1.dp,
        spring(dampingRatio = Spring.DampingRatioNoBouncy),
        label = "dividerWidth"
    )
    val gripHeight by animateDpAsState(
        if (active) 44.dp else 26.dp,
        spring(dampingRatio = Spring.DampingRatioLowBouncy),
        label = "dividerGrip"
    )

    Box(
        modifier = modifier
            .width(DividerHitWidth)
            .fillMaxHeight()
            .pointerHoverIcon(PointerIcon.Hand)
            .hoverable(interactionSource)
            .draggable(
                state = rememberDraggableState { delta -> currentOnDelta(delta) },
                orientation = Orientation.Horizontal,
                interactionSource = interactionSource
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .layoutWidth { lineWidth }
                .fillMaxHeight()
                .drawBehind { drawRect(lineColor) }
        )
        Box(
            Modifier
                .width(4.dp)
                .layoutHeight { gripHeight }
                .clip(RoundedCornerShape(2.dp))
                .background(
                    if (active) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outlineVariant
                )
        )
    }
}
