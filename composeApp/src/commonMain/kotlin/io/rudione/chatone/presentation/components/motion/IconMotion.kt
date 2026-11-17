package io.rudione.chatone.presentation.components.motion

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.DrawTransform
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.withTransform
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin

enum class IconMotion(
    internal val durationMs: Int,
    internal val pulseOnPress: Boolean = true,
    internal val pulseOnHover: Boolean = false
) {
    Gear(700),
    Mention(900),
    Bubble(950),
    Plus(520),
    Send(620, pulseOnPress = false),
    Smile(620, pulseOnHover = true),
    Bell(1000),
    Heart(720),
    Search(620),
    Points(900),
    Tilt(600)
}

internal class GlyphCanvas {
    private val measure = PathMeasure()
    private val segment = Path()

    fun draw(
        scope: DrawScope,
        layer: GlyphLayer,
        tint: Color,
        alpha: Float,
        trim: Float,
        transform: DrawTransform.() -> Unit
    ) {
        if (alpha <= 0f) return
        val path = if (trim >= 1f) layer.path else trimmed(layer.path, trim) ?: return
        val a = alpha.coerceAtMost(1f)
        scope.withTransform(transform) {
            if (layer.filled) drawPath(path, tint, alpha = a, style = Fill)
            layer.stroke?.let { drawPath(path, tint, alpha = a, style = it) }
        }
    }

    private fun trimmed(path: Path, fraction: Float): Path? {
        if (fraction <= 0f) return null
        measure.setPath(path, false)
        segment.reset()
        measure.getSegment(0f, measure.length * fraction, segment, true)
        return segment
    }
}

private class MotionPass(
    val scope: DrawScope,
    val canvas: GlyphCanvas,
    val glyph: GlyphLayers,
    val tint: Color
) {
    fun layer(
        index: Int,
        alpha: Float = 1f,
        trim: Float = 1f,
        transform: DrawTransform.() -> Unit = {}
    ) {
        val layer = glyph[index] ?: return
        canvas.draw(scope, layer, tint, alpha, trim, transform)
    }

    fun all(transform: DrawTransform.() -> Unit) {
        for (i in 0 until glyph.size) layer(i, transform = transform)
    }

    fun dot(center: Offset, radius: Float, alpha: Float) {
        if (alpha <= 0f) return
        scope.drawCircle(tint, radius, center, alpha = alpha.coerceAtMost(1f))
    }
}

internal fun DrawScope.drawMotion(
    canvas: GlyphCanvas,
    glyph: GlyphLayers,
    motion: IconMotion,
    tint: Color,
    hover: Float,
    pulse: Float
) {
    val edge = minOf(size.width, size.height)
    val unit = edge / glyph.viewportWidth
    withTransform({
        translate((size.width - edge) / 2f, (size.height - edge) / 2f)
        scale(unit, unit, Offset.Zero)
    }) {
        val pass = MotionPass(this, canvas, glyph, tint)
        when (motion) {
            IconMotion.Gear -> pass.gear(hover, pulse)
            IconMotion.Mention -> pass.mention(hover, pulse)
            IconMotion.Bubble -> pass.bubble(hover, pulse)
            IconMotion.Plus -> pass.plus(hover, pulse)
            IconMotion.Send -> pass.send(hover, pulse)
            IconMotion.Smile -> pass.smile(hover, pulse)
            IconMotion.Bell -> pass.bell(hover, pulse)
            IconMotion.Heart -> pass.heart(hover, pulse)
            IconMotion.Search -> pass.search(hover, pulse)
            IconMotion.Points -> pass.points(hover, pulse)
            IconMotion.Tilt -> pass.tilt(hover, pulse)
        }
    }
}

private val Center = Offset(12f, 12f)

private fun MotionPass.gear(hover: Float, pulse: Float) {
    val turn = 30f * hover + 120f * easeOutBack(pulse)
    val squeeze = 1f - 0.2f * bump(pulse)
    layer(0) { rotate(turn, Center) }
    layer(1) { scale(squeeze, squeeze, Center) }
}

private fun MotionPass.mention(hover: Float, pulse: Float) {
    val drawing = pulse > 0f && pulse < 1f
    val trace = if (drawing) 0.12f + 0.88f * easeOutCubic(window(pulse, 0f, 0.7f)) else 1f
    val pop = 1f + 0.38f * bump(window(pulse, 0.35f, 1f))
    val tilt = -22f * hover + 10f * wobble(window(pulse, 0.55f, 1f), 1.5f)
    layer(1, trim = trace) { rotate(tilt, Center) }
    layer(0) { scale(pop, pop, Center) }
}

private fun MotionPass.bubble(hover: Float, pulse: Float) {
    val squash = wobble(pulse, 1.5f, 1.6f)
    val hop = -2.4f * bump(window(pulse, 0f, 0.45f))
    val tilt = -7f * hover
    val tail = Offset(3f, 21f)
    layer(0) {
        rotate(tilt, Center)
        translate(0f, hop)
        scale(1f + 0.1f * squash, 1f - 0.1f * squash, tail)
    }
    for (i in 0..2) {
        val start = 0.12f + 0.1f * i
        val typing = bump(window(pulse, start, 0.92f))
        val bounce = -1.4f * bump(window(pulse, start, start + 0.3f))
        val alpha = maxOf(0.85f * hover, typing)
        if (alpha <= 0f) continue
        val x = 8f + 4f * i
        scope.withTransform({ rotate(tilt, Center); translate(0f, hop + bounce) }) {
            dot(Offset(x, 12f), 1.2f, alpha)
        }
    }
}

