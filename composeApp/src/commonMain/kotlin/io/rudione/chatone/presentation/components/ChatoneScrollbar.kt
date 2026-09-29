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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.channels.Channel
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

private fun computeLazyThumb(
    listState: LazyListState,
    averageHeight: Float,
    frozenAverage: Float?
): ScrollbarThumb {
    val info = listState.layoutInfo
    val total = info.totalItemsCount
    val visible = info.visibleItemsInfo
    if (total == 0 || visible.isEmpty()) return ScrollbarThumb.Hidden
    if (!listState.canScrollForward && !listState.canScrollBackward) return ScrollbarThumb.Hidden

    val viewport = info.viewportSize.height.toFloat()
    if (viewport <= 0f) return ScrollbarThumb.Hidden

    val fallback = visible.sumOf { it.size }.toFloat() / visible.size
    val average = frozenAverage ?: averageHeight.takeIf { it > 0f } ?: fallback
    if (average <= 0f) return ScrollbarThumb.Hidden

    val contentHeight = (average * total).coerceAtLeast(viewport)
    val maxScroll = contentHeight - viewport
    if (maxScroll <= 0f) return ScrollbarThumb.Hidden

    val scrolled = (average * listState.firstVisibleItemIndex + listState.firstVisibleItemScrollOffset)
        .coerceIn(0f, maxScroll)

    return ScrollbarThumb(
        offset = scrolled / maxScroll,
        size = (viewport / contentHeight).coerceIn(0f, 1f),
        scrollable = true
    )
}

private class LazyScrollTarget(val index: Int, val offset: Int)

@Composable
fun ChatoneLazyScrollbar(
    listState: LazyListState,
    itemCount: Int,
    modifier: Modifier = Modifier,
    ticks: List<ScrollbarTick> = emptyList(),
    onUserScroll: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val onUserScrollLatest by rememberUpdatedState(onUserScroll)
    val averageHeight = rememberLazyMetrics(listState, itemCount)
    val frozenAverage = remember { mutableStateOf<Float?>(null) }
    val isDragging = remember { mutableStateOf(false) }
    val isHovered = remember { mutableStateOf(false) }
    val thumbState = remember(listState, averageHeight) {
        derivedStateOf { computeLazyThumb(listState, averageHeight.value, frozenAverage.value) }
    }
    val scrollable by remember(thumbState) { derivedStateOf { thumbState.value.scrollable } }
    val targets = remember(listState) { Channel<LazyScrollTarget>(Channel.CONFLATED) }
    LaunchedEffect(listState, targets) {
        for (target in targets) {
            listState.scrollToItem(target.index, target.offset)
            onUserScrollLatest()
            withFrameNanos { }
        }
    }

    if (!scrollable) {
        Box(modifier = modifier)
        return
    }

    Canvas(
        modifier = modifier
            .pointerInput(listState) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    isDragging.value = true
                    val info = listState.layoutInfo
                    val dragAvg = averageHeight.value.takeIf { it > 0f } ?: run {
                        val visible = info.visibleItemsInfo
                        if (visible.isEmpty()) 1f else visible.sumOf { it.size }.toFloat() / visible.size
                    }
                    frozenAverage.value = dragAvg

                    val total = info.totalItemsCount
                    val viewport = info.viewportSize.height.toFloat()
                    val contentHeight = (dragAvg * total).coerceAtLeast(viewport)
                    val maxScroll = (contentHeight - viewport).coerceAtLeast(0f)

                    val thumb = thumbState.value
                    val trackHeight = size.height.toFloat()
                    val thumbPx = thumbHeightPx(trackHeight, thumb.size, MinThumbDp.toPx())
                    val usableTrack = (trackHeight - thumbPx).coerceAtLeast(1f)
                    val thumbTop = thumb.offset * usableTrack
                    val grabbedThumb = down.position.y in thumbTop..(thumbTop + thumbPx)
                    val grabOffset = if (grabbedThumb) down.position.y - thumbTop else thumbPx / 2f

                    fun jumpTo(pointerY: Float) {
                        if (total <= 0) return
                        val fraction = ((pointerY - grabOffset) / usableTrack).coerceIn(0f, 1f)
                        val targetPx = fraction * maxScroll
                        val index = (targetPx / dragAvg).toInt().coerceIn(0, total - 1)
                        val rest = (targetPx - index * dragAvg).toInt().coerceAtLeast(0)
                        targets.trySend(LazyScrollTarget(index, rest))
                    }

                    if (!grabbedThumb) jumpTo(down.position.y)
                    down.consume()

                    var pointerId = down.id
                    try {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                            if (!change.pressed) break
                            if (change.positionChanged()) {
                                jumpTo(change.position.y)
                                change.consume()
                            }
                            pointerId = change.id
                        }
                    } finally {
                        isDragging.value = false
                        frozenAverage.value = null
                    }
                }
            }
            .pointerInput(listState) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        when (event.type) {
                            PointerEventType.Enter -> isHovered.value = true
                            PointerEventType.Exit -> isHovered.value = false
                            PointerEventType.Scroll -> {
                                val amount = event.changes.firstOrNull()?.scrollDelta?.y ?: 0f
                                if (amount != 0f) {
                                    scope.launch {
                                        listState.scrollBy(amount)
                                        onUserScrollLatest()
                                    }
                                    event.changes.forEach { it.consume() }
                                }
                            }
                            else -> Unit
                        }
                    }
                }
            }
    ) {
        val thumb = thumbState.value
        drawScrollbar(
            trackHeight = size.height,
            trackWidth = size.width,
            progress = thumb.offset,
            sizeFraction = thumb.size,
            active = isDragging.value || isHovered.value,
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
