package io.rudione.chatone.presentation.stream

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.rudione.chatone.domain.stream.LandscapeChatMode
import io.rudione.chatone.domain.stream.StreamPhase
import io.rudione.chatone.domain.stream.StreamPlaybackEngine
import io.rudione.chatone.domain.stream.StreamVideoScale
import io.rudione.chatone.icons.material.Icons
import io.rudione.chatone.icons.material.rounded.Headphones
import io.rudione.chatone.presentation.theme.i18n.LocalStrings
import io.rudione.chatone.presentation.stream.ambient.StreamAmbience
import io.rudione.chatone.presentation.stream.ambient.ambientBars
import kotlinx.coroutines.delay

private const val CONTROLS_AUTO_HIDE_MS = 3_500L
private val BarThreshold = 1.dp
private const val PINCH_IN_THRESHOLD = 1.12f
private const val PINCH_OUT_THRESHOLD = 0.88f

@Composable
fun StreamPlayerView(
    host: StreamPlayerHost,
    liveInfo: StreamLiveInfo?,
    isFullscreen: Boolean,
    pictureInPicture: PictureInPictureController,
    onToggleFullscreen: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    ambience: StreamAmbience? = null,
    overlay: (@Composable BoxScope.() -> Unit)? = null
) {
    val state by host.state.collectAsState()
    val engine by host.engine.collectAsState()
    val currentOnBack by rememberUpdatedState(onBack)
    val fullscreenNow by rememberUpdatedState(isFullscreen)
    var controlsVisible by remember { mutableStateOf(true) }
    var interactionTick by remember { mutableIntStateOf(0) }
    var chatModeChanges by remember { mutableIntStateOf(0) }
    val autoHide = state.phase == StreamPhase.PLAYING && !state.settingsVisible

    LaunchedEffect(controlsVisible, interactionTick, autoHide) {
        if (controlsVisible && autoHide) {
            delay(CONTROLS_AUTO_HIDE_MS)
            controlsVisible = false
        }
    }

    val callbacks = remember(host, onToggleFullscreen, pictureInPicture) {
        StreamControlsCallbacks(
            onEvent = { event ->
                interactionTick++
                host.onEvent(event)
            },
            onBack = { currentOnBack() },
            onToggleFullscreen = {
                interactionTick++
                onToggleFullscreen()
            },
            onEnterPictureInPicture = pictureInPicture::enter
        )
    }

    Box(
        modifier = modifier
            .background(Color.Black)
            .clipToBounds()
            .pointerInput(host) {
                detectTapGestures(
                    onTap = {
                        controlsVisible = !controlsVisible
                        interactionTick++
                    },
                    onDoubleTap = {
                        if (fullscreenNow) {
                            host.onEvent(StreamPlayerEvent.CycleLandscapeChatMode)
                            chatModeChanges++
                        } else {
                            host.onEvent(StreamPlayerEvent.ToggleVideoScale)
                        }
                    }
                )
            }
            .pinchToScale(
                key = host,
                onZoomIn = { host.onEvent(StreamPlayerEvent.SetVideoScale(StreamVideoScale.FILL)) },
                onZoomOut = { host.onEvent(StreamPlayerEvent.SetVideoScale(StreamVideoScale.FIT)) }
            )
    ) {
        StreamVideoFrame(
            engine = engine,
            scale = state.preferences.videoScale,
            aspectRatio = state.videoAspectRatio,
            keepScreenOn = state.isPlaybackActive,
            onViewportChanged = { width, height -> host.onEvent(StreamPlayerEvent.ViewportChanged(width, height)) },
            modifier = Modifier.fillMaxSize(),
            audioOnly = state.isAudioOnly,
            ambience = ambience,
            ambientSampling = state.phase == StreamPhase.PLAYING && !state.isAudioOnly
        )

        if (overlay != null) {
            overlay()
        }

        if (state.isBusy) {
            CircularProgressIndicator(
                color = Color.White,
                strokeWidth = 3.dp,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(42.dp)
            )
        }

        val overlayChat = isFullscreen && state.preferences.landscapeChatMode == LandscapeChatMode.OVERLAY
        AnimatedVisibility(
            visible = controlsVisible || state.phase == StreamPhase.PAUSED || state.needsAttention,
            enter = fadeIn(tween(160)),
            exit = fadeOut(tween(220)),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(Modifier.fillMaxSize()) {
                StreamControlsOverlay(
                    state = state,
                    liveInfo = liveInfo,
                    latencySeconds = if (overlayChat) host.latencySeconds else null,
                    isFullscreen = isFullscreen,
                    pictureInPictureSupported = pictureInPicture.isSupported,
                    callbacks = callbacks
                )
            }
        }

        if (state.needsAttention) {
            StreamStatusOverlay(state = state, onRetry = { host.onEvent(StreamPlayerEvent.Retry) })
        }

        if (isFullscreen) {
            ChatModeHint(changes = chatModeChanges, mode = state.preferences.landscapeChatMode)
        }

        val chatChannel = state.chatChannelLogin
        val streamChannel = state.channelLogin
        if (state.showsOtherChannelNotice && chatChannel != null && streamChannel != null) {
            OtherChannelNotice(
                chatChannelLogin = chatChannel,
                streamChannelLogin = streamChannel,
                compact = !isFullscreen,
                onDismiss = { host.onEvent(StreamPlayerEvent.DismissChannelNotice) }
            )
        }

        if (state.statsVisible) {
            StreamStatsOverlay(
                host = host,
                onClose = { host.onEvent(StreamPlayerEvent.SetStatsVisible(false)) },
                compact = !isFullscreen
            )
        }
    }
}

