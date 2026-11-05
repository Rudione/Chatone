package io.rudione.chatone.presentation.browse

import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.browse_back
import chatone.composeapp.generated.resources.browse_categories_header
import chatone.composeapp.generated.resources.browse_channels_header
import chatone.composeapp.generated.resources.browse_follow_empty
import chatone.composeapp.generated.resources.browse_follow_reconnect
import chatone.composeapp.generated.resources.browse_follow_sign_in
import chatone.composeapp.generated.resources.browse_load_failed
import chatone.composeapp.generated.resources.browse_nothing_found
import chatone.composeapp.generated.resources.browse_popular_categories
import chatone.composeapp.generated.resources.browse_retry
import chatone.composeapp.generated.resources.browse_streams_header
import chatone.composeapp.generated.resources.browse_tab_categories
import chatone.composeapp.generated.resources.browse_tab_following
import chatone.composeapp.generated.resources.browse_tab_popular
import chatone.composeapp.generated.resources.browse_title
import org.jetbrains.compose.resources.stringResource
import androidx.compose.foundation.layout.Box
import kotlinx.coroutines.launch
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import io.rudione.chatone.presentation.components.expressive.ExpressiveTabs
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import io.rudione.chatone.icons.material.Icons
import io.rudione.chatone.icons.material.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import io.rudione.chatone.domain.browse.BrowseCategory
import io.rudione.chatone.domain.browse.BrowseChannel
import io.rudione.chatone.domain.browse.BrowseStream
import io.rudione.chatone.presentation.components.ChatoneIconButton
import io.rudione.chatone.presentation.components.SystemBackHandler
import kotlin.time.Clock
import org.koin.compose.viewmodel.koinViewModel

private val CategoryColumnMinWidth = 112.dp
private val BrowseTabs = BrowseTab.entries.toList()
private const val CATEGORY_PLACEHOLDERS = 12
private val EnterEasing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
private val ExitEasing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

@Composable
fun BrowseScreen(
    onClose: () -> Unit,
    onOpenStream: (BrowseStream) -> Unit,
    onOpenChannel: (BrowseChannel) -> Unit,
    onAskAi: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BrowseViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val categories = viewModel.categories.collectAsLazyPagingItems()
    val streams = viewModel.streams.collectAsLazyPagingItems()
    val popularStreams = viewModel.popularStreams.collectAsLazyPagingItems()
    val followedStreams = viewModel.followedStreams.collectAsLazyPagingItems()
    val directoryGrid = rememberLazyGridState()
    val onEvent: (BrowseEvent) -> Unit = remember(viewModel) { { viewModel.sendEvent(it) } }
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    var filtersOpen by rememberSaveable { mutableStateOf(false) }
    val promptScope = rememberCoroutineScope()

    val goBack: () -> Unit = {
        if (viewModel.state.value.category != null) onEvent(BrowseEvent.CloseCategory) else onClose()
    }
    SystemBackHandler(onBack = goBack)

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
            ) {
                BrowseTopBar(
                    title = state.category?.name ?: stringResource(Res.string.browse_title),
                    onBack = goBack
                )
                AnimatedVisibility(
                    visible = state.category == null,
                    enter = expandVertically(tween(220, easing = EnterEasing)) + fadeIn(tween(180, easing = EnterEasing)),
                    exit = shrinkVertically(tween(180, easing = ExitEasing)) + fadeOut(tween(120, easing = ExitEasing))
                ) {
                    BrowseSearchField(
                        query = state.query,
                        onQueryChange = { onEvent(BrowseEvent.QueryChanged(it)) },
                        modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 2.dp, bottom = 8.dp)
                    )
                }
                AnimatedContent(
                    targetState = state.category,
                    contentKey = { it?.id },
                    transitionSpec = { sharedAxisX(forward = targetState != null) },
                    label = "browsePane",
                    modifier = Modifier.fillMaxSize()
                ) { category ->
                    if (category == null) {
                        val onAskAiStream: (BrowseStream) -> Unit = { stream ->
                            promptScope.launch {
                                onAskAi(BrowseFormatting.aiPrompt(stream, "", Clock.System.now().toEpochMilliseconds()))
                            }
                        }
                        if (state.isSearching) {
                            DirectoryPane(
                                state = state,
                                categories = categories,
                                gridState = directoryGrid,
                                bottomInset = bottomInset,
                                onEvent = onEvent,
                                onOpenChannel = onOpenChannel,
                                onOpenStream = onOpenStream,
                                onAskAi = onAskAiStream
                            )
                        } else {
                            BrowseTabPager(
                                selected = state.tab,
                                onSelect = { onEvent(BrowseEvent.SelectTab(it)) }
                            ) { tab ->
                                when (tab) {
                                    BrowseTab.FOLLOWING -> PopularStreamsPane(
                                        streams = followedStreams,
                                        filter = state.filter,
                                        bottomInset = bottomInset,
                                        onEvent = onEvent,
                                        onOpenFilters = { filtersOpen = true },
                                        onOpenStream = onOpenStream,
                                        onAskAi = onAskAiStream,
                                        emptyText = if (state.signedIn) stringResource(Res.string.browse_follow_empty) else stringResource(Res.string.browse_follow_sign_in),
                                        emptyAction = false,
                                        failedText = stringResource(Res.string.browse_follow_reconnect)
                                    )
                                    BrowseTab.POPULAR -> PopularStreamsPane(
                                        streams = popularStreams,
                                        filter = state.filter,
                                        bottomInset = bottomInset,
                                        onEvent = onEvent,
                                        onOpenFilters = { filtersOpen = true },
                                        onOpenStream = onOpenStream,
                                        onAskAi = onAskAiStream
                                    )
                                    BrowseTab.CATEGORIES -> DirectoryPane(
                                        state = state,
                                        categories = categories,
                                        gridState = directoryGrid,
                                        bottomInset = bottomInset,
                                        onEvent = onEvent,
                                        onOpenChannel = onOpenChannel,
                                        onOpenStream = onOpenStream,
                                        onAskAi = onAskAiStream
                                    )
                                }
                            }
                        }
                    } else {
                        CategoryStreamsPane(
                            category = category,
                            filter = state.filter,
                            streams = streams,
                            bottomInset = bottomInset,
                            onEvent = onEvent,
                            onOpenFilters = { filtersOpen = true },
                            onOpenStream = onOpenStream,
                            onAskAi = { stream ->
                                promptScope.launch {
                                    onAskAi(BrowseFormatting.aiPrompt(stream, category.name, Clock.System.now().toEpochMilliseconds()))
                                }
                            }
                        )
                    }
                }
            }
            val filtersAvailable = state.category != null ||
                (!state.isSearching && state.tab != BrowseTab.CATEGORIES)
            if (filtersOpen && filtersAvailable) {
                val filterStreams = when {
                    state.category != null -> streams
                    state.tab == BrowseTab.FOLLOWING -> followedStreams
                    else -> popularStreams
                }
                StreamFilterSheet(
                    initial = state.filter,
                    tagSuggestions = remember(filterStreams.itemCount, state.filter.tag) {
                        BrowseViewModel.tagSuggestionsFor(filterStreams.itemSnapshotList.items, state.filter.tag)
                    },
                    onApply = { onEvent(BrowseEvent.ApplyFilter(it)) },
                    onDismiss = { filtersOpen = false }
                )
            }
        }
    }
}