private fun MotionPass.plus(hover: Float, pulse: Float) {
    val turn = 90f * hover + 180f * easeOutBack(pulse)
    val grow = 1f + 0.22f * bump(pulse)
    all {
        rotate(turn, Center)
        scale(grow, grow, Center)
    }
}

private fun MotionPass.send(hover: Float, pulse: Float) {
    var dx = 1.6f * hover
    var dy = 0f
    var alpha = 1f
    if (pulse > 0f && pulse < 1f) {
        if (pulse < 0.42f) {
            val t = window(pulse, 0f, 0.42f)
            dx += 15f * t * t
            dy = -2.5f * t
            alpha = 1f - t
        } else {
            val t = window(pulse, 0.42f, 1f)
            dx += -11f * (1f - easeOutBack(t))
            alpha = t
        }
    }
    val tilt = -10f * hover
    for (i in 0 until glyph.size) {
        layer(i, alpha = alpha) {
            translate(dx, dy)
            rotate(tilt, Center)
        }
    }
}

private fun MotionPass.smile(hover: Float, pulse: Float) {
    val tilt = -8f * hover
    val wink = 1f - 0.85f * bump(window(pulse, 0.15f, 0.75f))
    layer(0) { rotate(tilt, Center) }
    layer(1) {
        rotate(tilt, Center)
        scale(1f + 0.1f * hover, 1f + 0.6f * hover, Offset(12f, 14f))
    }
    layer(2) { rotate(tilt, Center) }
    layer(3) {
        rotate(tilt, Center)
        scale(1f, wink, Offset(15f, 9f))
    }
}

private fun MotionPass.bell(hover: Float, pulse: Float) {
    val hinge = Offset(12f, 2.5f)
    val swing = 18f * wobble(pulse, 3f, 1.3f) + 10f * hover
    val lagged = window(pulse, 0.07f, 1f)
    val clapper = 28f * wobble(lagged, 3f, 1.3f) + 12f * hover
    layer(1) { rotate(swing, hinge) }
    layer(0) { rotate(clapper, hinge) }
}

private fun MotionPass.heart(hover: Float, pulse: Float) {
    val beat = 0.24f * bump(window(pulse, 0f, 0.34f)) + 0.14f * bump(window(pulse, 0.34f, 0.68f))
    val s = 1f + beat + 0.08f * hover
    all { scale(s, s, Offset(12f, 13f)) }
}

private fun MotionPass.search(hover: Float, pulse: Float) {
    val lens = Offset(11f, 11f)
    val tilt = -14f * hover + 12f * wobble(pulse, 2f, 1.5f)
    val zoom = 1f + 0.2f * bump(pulse)
    layer(0) {
        translate(-0.8f * hover, -0.8f * hover)
        rotate(tilt, lens)
    }
    layer(1) {
        translate(-0.8f * hover, -0.8f * hover)
        rotate(tilt, lens)
        scale(zoom, zoom, lens)
    }
}

private fun MotionPass.points(hover: Float, pulse: Float) {
    val pop = 1f + 0.28f * bump(window(pulse, 0f, 0.5f)) - 0.06f * bump(window(pulse, 0.5f, 1f))
    val rock = 14f * wobble(pulse, 2f, 1.4f) + 12f * hover
    val shine = 360f * easeOutCubic(pulse) + 25f * hover
    layer(1) {
        rotate(rock, Center)
        scale(pop, pop, Center)
    }
    layer(0) {
        rotate(rock + shine, Center)
        scale(pop, pop, Center)
    }
}

private fun MotionPass.tilt(hover: Float, pulse: Float) {
    val turn = -7f * hover + 12f * wobble(pulse, 2f, 1.5f)
    val grow = 1f + 0.12f * bump(pulse)
    all {
        rotate(turn, Center)
        scale(grow, grow, Center)
    }
}

internal fun bump(t: Float): Float = if (t <= 0f || t >= 1f) 0f else sin(PI * t).toFloat()

internal fun wobble(t: Float, cycles: Float, decay: Float = 2f): Float {
    if (t <= 0f || t >= 1f) return 0f
    return sin(2.0 * PI * cycles * t).toFloat() * (1f - t).pow(decay)
}

internal fun window(t: Float, start: Float, end: Float): Float =
    ((t - start) / (end - start)).coerceIn(0f, 1f)

internal fun easeOutCubic(t: Float): Float {
    val x = 1f - t.coerceIn(0f, 1f)
    return 1f - x * x * x
}

internal fun easeOutBack(t: Float): Float {
    if (t <= 0f) return 0f
    if (t >= 1f) return 1f
    val c1 = 1.70158f
    val c3 = c1 + 1f
    val x = t - 1f
    return 1f + c3 * x * x * x + c1 * x * x
}
