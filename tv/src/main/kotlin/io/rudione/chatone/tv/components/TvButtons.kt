package io.rudione.chatone.tv.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceColors
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text

enum class TvButtonTone { Primary, Tonal, Ghost }

@Composable
private fun TvButtonTone.colors(): ClickableSurfaceColors {
    val scheme = MaterialTheme.colorScheme
    return when (this) {
        TvButtonTone.Primary -> ClickableSurfaceDefaults.colors(
            containerColor = scheme.primary,
            contentColor = scheme.onPrimary,
            focusedContainerColor = scheme.onSurface,
            focusedContentColor = scheme.surface,
            pressedContainerColor = scheme.primary,
            pressedContentColor = scheme.onPrimary
        )

        TvButtonTone.Tonal -> ClickableSurfaceDefaults.colors(
            containerColor = scheme.surfaceVariant,
            contentColor = scheme.onSurface,
            focusedContainerColor = scheme.onSurface,
            focusedContentColor = scheme.surface,
            pressedContainerColor = scheme.surfaceVariant,
            pressedContentColor = scheme.onSurface
        )

        TvButtonTone.Ghost -> ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            contentColor = scheme.onSurface,
            focusedContainerColor = scheme.onSurface,
            focusedContentColor = scheme.surface,
            pressedContainerColor = scheme.surfaceVariant,
            pressedContentColor = scheme.onSurface
        )
    }
}

@Composable
fun TvPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    tone: TvButtonTone = TvButtonTone.Primary,
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = ClickableSurfaceDefaults.shape(CircleShape),
        colors = tone.colors(),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.06f, pressedScale = 0.96f)
    ) {
        Row(
            Modifier.padding(contentPadding),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Text(
                text,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun TvIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tone: TvButtonTone = TvButtonTone.Tonal,
    selected: Boolean = false,
    size: Dp = 48.dp
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        modifier = modifier.size(size),
        shape = ClickableSurfaceDefaults.shape(CircleShape),
        colors = tone.colors(),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.14f, pressedScale = 0.94f),
        border = if (selected) {
            ClickableSurfaceDefaults.border(
                border = Border(BorderStroke(2.dp, scheme.primary), shape = CircleShape)
            )
        } else {
            ClickableSurfaceDefaults.border()
        }
    ) {
        Box(Modifier.size(size), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(22.dp))
        }
    }
}
