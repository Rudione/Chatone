package io.rudione.chatone.presentation.chat.timeline

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
internal fun <T> KeepNewestInView(state: LazyListState, newestFirst: List<T>, keyOf: (T) -> Any) {
    val head = remember(state) { NewestHead() }
    SideEffect {
        val current = newestFirst.firstOrNull()?.let(keyOf)
        val previous = head.key
        head.key = current
        if (previous == null || current == null || current == previous) return@SideEffect
        if (state.firstVisibleItemIndex != 0 || state.firstVisibleItemScrollOffset != 0) return@SideEffect
        if (state.layoutInfo.visibleItemsInfo.firstOrNull()?.key != previous) return@SideEffect
        state.requestScrollToItem(0, 0)
    }
}

@Composable
internal fun LoadOlderNearTop(state: LazyListState, canLoad: Boolean, isLoading: Boolean, onLoad: () -> Unit) {
    val open by rememberUpdatedState(canLoad && !isLoading)
    val load by rememberUpdatedState(onLoad)
    LaunchedEffect(state) {
        snapshotFlow {
            val info = state.layoutInfo
            val top = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            open && info.totalItemsCount > 0 && top >= info.totalItemsCount - 1 - OLDER_PREFETCH_ROWS
        }.distinctUntilChanged().collect { wanted -> if (wanted) load() }
    }
}

private class NewestHead {
    var key: Any? = null
}

private const val OLDER_PREFETCH_ROWS = 6
