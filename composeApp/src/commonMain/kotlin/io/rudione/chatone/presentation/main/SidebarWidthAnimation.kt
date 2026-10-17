package io.rudione.chatone.presentation.main

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Stable
import androidx.compose.ui.unit.Dp

@Stable
internal class SidebarWidthAnimation(initial: Dp) {
    private val width = Animatable(initial, Dp.VectorConverter)
    private var dragWidth = initial
    private var dragging = false

    fun current(): Dp = width.value

    suspend fun settle(target: Dp) {
        if (dragging || width.targetValue == target) return
        width.animateTo(target, tween(SETTLE_MS))
    }

    fun drag(delta: Dp, min: Dp, max: Dp): Dp {
        if (!dragging) {
            dragging = true
            dragWidth = width.value
        }
        dragWidth = (dragWidth + delta).coerceIn(min, max)
        return dragWidth
    }

    suspend fun snapTo(value: Dp) {
        width.snapTo(value)
    }

    fun endDrag(): Dp {
        dragging = false
        return dragWidth
    }

    private companion object {
        const val SETTLE_MS = 220
    }
}
