package io.rudione.chatone.tv.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Shapes
import androidx.tv.material3.darkColorScheme
import androidx.compose.material3.MaterialTheme as MobileMaterialTheme

object TvDimens {
    val ScreenHorizontal = 48.dp
    val ScreenVertical = 27.dp
    val CardGap = 16.dp
    val RowGap = 24.dp
    val StreamCardWidth = 240.dp
    val CategoryCardWidth = 132.dp
    val RailWidth = 72.dp
    val ChannelPanelWidth = 300.dp
}

object TvShapes {
    val Card = RoundedCornerShape(20.dp)
    val CardFocused = RoundedCornerShape(28.dp)
    val Panel = RoundedCornerShape(28.dp)
    val Chip = RoundedCornerShape(10.dp)
}

private val ExpressiveShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp)
)

@Composable
fun TvTheme(content: @Composable () -> Unit) {
    val mobile = MobileMaterialTheme.colorScheme
    val scheme = remember(mobile) { mobile.toTvColorScheme() }
    MaterialTheme(colorScheme = scheme, shapes = ExpressiveShapes) {
        CompositionLocalProvider(LocalContentColor provides scheme.onSurface, content = content)
    }
}

private fun ColorScheme.toTvColorScheme() = darkColorScheme(
    primary = primary,
    onPrimary = onPrimary,
    primaryContainer = primaryContainer,
    onPrimaryContainer = onPrimaryContainer,
    inversePrimary = inversePrimary,
    secondary = secondary,
    onSecondary = onSecondary,
    secondaryContainer = secondaryContainer,
    onSecondaryContainer = onSecondaryContainer,
    tertiary = tertiary,
    onTertiary = onTertiary,
    tertiaryContainer = tertiaryContainer,
    onTertiaryContainer = onTertiaryContainer,
    background = background,
    onBackground = onBackground,
    surface = surfaceContainer,
    onSurface = onSurface,
    surfaceVariant = surfaceContainerHighest,
    onSurfaceVariant = onSurfaceVariant,
    surfaceTint = surfaceTint,
    inverseSurface = inverseSurface,
    inverseOnSurface = inverseOnSurface,
    error = error,
    onError = onError,
    errorContainer = errorContainer,
    onErrorContainer = onErrorContainer,
    border = outline,
    borderVariant = outlineVariant,
    scrim = scrim
)
