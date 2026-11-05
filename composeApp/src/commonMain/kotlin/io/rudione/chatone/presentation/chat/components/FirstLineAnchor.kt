package io.rudione.chatone.presentation.chat

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.TextLayoutResult
import kotlin.math.roundToInt

@Stable
internal class FirstLineAnchor {
    private var top by mutableIntStateOf(NONE)
    private var bottom by mutableIntStateOf(NONE)

    fun update(layout: TextLayoutResult) {
        if (layout.lineCount == 0) return
        top = layout.getLineTop(0).roundToInt()
        bottom = layout.getLineBottom(0).roundToInt()
    }

    fun offsetFor(height: Int): Int {
        if (top == NONE || bottom <= top) return 0
        return ((top + bottom) / 2 - height / 2).coerceAtLeast(0)
    }

    private companion object {
        const val NONE = -1
    }
}

internal fun Modifier.centerOnFirstLine(anchor: FirstLineAnchor): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val offset = anchor.offsetFor(placeable.height)
    layout(placeable.width, placeable.height + offset) { placeable.place(0, offset) }
}
