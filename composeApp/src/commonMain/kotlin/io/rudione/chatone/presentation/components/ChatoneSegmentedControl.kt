package io.rudione.chatone.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

private val OuterShape = RoundedCornerShape(10.dp)
private val SegmentShape = RoundedCornerShape(8.dp)

@Composable
fun <T> ChatoneSegmentedControl(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    fillWidth: Boolean = false,
    accentOf: (T) -> Color? = { null },
    iconOf: (T) -> ImageVector? = { null }
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .then(if (fillWidth) Modifier.fillMaxWidth() else Modifier)
            .height(30.dp)
            .clip(OuterShape)
            .background(scheme.surfaceContainerHighest.copy(alpha = 0.6f))
            .border(1.dp, scheme.outlineVariant.copy(alpha = 0.45f), OuterShape)
            .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        options.forEach { (value, label) ->
            Segment(
                label = label,
                selected = value == selected,
                accent = accentOf(value) ?: scheme.primary,
                icon = iconOf(value),
                modifier = if (fillWidth) Modifier.weight(1f) else Modifier,
                onClick = { onSelect(value) }
            )
        }
    }
}

@Composable
private fun Segment(
    label: String,
    selected: Boolean,
    accent: Color,
    icon: ImageVector?,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val container by animateColorAsState(
        when {
            selected -> accent.copy(alpha = 0.2f)
            hovered -> scheme.onSurface.copy(alpha = 0.06f)
            else -> Color.Transparent
        },
        tween(140),
        label = "segmentContainer"
    )
    val content by animateColorAsState(
        when {
            selected -> accent
            hovered -> scheme.onSurface
            else -> scheme.onSurfaceVariant
        },
        tween(140),
        label = "segmentContent"
    )
    Row(
        modifier = modifier
            .fillMaxHeight()
            .clip(SegmentShape)
            .background(container)
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Tab,
                onClick = onClick
            )
            .pointerHoverIcon(PointerIcon.Hand)
            .padding(horizontal = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(13.dp))
        }
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = content,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
