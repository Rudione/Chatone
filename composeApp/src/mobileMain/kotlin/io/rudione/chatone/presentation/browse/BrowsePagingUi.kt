package io.rudione.chatone.presentation.browse

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import io.rudione.chatone.presentation.theme.i18n.BrowseStrings

internal enum class PagedContent { Loading, Failed, Empty, Items }

internal val LazyPagingItems<*>.content: PagedContent
    get() = when {
        itemCount > 0 -> PagedContent.Items
        loadState.refresh is LoadState.Error || loadState.append is LoadState.Error -> PagedContent.Failed
        loadState.refresh is LoadState.NotLoading && loadState.append.endOfPaginationReached -> PagedContent.Empty
        else -> PagedContent.Loading
    }

internal val LazyPagingItems<*>.isRefreshingContent: Boolean
    get() = itemCount > 0 && loadState.refresh is LoadState.Loading

internal fun LazyGridScope.pagingFooter(
    items: LazyPagingItems<*>,
    strings: BrowseStrings,
    keyPrefix: String
) {
    val failed =
        items.loadState.refresh is LoadState.Error || items.loadState.append is LoadState.Error
    when {
        failed -> fullSpanItem("${keyPrefix}_footer_error") {
            BrowseMessage(strings.loadFailed, strings.retry, items::retry)
        }

        items.loadState.append is LoadState.Loading -> fullSpanItem("${keyPrefix}_footer_loading") { LoadingFooter() }
    }
}

internal fun LazyGridScope.fullSpanItem(
    key: String,
    content: @Composable LazyGridItemScope.() -> Unit
) = item(key = key, span = { GridItemSpan(maxLineSpan) }, contentType = key, content = content)

@Composable
internal fun LoadingFooter() {
    Box(Modifier.fillMaxWidth().padding(vertical = 18.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(modifier = Modifier.size(26.dp), strokeWidth = 2.5.dp)
    }
}
