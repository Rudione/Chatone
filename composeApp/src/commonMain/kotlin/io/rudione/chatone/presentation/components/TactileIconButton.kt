package io.rudione.chatone.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp

private const val HOVER_FILL = 0.08f
private const val PRESS_FILL = 0.14f
private const val DISABLED_ALPHA = 0.38f
private const val COLOR_MS = 140

@Immutable
data class TactileIconColors(
    val content: Color,
    val hoveredContent: Color,
    val checkedContainer: Color,
    val checkedContent: Color,
    val overlay: Color
)

object TactileIconButtonDefaults {
    @Composable
    fun colors(
        content: Color = MaterialTheme.colorScheme.onSurfaceVariant,
        hoveredContent: Color = MaterialTheme.colorScheme.onSurface,
        checkedContainer: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
        checkedContent: Color = MaterialTheme.colorScheme.primary,
        overlay: Color = MaterialTheme.colorScheme.onSurface
    ): TactileIconColors = TactileIconColors(content, hoveredContent, checkedContainer, checkedContent, overlay)
}

@Composable
fun TactileIconButton(
    onClick: () -> Unit,
    size: Dp,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    checked: Boolean? = null,
    shape: Shape = CircleShape,
    pressedScale: Float = 0.84f,
    colors: TactileIconColors = TactileIconButtonDefaults.colors(),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable (tint: Color) -> Unit
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val hovered by source.collectIsHoveredAsState()
    val pressed by source.collectIsPressedAsState()
    val isChecked = checked == true
    val container by animateColorAsState(
        when {
            isChecked -> colors.checkedContainer
            else -> Color.Transparent
        },
        tween(COLOR_MS),
        label = "tactileContainer"
    )
    val overlay by animateColorAsState(
        colors.overlay.copy(
            alpha = when {
                !enabled -> 0f
                pressed -> PRESS_FILL
                hovered -> HOVER_FILL
                else -> 0f
            }
        ),
        tween(COLOR_MS),
        label = "tactileOverlay"
    )
    val tint by animateColorAsState(
        when {
            !enabled -> colors.content.copy(alpha = colors.content.alpha * DISABLED_ALPHA)
            isChecked -> colors.checkedContent
            hovered || pressed -> colors.hoveredContent
            else -> colors.content
        },
        tween(COLOR_MS),
        label = "tactileTint"
    )
    val interaction = if (checked != null) {
        Modifier.toggleable(
            value = isChecked,
            interactionSource = source,
            indication = null,
            enabled = enabled,
            role = Role.Switch,
            onValueChange = { onClick() }
        )
    } else {
        Modifier.clickable(
            interactionSource = source,
            indication = null,
            enabled = enabled,
            role = Role.Button,
            onClick = onClick
        )
    }
    Box(
        modifier = modifier
            .size(size)
            .tactilePress(source, pressedScale = if (enabled) pressedScale else 1f)
            .clip(shape)
            .drawBehind {
                if (container.alpha > 0f) drawRect(container)
                if (overlay.alpha > 0f) drawRect(overlay)
            }
            .then(interaction)
            .pointerHoverIcon(if (enabled) PointerIcon.Hand else PointerIcon.Default),
        contentAlignment = Alignment.Center
    ) {
        Box(modifier = Modifier.interactiveIcon(source, enabled = enabled), contentAlignment = Alignment.Center) {
            content(tint)
        }
    }
}
