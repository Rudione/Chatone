package io.rudione.chatone.presentation.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.rudione.chatone.presentation.theme.ChatoneTheme
import io.rudione.chatone.presentation.theme.LocalWallpaperController
import io.rudione.chatone.presentation.theme.glassAlphasFromIntensity
import io.rudione.chatone.presentation.theme.sidebarBackgroundColor

@Composable
fun Modifier.chatoneGlassPanel(
    shape: Shape = RoundedCornerShape(12.dp),
    elevation: Dp = 10.dp,
    topAlpha: Float = 0.93f,
    bottomAlpha: Float = 0.97f
): Modifier = this
    .shadow(
        elevation, shape,
        ambientColor = Color.Black.copy(alpha = 0.25f),
        spotColor = Color.Black.copy(alpha = 0.3f)
    )
    .clip(shape)
    .background(
        Brush.verticalGradient(
            listOf(
                MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = topAlpha),
                MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = bottomAlpha)
            )
        )
    )
    .border(
        1.dp,
        Brush.verticalGradient(
            listOf(Color.White.copy(alpha = 0.16f), Color.White.copy(alpha = 0.04f))
        ),
        shape
    )

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

    forceGlass: Boolean = false,
    shimmer: Boolean = false,
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

    val glowEnabled = liveWallpaper.glowEffectsEnabled || forceGlass

    val shimmerTransition = rememberInfiniteTransition(label = "liquidGlassShimmer")
    val shimmerProgress by shimmerTransition.animateFloat(
        initialValue = -0.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "liquidGlassShimmerProgress"
    )

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
            .then(
                if (shimmer && glowEnabled) {
                    Modifier.drawWithContent {
                        drawContent()
                        val bandWidth = size.width * 0.4f
                        val travel = size.width + bandWidth
                        val startX = shimmerProgress * travel - bandWidth
                        drawRect(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.White.copy(alpha = 0.20f),
                                    Color.Transparent
                                ),
                                start = Offset(startX, 0f),
                                end = Offset(startX + bandWidth, size.height)
                            ),
                            blendMode = BlendMode.Plus
                        )
                    }
                } else {
                    Modifier
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
