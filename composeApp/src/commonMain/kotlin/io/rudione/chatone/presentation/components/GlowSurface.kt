package io.rudione.chatone.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import io.rudione.chatone.presentation.theme.BlurType
import io.rudione.chatone.presentation.theme.LocalWallpaperController
import io.rudione.chatone.presentation.theme.radialAura

@Composable
fun GlowSurface(
    dominantColor: Color,
    modifier: Modifier = Modifier,
    intensity: Float = 1f,
    centerX: Float = 0.5f,
    centerY: Float = 0.5f,
    content: @Composable () -> Unit
) {
    val wallpaperController = LocalWallpaperController.current
    val state = wallpaperController.state
    val glowEnabled = state.glowEffectsEnabled
    val blurType = state.blurType

    val glowModifier = if (!glowEnabled) {
        modifier
    } else {
        when (blurType) {
            BlurType.NONE -> modifier

            BlurType.UNIFORM -> modifier.radialAura(
                color = dominantColor,
                radiusFactor = 1.2f,
                centerX = centerX,
                centerY = centerY
            )

            BlurType.FROSTED -> modifier.drawWithContent {
                drawContent()
                drawRect(
                    color = dominantColor.copy(alpha = 0.10f * intensity),
                    blendMode = BlendMode.SrcOver
                )
            }

            BlurType.RADIAL -> modifier.radialAura(
                color = dominantColor,
                radiusFactor = 1.4f * intensity,
                centerX = centerX,
                centerY = centerY
            )

            BlurType.VIGNETTE -> modifier.drawWithContent {
                drawContent()
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Transparent,
                            dominantColor.copy(alpha = 0.18f * intensity),
                            dominantColor.copy(alpha = 0.30f * intensity)
                        ),
                        center = Offset(size.width * centerX, size.height * centerY),
                        radius = size.maxDimension * 0.55f
                    ),
                    blendMode = BlendMode.SrcOver
                )
            }

            BlurType.LINEAR_H -> modifier.drawWithContent {
                drawContent()
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            dominantColor.copy(alpha = 0.22f * intensity),
                            dominantColor.copy(alpha = 0.06f * intensity),
                            dominantColor.copy(alpha = 0.22f * intensity)
                        )
                    ),
                    blendMode = BlendMode.SrcOver
                )
            }

            BlurType.LINEAR_V -> modifier.drawWithContent {
                drawContent()
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            dominantColor.copy(alpha = 0.22f * intensity),
                            dominantColor.copy(alpha = 0.06f * intensity),
                            dominantColor.copy(alpha = 0.22f * intensity)
                        )
                    ),
                    blendMode = BlendMode.SrcOver
                )
            }

            BlurType.SUNSHINE -> modifier.drawWithContent {
                drawContent()
                val cx = size.width * centerX
                val cy = size.height * centerY
                listOf(0.15f, 0.32f, 0.52f, 0.75f).forEachIndexed { i, frac ->
                    val alpha = (0.12f * (4 - i) * intensity).coerceAtMost(0.40f)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                dominantColor.copy(alpha = alpha),
                                dominantColor.copy(alpha = alpha * 0.4f),
                                Color.Transparent
                            ),
                            center = Offset(cx, cy),
                            radius = size.maxDimension * frac
                        ),
                        radius = size.maxDimension * frac,
                        center = Offset(cx, cy)
                    )
                }
            }
        }
    }

    Box(modifier = glowModifier) {
        content()
    }
}