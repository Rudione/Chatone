package io.rudione.chatone.presentation.theme

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Draws the wallpaper image behind chat content.
 * The image is centered and fills the area (ContentScale.Crop).
 * A semi-transparent overlay is applied to keep text readable,
 * adapting automatically to dark/light theme.
 */
@Composable
fun ChatBackgroundLayer(
    wallpaper: WallpaperState,
    modifier: Modifier = Modifier,
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val bitmap = wallpaper.imageBitmap
    val overlayAlpha = if (darkTheme) 0.55f else 0.45f
    val overlayColor = if (darkTheme)
        Color(0xFF0A0A0F).copy(alpha = overlayAlpha)
    else
        Color(0xFFF8F8FC).copy(alpha = overlayAlpha)

    Box(modifier = modifier) {
        if (bitmap != null && wallpaper.isActive) {
            Image(
                bitmap = wallpaper.imageBitmap,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(wallpaper.blurRadius.dp)
                    .drawWithContent {
                        drawContent()
                        drawRect(
                            brush = Brush.radialGradient(
                                colors = listOf(Color.Transparent, overlayColor.copy(alpha = overlayAlpha * 0.6f)),
                                radius = size.maxDimension * 0.7f
                            ),
                            blendMode = BlendMode.SrcOver
                        )
                        drawRect(color = overlayColor)
                    }
            )
        }

        content()
    }
}

/**
 * Sidebar/tab-bar glow effect.
 * Draws a gradient that bleeds the dominant wallpaper color
 * from the chat side (right edge) toward the sidebar (left/center).
 * Intensity fades with distance — the "aurora" effect.
 *
 * @param fromRight if true, gradient comes from the right (sidebar on left of chat)
 *                  if false, gradient comes from the bottom (tab bar above chat)
 */
@Composable
fun WallpaperGlowEdge(
    dominantColor: Color,
    fromRight: Boolean = false,  // true = sidebar left, false = tab bar top
    glowWidth: Dp = 120.dp,
    modifier: Modifier = Modifier
) {
    val glowColors = remember(dominantColor) {
        listOf(
            dominantColor.copy(alpha = 0.0f),  // far from chat — transparent
            dominantColor.copy(alpha = 0.06f),
            dominantColor.copy(alpha = 0.18f),
            dominantColor.copy(alpha = 0.30f),
            dominantColor.copy(alpha = 0.22f), // slight fade right at the edge
            dominantColor.copy(alpha = 0.12f)  // border glow
        )
    }

    Box(
        modifier = modifier.drawWithContent {
            drawContent()
            val brush = if (fromRight) {
                // Sidebar: glow bleeds from right edge (chat boundary) leftward
                Brush.horizontalGradient(
                    colors = glowColors.reversed(),
                    startX = size.width,
                    endX = size.width - glowWidth.toPx()
                )
            } else {
                // Tab bar: glow bleeds from bottom edge (chat boundary) upward
                Brush.verticalGradient(
                    colors = glowColors.reversed(),
                    startY = size.height,
                    endY = size.height - glowWidth.toPx()
                )
            }
            drawRect(brush = brush, blendMode = BlendMode.SrcOver)
        }
    )
}

fun Modifier.radialAura(
    color: Color,
    radiusFactor: Float = 1.2f,
    centerX: Float = 0.5f,
    centerY: Float = 0f // сверху по дефолту (для топбара)
): Modifier = this.then(
    Modifier.drawWithContent {
        drawContent()

        val radius = size.maxDimension * radiusFactor

        val brush = Brush.radialGradient(
            colors = listOf(
                color.copy(alpha = 0.35f), // центр
                color.copy(alpha = 0.25f),
                color.copy(alpha = 0.15f),
                color.copy(alpha = 0.08f),
                Color.Transparent // край
            ),
            center = Offset(size.width * centerX, size.height * centerY),
            radius = radius
        )

        drawRect(
            brush = brush,
            blendMode = BlendMode.SrcOver
        )
    }
)