private fun sharedAxisX(forward: Boolean): ContentTransform {
    val direction = if (forward) 1 else -1
    val enter = slideInHorizontally(
        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
    ) { width -> direction * width / 5 } + fadeIn(tween(220, delayMillis = 60, easing = EnterEasing))
    val exit = slideOutHorizontally(tween(200, easing = ExitEasing)) { width -> -direction * width / 5 } +
            fadeOut(tween(120, easing = ExitEasing))
    return enter togetherWith exit
}

@Composable
private fun BrowseTopBar(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ChatoneIconButton(onClick = onBack, modifier = Modifier.size(44.dp)) {
            Icon(
                Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = stringResource(Res.string.browse_back),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
        AnimatedContent(
            targetState = title,
            transitionSpec = { fadeIn(tween(180, easing = EnterEasing)) togetherWith fadeOut(tween(100, easing = ExitEasing)) },
            label = "browseTitle",
            modifier = Modifier.weight(1f).padding(start = 4.dp)
        ) { text ->
            Text(
                text,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun BrowseTabPager(
    selected: BrowseTab,
    onSelect: (BrowseTab) -> Unit,
    page: @Composable (BrowseTab) -> Unit
) {
    val pagerState = rememberPagerState(initialPage = selected.ordinal) { BrowseTabs.size }
    val scope = rememberCoroutineScope()
    val select by rememberUpdatedState(onSelect)
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { index -> select(BrowseTabs[index]) }
    }
    LaunchedEffect(selected) {
        if (pagerState.settledPage != selected.ordinal && !pagerState.isScrollInProgress) {
            pagerState.animateScrollToPage(selected.ordinal)
        }
    }
    Column(Modifier.fillMaxSize()) {
        BrowseTabs(
            selected = BrowseTabs[pagerState.targetPage],
            onSelect = { tab -> scope.launch { pagerState.animateScrollToPage(tab.ordinal) } },
            modifier = Modifier.padding(horizontal = 12.dp).padding(bottom = 4.dp)
        )
        HorizontalPager(
            state = pagerState,
            beyondViewportPageCount = 1,
            key = { BrowseTabs[it].name },
            modifier = Modifier.fillMaxSize()
        ) { index -> page(BrowseTabs[index]) }
    }
}

@Composable
private fun BrowseTabs(
    selected: BrowseTab,
    onSelect: (BrowseTab) -> Unit,
    modifier: Modifier = Modifier
) {
    ExpressiveTabs(
        tabs = BrowseTabs,
        selected = selected,
        onSelect = onSelect,
        label = { tab ->
            when (tab) {
                BrowseTab.FOLLOWING -> stringResource(Res.string.browse_tab_following)
                BrowseTab.POPULAR -> stringResource(Res.string.browse_tab_popular)
                BrowseTab.CATEGORIES -> stringResource(Res.string.browse_tab_categories)
            }
        },
        modifier = modifier.fillMaxWidth()
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DirectoryPane(
    state: BrowseState,
    categories: LazyPagingItems<BrowseCategory>,
    gridState: LazyGridState,
    bottomInset: Dp,
    onEvent: (BrowseEvent) -> Unit,
    onOpenChannel: (BrowseChannel) -> Unit,
    onOpenStream: (BrowseStream) -> Unit,
    onAskAi: (BrowseStream) -> Unit
) {
    val refreshing = if (state.isSearching) {
        state.searchLoading && state.searchResult != null
    } else {
        categories.isRefreshingContent
    }
    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = { if (state.isSearching) onEvent(BrowseEvent.RetrySearch) else categories.refresh() },
        modifier = Modifier.fillMaxSize()
    ) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(CategoryColumnMinWidth),
            state = gridState,
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 16.dp + bottomInset),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            if (state.isSearching) {
                searchResults(state, onEvent, onOpenChannel, onOpenStream, onAskAi)
            } else {
                topCategories(categories, onEvent)
            }
        }
        ScrollToTopButton(gridState, bottomInset)
    }
}

private fun LazyGridScope.topCategories(
    categories: LazyPagingItems<BrowseCategory>,
    onEvent: (BrowseEvent) -> Unit
) {
    fullSpanItem("popular_header") { BrowseSectionHeader(stringResource(Res.string.browse_popular_categories)) }
    when (categories.content) {
        PagedContent.Loading -> items(
            count = CATEGORY_PLACEHOLDERS,
            key = { "category_placeholder_$it" },
            contentType = { "placeholder" }
        ) { PlaceholderBlock(aspectRatio = 3f / 4f) }

        PagedContent.Failed -> fullSpanItem("categories_error") {
            BrowseMessage(stringResource(Res.string.browse_load_failed), stringResource(Res.string.browse_retry), categories::retry)
        }

        PagedContent.Empty -> fullSpanItem("categories_empty") {
            BrowseMessage(stringResource(Res.string.browse_nothing_found), stringResource(Res.string.browse_retry), categories::refresh)
        }

        PagedContent.Items -> {
            items(
                count = categories.itemCount,
                key = categories.itemKey { "category_${it.id}" },
                contentType = categories.itemContentType { "category" }
            ) { index ->
                val category = categories[index] ?: return@items
                CategoryCard(
                    category = category,
                    onClick = { onEvent(BrowseEvent.OpenCategory(category)) },
                    modifier = Modifier.animateItem()
                )
            }
            pagingFooter(categories, "categories")
        }
    }
}

private fun LazyGridScope.searchResults(
    state: BrowseState,
    onEvent: (BrowseEvent) -> Unit,
    onOpenChannel: (BrowseChannel) -> Unit,
    onOpenStream: (BrowseStream) -> Unit,
    onAskAi: (BrowseStream) -> Unit
) {
    val result = state.searchResult
    val searchNowMs = Clock.System.now().toEpochMilliseconds()
    when {
        state.searchFailed -> fullSpanItem("search_error") {
            BrowseMessage(stringResource(Res.string.browse_load_failed), stringResource(Res.string.browse_retry), { onEvent(BrowseEvent.RetrySearch) })
        }

        result == null -> fullSpanItem("search_loading") { LoadingFooter() }

        result.isEmpty -> fullSpanItem("search_empty") {
            BrowseMessage(stringResource(Res.string.browse_nothing_found), null, null)
        }

        else -> {
            if (result.channels.isNotEmpty()) {
                fullSpanItem("channels_header") { BrowseSectionHeader(stringResource(Res.string.browse_channels_header)) }
                result.channels.forEach { channel ->
                    fullSpanItem("channel_${channel.login}") {
                        ChannelResultRow(
                            channel = channel,
                            onClick = { onOpenChannel(channel) },
                            modifier = Modifier.animateItem()
                        )
                    }
                }
            }
            if (result.streams.isNotEmpty()) {
                fullSpanItem("streams_header") { BrowseSectionHeader(stringResource(Res.string.browse_streams_header)) }
                result.streams.forEach { stream ->
                    fullSpanItem("search_stream_${stream.login}") {
                        StreamCard(
                            stream = stream,
                            nowMs = searchNowMs,
                            onClick = { onOpenStream(stream) },
                            onAskAi = { onAskAi(stream) },
                            modifier = Modifier.animateItem()
                        )
                    }
                }
            }
            if (result.categories.isNotEmpty()) {
                fullSpanItem("search_categories_header") { BrowseSectionHeader(stringResource(Res.string.browse_categories_header)) }
                items(result.categories, key = { "search_category_${it.id}" }, contentType = { "category" }) { category ->
                    CategoryCard(
                        category = category,
                        onClick = { onEvent(BrowseEvent.OpenCategory(category)) },
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }
    }
}
