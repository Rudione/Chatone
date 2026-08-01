package io.rudione.chatone.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Stable
data class ScrollbarThumb(
    val offset: Float,
    val size: Float,
    val scrollable: Boolean
) {
    companion object {
        val Hidden = ScrollbarThumb(0f, 1f, scrollable = false)
    }
}

private val TrackColor = Color.White.copy(alpha = 0.05f)
private val ThumbColor = Color.White.copy(alpha = 0.22f)
private val ThumbColorHover = Color.White.copy(alpha = 0.40f)
private val MinThumbDp = 28.dp

@Stable
private class LazyContentMetrics {
    private val heights = HashMap<Any, Int>()
    var knownSum = 0L
        private set
    var knownCount = 0
        private set

    fun record(key: Any, height: Int) {
        val previous = heights.put(key, height)
        if (previous == null) {
            knownSum += height
            knownCount++
        } else if (previous != height) {
            knownSum += height - previous
        }
    }

    fun averageOr(fallback: Float): Float =
        if (knownCount > 0) knownSum.toFloat() / knownCount else fallback

    fun reset() {
        heights.clear()
        knownSum = 0L
        knownCount = 0
    }
}

private const val METRICS_RETENTION_SLACK = 4096

@Composable
private fun rememberLazyMetrics(listState: LazyListState, itemCount: Int): State<Float> {
    val metrics = remember { LazyContentMetrics() }
    val average = remember { mutableStateOf(0f) }

    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo }
            .collect { visible ->
                if (visible.isEmpty()) return@collect
                visible.forEach { metrics.record(it.key, it.size) }
                val next = metrics.averageOr(0f)
                if (next > 0f && next != average.value) average.value = next
            }
    }

    LaunchedEffect(itemCount) {
        if (metrics.knownCount > itemCount + METRICS_RETENTION_SLACK) {
            metrics.reset()
        }
    }

    return average
}

@Composable
private fun rememberLazyThumb(
    listState: LazyListState,
    averageHeight: State<Float>,
    frozenAverage: Float?
): ScrollbarThumb {
    return remember(listState, averageHeight, frozenAverage) {
        derivedStateOf {
            val info = listState.layoutInfo
            val total = info.totalItemsCount
            val visible = info.visibleItemsInfo
            if (total == 0 || visible.isEmpty()) return@derivedStateOf ScrollbarThumb.Hidden
            if (!listState.canScrollForward && !listState.canScrollBackward) {
                return@derivedStateOf ScrollbarThumb.Hidden
            }

            val viewport = info.viewportSize.height.toFloat()
            if (viewport <= 0f) return@derivedStateOf ScrollbarThumb.Hidden

            val fallback = visible.sumOf { it.size }.toFloat() / visible.size
            val average = frozenAverage
                ?: averageHeight.value.takeIf { it > 0f }
                ?: fallback
            if (average <= 0f) return@derivedStateOf ScrollbarThumb.Hidden

            val contentHeight = (average * total).coerceAtLeast(viewport)
            val maxScroll = contentHeight - viewport
            if (maxScroll <= 0f) return@derivedStateOf ScrollbarThumb.Hidden

            val scrolled = (average * listState.firstVisibleItemIndex +
                    listState.firstVisibleItemScrollOffset).coerceIn(0f, maxScroll)

            ScrollbarThumb(
                offset = scrolled / maxScroll,
                size = (viewport / contentHeight).coerceIn(0f, 1f),
                scrollable = true
            )
        }
    }.value
}

@Composable
fun ChatoneLazyScrollbar(
    listState: LazyListState,
    itemCount: Int,
    modifier: Modifier = Modifier,
    ticks: List<ScrollbarTick> = emptyList()
) {
    val scope = rememberCoroutineScope()
    val averageHeight = rememberLazyMetrics(listState, itemCount)
    var frozenAverage by remember { mutableStateOf<Float?>(null) }
    var isDragging by remember { mutableStateOf(false) }
    var isHovered by remember { mutableStateOf(false) }

    val thumb = rememberLazyThumb(listState, averageHeight, frozenAverage)
    if (!thumb.scrollable) {
        Box(modifier = modifier)
        return
    }

    Canvas(
        modifier = modifier
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    isDragging = true
                    val dragAvg = averageHeight.value.takeIf { it > 0f } ?: run {
                        val visible = listState.layoutInfo.visibleItemsInfo
                        if (visible.isEmpty()) 1f
                        else visible.sumOf { it.size }.toFloat() / visible.size
                    }
                    frozenAverage = dragAvg

                    val info = listState.layoutInfo
                    val viewport = info.viewportSize.height.toFloat()
                    val contentHeight = (dragAvg * info.totalItemsCount).coerceAtLeast(viewport)
                    val maxScroll = (contentHeight - viewport).coerceAtLeast(0f)

                    val thumbPx = thumbHeightPx(size.height.toFloat(), thumb.size, MinThumbDp.toPx())
                    val thumbTop = thumb.offset * (size.height - thumbPx).coerceAtLeast(0f)
                    val grabbedThumb = down.position.y in thumbTop..(thumbTop + thumbPx)

                    if (!grabbedThumb) {
                        val target = ((down.position.y - thumbPx / 2f) /
                                (size.height - thumbPx).coerceAtLeast(1f)).coerceIn(0f, 1f)
                        val targetPx = target * maxScroll
                        val index = (targetPx / dragAvg).toInt()
                            .coerceIn(0, (info.totalItemsCount - 1).coerceAtLeast(0))
                        val rest = (targetPx - index * dragAvg).toInt().coerceAtLeast(0)
                        scope.launch { listState.scrollToItem(index, rest) }
                    }
                    down.consume()

                    var pointerId = down.id
                    try {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                            if (!change.pressed) break
                            if (change.positionChanged()) {
                                val dragPx = change.position.y - change.previousPosition.y
                                val usableTrack = (size.height - thumbPx).coerceAtLeast(1f)
                                val delta = dragPx * (maxScroll / usableTrack)
                                if (delta != 0f) scope.launch { listState.scrollBy(delta) }
                                change.consume()
                            }
                            pointerId = change.id
                        }
                    } finally {
                        isDragging = false
                        frozenAverage = null
                    }
                }
            }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        when (event.type) {
                            PointerEventType.Enter -> isHovered = true
                            PointerEventType.Exit -> isHovered = false
                            PointerEventType.Scroll -> {
                                val amount = event.changes.firstOrNull()?.scrollDelta?.y ?: 0f
                                if (amount != 0f) {
                                    scope.launch { listState.scrollBy(amount) }
                                    event.changes.forEach { it.consume() }
                                }
                            }
                            else -> Unit
                        }
                    }
                }
            }
    ) {
        drawScrollbar(
            trackHeight = size.height,
            trackWidth = size.width,
            progress = thumb.offset,
            sizeFraction = thumb.size,
            active = isDragging || isHovered,
            ticks = ticks
        )
    }
}

