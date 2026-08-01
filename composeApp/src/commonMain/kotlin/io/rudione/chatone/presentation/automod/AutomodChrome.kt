package io.rudione.chatone.presentation.automod

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import io.rudione.chatone.presentation.components.interactiveIcon
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal val DividerHitWidth = 12.dp

@Composable
internal fun <T> AutomodSegmented(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        options.forEach { (value, label) ->
            val isSelected = value == selected
            val background by animateColorAsState(
                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)
                else Color.Transparent,
                tween(180),
                label = "segmentBg"
            )
            val content by animateColorAsState(
                if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                tween(180),
                label = "segmentFg"
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(background)
                    .clickable { onSelect(value) }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    color = content,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
internal fun <T> AutomodTabRow(
    tabs: List<Triple<T, String, Int>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        tabs.forEach { (value, label, count) ->
            val isSelected = value == selected
            val background by animateColorAsState(
                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                else Color.Transparent,
                tween(200),
                label = "tabBg"
            )
            val content by animateColorAsState(
                if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                tween(200),
                label = "tabFg"
            )
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .background(background)
                    .clickable { onSelect(value) }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge,
                    color = content,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (count > 0) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(content.copy(alpha = 0.16f))
                            .padding(horizontal = 6.dp, vertical = 1.dp)
                    ) {
                        Text(
                            count.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = content,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun AutomodActionButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    buttonSize: Dp = 32.dp
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .size(buttonSize)
            .clip(RoundedCornerShape(9.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick
            ),
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
                .width(lineWidth)
                .fillMaxHeight()
                .background(lineColor)
        )
        Box(
            Modifier
                .width(4.dp)
                .height(gripHeight)
                .clip(RoundedCornerShape(2.dp))
                .background(
                    if (active) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outlineVariant
                )
        )
    }
}
