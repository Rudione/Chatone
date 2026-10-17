package io.rudione.chatone.presentation.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSimple
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.ceil
import kotlin.math.min

fun Modifier.animatedBorder(width: Dp, shape: Shape, color: () -> Color): Modifier =
    this then AnimatedBorderElement(StaticWidth(width), shape, color)

fun Modifier.animatedBorder(width: () -> Dp, shape: Shape, color: () -> Color): Modifier =
    this then AnimatedBorderElement(width, shape, color)

private data class StaticWidth(val width: Dp) : () -> Dp {
    override fun invoke(): Dp = width
}

private data class AnimatedBorderElement(
    val width: () -> Dp,
    val shape: Shape,
    val color: () -> Color
) : ModifierNodeElement<AnimatedBorderNode>() {
    override fun create(): AnimatedBorderNode = AnimatedBorderNode(width, shape, color)

    override fun update(node: AnimatedBorderNode) = node.update(width, shape, color)
}

private class AnimatedBorderNode(
    private var width: () -> Dp,
    private var shape: Shape,
    private var color: () -> Color
) : Modifier.Node(), DrawModifierNode {

    private var outline: Outline? = null
    private var outlineSize = Size.Unspecified
    private var outlineDirection: LayoutDirection? = null
    private var outlineDensity = 0f
    private var outlinePath: Path? = null
    private var stroke: Stroke? = null

    fun update(width: () -> Dp, shape: Shape, color: () -> Color) {
        if (this.shape != shape) outline = null
        this.width = width
        this.shape = shape
        this.color = color
        invalidateDraw()
    }

    override fun ContentDrawScope.draw() {
        drawContent()
        val strokePx = min(ceil(width().toPx()), ceil(size.minDimension / 2f))
        if (strokePx <= 0f) return
        val style = stroke?.takeIf { it.width == strokePx } ?: Stroke(strokePx).also { stroke = it }
        val tint = color()
        val half = strokePx / 2f
        val inner = Size(size.width - strokePx, size.height - strokePx)
        when (val current = outlineFor(this)) {
            is Outline.Rectangle -> drawRect(tint, Offset(half, half), inner, style = style)
            is Outline.Rounded -> {
                val corner = current.roundRect.topLeftCornerRadius
                if (current.roundRect.isSimple && corner.x >= half && corner.y >= half) {
                    drawRoundRect(tint, Offset(half, half), inner, CornerRadius(corner.x - half, corner.y - half), style = style)
                } else {
                    drawInsideStroke(current, tint, strokePx)
                }
            }
            is Outline.Generic -> drawInsideStroke(current, tint, strokePx)
        }
    }

    private fun DrawScope.drawInsideStroke(outline: Outline, tint: Color, strokePx: Float) {
        val path = outlinePath ?: Path().apply { addOutline(outline) }.also { outlinePath = it }
        clipPath(path) { drawPath(path, tint, style = Stroke(strokePx * 2f)) }
    }

    private fun outlineFor(scope: DrawScope): Outline {
        val cached = outline
        if (cached != null && outlineSize == scope.size && outlineDirection == scope.layoutDirection &&
            outlineDensity == scope.density
        ) return cached
        outlinePath = null
        return shape.createOutline(scope.size, scope.layoutDirection, scope).also {
            outline = it
            outlineSize = scope.size
            outlineDirection = scope.layoutDirection
            outlineDensity = scope.density
        }
    }
}
