package io.rudione.chatone.presentation.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection

@Stable
class OverlayContentPadding(
    private val horizontal: Dp,
    private val vertical: Dp,
    private val density: Density,
    private val overlayHeightPx: () -> Int
) : PaddingValues {
    override fun calculateLeftPadding(layoutDirection: LayoutDirection): Dp = horizontal

    override fun calculateRightPadding(layoutDirection: LayoutDirection): Dp = horizontal

    override fun calculateTopPadding(): Dp = vertical

    override fun calculateBottomPadding(): Dp = vertical + with(density) { overlayHeightPx().toDp() }
}

fun Modifier.insetBottom(inset: () -> Int): Modifier = layout { measurable, constraints ->
    if (!constraints.hasBoundedHeight) {
        val placeable = measurable.measure(constraints)
        return@layout layout(placeable.width, placeable.height) { placeable.place(0, 0) }
    }
    val reserved = inset().coerceIn(0, constraints.maxHeight)
    val inner = constraints.copy(
        minHeight = (constraints.minHeight - reserved).coerceAtLeast(0),
        maxHeight = constraints.maxHeight - reserved
    )
    val placeable = measurable.measure(inner)
    val height = (placeable.height + reserved).coerceIn(constraints.minHeight, constraints.maxHeight)
    layout(placeable.width, height) { placeable.place(0, 0) }
}
