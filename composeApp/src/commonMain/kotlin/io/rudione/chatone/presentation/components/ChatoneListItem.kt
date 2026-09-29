package io.rudione.chatone.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp

private val ItemShape = RoundedCornerShape(10.dp)

@Composable
fun ChatoneListItem(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    highlighted: Boolean = false,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val container by animateColorAsState(
        when {
            highlighted -> lerp(scheme.surfaceContainerHighest, scheme.primary, 0.12f)
            hovered -> lerp(scheme.surfaceContainerHighest, scheme.onSurface, 0.04f)
            else -> scheme.surfaceContainerHighest.copy(alpha = 0.6f)
        },
        tween(140),
        label = "listItemContainer"
    )
    val border by animateColorAsState(
        when {
            highlighted -> scheme.primary.copy(alpha = 0.5f)
            hovered -> scheme.outlineVariant.copy(alpha = 0.7f)
            else -> scheme.outlineVariant.copy(alpha = 0.35f)
        },
        tween(140),
        label = "listItemBorder"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(ItemShape)
            .background(container)
            .border(1.dp, border, ItemShape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(interactionSource = interaction, indication = null, onClick = onClick)
                } else {
                    Modifier.hoverable(interaction)
                }
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (leading != null) leading()
        Column(modifier = Modifier.weight(1f), content = content)
        if (trailing != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                content = trailing
            )
        }
    }
}
