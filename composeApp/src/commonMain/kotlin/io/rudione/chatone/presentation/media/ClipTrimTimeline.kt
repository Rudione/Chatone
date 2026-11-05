package io.rudione.chatone.presentation.media

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import io.rudione.chatone.domain.media.ClipTrim
import io.rudione.chatone.domain.media.ClipTrimGrip
import kotlin.math.abs

private const val KEY_STEP_SECONDS = 1f
private const val HANDLE_REACH = 1.4f

@Composable
internal fun ClipTrimTimeline(
    totalSeconds: Float,
    start: Float,
    end: Float,
    playhead: () -> Float,
    onTrimStart: () -> Unit,
    onRangeChange: (start: Float, end: Float, previewAt: Float) -> Unit,
    onTrimEnd: () -> Unit,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    val tick = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    val grip = MaterialTheme.colorScheme.onPrimary
    val focusRing = MaterialTheme.colorScheme.onSurface
    val handleWidth = with(LocalDensity.current) { 18.dp.toPx() }
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    var dragging by remember { mutableStateOf(false) }
    var dragRange by remember { mutableStateOf<ClosedFloatingPointRange<Float>?>(null) }
    var keyGrip by remember { mutableStateOf(ClipTrimGrip.START) }
    val shown = dragRange ?: (start..end)
    val currentRange by rememberUpdatedState(shown)
    val begin by rememberUpdatedState(onTrimStart)
    val change by rememberUpdatedState(onRangeChange)
    val finish by rememberUpdatedState(onTrimEnd)

    LaunchedEffect(start, end, dragging) {
        if (!dragging) dragRange = null
    }

    fun apply(grab: ClipTrimGrip, anchor: ClosedFloatingPointRange<Float>, delta: Float) {
        val range = ClipTrim.move(grab, anchor, delta, totalSeconds)
        dragRange = range
        change(range.start, range.endInclusive, ClipTrim.previewAnchor(grab, range))
    }

    fun beginDrag() {
        dragging = true
        begin()
    }

    fun endDrag() {
        dragging = false
        finish()
    }

    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(64.dp)
            .onPreviewKeyEvent { event ->
                if (!focused) return@onPreviewKeyEvent false
                val direction = when (event.key) {
                    Key.DirectionLeft -> -1
                    Key.DirectionRight -> 1
                    else -> 0
                }
                when {
                    direction != 0 && event.type == KeyEventType.KeyDown -> {
                        if (!dragging) beginDrag()
                        apply(keyGrip, currentRange, direction * KEY_STEP_SECONDS)
                        true
                    }

                    direction != 0 && event.type == KeyEventType.KeyUp -> {
                        if (dragging) endDrag()
                        true
                    }

                    event.isConfirmKey() && event.type == KeyEventType.KeyUp -> {
                        keyGrip = keyGrip.next()
                        true
                    }

                    event.isConfirmKey() -> true
                    else -> false
                }
            }
            .focusable(interactionSource = interaction)
    ) {
        val widthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val perSecond = widthPx / totalSeconds.coerceAtLeast(1f)
        Canvas(
            Modifier.fillMaxSize()
                .pointerInput(totalSeconds, perSecond) {
                    detectTapGestures { offset ->
                        onSeek(
                            (offset.x / perSecond).coerceIn(
                                0f,
                                totalSeconds
                            )
                        )
                    }
                }
                .pointerInput(totalSeconds, perSecond) {
                    var grab = ClipTrimGrip.BODY
                    var anchor = 0f..0f
                    var travelled = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            anchor = currentRange
                            travelled = 0f
                            grab = pickGrip(offset.x, anchor, perSecond, handleWidth * HANDLE_REACH)
                            beginDrag()
                        },
                        onDragEnd = ::endDrag,
                        onDragCancel = ::endDrag,
                        onHorizontalDrag = { pointer, amount ->
                            pointer.consume()
                            travelled += amount / perSecond
                            apply(grab, anchor, travelled)
                        }
                    )
                }
        ) {
            val corner = CornerRadius(14.dp.toPx())
            drawRoundRect(track, cornerRadius = corner)
            drawTicks(tick, (perSecond * 2f).coerceAtLeast(6f))
            val left = shown.start * perSecond
            val right = shown.endInclusive * perSecond
            drawRoundRect(
                accent.copy(alpha = 0.28f),
                Offset(left, 0f),
                Size(right - left, size.height),
                corner
            )
            drawRoundRect(
                accent,
                Offset(left, 0f),
                Size(right - left, size.height),
                corner,
                style = Stroke(width = 3.dp.toPx())
            )
            drawHandle(left, handleWidth, accent, grip, corner)
            drawHandle(right - handleWidth, handleWidth, accent, grip, corner)
            if (focused) {
                val outline = Stroke(width = 3.dp.toPx())
                when (keyGrip) {
                    ClipTrimGrip.START -> drawRoundRect(
                        focusRing,
                        Offset(left, 0f),
                        Size(handleWidth, size.height),
                        corner,
                        style = outline
                    )

                    ClipTrimGrip.END -> drawRoundRect(
                        focusRing,
                        Offset(right - handleWidth, 0f),
                        Size(handleWidth, size.height),
                        corner,
                        style = outline
                    )

                    ClipTrimGrip.BODY -> drawRoundRect(
                        focusRing,
                        Offset(left, 0f),
                        Size(right - left, size.height),
                        corner,
                        style = outline
                    )
                }
            }
            val head = (ClipTrim.playhead(playhead(), shown) * perSecond).coerceIn(0f, size.width)
            drawLine(
                Color.White,
                Offset(head, -2f),
                Offset(head, size.height + 2f),
                strokeWidth = 3.dp.toPx()
            )
        }
    }
}

private fun pickGrip(
    x: Float,
    range: ClosedFloatingPointRange<Float>,
    perSecond: Float,
    reach: Float
): ClipTrimGrip {
    val toStart = abs(x - range.start * perSecond)
    val toEnd = abs(x - range.endInclusive * perSecond)
    return when {
        toStart <= reach && toStart <= toEnd -> ClipTrimGrip.START
        toEnd <= reach -> ClipTrimGrip.END
        else -> ClipTrimGrip.BODY
    }
}

private fun ClipTrimGrip.next(): ClipTrimGrip = when (this) {
    ClipTrimGrip.START -> ClipTrimGrip.END
    ClipTrimGrip.END -> ClipTrimGrip.BODY
    ClipTrimGrip.BODY -> ClipTrimGrip.START
}

private fun androidx.compose.ui.input.key.KeyEvent.isConfirmKey(): Boolean =
    key == Key.DirectionCenter || key == Key.Enter || key == Key.NumPadEnter

private fun DrawScope.drawTicks(color: Color, step: Float) {
    var x = step
    while (x < size.width) {
        drawLine(
            color,
            Offset(x, size.height * 0.3f),
            Offset(x, size.height * 0.7f),
            strokeWidth = 1.dp.toPx()
        )
        x += step
    }
}

private fun DrawScope.drawHandle(
    x: Float,
    width: Float,
    color: Color,
    dots: Color,
    corner: CornerRadius
) {
    drawRoundRect(color, Offset(x, 0f), Size(width, size.height), corner)
    repeat(3) { row ->
        val cy = size.height / 2f + (row - 1) * 7.dp.toPx()
        drawCircle(dots, 1.8.dp.toPx(), Offset(x + width / 2f, cy))
    }
}
