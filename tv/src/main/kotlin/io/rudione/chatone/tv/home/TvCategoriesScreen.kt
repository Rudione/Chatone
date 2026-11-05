package io.rudione.chatone.tv.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.browse_tab_categories
import chatone.composeapp.generated.resources.tv_load_failed
import io.rudione.chatone.domain.browse.BrowseCategory
import io.rudione.chatone.domain.browse.BrowseStream
import io.rudione.chatone.presentation.browse.BrowseEvent
import io.rudione.chatone.presentation.browse.BrowseViewModel
import io.rudione.chatone.tv.components.TvCategoryCard
import io.rudione.chatone.tv.components.TvLoading
import io.rudione.chatone.tv.components.TvMessage
import io.rudione.chatone.tv.components.TvStreamCard
import io.rudione.chatone.tv.theme.TvDimens
import org.jetbrains.compose.resources.stringResource

private const val CATEGORY_COLUMNS = 6
private const val STREAM_COLUMNS = 3

@Composable
fun TvCategoriesScreen(
    viewModel: BrowseViewModel,
    actions: TvBrowseActions,
    contentFocus: FocusRequester,
    modifier: Modifier = Modifier
) {
    val categories = viewModel.categories.collectAsLazyPagingItems()
    val gridState = rememberLazyGridState()
    LaunchedEffect(categories.itemCount > 0) {
        if (categories.itemCount > 0) {
            withFrameNanos { }
            runCatching { contentFocus.requestFocus() }
        }
    }
    Column(modifier
        .fillMaxSize()
        .padding(top = TvDimens.ScreenVertical)) {
        TvScreenTitle(stringResource(Res.string.browse_tab_categories))
        TvPagedState(categories) {
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Fixed(CATEGORY_COLUMNS),
                modifier = Modifier
                    .fillMaxSize()
                    .focusRestorer(),
                contentPadding = PaddingValues(horizontal = 32.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(TvDimens.CardGap),
                verticalArrangement = Arrangement.spacedBy(TvDimens.RowGap)
            ) {
                items(
                    count = categories.itemCount,
                    key = categories.itemKey { it.id },
                    contentType = { "category" }
                ) { index ->
                    categories[index]?.let { category ->
                        TvCategoryCard(
                            category = category,
                            onClick = { actions.onOpenCategory(category) },
                            width = TvDimens.CategoryCardWidth
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TvCategoryStreamsScreen(
    category: BrowseCategory,
    viewModel: BrowseViewModel,
    onOpenStream: (BrowseStream) -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(category.id) { viewModel.sendEvent(BrowseEvent.OpenCategory(category)) }
    val streams = viewModel.streams.collectAsLazyPagingItems()
    val focus = remember { FocusRequester() }
    Column(modifier
        .fillMaxSize()
        .padding(top = TvDimens.ScreenVertical)) {
        TvScreenTitle(category.name)
        TvPagedState(streams) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(STREAM_COLUMNS),
                modifier = Modifier
                    .fillMaxSize()
                    .focusRequester(focus)
                    .focusRestorer(),
                contentPadding = PaddingValues(
                    horizontal = TvDimens.ScreenHorizontal,
                    vertical = 16.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(TvDimens.CardGap),
                verticalArrangement = Arrangement.spacedBy(TvDimens.RowGap)
            ) {
                items(
                    count = streams.itemCount,
                    key = streams.itemKey { it.id },
                    contentType = { "stream" }
                ) { index ->
                    streams[index]?.let { stream ->
                        TvStreamCard(
                            stream = stream,
                            onClick = { onOpenStream(stream) },
                            width = 272.dp
                        )
                    }
                }
            }
            LaunchedEffect(streams.itemCount > 0) {
                if (streams.itemCount > 0) runCatching { focus.requestFocus() }
            }
        }
    }
}

@Composable
internal fun TvScreenTitle(title: String, modifier: Modifier = Modifier) {
    Text(
        title,
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        modifier = modifier.padding(horizontal = TvDimens.ScreenHorizontal)
    )
}

@Composable
private fun <T : Any> TvPagedState(items: LazyPagingItems<T>, content: @Composable () -> Unit) {
    val refresh = items.loadState.refresh
    when {
        items.itemCount > 0 -> content()
        refresh is LoadState.Loading -> TvLoading()
        refresh is LoadState.Error -> TvMessage(
            stringResource(Res.string.tv_load_failed),
            onRetry = items::retry
        )
    }
}
