package io.rudione.chatone.presentation.chat.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.toSize
import io.rudione.chatone.presentation.components.backdropTarget
import io.rudione.chatone.presentation.components.layoutWidth
import kotlin.math.min

private val SplitWidth = 480.dp
private val SegmentGap = 6.dp

@Immutable
internal data class SegmentSpan(val rect: Rect, val round: Boolean)

internal fun segmentRects(spans: List<SegmentSpan>, height: Float, split: Float, fieldCornerPx: Float): List<RoundRect> {
    val ordered = spans.sortedBy { it.rect.left }
    return ordered.mapIndexed { index, span ->
        val own = span.rect
        val rect = lerp(Rect(own.left, 0f, own.right, height), own, split)
        val half = min(rect.width, rect.height) / 2f
        val radius = if (span.round) half else min(fieldCornerPx, half)
        val inner = CornerRadius(radius * split)
        val outer = CornerRadius(radius)
        val left = if (index == 0) outer else inner
        val right = if (index == ordered.lastIndex) outer else inner
        RoundRect(rect, topLeft = left, topRight = right, bottomRight = right, bottomLeft = left)
    }
}

internal fun segmentOutline(rects: List<RoundRect>): Path = rects.fold(Path()) { joined, rect ->
    val piece = Path().apply { addRoundRect(rect) }
    if (joined.isEmpty) piece else Path.combine(PathOperation.Union, joined, piece)
}

@Stable
internal class GlassSegments(private val split: () -> Float) {
    internal val spans = mutableStateMapOf<Int, SegmentSpan>()
    internal var origin by mutableStateOf(Offset.Zero)
    internal var size by mutableStateOf(Size.Zero)
    internal var fieldCornerPx by mutableFloatStateOf(0f)

    val outline: Path by derivedStateOf {
        val local = spans.values.map { it.copy(rect = it.rect.translate(-origin)) }
        segmentOutline(segmentRects(local, size.height, split().coerceIn(0f, 1f), fieldCornerPx))
    }

    val outlineShape: Shape = GenericShape { _, _ -> addPath(outline) }
}

private class FixedPathShape(private val path: Path) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline =
        Outline.Generic(path)
}

private fun Modifier.glassSegment(segments: GlassSegments, id: Int, round: Boolean): Modifier =
    this then GlassSegmentElement(segments, id, round)

private data class GlassSegmentElement(
    val segments: GlassSegments,
    val id: Int,
    val round: Boolean
) : ModifierNodeElement<GlassSegmentNode>() {
    override fun create(): GlassSegmentNode = GlassSegmentNode(segments, id, round)

    override fun update(node: GlassSegmentNode) {
        node.retarget(segments, id, round)
    }
}

private class GlassSegmentNode(
    private var segments: GlassSegments,
    private var id: Int,
    private var round: Boolean
) : Modifier.Node(), GlobalPositionAwareModifierNode {

    private var bounds = Rect.Zero

    fun retarget(newSegments: GlassSegments, newId: Int, newRound: Boolean) {
        if (newSegments !== segments || newId != id) segments.spans.remove(id)
        segments = newSegments
        id = newId
        round = newRound
        publish()
    }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        bounds = Rect(coordinates.positionInRoot(), coordinates.size.toSize())
        publish()
    }

    override fun onDetach() {
        segments.spans.remove(id)
        bounds = Rect.Zero
    }

    private fun publish() {
        if (!isAttached || bounds.isEmpty) return
        val span = SegmentSpan(bounds, round)
        if (segments.spans[id] != span) segments.spans[id] = span
    }
}

@Immutable
internal class InputSurface(
    val resting: () -> Color,
    val border: () -> Color,
    val borderWidth: () -> Dp,
    val focused: Boolean,
    val shadow: Color
)

private const val SEGMENT_EMOJI = 0
private const val SEGMENT_FIELD = 1
private const val SEGMENT_UPLOAD = 2
private const val SEGMENT_SEND = 3

