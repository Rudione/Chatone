package io.rudione.chatone.presentation.chat

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.readRawBytes
import io.rudione.chatone.presentation.chat.rendering.LocalScrollActivity
import io.rudione.chatone.util.media.decodeImageBitmap
import io.rudione.chatone.util.media.rememberAnimatedFrame
import io.rudione.chatone.util.media.scaledTo
import org.koin.compose.koinInject
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.isSpecified
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
    stops.size >= 2 || color != null || imageUrl.isNotBlank()

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
    userColor: Color = Color.Unspecified,
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
        val brush = rememberSevenTvPaintBrush(paint, userColor)
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
                        if (event.type == PointerEventType.Press && event.changes.none { it.isConsumed }) {
                            if (event.buttons.isSecondaryPressed) onRightClick() else onClick()
                            event.changes.forEach { it.consume() }
                        }
                    }
                }
            }
        )
    }
}

private class SevenTvImageBrush(
    private val image: ImageBitmap,
    private val backdrop: Color
) : ShaderBrush() {

    override fun createShader(size: Size): Shader {
        val width = size.width.toInt().coerceAtLeast(1)
        val height = size.height.toInt().coerceAtLeast(1)
        val stretched = if (backdrop.isSpecified && backdrop.alpha > 0f) {
            val target = ImageBitmap(width, height)
            val canvas = Canvas(target)
            canvas.drawRect(
                left = 0f,
                top = 0f,
                right = width.toFloat(),
                bottom = height.toFloat(),
                paint = Paint().apply { color = backdrop }
            )
            canvas.drawImageRect(
                image = image,
                srcOffset = IntOffset.Zero,
                srcSize = IntSize(image.width, image.height),
                dstOffset = IntOffset.Zero,
                dstSize = IntSize(width, height),
                paint = Paint().apply { filterQuality = FilterQuality.Medium }
            )
            target
        } else {
            image.scaledTo(width, height)
        }
        return ImageShader(
            image = stretched,
            tileModeX = TileMode.Clamp,
            tileModeY = TileMode.Clamp
        )
    }

    override fun equals(other: Any?): Boolean =
        other is SevenTvImageBrush && other.image === image && other.backdrop == backdrop

    override fun hashCode(): Int = image.hashCode() * 31 + backdrop.hashCode()
}

private const val PAINT_MAX_DIMENSION = 192

private fun SevenTvCosmetics.Paint.renderableImageUrl(): String {
    if (imageUrl.isBlank()) return ""
    val function = function.uppercase()
    val isImagePaint = function.contains("URL") || function.contains("IMAGE")
    return if (isImagePaint || (stops.size < 2 && color == null)) imageUrl else ""
}

private val paintImageCache = mutableMapOf<String, ImageBitmap?>()

@Composable
private fun rememberPaintImage(url: String): ImageBitmap? {
    if (url.isBlank()) return null
    val httpClient: HttpClient = koinInject()
    var bitmap by remember(url) { mutableStateOf(paintImageCache[url]) }
    LaunchedEffect(url) {
        if (paintImageCache.containsKey(url)) {
            bitmap = paintImageCache[url]
            return@LaunchedEffect
        }
        val decoded = runCatching {
            decodeImageBitmap(httpClient.get(url).readRawBytes())
        }.getOrNull()
        paintImageCache[url] = decoded
        bitmap = decoded
    }
    return bitmap
}

@Composable
fun rememberSevenTvPaintBrush(
    paint: SevenTvCosmetics.Paint,
    userColor: Color = Color.Unspecified
): Brush? {
    val imageUrl = remember(paint) { paint.renderableImageUrl() }
    val scrolling = LocalScrollActivity.current.isScrolling
    val animatedFrame = rememberAnimatedFrame(imageUrl, scrolling, PAINT_MAX_DIMENSION)
    val staticImage = rememberPaintImage(imageUrl)
    val paintImage = animatedFrame ?: staticImage

    val transition = if (paint.stops.size >= 2) rememberInfiniteTransition() else null
    val phase = when {
        transition == null -> 0f
        paint.repeat -> transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(animation = tween(4000, easing = LinearEasing))
        ).value

        else -> transition.animateFloat(
            initialValue = -0.09f,
            targetValue = 0.09f,
            animationSpec = infiniteRepeatable(
                animation = tween(2800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            )
        ).value
    }

    if (paintImage != null) {
        return remember(paintImage, userColor) { SevenTvImageBrush(paintImage, userColor) }
    }
    return remember(paint, phase) { sevenTvPaintBrush(paint, phase) }
}
