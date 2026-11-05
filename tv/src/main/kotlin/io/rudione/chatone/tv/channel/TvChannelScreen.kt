package io.rudione.chatone.tv.channel

import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.tv_channel_clips
import chatone.composeapp.generated.resources.tv_channel_followers
import chatone.composeapp.generated.resources.tv_channel_videos
import chatone.composeapp.generated.resources.tv_channel_views
import chatone.composeapp.generated.resources.tv_channel_watch
import chatone.composeapp.generated.resources.tv_follow
import chatone.composeapp.generated.resources.tv_load_failed
import chatone.composeapp.generated.resources.tv_unfollow
import coil3.compose.AsyncImage
import io.rudione.chatone.domain.profile.ChannelClip
import io.rudione.chatone.domain.profile.ChannelProfile
import io.rudione.chatone.domain.profile.ChannelVideo
import io.rudione.chatone.icons.lucide.Heart
import io.rudione.chatone.icons.lucide.Lucide
import io.rudione.chatone.icons.lucide.Play
import io.rudione.chatone.presentation.browse.BrowseFormatting
import io.rudione.chatone.presentation.components.rememberScopedViewModelStoreOwner
import io.rudione.chatone.presentation.profile.ChannelProfileEvent
import io.rudione.chatone.presentation.profile.ChannelProfileState
import io.rudione.chatone.presentation.profile.ChannelProfileViewModel
import io.rudione.chatone.presentation.profile.FollowStatus
import io.rudione.chatone.presentation.profile.ProfileLoad
import io.rudione.chatone.presentation.profile.ProfileTab
import io.rudione.chatone.tv.components.TvButtonTone
import io.rudione.chatone.tv.components.TvLoading
import io.rudione.chatone.tv.components.TvMediaCard
import io.rudione.chatone.tv.components.TvMessage
import io.rudione.chatone.tv.components.TvPillButton
import io.rudione.chatone.tv.home.TvRowTitle
import io.rudione.chatone.tv.theme.TvDimens
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Immutable
class TvChannelActions(
    val onWatch: (ChannelProfile) -> Unit,
    val onOpenClip: (ChannelClip) -> Unit,
    val onOpenVideo: (ChannelVideo) -> Unit
)

@Composable
fun TvChannelScreen(
    login: String,
    actions: TvChannelActions,
    modifier: Modifier = Modifier
) {
    val session = rememberScopedViewModelStoreOwner()
    val viewModel: ChannelProfileViewModel =
        koinViewModel(viewModelStoreOwner = session) { parametersOf(login) }
    val state by viewModel.state.collectAsState()
    LaunchedEffect(viewModel) {
        viewModel.sendEvent(ChannelProfileEvent.SelectTab(ProfileTab.CLIPS))
        viewModel.sendEvent(ChannelProfileEvent.SelectTab(ProfileTab.VIDEOS))
    }
    Box(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when (state.load) {
            ProfileLoad.LOADING -> TvLoading(Modifier.align(Alignment.Center))
            ProfileLoad.READY -> state.profile?.let { profile ->
                TvChannelContent(
                    profile = profile,
                    state = state,
                    actions = actions,
                    onToggleFollow = { following ->
                        viewModel.sendEvent(ChannelProfileEvent.SetFollowing(following))
                    }
                )
            }

            ProfileLoad.NOT_FOUND, ProfileLoad.FAILED -> TvMessage(
                stringResource(Res.string.tv_load_failed),
                modifier = Modifier.align(Alignment.Center),
                onRetry = { viewModel.sendEvent(ChannelProfileEvent.Retry) }
            )
        }
    }
}

