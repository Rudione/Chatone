package io.rudione.chatone.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.rudione.chatone.icons.lucide.Check
import io.rudione.chatone.icons.lucide.Lucide

object ChatoneChipDefaults {
    val Height: Dp = 28.dp
    val CompactHeight: Dp = 22.dp
    val Shape = RoundedCornerShape(8.dp)
}

@Composable
fun ChatoneChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    accent: Color = MaterialTheme.colorScheme.primary,
    leadingIcon: ImageVector? = null,
    showCheckWhenSelected: Boolean = false,
    enabled: Boolean = true,
    height: Dp = ChatoneChipDefaults.Height
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val pressed by interaction.collectIsPressedAsState()
    val target = chipColors(selected, hovered && enabled, pressed && enabled, accent)
    val container by animateColorAsState(target.container, tween(140), label = "chipContainer")
    val border by animateColorAsState(target.border, tween(140), label = "chipBorder")
    val content by animateColorAsState(target.content, tween(140), label = "chipContent")
    val enabledAlpha = if (enabled) 1f else 0.45f
    val compact = height < ChatoneChipDefaults.Height
    val icon = leadingIcon ?: if (showCheckWhenSelected && selected) Lucide.Check else null

    Row(
        modifier = modifier
            .height(height)
            .clip(ChatoneChipDefaults.Shape)
            .background(container.copy(alpha = container.alpha * enabledAlpha))
            .border(1.dp, border.copy(alpha = border.alpha * enabledAlpha), ChatoneChipDefaults.Shape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick
            )
            .pointerHoverIcon(if (enabled) PointerIcon.Hand else PointerIcon.Default)
            .padding(horizontal = if (compact) 7.dp else 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally)
    ) {
        if (icon != null) {
            Icon(
                icon,
                contentDescription = null,
                tint = content.copy(alpha = content.alpha * enabledAlpha),
                modifier = Modifier.size(if (compact) 12.dp else 14.dp)
            )
        }
        Text(
            label,
            style = if (compact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium,
            color = content.copy(alpha = content.alpha * enabledAlpha),
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private data class ChipColors(val container: Color, val border: Color, val content: Color)

@Composable
private fun chipColors(selected: Boolean, hovered: Boolean, pressed: Boolean, accent: Color): ChipColors {
    val scheme = MaterialTheme.colorScheme
    return if (selected) {
        ChipColors(
            container = accent.copy(
                alpha = when {
                    pressed -> 0.32f
                    hovered -> 0.26f
                    else -> 0.18f
                }
            ),
            border = accent.copy(alpha = if (hovered) 0.7f else 0.5f),
            content = accent
        )
    } else {
        ChipColors(
            container = when {
                pressed -> lerp(scheme.surfaceContainerHighest, accent, 0.16f)
                hovered -> lerp(scheme.surfaceContainerHighest, accent, 0.08f)
                else -> scheme.surfaceContainerHighest.copy(alpha = 0.7f)
            },
            border = if (hovered) accent.copy(alpha = 0.45f) else scheme.outlineVariant.copy(alpha = 0.5f),
            content = if (hovered) scheme.onSurface else scheme.onSurfaceVariant
        )
    }
}
