package io.rudione.chatone.presentation.chat.modslider

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

val ModSliderGutter = 16.dp

@Composable
internal fun ModSliderGrabZone(
    handle: ModSliderHandle,
    reachTimestamp: Boolean,
    modifier: Modifier = Modifier
) {
    val state = handle.state
    val style = handle.binding.style
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .layout { measurable, constraints ->
                    val gutter = ModSliderGutter.roundToPx()
                    val stampEnd = handle.timestampEnd
                    val stamp = if (reachTimestamp && stampEnd > 0f) {
                        (ModSliderGutter.toPx() + stampEnd + TIMESTAMP_SLACK.toPx()).roundToInt()
                    } else 0
                    val width = max(gutter, stamp).coerceAtMost(constraints.maxWidth)
                    val height = constraints.maxHeight
                    val placeable = measurable.measure(Constraints.fixed(width, height))
                    layout(width, height) { placeable.place(0, 0) }
                }
                .semantics { contentDescription = style.gripLabel }
                .pointerHoverIcon(PointerIcon.Hand)
                .pointerInput(handle) {
                    awaitPointerEventScope {
                        while (true) {
                            when (awaitPointerEvent().type) {
                                PointerEventType.Enter -> {
                                    handle.zoneHovered = true
                                    state.zoneEntered()
                                }
                                PointerEventType.Exit -> {
                                    handle.zoneHovered = false
                                    state.zoneExited()
                                }
                                else -> Unit
                            }
                        }
                    }
                }
                .modSliderGrab(handle)
                .drawBehind { drawGrip(handle) }
        )
    }
}

private fun DrawScope.drawGrip(handle: ModSliderHandle) {
    val state = handle.state
    val style = handle.binding.style
    val engaged = state.engaged
    val zoneHovered = handle.zoneHovered
    val emphasis = when {
        engaged -> 1f
        zoneHovered -> 0.95f
        handle.hovered() -> 0.7f
        else -> IDLE_ALPHA
    }
    val color = if (engaged) style.colorOf(state.selected) else style.neutral
    val gutter = ModSliderGutter.toPx()
    val lineHeight = min(size.height, FIRST_LINE.toPx())
    val centerX = gutter / 2f
    val centerY = lineHeight / 2f
    if (zoneHovered || engaged) {
        val pillWidth = PILL_WIDTH.toPx()
        val pillHeight = min(lineHeight - PILL_INSET.toPx() * 2f, PILL_MAX_HEIGHT.toPx())
        drawRoundRect(
            color = color.copy(alpha = PILL_ALPHA),
            topLeft = Offset(centerX - pillWidth / 2f, centerY - pillHeight / 2f),
            size = Size(pillWidth, pillHeight),
            cornerRadius = CornerRadius(pillWidth / 2f)
        )
    }
    val dot = DOT_RADIUS.toPx()
    val columnGap = DOT_GAP_X.toPx()
    val rowGap = DOT_GAP_Y.toPx()
    for (column in -1..1 step 2) {
        for (row in -1..1) {
            drawCircle(
                color = color.copy(alpha = emphasis),
                radius = dot,
                center = Offset(centerX + column * columnGap / 2f, centerY + row * rowGap)
            )
        }
    }
}

private const val IDLE_ALPHA = 0.35f
private const val PILL_ALPHA = 0.16f
private val TIMESTAMP_SLACK = 3.dp
private val FIRST_LINE = 28.dp
private val PILL_WIDTH = 12.dp
private val PILL_MAX_HEIGHT = 22.dp
private val PILL_INSET = 2.dp
private val DOT_RADIUS = 1.2.dp
private val DOT_GAP_X = 5.dp
private val DOT_GAP_Y = 4.dp
