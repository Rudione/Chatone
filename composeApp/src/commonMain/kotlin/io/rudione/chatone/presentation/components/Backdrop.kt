package io.rudione.chatone.presentation.components

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.requireGraphicsContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import io.rudione.chatone.util.system.isBackdropBlurSupported
import kotlin.math.abs
import kotlin.math.ceil

private const val MIN_STRENGTH = 0.01f
private const val RADIUS_STEP_PX = 0.25f
private const val SATURATION_STEP = 0.02f
private const val PATCH_DOWNSCALE = 4f

@Stable
class BackdropState {
    internal var targetBounds by mutableStateOf(Rect.Zero)
    internal var targetCornerRadius by mutableStateOf(0.dp)
}

fun Modifier.backdropTarget(state: BackdropState, cornerRadius: Dp): Modifier =
    this then BackdropTargetElement(state, cornerRadius)

fun Modifier.backdropSource(
    state: BackdropState,
    blurRadius: Dp,
    saturation: Float = 1f,
    underlay: Color = Color.Unspecified,
    strength: () -> Float
): Modifier = this then BackdropSourceElement(state, blurRadius, saturation, underlay, strength)

private data class BackdropTargetElement(
    val state: BackdropState,
    val cornerRadius: Dp
) : ModifierNodeElement<BackdropTargetNode>() {
    override fun create(): BackdropTargetNode = BackdropTargetNode(state, cornerRadius)

    override fun update(node: BackdropTargetNode) {
        node.state = state
        node.cornerRadius = cornerRadius
        state.targetCornerRadius = cornerRadius
    }
}

private class BackdropTargetNode(
    var state: BackdropState,
    var cornerRadius: Dp
) : Modifier.Node(), GlobalPositionAwareModifierNode {

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        val bounds = coordinates.boundsInRoot()
        if (state.targetBounds != bounds) state.targetBounds = bounds
        if (state.targetCornerRadius != cornerRadius) state.targetCornerRadius = cornerRadius
    }

    override fun onDetach() {
        state.targetBounds = Rect.Zero
    }
}

private data class BackdropSourceElement(
    val state: BackdropState,
    val blurRadius: Dp,
    val saturation: Float,
    val underlay: Color,
    val strength: () -> Float
) : ModifierNodeElement<BackdropSourceNode>() {
    override fun create(): BackdropSourceNode =
        BackdropSourceNode(state, blurRadius, saturation, underlay, strength)

    override fun update(node: BackdropSourceNode) {
        node.state = state
        node.blurRadius = blurRadius
        node.saturation = saturation
        node.underlay = underlay
        node.strength = strength
        node.invalidateDraw()
    }
}

private class BackdropSourceNode(
    var state: BackdropState,
    var blurRadius: Dp,
    var saturation: Float,
    var underlay: Color,
    var strength: () -> Float
) : Modifier.Node(), DrawModifierNode, GlobalPositionAwareModifierNode {

    private var contentLayer: GraphicsLayer? = null
    private var patchLayer: GraphicsLayer? = null
    private var originInRoot = Offset.Zero
    private var cachedRadius = -1f
    private var cachedEffect: RenderEffect? = null
    private var cachedSaturation = 1f
    private var cachedFilter: ColorFilter? = null
    private val clip = Path()

    override fun onAttach() {
        if (!isBackdropBlurSupported) return
        val context = requireGraphicsContext()
        contentLayer = context.createGraphicsLayer()
        patchLayer = context.createGraphicsLayer()
    }

    override fun onDetach() {
        val context = requireGraphicsContext()
        contentLayer?.let(context::releaseGraphicsLayer)
        patchLayer?.let(context::releaseGraphicsLayer)
        contentLayer = null
        patchLayer = null
        cachedEffect = null
        cachedRadius = -1f
        cachedFilter = null
        cachedSaturation = 1f
    }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        val origin = coordinates.positionInRoot()
        if (origin != originInRoot) {
            originInRoot = origin
            invalidateDraw()
        }
    }

    override fun ContentDrawScope.draw() {
        val content = contentLayer
        val patch = patchLayer
        val amount = strength().coerceIn(0f, 1f)
        val target = state.targetBounds
        if (content == null || patch == null || amount <= MIN_STRENGTH || target.isEmpty) {
            drawContent()
            return
        }

        content.record { this@draw.drawContent() }

        val local = target.translate(-originInRoot)
        val radius = blurRadius.toPx() * amount
        val margin = ceil(radius * 2f)
        val area = Rect(
            left = local.left - margin,
            top = local.top - margin,
            right = local.right + margin,
            bottom = local.bottom + margin
        ).intersect(Rect(Offset.Zero, size))
        if (area.width < 1f || area.height < 1f) {
            drawLayer(content)
            return
        }

        val corner = state.targetCornerRadius.toPx()
        clip.reset()
        clip.addRoundRect(RoundRect(local, CornerRadius(corner, corner)))
        val opaquePatch = underlay.isSpecified
        if (opaquePatch) drawLayer(content) else clipPath(clip, ClipOp.Difference) { drawLayer(content) }

        val shrink = 1f / PATCH_DOWNSCALE
        patch.renderEffect = blurEffect(radius * shrink)
        patch.colorFilter = saturationFilter(1f + (saturation - 1f) * amount)
        patch.record(
            IntSize(
                ceil(area.width * shrink).toInt().coerceAtLeast(1),
                ceil(area.height * shrink).toInt().coerceAtLeast(1)
            )
        ) {
            if (opaquePatch) drawRect(underlay)
            scale(shrink, shrink, pivot = Offset.Zero) {
                translate(-area.left, -area.top) { drawLayer(content) }
            }
        }
        clipPath(clip) {
            translate(area.left, area.top) {
                scale(PATCH_DOWNSCALE, PATCH_DOWNSCALE, pivot = Offset.Zero) { drawLayer(patch) }
            }
        }
    }

    private fun blurEffect(radius: Float): RenderEffect? {
        if (radius < RADIUS_STEP_PX) return null
        val current = cachedEffect
        if (current != null && abs(radius - cachedRadius) < RADIUS_STEP_PX) return current
        return BlurEffect(radius, radius, TileMode.Clamp).also {
            cachedEffect = it
            cachedRadius = radius
        }
    }

    private fun saturationFilter(value: Float): ColorFilter? {
        if (abs(value - 1f) < SATURATION_STEP) return null
        val current = cachedFilter
        if (current != null && abs(value - cachedSaturation) < SATURATION_STEP) return current
        return ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(value) }).also {
            cachedFilter = it
            cachedSaturation = value
        }
    }
}
