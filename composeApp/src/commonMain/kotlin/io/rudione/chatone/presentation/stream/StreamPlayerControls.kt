package io.rudione.chatone.presentation.stream

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import io.rudione.chatone.icons.material.Icons
import io.rudione.chatone.icons.material.automirrored.rounded.VolumeOff
import io.rudione.chatone.icons.material.automirrored.rounded.VolumeUp
import io.rudione.chatone.icons.material.rounded.Close
import io.rudione.chatone.icons.material.rounded.CommentsDisabled
import io.rudione.chatone.icons.material.rounded.Fullscreen
import io.rudione.chatone.icons.material.rounded.FullscreenExit
import io.rudione.chatone.icons.material.rounded.Layers
import io.rudione.chatone.icons.material.rounded.Pause
import io.rudione.chatone.icons.material.rounded.PersonOutline
import io.rudione.chatone.icons.material.rounded.PictureInPictureAlt
import io.rudione.chatone.icons.material.rounded.PlayArrow
import io.rudione.chatone.icons.material.rounded.Refresh
import io.rudione.chatone.icons.material.rounded.Settings
import io.rudione.chatone.icons.material.rounded.VerticalSplit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.rudione.chatone.domain.stream.LandscapeChatMode
import io.rudione.chatone.domain.stream.StreamPhase
import io.rudione.chatone.domain.stream.StreamQualitySelection
import io.rudione.chatone.presentation.theme.i18n.LocalStrings
import io.rudione.chatone.presentation.theme.i18n.StreamPlayerStrings
import io.rudione.chatone.presentation.theme.i18n.format
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow

@Immutable
data class StreamLiveInfo(
    val viewerCount: Int,
    val startedAt: String
)

internal val LiveRed = Color(0xFFE91916)
private val ScrimColor = Color.Black

@Immutable
internal data class StreamControlsCallbacks(
    val onEvent: (StreamPlayerEvent) -> Unit,
    val onToggleFullscreen: () -> Unit,
    val onEnterPictureInPicture: () -> Unit
)

@Composable
internal fun BoxScope.StreamControlsOverlay(
    state: StreamPlayerUiState,
    liveInfo: StreamLiveInfo?,
    latencySeconds: StateFlow<Float?>?,
    isFullscreen: Boolean,
    pictureInPictureSupported: Boolean,
    callbacks: StreamControlsCallbacks
) {
    val strings = LocalStrings.current.player
    val compact = !isFullscreen
    val buttonSize = if (compact) 38.dp else 44.dp
    val iconSize = if (compact) 21.dp else 24.dp
    val edgePadding = if (compact) 6.dp else 14.dp

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0f to ScrimColor.copy(alpha = 0.55f),
                    0.28f to Color.Transparent,
                    0.62f to Color.Transparent,
                    1f to ScrimColor.copy(alpha = 0.65f)
                )
            )
    )

    Row(
        modifier = Modifier
            .align(Alignment.TopCenter)
            .fillMaxWidth()
            .padding(horizontal = edgePadding, vertical = edgePadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PlayerIconButton(
            icon = Icons.Rounded.Close,
            contentDescription = strings.close,
            onClick = { callbacks.onEvent(StreamPlayerEvent.Close) },
            size = buttonSize,
            iconSize = iconSize
        )
        Spacer(Modifier.weight(1f))
        if (pictureInPictureSupported && state.isPlaybackActive) {
            PlayerIconButton(
                icon = Icons.Rounded.PictureInPictureAlt,
                contentDescription = strings.pictureInPicture,
                onClick = callbacks.onEnterPictureInPicture,
                size = buttonSize,
                iconSize = iconSize
            )
        }
        PlayerIconButton(
            icon = Icons.Rounded.Settings,
            contentDescription = strings.settings,
            onClick = { callbacks.onEvent(StreamPlayerEvent.SetSettingsVisible(true)) },
            size = buttonSize,
            iconSize = iconSize
        )
    }

    if (!state.isBusy && !state.needsAttention) {
        val playing = state.isPlaybackActive
        PlayerIconButton(
            icon = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
            contentDescription = if (playing) strings.pause else strings.play,
            onClick = { callbacks.onEvent(StreamPlayerEvent.TogglePlayback) },
            size = if (compact) 58.dp else 68.dp,
            iconSize = if (compact) 34.dp else 40.dp,
            modifier = Modifier.align(Alignment.Center)
        )
    }

    Row(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .padding(horizontal = edgePadding, vertical = edgePadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        LiveBadge(
            liveInfo = liveInfo,
            fallbackStartedAtMs = state.streamStartedAtMs,
            latencySeconds = latencySeconds,
            strings = strings,
            compact = compact
        )
        Spacer(Modifier.weight(1f))
        QualityPill(state = state, strings = strings) {
            callbacks.onEvent(StreamPlayerEvent.SetSettingsVisible(true))
        }
        PlayerIconButton(
            icon = if (state.isMuted) Icons.AutoMirrored.Rounded.VolumeOff else Icons.AutoMirrored.Rounded.VolumeUp,
            contentDescription = if (state.isMuted) strings.unmute else strings.mute,
            onClick = { callbacks.onEvent(StreamPlayerEvent.ToggleMute) },
            size = buttonSize,
            iconSize = iconSize,
            active = state.isMuted
        )
        PlayerIconButton(
            icon = if (isFullscreen) Icons.Rounded.FullscreenExit else Icons.Rounded.Fullscreen,
            contentDescription = if (isFullscreen) strings.exitFullscreen else strings.fullscreen,
            onClick = callbacks.onToggleFullscreen,
            size = buttonSize,
            iconSize = iconSize
        )
        if (isFullscreen) {
            val mode = state.preferences.landscapeChatMode
            PlayerIconButton(
                icon = chatModeIcon(mode),
                contentDescription = "${strings.landscapeChat}: ${chatModeLabel(mode, strings)}",
                onClick = { callbacks.onEvent(StreamPlayerEvent.CycleLandscapeChatMode) },
                size = buttonSize,
                iconSize = iconSize
            )
        }
    }
}

@Composable
internal fun BoxScope.OtherChannelNotice(
    chatChannelLogin: String,
    streamChannelLogin: String,
    compact: Boolean,
    onDismiss: () -> Unit
) {
    val appStrings = LocalStrings.current
    Row(
        modifier = Modifier
            .align(Alignment.TopCenter)
            .padding(top = if (compact) 50.dp else 72.dp, start = 12.dp, end = 12.dp)
            .clip(RoundedCornerShape(50))
            .background(Color.Black.copy(alpha = 0.68f))
            .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = appStrings.format(appStrings.player.otherChannelNotice, chatChannelLogin, streamChannelLogin),
            color = Color.White,
            fontSize = if (compact) 11.sp else 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    role = Role.Button,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Close, contentDescription = appStrings.player.close, tint = Color.White, modifier = Modifier.size(15.dp))
        }
    }
}

