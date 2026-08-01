package io.rudione.chatone.presentation.settings.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.palette_fill_16
import io.rudione.chatone.presentation.components.ChatoneFieldLabel
import io.rudione.chatone.presentation.components.interactiveIcon
import io.rudione.chatone.presentation.settings.SettingsState
import io.rudione.chatone.presentation.theme.AccentPalette
import io.rudione.chatone.presentation.theme.DEFAULT_ACCENT_INDEX
import io.rudione.chatone.presentation.theme.ExpressivePalettes
import io.rudione.chatone.presentation.theme.LocalCustomThemeManager
import io.rudione.chatone.presentation.theme.accentColorScheme
import io.rudione.chatone.presentation.theme.accentPaletteAt
import io.rudione.chatone.presentation.theme.i18n.LocalStrings
import org.jetbrains.compose.resources.painterResource

private val PaletteCardHeight = 62.dp
private val PaletteCardCorner = 16.dp
private val PaletteCardSpacing = 8.dp
private val PaletteGridMaxHeight = 236.dp
private val TwoColumnMinWidth = 260.dp

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AccentColorPaletteRow(
    selectedIndex: Int,
    state: SettingsState,
    onOpenThemeCreator: (seedColor: Int?) -> Unit = {},
    onReset: () -> Unit = {},
    onSelect: (Int) -> Unit
) {
    val s = LocalStrings.current
    val customThemeManager = LocalCustomThemeManager.current
    val activeTheme by customThemeManager.currentTheme.collectAsState()
    val isCustomised = activeTheme != null || selectedIndex != DEFAULT_ACCENT_INDEX

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                ChatoneFieldLabel(
                    text = s.themeAccentColor,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(2.dp))
                ChatoneFieldLabel(
                    text = s.themeAccentColorHint,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (isCustomised) {
                    PaletteActionButton(
                        label = s.themeReset,
                        tint = MaterialTheme.colorScheme.error,
                        onClick = onReset
                    ) { iconModifier ->
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = iconModifier.size(14.dp)
                        )
                    }
                }
                PaletteActionButton(
                    label = s.settingsSetCustom,
                    tint = MaterialTheme.colorScheme.primary,
                    onClick = { onOpenThemeCreator(null) }
                ) { iconModifier ->
                    Icon(
                        painterResource(Res.drawable.palette_fill_16),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = iconModifier.size(14.dp)
                    )
                }
            }
        }

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val columns = if (maxWidth < TwoColumnMinWidth) 1 else 2
            val rows = (ExpressivePalettes.size + columns - 1) / columns
            val fullHeight = PaletteCardHeight * rows + PaletteCardSpacing * (rows - 1)
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = minOf(fullHeight, PaletteGridMaxHeight))
                    .verticalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(PaletteCardSpacing),
                verticalArrangement = Arrangement.spacedBy(PaletteCardSpacing),
                maxItemsInEachRow = columns
            ) {
                ExpressivePalettes.forEachIndexed { index, palette ->
                    PaletteCard(
                        palette = palette,
                        selected = index == selectedIndex && activeTheme == null,
                        modifier = Modifier.weight(1f),
                        onClick = { onSelect(index) }
                    )
                }
            }
        }

        ChatoneFieldLabel(
            text = activeTheme?.name ?: accentPaletteAt(selectedIndex).name,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PaletteActionButton(
    label: String,
    tint: Color,
    onClick: () -> Unit,
    icon: @Composable (Modifier) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(9.dp))
            .background(tint.copy(alpha = 0.10f))
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick
            )
            .padding(horizontal = 9.dp, vertical = 6.dp)
    ) {
        ChatoneFieldLabel(text = label, color = tint)
        icon(Modifier.interactiveIcon(interactionSource))
    }
}

@Composable
private fun PaletteCard(
    palette: AccentPalette,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val scheme = remember(palette) { accentColorScheme(palette) }
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val shape = RoundedCornerShape(PaletteCardCorner)

    val borderColor by animateColorAsState(
        targetValue = when {
            selected -> palette.accent.copy(alpha = 0.85f)
            hovered -> palette.accent.copy(alpha = 0.40f)
            else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
        },
        animationSpec = tween(180),
        label = "paletteBorder"
    )
    val scale by animateFloatAsState(
        targetValue = when {
            selected -> 1f
            hovered -> 0.998f
            else -> 0.985f
        },
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
        label = "paletteScale"
    )
    val bloom by animateFloatAsState(
        targetValue = when {
            selected -> 0.26f
            hovered -> 0.18f
            else -> 0.09f
        },
        animationSpec = tween(180),
        label = "paletteBloom"
    )

    Box(
        modifier = modifier
            .height(PaletteCardHeight)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(scheme.surfaceContainerHigh, scheme.surfaceContainerLowest)
                )
            )
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = borderColor,
                shape = shape
            )
            .hoverable(interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            palette.accent.copy(alpha = bloom),
                            Color.Transparent
                        ),
                        center = Offset(0f, 0f),
                        radius = 220f
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color.Transparent,
                            palette.accent.copy(alpha = if (selected) 0.75f else 0.30f),
                            Color.Transparent
                        )
                    )
                )
        )
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(palette.accent)
                )
                if (selected) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = scheme.onPrimary,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                ChatoneFieldLabel(text = palette.name, color = scheme.onSurface)
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().widthIn(max = 76.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    listOf(
                        scheme.surfaceContainerLowest,
                        scheme.surfaceContainer,
                        scheme.surfaceContainerHighest,
                        scheme.secondary,
                        scheme.tertiary
                    ).forEach { swatch ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(swatch)
                        )
                    }
                }
            }
        }
    }
}
