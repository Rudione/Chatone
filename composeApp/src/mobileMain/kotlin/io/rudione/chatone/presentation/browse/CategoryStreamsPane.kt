package io.rudione.chatone.presentation.browse

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import io.rudione.chatone.icons.material.Icons
import io.rudione.chatone.icons.material.rounded.Close
import io.rudione.chatone.icons.material.rounded.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import coil3.compose.AsyncImage
import io.rudione.chatone.domain.browse.BrowseCategory
import io.rudione.chatone.domain.browse.BrowseStream
import io.rudione.chatone.domain.browse.StreamFilter
import io.rudione.chatone.domain.browse.StreamSort
import io.rudione.chatone.presentation.components.ChatoneChip
import io.rudione.chatone.presentation.theme.i18n.BrowseStrings
import kotlinx.coroutines.delay
import kotlin.time.Clock

private val StreamColumnMinWidth = 300.dp
private const val STREAM_PLACEHOLDERS = 6
private const val UPTIME_TICK_MS = 60_000L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CategoryStreamsPane(
    category: BrowseCategory,
    filter: StreamFilter,
    streams: LazyPagingItems<BrowseStream>,
    strings: BrowseStrings,
    bottomInset: Dp,
    onEvent: (BrowseEvent) -> Unit,
    onOpenFilters: () -> Unit,
    onOpenStream: (BrowseStream) -> Unit,
    onAskAi: (BrowseStream) -> Unit,
    modifier: Modifier = Modifier
) {
    val gridState = rememberLazyGridState()
    val nowMs by produceState(Clock.System.now().toEpochMilliseconds()) {
        while (true) {
            delay(UPTIME_TICK_MS)
            value = Clock.System.now().toEpochMilliseconds()
        }
    }
    LaunchedEffect(filter) {
        if (gridState.firstVisibleItemIndex > 0) gridState.scrollToItem(0)
    }
    PullToRefreshBox(
        isRefreshing = streams.isRefreshingContent,
        onRefresh = streams::refresh,
        modifier = modifier.fillMaxSize()
    ) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(StreamColumnMinWidth),
            state = gridState,
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 16.dp + bottomInset),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            fullSpanItem("hero") { CategoryHero(category, strings) }
            fullSpanItem("filters") {
                ActiveFiltersBar(filter, strings, onOpenFilters, onChange = { onEvent(BrowseEvent.ApplyFilter(it)) })
            }
            when (streams.content) {
                PagedContent.Loading -> items(
                    count = STREAM_PLACEHOLDERS,
                    key = { "stream_placeholder_$it" },
                    contentType = { "placeholder" }
                ) { PlaceholderBlock(aspectRatio = 16f / 9f) }

                PagedContent.Failed -> fullSpanItem("streams_error") {
                    BrowseMessage(strings.loadFailed, strings.retry, streams::retry)
                }

                PagedContent.Empty -> fullSpanItem("streams_empty") {
                    BrowseMessage(
                        text = strings.noStreams,
                        actionLabel = strings.resetFilters.takeUnless { filter.isDefault },
                        onAction = { onEvent(BrowseEvent.ResetFilters) }
                    )
                }

                PagedContent.Items -> {
                    items(
                        count = streams.itemCount,
                        key = streams.itemKey { it.login },
                        contentType = streams.itemContentType { "stream" }
                    ) { index ->
                        val stream = streams[index] ?: return@items
                        StreamCard(
                            stream = stream,
                            strings = strings,
                            nowMs = nowMs,
                            onClick = { onOpenStream(stream) },
                            onAskAi = { onAskAi(stream) },
                            modifier = Modifier.animateItem()
                        )
                    }
                    pagingFooter(streams, strings, "streams")
                }
            }
        }
    }
}

