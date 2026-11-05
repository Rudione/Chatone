package io.rudione.chatone.presentation.profile

import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.channel_page_featured_only
import chatone.composeapp.generated.resources.channel_page_filters
import chatone.composeapp.generated.resources.channel_page_load_failed
import chatone.composeapp.generated.resources.channel_page_no_clips
import chatone.composeapp.generated.resources.channel_page_no_videos
import chatone.composeapp.generated.resources.channel_page_retry
import org.jetbrains.compose.resources.stringResource
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.rudione.chatone.icons.lucide.Lucide
import io.rudione.chatone.icons.lucide.SlidersHorizontal
import io.rudione.chatone.presentation.components.expressive.pressSquish

internal fun LazyGridScope.clipsSection(
    state: ChannelProfileState,
    onOpenFilters: () -> Unit,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
    onOpenClip: (String) -> Unit
) {
    val s = state.clipQuery
    span("clips_bar") {
        val summary = listOfNotNull(
            clipPeriodChoices().first { it.value == s.period }.label,
            clipOrderChoices().first { it.value == s.order }.label,
            stringResource(Res.string.channel_page_featured_only).takeIf { s.featuredOnly }
        ).joinToString(" · ")
        FilterBar(summary, s.activeCount(), onOpenFilters)
    }
    feedItems(
        feed = state.clips,
        keyPrefix = "clip",
        key = { it.slug },
        emptyText = { stringResource(Res.string.channel_page_no_clips) },
        onLoadMore = onLoadMore,
        onRetry = onRetry
    ) { clip -> ClipCard(clip, onClick = { onOpenClip(clip.slug) }) }
}

internal fun LazyGridScope.videosSection(
    state: ChannelProfileState,
    onOpenFilters: () -> Unit,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
    onOpenVideo: (String) -> Unit
) {
    val q = state.videoQuery
    span("videos_bar") {
        val summary = listOf(
            videoKindChoices().first { it.value == q.kind }.label,
            videoOrderChoices().first { it.value == q.order }.label
        ).joinToString(" · ")
        FilterBar(summary, q.activeCount(), onOpenFilters)
    }
    feedItems(
        feed = state.videos,
        keyPrefix = "video",
        key = { it.id },
        emptyText = { stringResource(Res.string.channel_page_no_videos) },
        onLoadMore = onLoadMore,
        onRetry = onRetry
    ) { video -> VideoCard(video, onClick = { onOpenVideo(video.id) }) }
}

private fun <T> LazyGridScope.feedItems(
    feed: MediaFeed<T>,
    keyPrefix: String,
    key: (T) -> String,
    emptyText: @Composable () -> String,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
    card: @Composable (T) -> Unit
) {
    when {
        !feed.loaded && feed.loading -> span("${keyPrefix}_loading") { LoadingBlock() }
        !feed.loaded && feed.failed -> span("${keyPrefix}_failed") { RetryBlock(onRetry) }
        feed.loaded && feed.items.isEmpty() -> span("${keyPrefix}_empty") { EmptyNote(emptyText()) }
        else -> {
            items(feed.items, key = { "${keyPrefix}_${key(it)}" }, contentType = { keyPrefix }) { item ->
                Box(Modifier.animateItem().followsProfileTabSwipe(LocalProfileTabSwipe.current)) { card(item) }
            }
            if (feed.cursor != null || feed.loading || feed.failed) {
                span("${keyPrefix}_more") {
                    if (feed.failed) RetryBlock(onLoadMore) else {
                        LaunchedEffect(feed.items.size) { onLoadMore() }
                        LoadingBlock()
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterBar(summary: String, activeCount: Int, onOpenFilters: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            summary.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Row(
            modifier = Modifier.pressSquish(interaction).clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .clickable(interactionSource = interaction, indication = null, onClick = onOpenFilters)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(Lucide.SlidersHorizontal, contentDescription = null, modifier = Modifier.size(16.dp))
            Text(stringResource(Res.string.channel_page_filters), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            AnimatedVisibility(visible = activeCount > 0, enter = scaleIn(), exit = scaleOut()) {
                Box(
                    Modifier.size(18.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(activeCount.toString(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
    }
}

@Composable
private fun LoadingBlock() {
    Box(Modifier.fillMaxWidth().height(88.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
    }
}

@Composable
private fun RetryBlock(onRetry: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(stringResource(Res.string.channel_page_load_failed), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        TextButton(onClick = onRetry) { Text(stringResource(Res.string.channel_page_retry)) }
    }
}
