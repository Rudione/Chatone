package io.rudione.chatone.presentation.stream.ambient

import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositeShader
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.luminance

private const val VIGNETTE_ALPHA = 0.35f
private val GlowMaskColors = listOf(Color.Black, Color.Black.copy(alpha = 0.45f), Color.Transparent)
private val GlowMaskStops = listOf(0f, 0.4f, 1f)

internal fun Modifier.ambientBars(ambience: StreamAmbience, videoWidth: Float, videoHeight: Float): Modifier =
    drawWithCache {
        val area = size
        val sideGap = ((area.width - videoWidth) / 2f).coerceAtLeast(0f)
        val endGap = ((area.height - videoHeight) / 2f).coerceAtLeast(0f)
        val vignettes = buildList {
            if (sideGap >= 1f) {
                add(Vignette(Offset.Zero, Size(sideGap, area.height), Brush.horizontalGradient(edgeShade(), 0f, sideGap)))
                add(
                    Vignette(
                        Offset(area.width - sideGap, 0f),
                        Size(sideGap, area.height),
                        Brush.horizontalGradient(edgeShade().asReversed(), area.width - sideGap, area.width)
                    )
                )
            }
            if (endGap >= 1f) {
                add(Vignette(Offset.Zero, Size(area.width, endGap), Brush.verticalGradient(edgeShade(), 0f, endGap)))
                add(
                    Vignette(
                        Offset(0f, area.height - endGap),
                        Size(area.width, endGap),
                        Brush.verticalGradient(edgeShade().asReversed(), area.height - endGap, area.height)
                    )
                )
            }
        }
        onDrawBehind {
            val intensity = ambience.visibleIntensity()
            if (intensity <= 0f || vignettes.isEmpty()) return@onDrawBehind
            if (sideGap >= 1f) {
                drawSideBar(ambience.barColors(AmbientEdge.LEFT, intensity), Offset.Zero, Size(sideGap, size.height))
                drawSideBar(ambience.barColors(AmbientEdge.RIGHT, intensity), Offset(size.width - sideGap, 0f), Size(sideGap, size.height))
            }
            if (endGap >= 1f) {
                drawEndBar(ambience.barColors(AmbientEdge.TOP, intensity), Offset.Zero, Size(size.width, endGap))
                drawEndBar(ambience.barColors(AmbientEdge.BOTTOM, intensity), Offset(0f, size.height - endGap), Size(size.width, endGap))
            }
            vignettes.forEach { drawRect(it.brush, it.topLeft, it.size, alpha = intensity) }
        }
    }

@Composable
internal fun AmbientGlow(ambience: StreamAmbience, modifier: Modifier = Modifier) {
    DisposableEffect(ambience) {
        ambience.attachGlow()
        onDispose { ambience.detachGlow() }
    }
    val darkBackground = MaterialTheme.colorScheme.background.luminance() < 0.5f
    Spacer(
        modifier.drawWithCache {
            val mask = LinearGradientShader(Offset.Zero, Offset(0f, size.height), GlowMaskColors, GlowMaskStops)
            val blendMode = if (darkBackground) BlendMode.Screen else BlendMode.Multiply
            onDrawBehind {
                val intensity = ambience.visibleIntensity()
                if (intensity <= 0f) return@onDrawBehind
                val tint = LinearGradientShader(
                    from = Offset.Zero,
                    to = Offset(size.width, 0f),
                    colors = ambience.glowColors(AmbientEdge.BOTTOM, intensity, darkBackground)
                )
                drawRect(ShaderBrush(CompositeShader(tint, mask, BlendMode.DstIn)), blendMode = blendMode)
            }
        }
    )
}

private class Vignette(val topLeft: Offset, val size: Size, val brush: Brush)

private fun edgeShade(): List<Color> = listOf(Color.Black.copy(alpha = VIGNETTE_ALPHA), Color.Transparent)

private fun DrawScope.drawSideBar(colors: List<Color>, topLeft: Offset, barSize: Size) {
    drawRect(Brush.verticalGradient(colors, topLeft.y, topLeft.y + barSize.height), topLeft, barSize)
}

private fun DrawScope.drawEndBar(colors: List<Color>, topLeft: Offset, barSize: Size) {
    drawRect(Brush.horizontalGradient(colors, topLeft.x, topLeft.x + barSize.width), topLeft, barSize)
}
