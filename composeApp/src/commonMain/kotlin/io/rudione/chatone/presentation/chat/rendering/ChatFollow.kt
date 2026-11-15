package io.rudione.chatone.presentation.chat.rendering

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import androidx.compose.ui.unit.IntSize

val ChatFollowPauseThreshold: Dp = 12.dp

internal fun shouldPauseFollowing(overflowPx: Float?, itemCount: Int, thresholdPx: Float): Boolean {
    if (itemCount == 0) return false
    return overflowPx == null || overflowPx > thresholdPx
}

internal data class BottomEdge(
    val itemCount: Int,
    val lastKey: Any?,
    val overflowPx: Float?,
    val scrolling: Boolean
)

internal fun growthToAbsorb(previous: BottomEdge?, current: BottomEdge, tolerancePx: Float): Float {
    if (previous == null || current.scrolling) return 0f
    if (previous.itemCount != current.itemCount || previous.lastKey != current.lastKey) return 0f
    val before = previous.overflowPx ?: return 0f
    val after = current.overflowPx ?: return 0f
    if (before > tolerancePx || after <= tolerancePx) return 0f
    return after
}

@Stable
class ChatFollowState internal constructor(
    private val listState: LazyListState,
    private val pauseThresholdPx: Float
) {
    var isPausedByScroll by mutableStateOf(false)
        private set

    var isTouchHeld by mutableStateOf(false)
        internal set

    internal var bottomRequest by mutableIntStateOf(0)
        private set

    val nestedScrollConnection: NestedScrollConnection = object : NestedScrollConnection {
        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
            if (consumed.y != 0f) onUserScrolled()
            return Offset.Zero
        }
    }

    fun onUserScrolled() {
        isPausedByScroll = shouldPauseFollowing(
            overflowPx = listState.bottomOverflow(),
            itemCount = listState.layoutInfo.totalItemsCount,
            thresholdPx = pauseThresholdPx
        )
    }

    fun pause() {
        isPausedByScroll = true
    }

    fun resume() {
        isPausedByScroll = false
    }

    fun requestBottom() {
        bottomRequest++
    }
}

fun Modifier.holdChatFollowing(follow: ChatFollowState): Modifier = pointerInput(follow) {
    try {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                follow.isTouchHeld = event.changes.any { it.pressed && it.type != PointerType.Mouse }
            }
        }
    } finally {
        follow.isTouchHeld = false
    }
}

@Composable
fun rememberChatFollowState(
    listState: LazyListState,
    pauseThreshold: Dp = ChatFollowPauseThreshold
): ChatFollowState {
    val thresholdPx = with(LocalDensity.current) { pauseThreshold.toPx() }
    return remember(listState, thresholdPx) { ChatFollowState(listState, thresholdPx) }
}

@Composable
fun ChatFollowEffects(
    listState: LazyListState,
    newestItemKey: Any?,
    paused: Boolean,
    onPinnedToBottom: () -> Unit,
    holdBottomWhilePaused: Boolean = false,
    follow: ChatFollowState? = null
) {
    val newestKeyLatest by rememberUpdatedState(newestItemKey)
    val pausedLatest by rememberUpdatedState(paused)
    val holdBottomLatest by rememberUpdatedState(holdBottomWhilePaused)
    val onPinnedLatest by rememberUpdatedState(onPinnedToBottom)

    LaunchedEffect(listState, follow) {
        var previous: BottomEdge? = null
        var initialPinDone = false
        snapshotFlow {
            val info = listState.layoutInfo
            FollowObservation(
                edge = BottomEdge(
                    itemCount = info.totalItemsCount,
                    lastKey = info.visibleItemsInfo.lastOrNull()?.key,
                    overflowPx = listState.bottomOverflow(),
                    scrolling = listState.isScrollInProgress
                ),
                newestKey = newestKeyLatest,
                viewportSize = info.viewportSize,
                paused = pausedLatest,
                holdBottom = holdBottomLatest,
                touchHeld = follow?.isTouchHeld == true,
                bottomRequest = follow?.bottomRequest ?: 0
            )
        }.collect { observation ->
            val edge = observation.edge
            val delta = growthToAbsorb(previous, edge, BOTTOM_TOLERANCE_PX)
            previous = edge
            if (observation.touchHeld || edge.scrolling || edge.itemCount == 0) return@collect
            if (observation.paused && initialPinDone) {
                if (observation.holdBottom && delta > 0f) listState.scrollByUnlessInterrupted(delta)
            } else {
                try {
                    listState.stickToBottomExact()
                    initialPinDone = true
                    onPinnedLatest()
                } catch (interrupted: CancellationException) {
                    currentCoroutineContext().ensureActive()
                }
            }
        }
    }
}

private data class FollowObservation(
    val edge: BottomEdge,
    val newestKey: Any?,
    val viewportSize: IntSize,
    val paused: Boolean,
    val holdBottom: Boolean,
    val touchHeld: Boolean,
    val bottomRequest: Int
)

private const val BOTTOM_TOLERANCE_PX = 1f

private suspend fun LazyListState.scrollByUnlessInterrupted(delta: Float) {
    try {
        scrollBy(delta)
    } catch (interrupted: CancellationException) {
        currentCoroutineContext().ensureActive()
    }
}
