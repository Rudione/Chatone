package io.rudione.chatone.presentation.stream.ambient

import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositeShader
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.DrawScope

@Composable
internal fun AmbienceViewer(ambience: StreamAmbience) {
    DisposableEffect(ambience) {
        ambience.attachViewer()
        onDispose { ambience.detachViewer() }
    }
}

@Composable
internal fun AmbientInsetStrip(ambience: StreamAmbience, videoSide: AmbientEdge, modifier: Modifier = Modifier) {
    AmbienceViewer(ambience)
    Spacer(
        modifier.drawWithCache {
            val strip = SurroundStrip(videoSide, Rect(Offset.Zero, size))
            onDrawBehind {
                drawRect(Color.Black)
                drawSurround(ambience, strip, fadeAway = true)
            }
        }
    )
}

@Immutable
internal data class AmbientSurroundInsets(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val isEmpty: Boolean get() = left <= 0f && top <= 0f && right <= 0f && bottom <= 0f
}

internal fun Modifier.ambientSurround(ambience: StreamAmbience, insets: AmbientSurroundInsets): Modifier =
    if (insets.isEmpty) this else drawWithCache {
        val width = size.width
        val height = size.height
        val strips = buildList {
            val spanTop = -insets.top
            val spanHeight = height + insets.top + insets.bottom
            if (insets.left > 0f) {
                add(SurroundStrip(AmbientEdge.LEFT, Rect(Offset(-insets.left, spanTop), Size(insets.left, spanHeight))))
            }
            if (insets.right > 0f) {
                add(SurroundStrip(AmbientEdge.RIGHT, Rect(Offset(width, spanTop), Size(insets.right, spanHeight))))
            }
            if (insets.top > 0f) {
                add(SurroundStrip(AmbientEdge.TOP, Rect(Offset(0f, -insets.top), Size(width, insets.top))))
            }
            if (insets.bottom > 0f) {
                add(SurroundStrip(AmbientEdge.BOTTOM, Rect(Offset(0f, height), Size(width, insets.bottom))))
            }
        }
        onDrawBehind {
            strips.forEach { drawRect(Color.Black, it.area.topLeft, it.area.size) }
            strips.forEach { drawSurround(ambience, it, fadeAway = false) }
        }
    }

private class SurroundStrip(val videoSide: AmbientEdge, val area: Rect) {
    val alongEdge: Boolean = videoSide == AmbientEdge.TOP || videoSide == AmbientEdge.BOTTOM

    fun tint(colors: List<Color>): Shader =
        if (alongEdge) LinearGradientShader(Offset(area.left, 0f), Offset(area.right, 0f), colors)
        else LinearGradientShader(Offset(0f, area.top), Offset(0f, area.bottom), colors)

    fun fade(): Shader {
        val (far, near) = when (videoSide) {
            AmbientEdge.TOP -> Offset(0f, area.top) to Offset(0f, area.bottom)
            AmbientEdge.BOTTOM -> Offset(0f, area.bottom) to Offset(0f, area.top)
            AmbientEdge.LEFT -> Offset(area.left, 0f) to Offset(area.right, 0f)
            AmbientEdge.RIGHT -> Offset(area.right, 0f) to Offset(area.left, 0f)
        }
        return LinearGradientShader(far, near, FadeMaskColors)
    }
}

private fun DrawScope.drawSurround(ambience: StreamAmbience, strip: SurroundStrip, fadeAway: Boolean) {
    val intensity = ambience.visibleIntensity()
    if (intensity <= 0f || strip.area.isEmpty) return
    if (fadeAway) {
        val tint = strip.tint(ambience.surroundColors(strip.videoSide, intensity))
        drawRect(ShaderBrush(CompositeShader(tint, strip.fade(), BlendMode.DstIn)), strip.area.topLeft, strip.area.size)
    } else {
        drawRect(ShaderBrush(strip.tint(ambience.barColors(strip.videoSide, intensity))), strip.area.topLeft, strip.area.size)
        drawRect(Color.Black, strip.area.topLeft, strip.area.size, alpha = AMBIENT_VIGNETTE_ALPHA * intensity)
    }
}

private val FadeMaskColors = listOf(Color.Black.copy(alpha = 0.62f), Color.Black)