@Composable
fun StreamPictureInPictureView(
    host: StreamPlayerHost,
    modifier: Modifier = Modifier,
    surfaceKind: VideoSurfaceKind = VideoSurfaceKind.Direct
) {
    val state by host.state.collectAsState()
    val engine by host.engine.collectAsState()
    val latency by host.latencySeconds.collectAsState()
    Box(modifier = modifier.background(Color.Black).clipToBounds()) {
        StreamVideoFrame(
            engine = engine,
            scale = StreamVideoScale.FIT,
            aspectRatio = state.videoAspectRatio,
            keepScreenOn = state.isPlaybackActive,
            onViewportChanged = { width, height -> host.onEvent(StreamPlayerEvent.ViewportChanged(width, height)) },
            modifier = Modifier.fillMaxSize(),
            audioOnly = state.isAudioOnly,
            compactCover = true,
            surfaceKind = surfaceKind
        )
        if (state.isBusy) {
            CircularProgressIndicator(
                color = Color.White,
                strokeWidth = 2.dp,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(24.dp)
            )
        }
        latency?.let { seconds ->
            Text(
                text = streamLatencyText(seconds, compact = true),
                color = Color.White,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(4.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            )
        }
    }
}

@Composable
internal fun StreamVideoFrame(
    engine: StreamPlaybackEngine?,
    scale: StreamVideoScale,
    aspectRatio: Float,
    keepScreenOn: Boolean,
    onViewportChanged: (Int, Int) -> Unit,
    modifier: Modifier = Modifier,
    audioOnly: Boolean = false,
    compactCover: Boolean = false,
    surfaceKind: VideoSurfaceKind = VideoSurfaceKind.Direct,
    ambience: StreamAmbience? = null,
    ambientSampling: Boolean = false
) {
    val latestViewport by rememberUpdatedState(onViewportChanged)
    BoxWithConstraints(modifier = modifier.clipToBounds(), contentAlignment = Alignment.Center) {
        val ratio = aspectRatio.takeIf { it.isFinite() && it > 0f } ?: StreamPlayerUiState.DEFAULT_ASPECT_RATIO
        val containerWidth = maxWidth
        val containerHeight = if (constraints.hasBoundedHeight) maxHeight else maxWidth / ratio
        val containerIsWider = containerWidth / containerHeight > ratio
        val fitByHeight = if (scale == StreamVideoScale.FIT) containerIsWider else !containerIsWider
        val targetWidth = if (fitByHeight) containerHeight * ratio else containerWidth
        val targetHeight = if (fitByHeight) containerHeight else containerWidth / ratio
        val letterboxed = scale == StreamVideoScale.FIT &&
            (containerWidth - targetWidth > BarThreshold || containerHeight - targetHeight > BarThreshold)
        val bars = ambience?.takeIf { letterboxed }
        if (bars != null) {
            val density = LocalDensity.current
            val videoWidthPx = with(density) { targetWidth.toPx() }
            val videoHeightPx = with(density) { targetHeight.toPx() }
            Spacer(Modifier.fillMaxSize().ambientBars(bars, videoWidthPx, videoHeightPx))
        }
        PlatformVideoView(
            engine = engine,
            keepScreenOn = keepScreenOn,
            modifier = Modifier.requiredSize(targetWidth, targetHeight),
            surfaceKind = surfaceKind,
            frameSink = ambience?.takeIf { ambientSampling && (letterboxed || it.glowAttached) }
        )
        if (audioOnly) {
            AudioOnlyCover(compact = compactCover, modifier = Modifier.fillMaxSize())
        }

        val viewportWidth = constraints.maxWidth
        val viewportHeight = if (constraints.hasBoundedHeight) constraints.maxHeight else (constraints.maxWidth / ratio).toInt()
        LaunchedEffect(viewportWidth, viewportHeight) {
            latestViewport(viewportWidth, viewportHeight)
        }
    }
}

@Composable
private fun AudioOnlyCover(compact: Boolean, modifier: Modifier = Modifier) {
    val label = LocalStrings.current.player.audioOnly
    Column(
        modifier = modifier.background(Color.Black),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.Headphones,
            contentDescription = label,
            tint = Color.White.copy(alpha = 0.82f),
            modifier = Modifier.size(if (compact) 22.dp else 40.dp)
        )
        if (!compact) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = label,
                color = Color.White.copy(alpha = 0.72f),
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

private fun Modifier.pinchToScale(key: Any, onZoomIn: () -> Unit, onZoomOut: () -> Unit): Modifier =
    pointerInput(key) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            var zoom = 1f
            var handled = false
            do {
                val event = awaitPointerEvent()
                if (event.changes.count { it.pressed } >= 2) {
                    zoom *= event.calculateZoom()
                    if (!handled && zoom > PINCH_IN_THRESHOLD) {
                        handled = true
                        onZoomIn()
                    } else if (!handled && zoom < PINCH_OUT_THRESHOLD) {
                        handled = true
                        onZoomOut()
                    }
                    event.changes.forEach { if (it.positionChanged()) it.consume() }
                }
            } while (event.changes.any { it.pressed })
        }
    }
