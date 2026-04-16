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


@Composable
fun WallpaperGlowEdge(
    dominantColor: Color,
    fromRight: Boolean = false,
    glowWidth: Dp = 120.dp,
    modifier: Modifier = Modifier
) {
    val glowColors = remember(dominantColor) {
        listOf(
            dominantColor.copy(alpha = 0.0f),
            dominantColor.copy(alpha = 0.06f),
            dominantColor.copy(alpha = 0.18f),
            dominantColor.copy(alpha = 0.30f),
            dominantColor.copy(alpha = 0.22f),
            dominantColor.copy(alpha = 0.12f)
        )
    }

    Box(
        modifier = modifier.drawWithContent {
            drawContent()
            val brush = if (fromRight) {

                Brush.horizontalGradient(
                    colors = glowColors.reversed(),
                    startX = size.width,
                    endX = size.width - glowWidth.toPx()
                )
            } else {

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
    centerY: Float = 0f
): Modifier = this.then(
    Modifier.drawWithContent {
        drawContent()

        val radius = size.maxDimension * radiusFactor

        val brush = Brush.radialGradient(
            colors = listOf(
                color.copy(alpha = 0.35f),
                color.copy(alpha = 0.25f),
                color.copy(alpha = 0.15f),
                color.copy(alpha = 0.08f),
                Color.Transparent
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
