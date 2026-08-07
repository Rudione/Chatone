package io.rudione.chatone.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.rudione.chatone.presentation.chat.components.LiquidGlassTooltipBox
import io.rudione.chatone.presentation.theme.ChatoneIndication

@Immutable
data class RailAction(
    val icon: ImageVector,
    val label: String,
    val badge: Int = 0,
    val selected: Boolean = false,
    val enabled: Boolean = true,
    val onClick: () -> Unit
)

@Composable
fun ChatoneIconRail(
    actions: List<RailAction>,
    modifier: Modifier = Modifier,
    itemSize: Dp = 30.dp,
    iconSize: Dp = 16.dp,
    spacing: Dp = 6.dp
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        actions.forEach { action ->
            ChatoneRailButton(
                action = action,
                itemSize = itemSize,
                iconSize = iconSize
            )
        }
    }
}

@Composable
fun ChatoneRailButton(
    action: RailAction,
    modifier: Modifier = Modifier,
    itemSize: Dp = 30.dp,
    iconSize: Dp = 16.dp
) {
    val shape = RoundedCornerShape(9.dp)
    val background = when {
        action.selected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
    }
    val tint = when {
        !action.enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.30f)
        action.selected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    LiquidGlassTooltipBox(tooltip = action.label) {
        Box(modifier = modifier.size(itemSize)) {
            Box(
                modifier = Modifier
                    .size(itemSize)
                    .clip(shape)
                    .background(background)
                    .then(
                        if (action.selected) {
                            Modifier.border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f), shape)
                        } else Modifier
                    )
                    .clickable(
                        enabled = action.enabled,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ChatoneIndication,
                        onClick = action.onClick
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    action.icon,
                    contentDescription = action.label,
                    tint = tint,
                    modifier = Modifier.size(iconSize)
                )
            }
            if (action.badge > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 3.dp, y = (-3).dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error)
                        .padding(horizontal = 4.dp, vertical = 1.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (action.badge > 99) "99+" else action.badge.toString(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 8.sp,
                            fontFeatureSettings = "tnum"
                        ),
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}