@Composable
private fun CategoryHero(category: BrowseCategory, strings: BrowseStrings) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(width = 60.dp, height = 80.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
        ) {
            AsyncImage(
                model = category.boxArtUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                category.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                listOf(
                    BrowseFormatting.viewers(category.viewers, strings),
                    BrowseFormatting.channels(category.broadcasters, strings)
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PopularStreamsPane(
    streams: LazyPagingItems<BrowseStream>,
    filter: StreamFilter,
    strings: BrowseStrings,
    bottomInset: Dp,
    header: @Composable () -> Unit,
    onEvent: (BrowseEvent) -> Unit,
    onOpenFilters: () -> Unit,
    onOpenStream: (BrowseStream) -> Unit,
    onAskAi: (BrowseStream) -> Unit,
    modifier: Modifier = Modifier,
    emptyText: String? = null,
    emptyAction: Boolean = true,
    failedText: String? = null
) {
    val gridState = rememberLazyGridState()
    val nowMs by produceState(Clock.System.now().toEpochMilliseconds()) {
        while (true) {
            delay(UPTIME_TICK_MS)
            value = Clock.System.now().toEpochMilliseconds()
        }
    }
    LaunchedEffect(filter) {
        if (gridState.firstVisibleItemIndex > 0) gridState.scrollToItem(0)
    }
    PullToRefreshBox(
        isRefreshing = streams.isRefreshingContent,
        onRefresh = streams::refresh,
        modifier = modifier.fillMaxSize()
    ) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(StreamColumnMinWidth),
            state = gridState,
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 16.dp + bottomInset),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            fullSpanItem("popular_header") { header() }
            fullSpanItem("popular_filters") {
                ActiveFiltersBar(filter, strings, onOpenFilters, onChange = { onEvent(BrowseEvent.ApplyFilter(it)) })
            }
            when (streams.content) {
                PagedContent.Loading -> items(
                    count = STREAM_PLACEHOLDERS,
                    key = { "popular_placeholder_$it" },
                    contentType = { "placeholder" }
                ) { PlaceholderBlock(aspectRatio = 16f / 9f) }

                PagedContent.Failed -> fullSpanItem("popular_error") {
                    BrowseMessage(failedText ?: strings.loadFailed, strings.retry, streams::retry)
                }

                PagedContent.Empty -> fullSpanItem("popular_empty") {
                    BrowseMessage(
                        text = emptyText ?: strings.noStreams,
                        actionLabel = if (emptyAction) strings.resetFilters.takeUnless { filter.isDefault } else null,
                        onAction = { onEvent(BrowseEvent.ResetFilters) }
                    )
                }

                PagedContent.Items -> {
                    items(
                        count = streams.itemCount,
                        key = streams.itemKey { it.login },
                        contentType = streams.itemContentType { "stream" }
                    ) { index ->
                        val stream = streams[index] ?: return@items
                        StreamCard(
                            stream = stream,
                            strings = strings,
                            nowMs = nowMs,
                            onClick = { onOpenStream(stream) },
                            onAskAi = { onAskAi(stream) },
                            modifier = Modifier.animateItem()
                        )
                    }
                    pagingFooter(streams, strings, "popular")
                }
            }
        }
    }
}

@Composable
internal fun ActiveFiltersBar(
    filter: StreamFilter,
    strings: BrowseStrings,
    onOpenFilters: () -> Unit,
    onChange: (StreamFilter) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val count = filter.activeCount
        ChatoneChip(
            label = if (count > 0) "${strings.filters} · $count" else strings.filters,
            selected = count > 0,
            leadingIcon = Icons.Rounded.Tune,
            onClick = onOpenFilters
        )
        if (filter.sort != StreamSort.VIEWERS_HIGH) {
            RemovableChip(sortLabel(filter.sort, strings)) { onChange(filter.copy(sort = StreamSort.VIEWERS_HIGH)) }
        }
        filter.language?.let { language ->
            RemovableChip(language) { onChange(filter.copy(language = null)) }
        }
        filter.tag?.let { tag ->
            RemovableChip(tag) { onChange(filter.copy(tag = null)) }
        }
        if (filter.minViewers > 0) {
            RemovableChip("≥ " + BrowseFormatting.viewers(filter.minViewers, strings)) {
                onChange(filter.copy(minViewers = 0))
            }
        }
        if (filter.hideMature) {
            RemovableChip(strings.hideMature) { onChange(filter.copy(hideMature = false)) }
        }
    }
}

@Composable
private fun RemovableChip(label: String, onRemove: () -> Unit) {
    ChatoneChip(
        label = label,
        selected = true,
        accent = MaterialTheme.colorScheme.tertiary,
        leadingIcon = Icons.Rounded.Close,
        onClick = onRemove
    )
}

internal fun sortLabel(sort: StreamSort, strings: BrowseStrings): String = when (sort) {
    StreamSort.VIEWERS_HIGH -> strings.sortViewersHigh
    StreamSort.VIEWERS_LOW -> strings.sortViewersLow
    StreamSort.RECENT -> strings.sortRecent
}
