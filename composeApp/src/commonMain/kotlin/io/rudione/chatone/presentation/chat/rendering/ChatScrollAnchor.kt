package io.rudione.chatone.presentation.chat.rendering

import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState

suspend fun LazyListState.stickToBottomExact(maxPasses: Int = 3) {
    val lastIndex = layoutInfo.totalItemsCount - 1
    if (lastIndex < 0) return

    scrollToItem(lastIndex)

    repeat(maxPasses) {
        val overflow = bottomOverflow() ?: return
        if (overflow <= 0f) return
        scrollBy(overflow)
    }
}

fun LazyListState.bottomOverflow(): Float? {
    val info = layoutInfo
    val last = info.visibleItemsInfo.lastOrNull() ?: return null
    if (last.index != info.totalItemsCount - 1) return null
    return ((last.offset + last.size) - info.viewportEndOffset + info.afterContentPadding).toFloat()
}
