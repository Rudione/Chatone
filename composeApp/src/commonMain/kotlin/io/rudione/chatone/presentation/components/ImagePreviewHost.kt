package io.rudione.chatone.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.close
import chatone.composeapp.generated.resources.image_preview_expand
import chatone.composeapp.generated.resources.ui_image_preview
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.compose.LocalPlatformContext
import io.rudione.chatone.icons.lucide.Lucide
import io.rudione.chatone.icons.lucide.X
import io.rudione.chatone.presentation.nativecontext.SuspendNativeMessageMenus
import org.jetbrains.compose.resources.stringResource
import kotlin.math.min

private data class ImagePreviewRequest(
    val imageUrl: String,
    val description: String,
    val painter: Painter,
    val coordinates: LayoutCoordinates,
    val origin: Rect,
    val currentBounds: () -> Rect?,
    val aspectRatio: Float
)

@Stable
internal class ImagePreviewController {
    private var request by mutableStateOf<ImagePreviewRequest?>(null)

    fun open(imageUrl: String, description: String, painter: Painter, coordinates: LayoutCoordinates?) {
        if (request != null || coordinates?.isAttached != true) return
        val origin = coordinates.boundsInWindow()
        if (origin.width <= 0f || origin.height <= 0f) return
        val intrinsic = painter.intrinsicSize
        val ratio = if (intrinsic.width > 0f && intrinsic.height > 0f) intrinsic.width / intrinsic.height
        else origin.width / origin.height
        request = ImagePreviewRequest(imageUrl, description, painter, coordinates, origin, {
            coordinates.takeIf { it.isAttached }?.boundsInWindow()
        }, ratio.takeIf { it.isFinite() && it > 0f } ?: 1f)
    }

    fun isSource(coordinates: LayoutCoordinates?): Boolean =
        coordinates != null && request?.coordinates === coordinates

    @Composable
    fun Content() {
        request?.let { current -> ImagePreviewLayer(current) { if (request === current) request = null } }
    }
}

internal val LocalImagePreviewController = staticCompositionLocalOf<ImagePreviewController?> { null }

@Composable
fun ImagePreviewHost(content: @Composable () -> Unit) {
    val controller = remember { ImagePreviewController() }
    CompositionLocalProvider(LocalImagePreviewController provides controller) {
        content()
        controller.Content()
    }
}

@Composable
internal fun ImagePreviewExpandButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    ImageOverlayButton(onClick, stringResource(Res.string.image_preview_expand), modifier) {
        Canvas(Modifier.size(16.dp)) {
            val stroke = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
            val path = Path().apply {
                moveTo(size.width * 0.58f, size.height * 0.16f)
                lineTo(size.width * 0.84f, size.height * 0.16f)
                lineTo(size.width * 0.84f, size.height * 0.42f)
                moveTo(size.width * 0.84f, size.height * 0.16f)
                lineTo(size.width * 0.55f, size.height * 0.45f)
                moveTo(size.width * 0.16f, size.height * 0.58f)
                lineTo(size.width * 0.16f, size.height * 0.84f)
                lineTo(size.width * 0.42f, size.height * 0.84f)
                moveTo(size.width * 0.16f, size.height * 0.84f)
                lineTo(size.width * 0.45f, size.height * 0.55f)
            }
            drawPath(path, Color.White, style = stroke)
        }
    }
}

@Composable
private fun ImageOverlayButton(
    onClick: () -> Unit,
    description: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier.size(40.dp).clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.64f))
            .clickable(onClickLabel = description, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) { content() }
}

private val WindowOrigin = object : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ) = IntOffset.Zero
}

@Composable
private fun ImagePreviewLayer(request: ImagePreviewRequest, onGone: () -> Unit) {
    SuspendNativeMessageMenus()
    val progress = remember(request) { Animatable(0f) }
    val reducedMotion = LocalControlMotion.current == ControlMotion.Reduced
    var closing by remember(request) { mutableStateOf(false) }
    var returnBounds by remember(request) { mutableStateOf(request.origin) }
    val close = {
        if (!closing) {
            returnBounds = request.currentBounds() ?: request.origin
            closing = true
        }
    }
    LaunchedEffect(closing, reducedMotion) {
        val duration = if (reducedMotion) 0 else if (closing) 280 else 420
        progress.animateTo(if (closing) 0f else 1f, tween(duration, easing = FastOutSlowInEasing))
        if (closing) onGone()
    }
    val context = LocalPlatformContext.current
    val model = remember(request.imageUrl, context) {
        ImageRequest.Builder(context).data(request.imageUrl).size(1600, 1600).build()
    }
    val painter = rememberAsyncImagePainter(model, placeholder = request.painter, error = request.painter)
    val title = stringResource(Res.string.ui_image_preview)
    Popup(
        popupPositionProvider = WindowOrigin,
        onDismissRequest = close,
        properties = PopupProperties(focusable = true, clippingEnabled = false)
    ) {
        Box(
            Modifier.fillMaxSize().semantics { paneTitle = title }
                .drawBehind { drawRect(Color.Black.copy(alpha = 0.76f * progress.value)) }
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = close)
        ) {
            var layerOrigin by remember { mutableStateOf(Offset.Zero) }
            BoxWithConstraints(
                Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(16.dp)
                    .onGloballyPositioned { layerOrigin = it.positionInWindow() }
            ) {
                val density = LocalDensity.current
                val availableWidth = with(density) { min(maxWidth.toPx(), 900.dp.toPx()) }
                val availableHeight = with(density) { min(maxHeight.toPx(), 800.dp.toPx()) }
                if (availableWidth <= 0f || availableHeight <= 0f) return@BoxWithConstraints
                val height = min(availableHeight, availableWidth / request.aspectRatio).coerceAtLeast(1f)
                val width = min(availableWidth, height * request.aspectRatio).coerceAtLeast(1f)
                val left = (constraints.maxWidth - width) / 2f
                val top = (constraints.maxHeight - height) / 2f
                val origin = returnBounds.translate(-layerOrigin)
                Box(
                    Modifier.offset { IntOffset(left.toInt(), top.toInt()) }
                        .size(with(density) { width.toDp() }, with(density) { height.toDp() })
                        .graphicsLayer {
                            val remaining = 1f - progress.value
                            transformOrigin = TransformOrigin(0f, 0f)
                            translationX = (origin.left - left) * remaining
                            translationY = (origin.top - top) * remaining
                            scaleX = 1f + (origin.width / width - 1f) * remaining
                            scaleY = 1f + (origin.height / height - 1f) * remaining
                            alpha = if (closing) (progress.value * 5f).coerceAtMost(1f) else 1f
                        }
                        .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 8.dp, bottomEnd = 20.dp, bottomStart = 8.dp))
                        .background(Color(0xFF17161B))
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
                    contentAlignment = Alignment.Center
                ) {
                    Image(painter, request.description, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                    ImageOverlayButton(
                        onClick = close,
                        description = stringResource(Res.string.close),
                        modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).graphicsLayer { alpha = progress.value }
                    ) {
                        Icon(Lucide.X, null, Modifier.size(20.dp), tint = Color.White)
                    }
                }
            }
        }
    }
}
