package io.rudione.chatone.tv.player

import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.add
import chatone.composeapp.generated.resources.remove
import chatone.composeapp.generated.resources.tv_confirm_follow_title
import chatone.composeapp.generated.resources.tv_confirm_unfollow_title
import chatone.composeapp.generated.resources.tv_confirm_clip_title
import chatone.composeapp.generated.resources.tv_confirm_clip_body
import chatone.composeapp.generated.resources.tv_confirm_clip_action
import chatone.composeapp.generated.resources.tv_confirm_pin_title
import chatone.composeapp.generated.resources.tv_confirm_unpin_title
import chatone.composeapp.generated.resources.media_clip
import chatone.composeapp.generated.resources.media_mute
import chatone.composeapp.generated.resources.media_pause
import chatone.composeapp.generated.resources.media_play
import chatone.composeapp.generated.resources.media_profile
import chatone.composeapp.generated.resources.media_unmute
import chatone.composeapp.generated.resources.player_chat
import chatone.composeapp.generated.resources.player_quality
import chatone.composeapp.generated.resources.tv_follow
import chatone.composeapp.generated.resources.tv_unfollow
import coil3.compose.AsyncImage
import io.rudione.chatone.domain.channel.ChannelSnapshot
import io.rudione.chatone.domain.stream.LandscapeChatMode
import io.rudione.chatone.icons.lucide.Clapperboard
import io.rudione.chatone.icons.lucide.Heart
import io.rudione.chatone.icons.lucide.Lucide
import io.rudione.chatone.icons.lucide.MessagesSquare
import io.rudione.chatone.icons.lucide.Pause
import io.rudione.chatone.icons.lucide.Play
import io.rudione.chatone.icons.lucide.SlidersHorizontal
import io.rudione.chatone.icons.lucide.User
import io.rudione.chatone.icons.lucide.Volume2
import io.rudione.chatone.icons.lucide.VolumeX
import io.rudione.chatone.presentation.stream.StreamPlayerUiState
import io.rudione.chatone.presentation.stream.channel.FollowStatus
import io.rudione.chatone.tv.components.TvButtonTone
import io.rudione.chatone.tv.components.TvIconButton
import io.rudione.chatone.tv.components.TvLiveBadge
import io.rudione.chatone.tv.components.tvFocusTrap
import io.rudione.chatone.tv.theme.TvDimens
import chatone.composeapp.generated.resources.browse_keep_channel
import chatone.composeapp.generated.resources.tv_menu_remove_channel
import io.rudione.chatone.icons.lucide.Pin
import io.rudione.chatone.icons.lucide.PinOff
import org.jetbrains.compose.resources.stringResource

@Immutable
internal class TvPlayerInfo(
    val channel: ChannelSnapshot,
    val title: String,
    val summary: String,
    val follow: FollowStatus,
    val canClip: Boolean,
    val pinned: Boolean
)

internal enum class TvControl { PLAY, FOLLOW, CLIP, PIN }

@Immutable
internal class TvControlsActions(
    val onTogglePlayback: () -> Unit,
    val onToggleMute: () -> Unit,
    val onQuality: () -> Unit,
    val onCycleChat: () -> Unit,
    val onToggleFollow: () -> Unit,
    val onClip: () -> Unit,
    val onChannel: () -> Unit,
    val onTogglePin: () -> Unit,
    val onInteraction: () -> Unit
)

