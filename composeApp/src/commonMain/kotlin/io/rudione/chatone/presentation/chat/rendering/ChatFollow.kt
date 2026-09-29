package io.rudione.chatone.presentation.chat.rendering

import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first

val ChatFollowPauseThreshold: Dp = 12.dp

internal fun shouldPauseFollowing(overflowPx: Float?, itemCount: Int, thresholdPx: Float): Boolean {
    if (itemCount == 0) return false
    return overflowPx == null || overflowPx > thresholdPx
}

@Stable
class ChatFollowState internal constructor(
    private val listState: LazyListState,
    private val pauseThresholdPx: Float
) {
    var isPausedByScroll by mutableStateOf(false)
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
    onPinnedToBottom: () -> Unit
) {
    val pausedLatest by rememberUpdatedState(paused)
    val onPinnedLatest by rememberUpdatedState(onPinnedToBottom)

    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.totalItemsCount }.first { it > 0 }
        listState.stickToBottomExact()
        onPinnedLatest()
    }

    LaunchedEffect(listState, newestItemKey, paused) {
        if (newestItemKey == null || paused) return@LaunchedEffect
        listState.stickToBottomExact()
        onPinnedLatest()
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.viewportSize }
            .drop(1)
            .collect { if (!pausedLatest) listState.stickToBottomExact() }
    }

    LaunchedEffect(listState) {
        snapshotFlow { (listState.bottomOverflow() ?: 0f) to listState.isScrollInProgress }
            .collect { (overflow, scrolling) ->
                if (overflow > 0f && !scrolling && !pausedLatest) listState.scrollByUnlessInterrupted(overflow)
            }
    }
}

private suspend fun LazyListState.scrollByUnlessInterrupted(delta: Float) {
    try {
        scrollBy(delta)
    } catch (interrupted: CancellationException) {
        currentCoroutineContext().ensureActive()
    }
}
