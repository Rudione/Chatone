package io.rudione.chatone.presentation.chat

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.RadialGradientShader
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TileMode
import io.rudione.chatone.domain.model.SevenTvCosmetics
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

fun sevenTvColor(rgba: Int): Color {
    val a = rgba and 0xFF
    val rgb = rgba ushr 8
    return Color((a shl 24) or rgb)
}

fun SevenTvCosmetics.Paint.hasRenderableGradient(): Boolean =
    stops.size >= 2 || color != null

private class SevenTvGradientBrush(
    private val paint: SevenTvCosmetics.Paint,
    private val phase: Float
) : ShaderBrush() {

    override fun createShader(size: Size): Shader {
        val colors = paint.stops.map { sevenTvColor(it.color) }
        val rawPositions = paint.stops.map { it.at }
        val first = rawPositions.first()
        val last = rawPositions.last()
        val span = (last - first).takeIf { it > 0.0001f } ?: 1f
        val positions =
            if (paint.repeat) rawPositions.map { ((it - first) / span).coerceIn(0f, 1f) }
            else rawPositions.map { it.coerceIn(0f, 1f) }
        val tile = if (paint.repeat) TileMode.Repeated else TileMode.Clamp

        if (paint.function.uppercase().contains("RADIAL")) {
            return RadialGradientShader(
                center = Offset(size.width / 2f, size.height / 2f),
                radius = (maxOf(size.width, size.height) / 2f).coerceAtLeast(1f),
                colors = colors,
                colorStops = positions,
                tileMode = tile
            )
        }

        val rad = paint.angle * PI / 180.0
        val dx = sin(rad).toFloat()
        val dy = -cos(rad).toFloat()
        val halfLen = ((abs(size.width * dx) + abs(size.height * dy)) / 2f).coerceAtLeast(1f)
        val cx = size.width / 2f
        val cy = size.height / 2f
        var from = Offset(cx - dx * halfLen, cy - dy * halfLen)
        var to = Offset(cx + dx * halfLen, cy + dy * halfLen)
        if (paint.repeat) {
            val dir = to - from
            val f = from
            from = f + dir * first
            to = f + dir * last
        }
        if (phase != 0f) {
            val shift = (to - from) * phase
            from += shift
            to += shift
        }
        return LinearGradientShader(
            from = from,
            to = to,
            colors = colors,
            colorStops = positions,
            tileMode = tile
        )
    }

    override fun equals(other: Any?): Boolean =
        other is SevenTvGradientBrush && other.paint == paint && other.phase == phase

    override fun hashCode(): Int = paint.hashCode() * 31 + phase.hashCode()
}

fun sevenTvPaintBrush(paint: SevenTvCosmetics.Paint, phase: Float = 0f): Brush? {
    if (paint.stops.size < 2) {
        val c = paint.color ?: return null
        return SolidColor(sevenTvColor(c))
    }
    return SevenTvGradientBrush(paint, phase)
}

fun sevenTvPaintShadow(paint: SevenTvCosmetics.Paint, density: Float): Shadow? {
    val s = paint.shadows.firstOrNull() ?: return null
    return Shadow(
        color = sevenTvColor(s.color),
        offset = Offset(s.xOffset * density, s.yOffset * density),
        blurRadius = (s.radius * density).coerceAtLeast(0.1f)
    )
}

@Composable
fun registerPaintedNick(
    inlineContent: MutableMap<String, InlineTextContent>,
    key: String,
    name: String,
    paint: SevenTvCosmetics.Paint,
    fontSizeSp: Float,
    isAction: Boolean,
    onClick: () -> Unit,
    onRightClick: () -> Unit
) {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val nickStyle = TextStyle(
        fontSize = fontSizeSp.sp,
        fontWeight = if (isAction) FontWeight.SemiBold else FontWeight.Bold,
        fontStyle = if (isAction) FontStyle.Italic else FontStyle.Normal
    )
    val nickWidthSp = remember(name, fontSizeSp, isAction) {
        val px = textMeasurer.measure(AnnotatedString(name), nickStyle).size.width
        with(density) { px.toSp() }
    }
    inlineContent[key] = InlineTextContent(
        Placeholder(
            width = nickWidthSp,
            height = (fontSizeSp * 1.35f).sp,
            placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
        )
    ) {
        val brush = rememberSevenTvPaintBrush(paint)
        val shadow = remember(paint) { sevenTvPaintShadow(paint, density.density) }
        Text(
            text = name,
            maxLines = 1,
            softWrap = false,
            style = TextStyle(
                brush = brush,
                fontSize = fontSizeSp.sp,
                fontWeight = nickStyle.fontWeight,
                fontStyle = nickStyle.fontStyle,
                shadow = shadow
            ),
            modifier = Modifier.pointerInput(name) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.type == PointerEventType.Press) {
                            if (event.buttons.isSecondaryPressed) onRightClick() else onClick()
                        }
                    }
                }
            }
        )
    }
}

@Composable
fun rememberSevenTvPaintBrush(paint: SevenTvCosmetics.Paint): Brush? {
    val phase = if (paint.repeat && paint.stops.size >= 2) {
        val transition = rememberInfiniteTransition()
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(animation = tween(4000, easing = LinearEasing))
        ).value
    } else 0f
    return remember(paint, phase) { sevenTvPaintBrush(paint, phase) }
}
