package io.rudione.chatone.presentation.stream.ambient

import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.isSpecified

internal const val AMBIENT_VIGNETTE_ALPHA = 0.35f
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
internal fun AmbientGlow(ambience: StreamAmbience, modifier: Modifier = Modifier, alpha: () -> Float = { 1f }) {
    AmbienceViewer(ambience)
    val darkBackground = MaterialTheme.colorScheme.background.luminance() < 0.5f
    Spacer(modifier.ambientGlow(ambience, darkBackground, alpha = alpha))
}

internal fun Modifier.ambientGlow(
    ambience: StreamAmbience,
    darkBackground: Boolean,
    reach: Dp = Dp.Unspecified,
    alpha: () -> Float = { 1f }
): Modifier = drawWithCache {
    val glowHeight = if (reach.isSpecified) reach.toPx().coerceAtMost(size.height) else size.height
    val glowSize = Size(size.width, glowHeight)
    val mask = LinearGradientShader(Offset.Zero, Offset(0f, glowHeight), GlowMaskColors, GlowMaskStops)
    val blendMode = if (darkBackground) BlendMode.Screen else BlendMode.Multiply
    onDrawBehind {
        val intensity = ambience.visibleIntensity() * alpha().coerceIn(0f, 1f)
        if (intensity <= 0f || glowHeight <= 0f) return@onDrawBehind
        val tint = LinearGradientShader(
            from = Offset.Zero,
            to = Offset(size.width, 0f),
            colors = ambience.glowColors(AmbientEdge.BOTTOM, intensity, darkBackground)
        )
        drawRect(ShaderBrush(CompositeShader(tint, mask, BlendMode.DstIn)), size = glowSize, blendMode = blendMode)
    }
}

internal fun Modifier.ambientHalo(
    ambience: StreamAmbience,
    darkBackground: Boolean,
    spread: Dp
): Modifier = drawWithCache {
    val reach = spread.toPx()
    val w = size.width
    val h = size.height
    val masks = mapOf(
        AmbientEdge.TOP to LinearGradientShader(Offset(0f, 0f), Offset(0f, -reach), HaloMaskColors),
        AmbientEdge.BOTTOM to LinearGradientShader(Offset(0f, h), Offset(0f, h + reach), HaloMaskColors),
        AmbientEdge.LEFT to LinearGradientShader(Offset(0f, 0f), Offset(-reach, 0f), HaloMaskColors),
        AmbientEdge.RIGHT to LinearGradientShader(Offset(w, 0f), Offset(w + reach, 0f), HaloMaskColors)
    )
    val blendMode = if (darkBackground) BlendMode.Screen else BlendMode.Multiply
    onDrawBehind {
        val intensity = ambience.visibleIntensity()
        if (intensity <= 0f || reach <= 0f) return@onDrawBehind
        AmbientEdge.entries.forEach { edge ->
            val colors = ambience.glowColors(edge, intensity, darkBackground)
            val horizontal = edge == AmbientEdge.TOP || edge == AmbientEdge.BOTTOM
            val tint = if (horizontal) {
                LinearGradientShader(Offset(-reach, 0f), Offset(w + reach, 0f), colors)
            } else {
                LinearGradientShader(Offset(0f, -reach), Offset(0f, h + reach), colors)
            }
            val (topLeft, area) = when (edge) {
                AmbientEdge.TOP -> Offset(-reach, -reach) to Size(w + reach * 2, reach)
                AmbientEdge.BOTTOM -> Offset(-reach, h) to Size(w + reach * 2, reach)
                AmbientEdge.LEFT -> Offset(-reach, 0f) to Size(reach, h)
                AmbientEdge.RIGHT -> Offset(w, 0f) to Size(reach, h)
            }
            drawRect(ShaderBrush(CompositeShader(tint, masks.getValue(edge), BlendMode.DstIn)), topLeft, area, blendMode = blendMode)
        }
    }
}

private val HaloMaskColors = listOf(Color.Black.copy(alpha = 0.85f), Color.Black.copy(alpha = 0.3f), Color.Transparent)

private class Vignette(val topLeft: Offset, val size: Size, val brush: Brush)

private fun edgeShade(): List<Color> = listOf(Color.Black.copy(alpha = AMBIENT_VIGNETTE_ALPHA), Color.Transparent)

private fun DrawScope.drawSideBar(colors: List<Color>, topLeft: Offset, barSize: Size) {
    drawRect(Brush.verticalGradient(colors, topLeft.y, topLeft.y + barSize.height), topLeft, barSize)
}

private fun DrawScope.drawEndBar(colors: List<Color>, topLeft: Offset, barSize: Size) {
    drawRect(Brush.horizontalGradient(colors, topLeft.x, topLeft.x + barSize.width), topLeft, barSize)
}