@Composable
internal fun TvPlayerControls(
    state: StreamPlayerUiState,
    info: TvPlayerInfo,
    actions: TvControlsActions,
    focusRequester: FocusRequester,
    endInset: Dp,
    modifier: Modifier = Modifier,
    focusTarget: TvControl = TvControl.PLAY,
    interactive: Boolean = true
) {
    val target = when (focusTarget) {
        TvControl.FOLLOW -> focusTarget.takeIf { info.follow != FollowStatus.SIGNED_OUT }
        TvControl.CLIP -> focusTarget.takeIf { info.canClip }
        else -> focusTarget
    } ?: TvControl.PLAY
    fun Modifier.focusFor(control: TvControl): Modifier =
        if (control == target) focusRequester(focusRequester) else this
    Column(
        modifier
            .fillMaxSize()
            .padding(end = endInset)
            .background(
                Brush.verticalGradient(
                    0f to Color.Black.copy(alpha = 0.78f),
                    0.32f to Color.Transparent,
                    0.62f to Color.Transparent,
                    1f to Color.Black.copy(alpha = 0.82f)
                )
            )
            .padding(horizontal = TvDimens.ScreenHorizontal, vertical = TvDimens.ScreenVertical)
    ) {
        TvPlayerHeader(info)
        Spacer(Modifier.weight(1f))
        Row(
            Modifier
                .fillMaxWidth()
                .then(if (interactive) Modifier.tvFocusTrap() else Modifier)
                .focusGroup()
                .onFocusChanged { if (it.hasFocus) actions.onInteraction() }
                .onPreviewKeyEvent {
                    actions.onInteraction()
                    false
                },
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val playing = state.isPlaybackActive
            TvIconButton(
                if (playing) Lucide.Pause else Lucide.Play,
                stringResource(if (playing) Res.string.media_pause else Res.string.media_play),
                onClick = actions.onTogglePlayback,
                tone = TvButtonTone.Primary,
                size = 56.dp,
                modifier = Modifier.focusFor(TvControl.PLAY)
            )
            TvIconButton(
                if (state.isMuted) Lucide.VolumeX else Lucide.Volume2,
                stringResource(if (state.isMuted) Res.string.media_unmute else Res.string.media_mute),
                onClick = actions.onToggleMute
            )
            Spacer(Modifier.weight(1f))
            TvIconButton(
                Lucide.MessagesSquare,
                stringResource(Res.string.player_chat),
                onClick = actions.onCycleChat,
                selected = state.preferences.landscapeChatMode != LandscapeChatMode.HIDDEN
            )
            TvIconButton(
                Lucide.SlidersHorizontal,
                stringResource(Res.string.player_quality),
                onClick = actions.onQuality
            )
            if (info.follow != FollowStatus.SIGNED_OUT) {
                TvIconButton(
                    Lucide.Heart,
                    stringResource(if (info.follow == FollowStatus.FOLLOWING) Res.string.tv_unfollow else Res.string.tv_follow),
                    onClick = actions.onToggleFollow,
                    selected = info.follow == FollowStatus.FOLLOWING,
                    modifier = Modifier.focusFor(TvControl.FOLLOW)
                )
            }
            if (info.canClip) {
                TvIconButton(
                    Lucide.Clapperboard,
                    stringResource(Res.string.media_clip),
                    onClick = actions.onClip,
                    modifier = Modifier.focusFor(TvControl.CLIP)
                )
            }
            TvIconButton(
                if (info.pinned) Lucide.PinOff else Lucide.Pin,
                stringResource(if (info.pinned) Res.string.tv_menu_remove_channel else Res.string.browse_keep_channel),
                onClick = actions.onTogglePin,
                selected = info.pinned,
                modifier = Modifier.focusFor(TvControl.PIN)
            )
            TvIconButton(
                Lucide.User,
                stringResource(Res.string.media_profile),
                onClick = actions.onChannel
            )
        }
    }
}

@Composable
private fun TvPlayerHeader(info: TvPlayerInfo) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = info.channel.avatarUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
        Column(Modifier.widthIn(max = 640.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    info.channel.displayName.ifBlank { info.channel.login },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                TvLiveBadge()
            }
            Text(
                info.title,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                info.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.72f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Immutable
internal class TvControlPrompts(
    val follow: String,
    val unfollow: String,
    val followAction: String,
    val unfollowAction: String,
    val clip: String,
    val clipBody: String,
    val clipAction: String,
    val pin: String,
    val unpin: String,
    val pinAction: String,
    val unpinAction: String
)

@Composable
internal fun tvControlPrompts(channelName: String) = TvControlPrompts(
    follow = stringResource(Res.string.tv_confirm_follow_title, channelName),
    unfollow = stringResource(Res.string.tv_confirm_unfollow_title, channelName),
    followAction = stringResource(Res.string.tv_follow),
    unfollowAction = stringResource(Res.string.tv_unfollow),
    clip = stringResource(Res.string.tv_confirm_clip_title),
    clipBody = stringResource(Res.string.tv_confirm_clip_body),
    clipAction = stringResource(Res.string.tv_confirm_clip_action),
    pin = stringResource(Res.string.tv_confirm_pin_title, channelName),
    unpin = stringResource(Res.string.tv_confirm_unpin_title, channelName),
    pinAction = stringResource(Res.string.add),
    unpinAction = stringResource(Res.string.remove)
)