@Composable
internal fun SegmentedMessageBar(
    glass: ChatInputGlass,
    metrics: MessageInputMetrics,
    surface: InputSurface,
    upload: MessageInputUploadState,
    emoji: (@Composable () -> Unit)?,
    send: @Composable (size: () -> Dp) -> Unit,
    modifier: Modifier = Modifier,
    field: @Composable BoxScope.(split: () -> Float) -> Unit
) {
    val density = LocalDensity.current
    val splitPx = with(density) { SplitWidth.toPx() }
    var wide by remember { mutableStateOf<Boolean?>(null) }
    var settled by remember { mutableStateOf(false) }
    val split = remember { Animatable(0f) }
    LaunchedEffect(wide) {
        val target = if (wide ?: return@LaunchedEffect) 1f else 0f
        if (!settled) {
            split.snapTo(target)
            settled = true
        } else {
            split.animateTo(target, spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow))
        }
    }
    val amount: () -> Float = { split.value.coerceIn(0f, 1f) }
    val segments = remember { GlassSegments(amount) }
    segments.fieldCornerPx = with(density) { glass.style.cornerRadius.toPx() }
    val gap: () -> Dp = { SegmentGap * amount() }
    val side = metrics.rowMinHeight

    Row(
        modifier = modifier
            .onSizeChanged { wide = it.width >= splitPx }
            .onGloballyPositioned {
                segments.origin = it.positionInRoot()
                segments.size = it.size.toSize()
            }
            .backdropTarget(glass.backdrop, segments.outlineShape)
            .graphicsLayer {
                val frost = glass.progress()
                val rest = if (surface.focused) ChatGlassFocusedElevation else ChatGlassRestingElevation
                shadowElevation = rest.toPx() + glass.style.shadowBoost.toPx() * frost
                shape = FixedPathShape(segments.outline)
                clip = false
                ambientShadowColor = lerp(surface.shadow, ChatGlassShadow, frost)
                spotShadowColor = lerp(surface.shadow, ChatGlassShadow, frost)
            }
            .drawBehind {
                val path = segments.outline
                val frost = glass.progress()
                drawPath(path, glass.fill(surface.resting()))
                if (frost > 0f) drawPath(path, glass.rim(frost))
                drawPath(path, surface.border(), style = Stroke(surface.borderWidth().toPx()))
            },
        verticalAlignment = Alignment.Bottom
    ) {
        if (emoji != null) {
            Box(
                modifier = Modifier.size(side).glassSegment(segments, SEGMENT_EMOJI, round = true),
                contentAlignment = Alignment.Center
            ) { emoji() }
            Spacer(Modifier.layoutWidth(gap))
        }
        Box(modifier = Modifier.weight(1f).glassSegment(segments, SEGMENT_FIELD, round = false)) {
            field(amount)
        }
        AnimatedVisibility(
            visible = upload.progress != null || upload.link != null,
            enter = expandHorizontally(expandFrom = Alignment.Start, clip = false) +
                scaleIn(spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow), 0.4f) +
                fadeIn(tween(140)),
            exit = shrinkHorizontally(shrinkTowards = Alignment.Start, clip = false) +
                scaleOut(tween(160), 0.4f) +
                fadeOut(tween(120))
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Spacer(Modifier.layoutWidth(gap))
                Box(
                    modifier = Modifier.size(side).glassSegment(segments, SEGMENT_UPLOAD, round = true),
                    contentAlignment = Alignment.Center
                ) {
                    MessageInputUploadButton(upload = upload, size = metrics.emojiTarget)
                }
            }
        }
        Spacer(Modifier.layoutWidth(gap))
        Box(
            modifier = Modifier.size(side).glassSegment(segments, SEGMENT_SEND, round = true),
            contentAlignment = Alignment.Center
        ) {
            send { lerp(metrics.send, side, amount()) }
        }
    }
}