@Composable
internal fun BoxScope.StreamStatusOverlay(
    state: StreamPlayerUiState,
    onRetry: () -> Unit
) {
    val appStrings = LocalStrings.current
    val strings = appStrings.player
    val message = when (state.phase) {
        StreamPhase.OFFLINE -> appStrings.format(strings.offline, state.channelLogin.orEmpty())
        else -> StreamFormatting.errorMessage(state.errorKind, strings)
    }
    Column(
        modifier = Modifier
            .align(Alignment.Center)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = message,
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        if (state.phase != StreamPhase.OFFLINE) {
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.16f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        role = Role.Button,
                        onClick = onRetry
                    )
                    .padding(horizontal = 14.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(strings.retry, color = Color.White, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
internal fun PlayerIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    iconSize: Dp = 22.dp,
    active: Boolean = false
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val accent = MaterialTheme.colorScheme.primary
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.88f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "playerButtonScale"
    )
    val container by animateColorAsState(
        targetValue = when {
            active -> accent.copy(alpha = 0.82f)
            pressed -> Color.White.copy(alpha = 0.3f)
            else -> Color.Black.copy(alpha = 0.42f)
        },
        animationSpec = tween(140),
        label = "playerButtonContainer"
    )
    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(container)
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(iconSize))
    }
}

@Composable
private fun LiveBadge(
    liveInfo: StreamLiveInfo?,
    fallbackStartedAtMs: Long?,
    latencySeconds: StateFlow<Float?>?,
    strings: StreamPlayerStrings,
    compact: Boolean
) {
    val startedAtIso = liveInfo?.startedAt?.takeIf { it.isNotEmpty() }
    val uptime by produceState<String?>(initialValue = null, startedAtIso, fallbackStartedAtMs) {
        if (startedAtIso == null && fallbackStartedAtMs == null) {
            value = null
            return@produceState
        }
        while (true) {
            value = startedAtIso?.let(StreamFormatting::uptime)
                ?: fallbackStartedAtMs?.let { StreamFormatting.uptime(it) }
            delay(1_000)
        }
    }
    val textSize = if (compact) 12.sp else 13.sp
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black.copy(alpha = 0.42f))
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(LiveRed)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = uptime ?: strings.live,
            color = Color.White,
            fontSize = textSize,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
        val latency = latencySeconds?.collectAsState()?.value
        if (latency != null) {
            Spacer(Modifier.width(8.dp))
            Text(
                text = streamLatencyText(latency, compact = true),
                color = Color.White.copy(alpha = 0.72f),
                fontSize = textSize,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
        }
        if (liveInfo != null && liveInfo.viewerCount > 0) {
            Spacer(Modifier.width(10.dp))
            Icon(Icons.Rounded.PersonOutline, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(3.dp))
            Text(
                text = StreamFormatting.compactCount(liveInfo.viewerCount, strings.decimalSeparator),
                color = Color.White,
                fontSize = textSize,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun QualityPill(state: StreamPlayerUiState, strings: StreamPlayerStrings, onClick: () -> Unit) {
    val label = when (val selection = state.selection) {
        StreamQualitySelection.Auto -> state.activeVariant?.displayName ?: strings.qualityAuto
        StreamQualitySelection.AudioOnly -> strings.audioOnly
        is StreamQualitySelection.Fixed ->
            state.variants.firstOrNull { it.groupId == selection.groupId }?.displayName ?: strings.qualityAuto
    }
    Text(
        text = label,
        color = Color.White,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black.copy(alpha = 0.42f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
            .padding(horizontal = 9.dp, vertical = 6.dp)
    )
}

internal fun chatModeIcon(mode: LandscapeChatMode): ImageVector = when (mode) {
    LandscapeChatMode.SIDE -> Icons.Rounded.VerticalSplit
    LandscapeChatMode.OVERLAY -> Icons.Rounded.Layers
    LandscapeChatMode.HIDDEN -> Icons.Rounded.CommentsDisabled
}

internal fun chatModeLabel(mode: LandscapeChatMode, strings: StreamPlayerStrings): String = when (mode) {
    LandscapeChatMode.SIDE -> strings.chatSide
    LandscapeChatMode.OVERLAY -> strings.chatOverlay
    LandscapeChatMode.HIDDEN -> strings.chatHidden
}
