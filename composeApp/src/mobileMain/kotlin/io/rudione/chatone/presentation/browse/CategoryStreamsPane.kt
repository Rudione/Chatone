package io.rudione.chatone.presentation.browse

import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.browse_filters
import chatone.composeapp.generated.resources.browse_hide_mature
import chatone.composeapp.generated.resources.browse_load_failed
import chatone.composeapp.generated.resources.browse_no_streams
import chatone.composeapp.generated.resources.browse_reset_filters
import chatone.composeapp.generated.resources.browse_retry
import chatone.composeapp.generated.resources.browse_sort_recent
import chatone.composeapp.generated.resources.browse_sort_viewers_high
import chatone.composeapp.generated.resources.browse_sort_viewers_low
import org.jetbrains.compose.resources.stringResource
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
    ResetScrollOnChange(gridState, filter)
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
            fullSpanItem("hero") { CategoryHero(category) }
            fullSpanItem("filters") {
                ActiveFiltersBar(filter, onOpenFilters, onChange = { onEvent(BrowseEvent.ApplyFilter(it)) })
            }
            when (streams.content) {
                PagedContent.Loading -> items(
                    count = STREAM_PLACEHOLDERS,
                    key = { "stream_placeholder_$it" },
                    contentType = { "placeholder" }
                ) { PlaceholderBlock(aspectRatio = 16f / 9f) }

                PagedContent.Failed -> fullSpanItem("streams_error") {
                    BrowseMessage(stringResource(Res.string.browse_load_failed), stringResource(Res.string.browse_retry), streams::retry)
                }

                PagedContent.Empty -> fullSpanItem("streams_empty") {
                    BrowseMessage(
                        text = stringResource(Res.string.browse_no_streams),
                        actionLabel = stringResource(Res.string.browse_reset_filters).takeUnless { filter.isDefault },
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
                            nowMs = nowMs,
                            onClick = { onOpenStream(stream) },
                            onAskAi = { onAskAi(stream) },
                            modifier = Modifier.animateItem()
                        )
                    }
                    pagingFooter(streams, "streams")
                }
            }
        }
        ScrollToTopButton(gridState, bottomInset)
    }
}

@Composable
private fun CategoryHero(category: BrowseCategory) {
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
                    BrowseFormatting.viewers(category.viewers),
                    BrowseFormatting.channels(category.broadcasters)
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
    bottomInset: Dp,
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
    ResetScrollOnChange(gridState, filter)
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
            fullSpanItem("popular_filters") {
                ActiveFiltersBar(filter, onOpenFilters, onChange = { onEvent(BrowseEvent.ApplyFilter(it)) })
            }
            when (streams.content) {
                PagedContent.Loading -> items(
                    count = STREAM_PLACEHOLDERS,
                    key = { "popular_placeholder_$it" },
                    contentType = { "placeholder" }
                ) { PlaceholderBlock(aspectRatio = 16f / 9f) }

                PagedContent.Failed -> fullSpanItem("popular_error") {
                    BrowseMessage(failedText ?: stringResource(Res.string.browse_load_failed), stringResource(Res.string.browse_retry), streams::retry)
                }

                PagedContent.Empty -> fullSpanItem("popular_empty") {
                    BrowseMessage(
                        text = emptyText ?: stringResource(Res.string.browse_no_streams),
                        actionLabel = if (emptyAction) stringResource(Res.string.browse_reset_filters).takeUnless { filter.isDefault } else null,
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
                            nowMs = nowMs,
                            onClick = { onOpenStream(stream) },
                            onAskAi = { onAskAi(stream) },
                            modifier = Modifier.animateItem()
                        )
                    }
                    pagingFooter(streams, "popular")
                }
            }
        }
        ScrollToTopButton(gridState, bottomInset)
    }
}

@Composable
internal fun ActiveFiltersBar(
    filter: StreamFilter,
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
            label = if (count > 0) "${stringResource(Res.string.browse_filters)} · $count" else stringResource(Res.string.browse_filters),
            selected = count > 0,
            leadingIcon = Icons.Rounded.Tune,
            onClick = onOpenFilters
        )
        if (filter.sort != StreamSort.VIEWERS_HIGH) {
            RemovableChip(sortLabel(filter.sort)) { onChange(filter.copy(sort = StreamSort.VIEWERS_HIGH)) }
        }
        filter.language?.let { language ->
            RemovableChip(language) { onChange(filter.copy(language = null)) }
        }
        filter.tag?.let { tag ->
            RemovableChip(tag) { onChange(filter.copy(tag = null)) }
        }
        if (filter.minViewers > 0) {
            RemovableChip("≥ " + BrowseFormatting.viewers(filter.minViewers)) {
                onChange(filter.copy(minViewers = 0))
            }
        }
        if (filter.hideMature) {
            RemovableChip(stringResource(Res.string.browse_hide_mature)) { onChange(filter.copy(hideMature = false)) }
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

@Composable
internal fun sortLabel(sort: StreamSort): String = when (sort) {
    StreamSort.VIEWERS_HIGH -> stringResource(Res.string.browse_sort_viewers_high)
    StreamSort.VIEWERS_LOW -> stringResource(Res.string.browse_sort_viewers_low)
    StreamSort.RECENT -> stringResource(Res.string.browse_sort_recent)
}
