package io.rudione.chatone.presentation.browse

import androidx.compose.animation.AnimatedContent
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
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.graphics.Color
import io.rudione.chatone.presentation.components.ChatoneIconButton
import io.rudione.chatone.presentation.components.SystemBackHandler
import io.rudione.chatone.presentation.theme.i18n.BrowseStrings
import io.rudione.chatone.presentation.theme.i18n.LocalStrings
import kotlin.time.Clock
import org.koin.compose.viewmodel.koinViewModel

private val CategoryColumnMinWidth = 112.dp
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
    val strings = LocalStrings.current.explore
    val onEvent: (BrowseEvent) -> Unit = remember(viewModel) { { viewModel.sendEvent(it) } }
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    var filtersOpen by rememberSaveable { mutableStateOf(false) }

    val goBack: () -> Unit = {
        if (viewModel.state.value.category != null) onEvent(BrowseEvent.CloseCategory) else onClose()
    }
    SystemBackHandler(onBack = goBack)

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
        ) {
            BrowseTopBar(
                title = state.category?.name ?: strings.title,
                strings = strings,
                onBack = goBack
            )
            AnimatedContent(
                targetState = state.category,
                contentKey = { it?.id },
                transitionSpec = { sharedAxisX(forward = targetState != null) },
                label = "browsePane",
                modifier = Modifier.fillMaxSize()
            ) { category ->
                if (category == null) {
                    val onAskAiStream: (BrowseStream) -> Unit = { stream ->
                        onAskAi(
                            BrowseFormatting.aiPrompt(
                                stream = stream,
                                category = "",
                                strings = strings,
                                nowMs = Clock.System.now().toEpochMilliseconds()
                            )
                        )
                    }
                    when {
                        state.isSearching -> DirectoryPane(
                            state = state,
                            categories = categories,
                            gridState = directoryGrid,
                            strings = strings,
                            bottomInset = bottomInset,
                            onEvent = onEvent,
                            onOpenChannel = onOpenChannel
                        )

                        state.tab == BrowseTab.FOLLOWING -> PopularStreamsPane(
                            streams = followedStreams,
                            filter = state.filter,
                            strings = strings,
                            bottomInset = bottomInset,
                            header = { StreamsHeader(state, strings, onEvent) },
                            onEvent = onEvent,
                            onOpenFilters = { filtersOpen = true },
                            onOpenStream = onOpenStream,
                            onAskAi = onAskAiStream,
                            emptyText = if (state.signedIn) strings.followEmpty else strings.followSignIn,
                            emptyAction = false,
                            failedText = strings.followReconnect
                        )

                        state.tab == BrowseTab.POPULAR -> PopularStreamsPane(
                            streams = popularStreams,
                            filter = state.filter,
                            strings = strings,
                            bottomInset = bottomInset,
                            header = { StreamsHeader(state, strings, onEvent) },
                            onEvent = onEvent,
                            onOpenFilters = { filtersOpen = true },
                            onOpenStream = onOpenStream,
                            onAskAi = onAskAiStream
                        )

                        else -> DirectoryPane(
                            state = state,
                            categories = categories,
                            gridState = directoryGrid,
                            strings = strings,
                            bottomInset = bottomInset,
                            onEvent = onEvent,
                            onOpenChannel = onOpenChannel
                        )
                    }
                } else {
                    CategoryStreamsPane(
                        category = category,
                        filter = state.filter,
                        streams = streams,
                        strings = strings,
                        bottomInset = bottomInset,
                        onEvent = onEvent,
                        onOpenFilters = { filtersOpen = true },
                        onOpenStream = onOpenStream,
                        onAskAi = { stream ->
                            onAskAi(
                                BrowseFormatting.aiPrompt(
                                    stream = stream,
                                    category = category.name,
                                    strings = strings,
                                    nowMs = Clock.System.now().toEpochMilliseconds()
                                )
                            )
                        }
                    )
                }
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
            strings = strings,
            onApply = { onEvent(BrowseEvent.ApplyFilter(it)) },
            onDismiss = { filtersOpen = false }
        )
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
private fun BrowseTopBar(title: String, strings: BrowseStrings, onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ChatoneIconButton(onClick = onBack, modifier = Modifier.size(44.dp)) {
            Icon(
                Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = strings.back,
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
private fun StreamsHeader(
    state: BrowseState,
    strings: BrowseStrings,
    onEvent: (BrowseEvent) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        BrowseSearchField(
            query = state.query,
            strings = strings,
            onQueryChange = { onEvent(BrowseEvent.QueryChanged(it)) }
        )
        BrowseTabs(state.tab, strings, onSelect = { onEvent(BrowseEvent.SelectTab(it)) })
    }
}

@Composable
private fun BrowseTabs(
    selected: BrowseTab,
    strings: BrowseStrings,
    onSelect: (BrowseTab) -> Unit
) {
    val tabs = remember(strings) {
        listOf(
            BrowseTab.FOLLOWING to strings.tabFollowing,
            BrowseTab.POPULAR to strings.tabPopular,
            BrowseTab.CATEGORIES to strings.tabCategories
        )
    }
    val selectedIndex = tabs.indexOfFirst { it.first == selected }.coerceAtLeast(0)
    PrimaryTabRow(
        selectedTabIndex = selectedIndex,
        containerColor = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        tabs.forEach { (tab, label) ->
            Tab(
                selected = tab == selected,
                onClick = { onSelect(tab) },
                text = {
                    Text(
                        label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleSmall
                    )
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DirectoryPane(
    state: BrowseState,
    categories: LazyPagingItems<BrowseCategory>,
    gridState: LazyGridState,
    strings: BrowseStrings,
    bottomInset: Dp,
    onEvent: (BrowseEvent) -> Unit,
    onOpenChannel: (BrowseChannel) -> Unit
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
            fullSpanItem("search") {
                BrowseSearchField(
                    query = state.query,
                    strings = strings,
                    onQueryChange = { onEvent(BrowseEvent.QueryChanged(it)) }
                )
            }
            if (state.isSearching) {
                searchResults(state, strings, onEvent, onOpenChannel)
            } else {
                fullSpanItem("tabs") {
                    BrowseTabs(state.tab, strings, onSelect = { onEvent(BrowseEvent.SelectTab(it)) })
                }
                topCategories(categories, strings, onEvent)
            }
        }
    }
}

private fun LazyGridScope.topCategories(
    categories: LazyPagingItems<BrowseCategory>,
    strings: BrowseStrings,
    onEvent: (BrowseEvent) -> Unit
) {
    fullSpanItem("popular_header") { BrowseSectionHeader(strings.popularCategories) }
    when (categories.content) {
        PagedContent.Loading -> items(
            count = CATEGORY_PLACEHOLDERS,
            key = { "category_placeholder_$it" },
            contentType = { "placeholder" }
        ) { PlaceholderBlock(aspectRatio = 3f / 4f) }

        PagedContent.Failed -> fullSpanItem("categories_error") {
            BrowseMessage(strings.loadFailed, strings.retry, categories::retry)
        }

        PagedContent.Empty -> fullSpanItem("categories_empty") {
            BrowseMessage(strings.nothingFound, strings.retry, categories::refresh)
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
                    strings = strings,
                    onClick = { onEvent(BrowseEvent.OpenCategory(category)) },
                    modifier = Modifier.animateItem()
                )
            }
            pagingFooter(categories, strings, "categories")
        }
    }
}

private fun LazyGridScope.searchResults(
    state: BrowseState,
    strings: BrowseStrings,
    onEvent: (BrowseEvent) -> Unit,
    onOpenChannel: (BrowseChannel) -> Unit
) {
    val result = state.searchResult
    when {
        state.searchFailed -> fullSpanItem("search_error") {
            BrowseMessage(strings.loadFailed, strings.retry, { onEvent(BrowseEvent.RetrySearch) })
        }

        result == null -> fullSpanItem("search_loading") { LoadingFooter() }

        result.channels.isEmpty() && result.categories.isEmpty() -> fullSpanItem("search_empty") {
            BrowseMessage(strings.nothingFound, null, null)
        }

        else -> {
            if (result.channels.isNotEmpty()) {
                fullSpanItem("channels_header") { BrowseSectionHeader(strings.channelsHeader) }
                result.channels.forEach { channel ->
                    fullSpanItem("channel_${channel.login}") {
                        ChannelResultRow(
                            channel = channel,
                            strings = strings,
                            onClick = { onOpenChannel(channel) },
                            modifier = Modifier.animateItem()
                        )
                    }
                }
            }
            if (result.categories.isNotEmpty()) {
                fullSpanItem("search_categories_header") { BrowseSectionHeader(strings.categoriesHeader) }
                items(result.categories, key = { "search_category_${it.id}" }, contentType = { "category" }) { category ->
                    CategoryCard(
                        category = category,
                        strings = strings,
                        onClick = { onEvent(BrowseEvent.OpenCategory(category)) },
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }
    }
}
