package io.rudione.chatone.presentation.chat.rendering

import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember

suspend fun LazyListState.stickToBottomExact(maxPasses: Int = 3) {
    val lastIndex = layoutInfo.totalItemsCount - 1
    if (lastIndex < 0) return
    if (bottomOverflow()?.let { it <= 0f } == true) return

    scrollToItem(lastIndex)

    repeat(maxPasses) {
        val overflow = bottomOverflow() ?: return
        if (overflow <= 0f) return
        if (scrollBy(overflow) <= 0f) return
    }
}

fun LazyListState.bottomOverflow(): Float? {
    val info = layoutInfo
    val last = info.visibleItemsInfo.lastOrNull() ?: return null
    if (last.index != info.totalItemsCount - 1) return null
    return ((last.offset + last.size) - info.viewportEndOffset + info.afterContentPadding).toFloat()
}

@Composable
fun <T> PreserveScrollAnchor(state: LazyListState, items: List<T>, keyOf: (T) -> Any) {
    val applied = remember(state) { AppliedItems() }
    SideEffect {
        if (applied.items === items) return@SideEffect
        applied.items = items
        val anchorIndex = state.firstVisibleItemIndex
        val anchor = state.layoutInfo.visibleItemsInfo.firstOrNull { it.index == anchorIndex } ?: return@SideEffect
        val target = relocatedAnchorIndex(items, keyOf, anchor.key, anchorIndex) ?: return@SideEffect
        state.requestScrollToItem(target, state.firstVisibleItemScrollOffset)
    }
}

internal fun <T> relocatedAnchorIndex(
    items: List<T>,
    keyOf: (T) -> Any,
    anchorKey: Any,
    lastIndex: Int,
    reach: Int = LAZY_LIST_TRACKED_SHIFT
): Int? {
    for (distance in 0..reach) {
        val before = lastIndex - distance
        if (before in items.indices && keyOf(items[before]) == anchorKey) return null
        val after = lastIndex + distance
        if (distance > 0 && after in items.indices && keyOf(items[after]) == anchorKey) return null
    }
    return items.indexOfFirst { keyOf(it) == anchorKey }.takeIf { it >= 0 }
}

private class AppliedItems {
    var items: List<*>? = null
}

private const val LAZY_LIST_TRACKED_SHIFT = 64
