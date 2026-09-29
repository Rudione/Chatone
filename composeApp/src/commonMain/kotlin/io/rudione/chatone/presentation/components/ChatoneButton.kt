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

enum class ChatoneButtonTone { Primary, Neutral, Danger }

private val ButtonShape = RoundedCornerShape(10.dp)

@Composable
fun ChatoneButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tone: ChatoneButtonTone = ChatoneButtonTone.Primary,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    height: Dp = 32.dp
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val pressed by interaction.collectIsPressedAsState()
    val palette = buttonPalette(tone)
    val container by animateColorAsState(
        when {
            !enabled -> palette.container.copy(alpha = palette.container.alpha * 0.4f)
            pressed -> palette.pressed
            hovered -> palette.hovered
            else -> palette.container
        },
        tween(140),
        label = "buttonContainer"
    )
    val contentColor = if (enabled) palette.content else palette.content.copy(alpha = 0.5f)

    Row(
        modifier = modifier
            .height(height)
            .clip(ButtonShape)
            .background(container)
            .then(
                if (palette.border.alpha > 0f) Modifier.border(1.dp, palette.border, ButtonShape)
                else Modifier
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick
            )
            .pointerHoverIcon(if (enabled) PointerIcon.Hand else PointerIcon.Default)
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(16.dp))
        }
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            color = contentColor,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private data class ButtonPalette(
    val container: Color,
    val hovered: Color,
    val pressed: Color,
    val content: Color,
    val border: Color
)

@Composable
private fun buttonPalette(tone: ChatoneButtonTone): ButtonPalette {
    val scheme = MaterialTheme.colorScheme
    return when (tone) {
        ChatoneButtonTone.Primary -> ButtonPalette(
            container = scheme.primary,
            hovered = lerp(scheme.primary, Color.White, 0.12f),
            pressed = lerp(scheme.primary, Color.Black, 0.12f),
            content = scheme.onPrimary,
            border = Color.Transparent
        )
        ChatoneButtonTone.Neutral -> ButtonPalette(
            container = scheme.surfaceContainerHighest,
            hovered = lerp(scheme.surfaceContainerHighest, scheme.onSurface, 0.08f),
            pressed = lerp(scheme.surfaceContainerHighest, scheme.onSurface, 0.14f),
            content = scheme.onSurface,
            border = scheme.outlineVariant.copy(alpha = 0.55f)
        )
        ChatoneButtonTone.Danger -> ButtonPalette(
            container = scheme.error.copy(alpha = 0.14f),
            hovered = scheme.error.copy(alpha = 0.22f),
            pressed = scheme.error.copy(alpha = 0.3f),
            content = scheme.error,
            border = scheme.error.copy(alpha = 0.45f)
        )
    }
}
