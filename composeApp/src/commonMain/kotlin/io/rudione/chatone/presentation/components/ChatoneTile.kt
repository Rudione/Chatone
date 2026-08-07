package io.rudione.chatone.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.rudione.chatone.presentation.theme.ChatoneTheme

object ChatoneTileDefaults {
    val gap: Dp = 6.dp
    val outerPadding: Dp = 6.dp
    val radius: Dp = 16.dp
    val radiusSmall: Dp = 12.dp

    val contentPadding = PaddingValues(0.dp)
}

enum class TileTone { Base, Raised, Sunken }

@Composable
@ReadOnlyComposable
fun tileColor(tone: TileTone): Color = when (tone) {
    TileTone.Base -> MaterialTheme.colorScheme.surfaceContainerLow
    TileTone.Raised -> MaterialTheme.colorScheme.surfaceContainer
    TileTone.Sunken -> MaterialTheme.colorScheme.surfaceContainerLowest
}

@Composable
fun Modifier.chatoneTile(
    tone: TileTone = TileTone.Base,
    shape: Shape = RoundedCornerShape(ChatoneTileDefaults.radius),
    outlined: Boolean = true,
    alpha: Float = 1f
): Modifier {
    val fill = tileColor(tone)
    val line = ChatoneTheme.extraColors.cardBorder
    return this
        .clip(shape)
        .background(if (alpha == 1f) fill else fill.copy(alpha = alpha))
        .then(
            if (outlined) Modifier.border(1.dp, line.copy(alpha = 0.42f), shape) else Modifier
        )
}

@Composable
fun accentBrush(
    horizontal: Boolean = true,
    stops: List<Color> = ChatoneTheme.extraColors.accentGradient
): Brush = if (horizontal) Brush.horizontalGradient(stops) else Brush.verticalGradient(stops)
