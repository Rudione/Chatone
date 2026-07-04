package io.rudione.chatone.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import io.rudione.chatone.presentation.theme.ChatoneTheme
import io.rudione.chatone.presentation.theme.LocalWallpaperController
import io.rudione.chatone.presentation.theme.glassAlphasFromIntensity
import io.rudione.chatone.presentation.theme.sidebarBackgroundColor


@Composable
fun LiquidGlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
   
    glassIntensity: Float = 0.72f,
   
    tintColor: Color? = null,
   
    backgroundAlphaHigh: Float = 0.92f,
    backgroundAlphaLow: Float = 0.80f,
    borderAlphaHigh: Float = 0.20f,
    borderAlphaLow: Float = 0.05f,
    content: @Composable () -> Unit
) {
    val extra = ChatoneTheme.extraColors
    val wallpaperCtrl = LocalWallpaperController.current
    val liveWallpaper by remember { derivedStateOf { wallpaperCtrl.state } }

   
    val baseColor = tintColor ?: sidebarBackgroundColor(liveWallpaper, extra.sidebarSurface)

   
    val effectiveIntensity = if (tintColor != null) glassIntensity
    else liveWallpaper.panelColorConfig.sidebarGlassIntensity

    val (alphaHigh, alphaLow) = glassAlphasFromIntensity(effectiveIntensity)


    val bgLum = baseColor.luminance()
    val bHigh = if (bgLum < 0.25f) 0.30f else 0.08f
    val bLow  = if (bgLum < 0.25f) 0.07f else 0.02f

    val glowEnabled = liveWallpaper.glowEffectsEnabled

    Box(
        modifier = modifier
            .clip(shape)
            .then(
                if (glowEnabled) {
                    Modifier
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    baseColor.copy(alpha = alphaHigh),
                                    baseColor.copy(alpha = alphaLow)
                                )
                            )
                        )
                        .border(
                            width = 1.dp,
                            brush = Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(alpha = bHigh),
                                    Color.White.copy(alpha = bLow)
                                )
                            ),
                            shape = shape
                        )
                } else {
                    Modifier
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            shape = shape
                        )
                }
            )
    ) {
        Box(modifier = Modifier.padding(contentPadding)) { content() }
    }
}

@Composable
fun LiquidGlassDropdownItem(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = iconTint)
        Text(text, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}