package io.rudione.chatone.tv.player

import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import androidx.tv.material3.Icon
import androidx.tv.material3.ListItem
import androidx.tv.material3.ListItemDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Switch
import androidx.tv.material3.Text
import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.browse_tab_following
import chatone.composeapp.generated.resources.player_audio_only
import chatone.composeapp.generated.resources.player_low_latency
import chatone.composeapp.generated.resources.player_low_latency_description
import chatone.composeapp.generated.resources.player_quality
import chatone.composeapp.generated.resources.player_quality_auto
import chatone.composeapp.generated.resources.player_quality_auto_active
import chatone.composeapp.generated.resources.tv_follows_sign_in_title
import chatone.composeapp.generated.resources.tv_load_failed
import chatone.composeapp.generated.resources.tv_no_follows_live
import coil3.compose.AsyncImage
import io.rudione.chatone.domain.browse.BrowseStream
import io.rudione.chatone.domain.stream.StreamQualitySelection
import io.rudione.chatone.domain.stream.StreamVariant
import io.rudione.chatone.icons.lucide.Check
import io.rudione.chatone.icons.lucide.Lucide
import io.rudione.chatone.presentation.browse.BrowseFormatting
import io.rudione.chatone.presentation.stream.StreamPlayerUiState
import io.rudione.chatone.tv.components.claim
import io.rudione.chatone.tv.components.tvFocusTrap
import io.rudione.chatone.tv.theme.TvDimens
import org.jetbrains.compose.resources.stringResource

@Composable
private fun TvSidePanel(title: String, modifier: Modifier, content: @Composable () -> Unit) {
    Column(
        modifier
            .fillMaxHeight()
            .width(TvDimens.ChannelPanelWidth)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.96f))
            .padding(vertical = TvDimens.ScreenVertical, horizontal = 12.dp)
            .tvFocusTrap()
            .focusGroup(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        content()
    }
}

@Composable
internal fun TvChannelsPanel(
    streams: LazyPagingItems<BrowseStream>,
    currentLogin: String,
    needsSignIn: Boolean,
    onSelect: (BrowseStream) -> Unit,
    modifier: Modifier = Modifier
) {
    val focus = remember { FocusRequester() }
    val failed = streams.loadState.refresh is LoadState.Error
    val focusIndex = streams.itemSnapshotList.items
        .indexOfFirst { it.login == currentLogin }
        .coerceAtLeast(0)
    TvSidePanel(stringResource(Res.string.browse_tab_following), modifier) {
        if (streams.itemCount == 0) {
            Text(
                stringResource(
                    when {
                        needsSignIn -> Res.string.tv_follows_sign_in_title
                        failed -> Res.string.tv_load_failed
                        else -> Res.string.tv_no_follows_live
                    }
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(8.dp)
                    .focusRequester(focus)
                    .focusable()
            )
            return@TvSidePanel
        }
        LazyColumn(
            contentPadding = PaddingValues(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(
                count = streams.itemCount,
                key = streams.itemKey { it.id },
                contentType = { "channel" }) { index ->
                streams[index]?.let { stream ->
                    ListItem(
                        selected = stream.login == currentLogin,
                        onClick = { onSelect(stream) },
                        modifier = if (index == focusIndex) Modifier.focusRequester(focus) else Modifier,
                        headlineContent = {
                            Text(
                                stream.displayName.ifBlank { stream.login },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        supportingContent = {
                            Text(stream.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        },
                        leadingContent = {
                            AsyncImage(
                                model = stream.avatarUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(ListItemDefaults.IconSize)
                                    .clip(CircleShape)
                            )
                        },
                        trailingContent = { Text(BrowseFormatting.compact(stream.viewers)) }
                    )
                }
            }
        }
    }
    LaunchedEffect(streams.itemCount > 0) { focus.claim() }
}

@Composable
internal fun TvQualityPanel(
    state: StreamPlayerUiState,
    onSelect: (StreamQualitySelection) -> Unit,
    onLowLatency: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val focus = remember { FocusRequester() }
    val active = state.activeVariant?.displayName.orEmpty()
    val video = remember(state.variants) { state.variants.filterNot(StreamVariant::isAudioOnly) }
    val hasAudioOnly = remember(state.variants) { state.variants.any(StreamVariant::isAudioOnly) }
    TvSidePanel(stringResource(Res.string.player_quality), modifier) {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            item(key = "auto") {
                val auto = state.selection == StreamQualitySelection.Auto
                TvOptionItem(
                    title = if (auto && active.isNotEmpty()) {
                        stringResource(Res.string.player_quality_auto_active, active)
                    } else {
                        stringResource(Res.string.player_quality_auto)
                    },
                    selected = auto,
                    onClick = { onSelect(StreamQualitySelection.Auto) },
                    modifier = if (auto) Modifier.focusRequester(focus) else Modifier
                )
            }
            items(video, key = { it.groupId }) { variant ->
                val selection = StreamQualitySelection.Fixed(variant.groupId)
                val selected = state.selection == selection
                TvOptionItem(
                    title = variant.displayName,
                    selected = selected,
                    onClick = { onSelect(selection) },
                    modifier = if (selected) Modifier.focusRequester(focus) else Modifier
                )
            }
            if (hasAudioOnly) {
                item(key = "audio") {
                    val selected = state.selection == StreamQualitySelection.AudioOnly
                    TvOptionItem(
                        title = stringResource(Res.string.player_audio_only),
                        selected = selected,
                        onClick = { onSelect(StreamQualitySelection.AudioOnly) },
                        modifier = if (selected) Modifier.focusRequester(focus) else Modifier
                    )
                }
            }
            item(key = "latency") {
                ListItem(
                    selected = false,
                    onClick = { onLowLatency(!state.preferences.lowLatency) },
                    headlineContent = { Text(stringResource(Res.string.player_low_latency)) },
                    supportingContent = {
                        Text(
                            stringResource(Res.string.player_low_latency_description),
                            maxLines = 2
                        )
                    },
                    trailingContent = {
                        Switch(
                            checked = state.preferences.lowLatency,
                            onCheckedChange = null
                        )
                    }
                )
            }
        }
    }
    LaunchedEffect(Unit) { focus.claim() }
}

@Composable
private fun TvOptionItem(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ListItem(
        selected = selected,
        onClick = onClick,
        modifier = modifier,
        headlineContent = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        trailingContent = if (selected) {
            {
                Icon(
                    Lucide.Check,
                    contentDescription = null,
                    modifier = Modifier.size(ListItemDefaults.IconSize)
                )
            }
        } else {
            null
        }
    )
}
