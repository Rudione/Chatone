package io.rudione.chatone.presentation.stream.ambient

import androidx.compose.ui.graphics.Color
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

internal enum class AmbientEdge { LEFT, RIGHT, TOP, BOTTOM }

internal object AmbientColors {
    const val STOPS = 4
    const val SIZE = 4 * STOPS * 3

    private const val SATURATION = 1.1f
    private const val BAR_DIM = 0.36f
    private const val BAR_MAX_LUMINANCE = 0.055f
    private const val GLOW_DIM = 0.6f
    private const val GLOW_MAX_LUMINANCE = 0.14f
    private const val GLOW_ALPHA = 0.8f
    private const val PASTEL_MIX = 0.72f
    private const val PASTEL_ALPHA = 0.5f
    private const val MIN_LUMINANCE = 1e-4f

    private val linearLut = FloatArray(256) { toLinear(it / 255f) }

    fun index(edge: AmbientEdge, stop: Int): Int = (edge.ordinal * STOPS + stop) * 3

    fun analyze(argb: IntArray, width: Int, height: Int, out: FloatArray) {
        if (width <= 0 || height <= 0 || argb.size < width * height || out.size < SIZE) return
        val stripWidth = max(1, width / 8)
        val stripHeight = max(1, height / 6)
        for (stop in 0 until STOPS) {
            val rowFrom = stop * height / STOPS
            val rowTo = max(rowFrom + 1, (stop + 1) * height / STOPS)
            val columnFrom = stop * width / STOPS
            val columnTo = max(columnFrom + 1, (stop + 1) * width / STOPS)
            averageColumns(argb, width, rowFrom, rowTo, stripWidth, fromLeft = true, out, index(AmbientEdge.LEFT, stop))
            averageColumns(argb, width, rowFrom, rowTo, stripWidth, fromLeft = false, out, index(AmbientEdge.RIGHT, stop))
            averageRows(argb, width, height, columnFrom, columnTo, stripHeight, fromTop = true, out, index(AmbientEdge.TOP, stop))
            averageRows(argb, width, height, columnFrom, columnTo, stripHeight, fromTop = false, out, index(AmbientEdge.BOTTOM, stop))
        }
        for (offset in 0 until SIZE step 3) saturate(out, offset)
    }

    fun barColor(palette: FloatArray, offset: Int, alpha: Float): Color =
        scaled(palette, offset, BAR_DIM, BAR_MAX_LUMINANCE, alpha)

    fun glowColor(palette: FloatArray, offset: Int, alpha: Float, darkBackground: Boolean): Color {
        if (darkBackground) return scaled(palette, offset, GLOW_DIM, GLOW_MAX_LUMINANCE, alpha * GLOW_ALPHA)
        val r = palette[offset]
        val g = palette[offset + 1]
        val b = palette[offset + 2]
        val peak = max(r, max(g, b))
        if (peak <= MIN_LUMINANCE) return Color.Transparent
        return Color(
            red = toSrgb(mix(r / peak)),
            green = toSrgb(mix(g / peak)),
            blue = toSrgb(mix(b / peak)),
            alpha = (alpha * PASTEL_ALPHA).coerceIn(0f, 1f)
        )
    }

    fun luminance(r: Float, g: Float, b: Float): Float = 0.2126f * r + 0.7152f * g + 0.0722f * b

    private fun mix(channel: Float): Float = channel + (1f - channel) * PASTEL_MIX

    private fun scaled(palette: FloatArray, offset: Int, dim: Float, maxLuminance: Float, alpha: Float): Color {
        val r = palette[offset]
        val g = palette[offset + 1]
        val b = palette[offset + 2]
        val luminance = luminance(r, g, b)
        val scale = if (luminance <= MIN_LUMINANCE) 0f else min(luminance * dim, maxLuminance) / luminance
        return Color(
            red = toSrgb(r * scale),
            green = toSrgb(g * scale),
            blue = toSrgb(b * scale),
            alpha = alpha.coerceIn(0f, 1f)
        )
    }

    private fun averageColumns(
        argb: IntArray,
        width: Int,
        rowFrom: Int,
        rowTo: Int,
        strip: Int,
        fromLeft: Boolean,
        out: FloatArray,
        offset: Int
    ) {
        var r = 0f
        var g = 0f
        var b = 0f
        var total = 0f
        for (distance in 0 until strip) {
            val column = if (fromLeft) distance else width - 1 - distance
            val weight = (strip - distance).toFloat()
            for (row in rowFrom until rowTo) {
                val pixel = argb[row * width + column]
                r += linearLut[(pixel shr 16) and 0xFF] * weight
                g += linearLut[(pixel shr 8) and 0xFF] * weight
                b += linearLut[pixel and 0xFF] * weight
                total += weight
            }
        }
        store(out, offset, r, g, b, total)
    }

    private fun averageRows(
        argb: IntArray,
        width: Int,
        height: Int,
        columnFrom: Int,
        columnTo: Int,
        strip: Int,
        fromTop: Boolean,
        out: FloatArray,
        offset: Int
    ) {
        var r = 0f
        var g = 0f
        var b = 0f
        var total = 0f
        for (distance in 0 until strip) {
            val row = if (fromTop) distance else height - 1 - distance
            val weight = (strip - distance).toFloat()
            val base = row * width
            for (column in columnFrom until columnTo) {
                val pixel = argb[base + column]
                r += linearLut[(pixel shr 16) and 0xFF] * weight
                g += linearLut[(pixel shr 8) and 0xFF] * weight
                b += linearLut[pixel and 0xFF] * weight
                total += weight
            }
        }
        store(out, offset, r, g, b, total)
    }

    private fun store(out: FloatArray, offset: Int, r: Float, g: Float, b: Float, total: Float) {
        val divisor = if (total > 0f) total else 1f
        out[offset] = r / divisor
        out[offset + 1] = g / divisor
        out[offset + 2] = b / divisor
    }

    private fun saturate(values: FloatArray, offset: Int) {
        val r = values[offset]
        val g = values[offset + 1]
        val b = values[offset + 2]
        val luminance = luminance(r, g, b)
        values[offset] = (luminance + (r - luminance) * SATURATION).coerceIn(0f, 1f)
        values[offset + 1] = (luminance + (g - luminance) * SATURATION).coerceIn(0f, 1f)
        values[offset + 2] = (luminance + (b - luminance) * SATURATION).coerceIn(0f, 1f)
    }

    private fun toLinear(value: Float): Float =
        if (value <= 0.04045f) value / 12.92f else ((value + 0.055f) / 1.055f).pow(2.4f)

    private fun toSrgb(value: Float): Float {
        val v = value.coerceIn(0f, 1f)
        return if (v <= 0.0031308f) v * 12.92f else 1.055f * v.pow(1f / 2.4f) - 0.055f
    }
}