@Composable
fun ChatoneScrollbar(
    scrollState: ScrollState,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var isDragging by remember { mutableStateOf(false) }
    var isHovered by remember { mutableStateOf(false) }

    val maxValue = scrollState.maxValue
    if (maxValue <= 0 || maxValue == Int.MAX_VALUE) {
        Box(modifier = modifier)
        return
    }

    Canvas(
        modifier = modifier
            .pointerInput(maxValue) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    isDragging = true
                    val viewport = size.height.toFloat()
                    val contentHeight = viewport + maxValue
                    val thumbPx = thumbHeightPx(viewport, viewport / contentHeight, MinThumbDp.toPx())
                    val usableTrack = (viewport - thumbPx).coerceAtLeast(1f)
                    val thumbTop = (scrollState.value.toFloat() / maxValue) * usableTrack

                    if (down.position.y !in thumbTop..(thumbTop + thumbPx)) {
                        val target = ((down.position.y - thumbPx / 2f) / usableTrack)
                            .coerceIn(0f, 1f)
                        scope.launch { scrollState.scrollTo((target * maxValue).toInt()) }
                    }
                    down.consume()

                    var pointerId = down.id
                    try {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                            if (!change.pressed) break
                            if (change.positionChanged()) {
                                val dragPx = change.position.y - change.previousPosition.y
                                val delta = dragPx * (maxValue / usableTrack)
                                if (delta != 0f) scope.launch { scrollState.scrollBy(delta) }
                                change.consume()
                            }
                            pointerId = change.id
                        }
                    } finally {
                        isDragging = false
                    }
                }
            }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        when (event.type) {
                            PointerEventType.Enter -> isHovered = true
                            PointerEventType.Exit -> isHovered = false
                            PointerEventType.Scroll -> {
                                val amount = event.changes.firstOrNull()?.scrollDelta?.y ?: 0f
                                if (amount != 0f) {
                                    scope.launch { scrollState.scrollBy(amount) }
                                    event.changes.forEach { it.consume() }
                                }
                            }
                            else -> Unit
                        }
                    }
                }
            }
    ) {
        val viewport = size.height
        val contentHeight = viewport + maxValue
        val sizeFraction = (viewport / contentHeight).coerceIn(0f, 1f)
        val progress = if (maxValue > 0) scrollState.value.toFloat() / maxValue else 0f

        drawScrollbar(
            trackHeight = viewport,
            trackWidth = size.width,
            progress = progress,
            sizeFraction = sizeFraction,
            active = isDragging || isHovered,
            ticks = emptyList()
        )
    }
}

@Stable
data class ScrollbarTick(val fraction: Float, val color: Color)

private fun thumbHeightPx(trackHeight: Float, sizeFraction: Float, minThumb: Float): Float =
    (sizeFraction * trackHeight).coerceIn(minThumb.coerceAtMost(trackHeight), trackHeight)

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawScrollbar(
    trackHeight: Float,
    trackWidth: Float,
    progress: Float,
    sizeFraction: Float,
    active: Boolean,
    ticks: List<ScrollbarTick>
) {
    drawRect(color = TrackColor)

    ticks.forEach { tick ->
        drawRect(
            color = tick.color,
            topLeft = Offset(0f, tick.fraction * trackHeight - 1.dp.toPx()),
            size = Size(trackWidth, 2.dp.toPx())
        )
    }

    val thumbHeight = thumbHeightPx(trackHeight, sizeFraction, MinThumbDp.toPx())
    val usableTrack = (trackHeight - thumbHeight).coerceAtLeast(0f)
    val thumbTop = progress.coerceIn(0f, 1f) * usableTrack

    drawRoundRect(
        color = if (active) ThumbColorHover else ThumbColor,
        topLeft = Offset(1.dp.toPx(), thumbTop),
        size = Size((trackWidth - 2.dp.toPx()).coerceAtLeast(1f), thumbHeight),
        cornerRadius = CornerRadius(4.dp.toPx())
    )
}
