package io.rudione.chatone.presentation.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateMeasurement
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import kotlin.math.roundToInt

fun Modifier.layoutWidth(width: () -> Dp): Modifier = this then DeferredSizeElement(width, horizontal = true)

fun Modifier.layoutHeight(height: () -> Dp): Modifier = this then DeferredSizeElement(height, horizontal = false)

fun Modifier.layoutWidthFraction(fraction: () -> Float): Modifier = this then WidthFractionElement(fraction)

private data class DeferredSizeElement(
    val size: () -> Dp,
    val horizontal: Boolean
) : ModifierNodeElement<DeferredSizeNode>() {
    override fun create(): DeferredSizeNode = DeferredSizeNode(size, horizontal)

    override fun update(node: DeferredSizeNode) {
        node.size = size
        node.horizontal = horizontal
        node.invalidateMeasurement()
    }
}

private class DeferredSizeNode(
    var size: () -> Dp,
    var horizontal: Boolean
) : Modifier.Node(), LayoutModifierNode {

    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        val px = size().roundToPx()
        val fixed = if (horizontal) {
            val width = px.coerceIn(constraints.minWidth, constraints.maxWidth)
            constraints.copy(minWidth = width, maxWidth = width)
        } else {
            val height = px.coerceIn(constraints.minHeight, constraints.maxHeight)
            constraints.copy(minHeight = height, maxHeight = height)
        }
        val placeable = measurable.measure(fixed)
        return layout(placeable.width, placeable.height) { placeable.placeRelative(0, 0) }
    }
}

private data class WidthFractionElement(val fraction: () -> Float) : ModifierNodeElement<WidthFractionNode>() {
    override fun create(): WidthFractionNode = WidthFractionNode(fraction)

    override fun update(node: WidthFractionNode) {
        node.fraction = fraction
        node.invalidateMeasurement()
    }
}

private class WidthFractionNode(var fraction: () -> Float) : Modifier.Node(), LayoutModifierNode {

    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        val width = if (constraints.hasBoundedWidth) {
            (constraints.maxWidth * fraction().coerceIn(0f, 1f)).roundToInt()
                .coerceIn(constraints.minWidth, constraints.maxWidth)
        } else {
            constraints.minWidth
        }
        val placeable = measurable.measure(constraints.copy(minWidth = width, maxWidth = width))
        return layout(placeable.width, placeable.height) { placeable.placeRelative(0, 0) }
    }
}