@Composable
private fun TvChannelContent(
    profile: ChannelProfile,
    state: ChannelProfileState,
    actions: TvChannelActions,
    onToggleFollow: (Boolean) -> Unit
) {
    val rowPadding = PaddingValues(horizontal = TvDimens.ScreenHorizontal, vertical = 8.dp)
    val following = state.follow == FollowStatus.FOLLOWING
    val focus = remember { FocusRequester() }
    val canFollow =
        state.follow == FollowStatus.FOLLOWING || state.follow == FollowStatus.NOT_FOLLOWING
    val hasContent =
        profile.isLive || canFollow || state.clips.items.isNotEmpty() || state.topClips.isNotEmpty() ||
                state.videos.items.isNotEmpty() || state.recentVideos.isNotEmpty()
    LaunchedEffect(hasContent) {
        if (hasContent) {
            withFrameNanos { }
            runCatching { focus.requestFocus() }
        }
    }
    LazyColumn(
        Modifier
            .fillMaxSize()
            .focusRequester(focus)
            .focusGroup(),
        contentPadding = PaddingValues(bottom = TvDimens.ScreenVertical),
        verticalArrangement = Arrangement.spacedBy(TvDimens.RowGap)
    ) {
        item(key = "header") {
            TvChannelHeader(profile) {
                if (profile.isLive) {
                    TvPillButton(
                        stringResource(Res.string.tv_channel_watch),
                        onClick = { actions.onWatch(profile) },
                        icon = Lucide.Play
                    )
                }
                if (canFollow) {
                    TvPillButton(
                        stringResource(if (following) Res.string.tv_unfollow else Res.string.tv_follow),
                        onClick = { onToggleFollow(!following) },
                        icon = Lucide.Heart,
                        tone = TvButtonTone.Tonal
                    )
                }
            }
        }
        val clips = state.clips.items.ifEmpty { state.topClips }
        if (clips.isNotEmpty()) {
            item(key = "clips") {
                TvRowTitle(stringResource(Res.string.tv_channel_clips))
                LazyRow(
                    modifier = Modifier.focusRestorer(),
                    contentPadding = rowPadding,
                    horizontalArrangement = Arrangement.spacedBy(TvDimens.CardGap)
                ) {
                    items(clips, key = { it.id }) { clip ->
                        TvMediaCard(
                            title = clip.title,
                            subtitle = stringResource(
                                Res.string.tv_channel_views,
                                BrowseFormatting.compact(clip.views)
                            ),
                            thumbnailUrl = clip.thumbnailUrl,
                            onClick = { actions.onOpenClip(clip) }
                        )
                    }
                }
            }
        }
        val videos = state.videos.items.ifEmpty { state.recentVideos }
        if (videos.isNotEmpty()) {
            item(key = "videos") {
                TvRowTitle(stringResource(Res.string.tv_channel_videos))
                LazyRow(
                    modifier = Modifier.focusRestorer(),
                    contentPadding = rowPadding,
                    horizontalArrangement = Arrangement.spacedBy(TvDimens.CardGap)
                ) {
                    items(videos, key = { it.id }) { video ->
                        TvMediaCard(
                            title = video.title,
                            subtitle = video.category,
                            thumbnailUrl = video.thumbnailUrl,
                            onClick = { actions.onOpenVideo(video) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TvChannelHeader(profile: ChannelProfile, actions: @Composable () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Box(
        Modifier
            .fillMaxWidth()
            .height(220.dp)
    ) {
        AsyncImage(
            model = profile.bannerUrl.ifBlank { profile.offlineImageUrl },
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to scheme.background.copy(alpha = 0.4f),
                        1f to scheme.background
                    )
                )
        )
        Row(
            Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = TvDimens.ScreenHorizontal, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = profile.avatarUrl,
                contentDescription = profile.displayName,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
            )
            Column(
                Modifier.widthIn(max = 640.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    profile.displayName,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                profile.followers?.let { followers ->
                    Text(
                        stringResource(
                            Res.string.tv_channel_followers,
                            BrowseFormatting.compact(followers)
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant
                    )
                }
                Text(
                    profile.live?.title ?: profile.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    Modifier.padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) { actions() }
            }
        }
    }
}
