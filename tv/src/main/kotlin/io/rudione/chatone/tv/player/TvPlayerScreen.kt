package io.rudione.chatone.tv.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.paging.compose.LazyPagingItems
import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.player_offline
import chatone.composeapp.generated.resources.tv_delay_seconds
import io.rudione.chatone.domain.browse.BrowseStream
import io.rudione.chatone.domain.channel.ChannelSnapshot
import io.rudione.chatone.domain.stream.LandscapeChatMode
import io.rudione.chatone.domain.stream.StreamPhase
import io.rudione.chatone.presentation.browse.BrowseFormatting
import io.rudione.chatone.presentation.chat.ChatScreenCallbacks
import io.rudione.chatone.presentation.chat.ChatSessionIdentity
import io.rudione.chatone.presentation.chat.ChatViewModel
import io.rudione.chatone.presentation.components.SystemBackHandler
import io.rudione.chatone.presentation.components.expressive.MorphingLoadingIndicator
import io.rudione.chatone.presentation.stream.StreamFormatting
import io.rudione.chatone.presentation.stream.StreamLatencyBadge
import io.rudione.chatone.presentation.stream.StreamMediaInfoEffect
import io.rudione.chatone.presentation.stream.StreamPlayerEvent
import io.rudione.chatone.presentation.stream.StreamPlayerHost
import io.rudione.chatone.presentation.stream.StreamPlayerUiState
import io.rudione.chatone.presentation.stream.StreamVideoFrame
import io.rudione.chatone.presentation.stream.StreamVisibilityEffect
import io.rudione.chatone.presentation.stream.ambient.rememberStreamAmbience
import io.rudione.chatone.presentation.stream.channel.FollowStatus
import io.rudione.chatone.presentation.stream.channel.StreamChannelEvent
import io.rudione.chatone.presentation.stream.channel.StreamChannelViewModel
import io.rudione.chatone.tv.components.TvConfirmDialog
import io.rudione.chatone.tv.components.TvConfirmRequest
import io.rudione.chatone.tv.components.TvMessage
import io.rudione.chatone.tv.components.claim
import io.rudione.chatone.tv.theme.TvDimens
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private const val CONTROLS_HIDE_MS = 5_000L
private const val OVERLAY_FADE_MS = 350
private const val SIDE_CHAT_ALPHA = 0.92f

@Immutable
class TvPlayerActions(
    val onClose: () -> Unit,
    val onSwitchStream: (BrowseStream) -> Unit,
    val onOpenChannel: (String) -> Unit,
    val onCreateClip: (login: String, delaySeconds: Float?) -> Unit,
    val onTogglePin: (ChannelSnapshot) -> Unit
)

@Composable
fun TvPlayerScreen(
    login: String,
    active: Boolean,
    host: StreamPlayerHost,
    identity: ChatSessionIdentity,
    chatCallbacks: ChatScreenCallbacks,
    followed: LazyPagingItems<BrowseStream>,
    needsSignIn: Boolean,
    pinned: Boolean,
    actions: TvPlayerActions,
    modifier: Modifier = Modifier
) {
    val state by host.state.collectAsState()
    val engine by host.engine.collectAsState()
    val latency by host.latencySeconds.collectAsState()
    val returnToLiveMs = state.preferences.overlayAutoLockSeconds * 1_000L
    val channelViewModel: StreamChannelViewModel = koinViewModel()
    val channelState by channelViewModel.state.collectAsState()
    val chatViewModel: ChatViewModel = koinViewModel()
    val chatState by chatViewModel.state.collectAsState()
    var panel by remember { mutableStateOf(TvPlayerPanel.NONE) }
    var interactions by remember { mutableIntStateOf(0) }
    val windowSize = LocalWindowInfo.current.containerSize
    val rootFocus = remember { FocusRequester() }
    val controlsFocus = remember { FocusRequester() }
    val chatFocus = remember { FocusRequester() }
    val ambience = rememberStreamAmbience(host)
    var confirm by remember { mutableStateOf<TvConfirmRequest?>(null) }
    var controlsFocusTarget by remember { mutableStateOf(TvControl.PLAY) }
    val ask: (TvControl, TvConfirmRequest) -> Unit = { control, request ->
        controlsFocusTarget = control
        confirm = request
        panel = TvPlayerPanel.CONFIRM
    }
    val overlayActive = panel == TvPlayerPanel.CONTROLS || panel == TvPlayerPanel.CHAT || panel == TvPlayerPanel.CONFIRM
    var overlayIdle by remember { mutableStateOf(false) }
    LaunchedEffect(overlayActive, state.preferences.overlayHideDelaySeconds) {
        if (overlayActive) {
            overlayIdle = false
        } else {
            delay(state.preferences.overlayHideDelaySeconds * 1_000L)
            overlayIdle = true
        }
    }
    val overlayBackground by animateFloatAsState(
        targetValue = if (overlayIdle) state.preferences.overlayIdleOpacity else state.preferences.overlayOpacity,
        animationSpec = tween(OVERLAY_FADE_MS),
        label = "tvOverlayBackground"
    )
    val overlayChrome by animateFloatAsState(
        targetValue = if (overlayIdle) 0f else 1f,
        animationSpec = tween(OVERLAY_FADE_MS),
        label = "tvOverlayChrome"
    )

    LaunchedEffect(login) {
        host.onEvent(StreamPlayerEvent.Open(login))
        channelViewModel.sendEvent(StreamChannelEvent.Bind(login))
    }
    LaunchedEffect(Unit) {
        host.onEvent(StreamPlayerEvent.SetBackgroundAudio(false))
        host.onEvent(StreamPlayerEvent.SetAutoPictureInPicture(false))
    }
    val chatMode = state.preferences.landscapeChatMode
    val chatWidth = with(LocalDensity.current) { (windowSize.width * state.preferences.sideChatFraction).toDp() }
    val showLatency = state.phase == StreamPhase.PLAYING
    LaunchedEffect(panel, active) {
        if (!active) return@LaunchedEffect
        when (panel) {
            TvPlayerPanel.NONE -> rootFocus.claim()
            TvPlayerPanel.CONTROLS -> controlsFocus.claim()
            TvPlayerPanel.CHAT -> chatFocus.claim()
            else -> Unit
        }
    }
    LaunchedEffect(panel, interactions, state.phase, active) {
        if (active && panel == TvPlayerPanel.CONTROLS && state.phase == StreamPhase.PLAYING) {
            delay(CONTROLS_HIDE_MS)
            panel = TvPlayerPanel.NONE
        }
    }
    StreamMediaInfoEffect(host = host, channelLogin = login)
    StreamVisibilityEffect { visible -> host.onEvent(StreamPlayerEvent.AppVisibilityChanged(visible)) }
    SystemBackHandler(enabled = active) {
        when (panel) {
            TvPlayerPanel.NONE -> actions.onClose()
            TvPlayerPanel.QUALITY -> panel = TvPlayerPanel.CONTROLS
            else -> panel = TvPlayerPanel.NONE
        }
    }

    val switchBy: (Int) -> Unit = { step ->
        val snapshot = followed.itemSnapshotList.items
        val index = snapshot.indexOfFirst { it.login == login }
        snapshot.neighbour(index, step)?.let(actions.onSwitchStream)
    }
    val run: (TvPlayerCommand) -> Unit = { command ->
        when (command) {
            TvPlayerCommand.SHOW_CONTROLS -> {
                controlsFocusTarget = TvControl.PLAY
                panel = TvPlayerPanel.CONTROLS
            }

            TvPlayerCommand.TOGGLE_PLAYBACK -> {
                host.onEvent(StreamPlayerEvent.TogglePlayback)
                controlsFocusTarget = TvControl.PLAY
                panel = TvPlayerPanel.CONTROLS
                interactions++
            }

            TvPlayerCommand.OPEN_CHANNELS -> panel = TvPlayerPanel.CHANNELS
            TvPlayerCommand.FOCUS_CHAT -> {
                if (chatMode == LandscapeChatMode.HIDDEN) {
                    host.onEvent(StreamPlayerEvent.SetLandscapeChatMode(LandscapeChatMode.SIDE))
                }
                panel = TvPlayerPanel.CHAT
            }

            TvPlayerCommand.NEXT_CHANNEL -> switchBy(1)
            TvPlayerCommand.PREVIOUS_CHANNEL -> switchBy(-1)
        }
    }

    Box(
        modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusProperties {
                onEnter = { if (!active) cancelFocusChange() }
                onExit = { if (active) cancelFocusChange() }
            }
            .focusGroup()
            .onPreviewKeyEvent { event ->
                if (!active) return@onPreviewKeyEvent false
                val command = mediaCommand(event.key)
                if (command != null && event.type == KeyEventType.KeyDown) {
                    run(command)
                    true
                } else {
                    false
                }
            }
    ) {
        Row(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .focusRequester(rootFocus)
                    .onKeyEvent { event ->
                        val command = idleCommand(event.key)
                        if (command != null && event.type == KeyEventType.KeyDown) {
                            run(command)
                            true
                        } else {
                            false
                        }
                    }
                    .focusable()
            ) {
                StreamVideoFrame(
                    engine = engine,
                    scale = state.preferences.videoScale,
                    aspectRatio = state.videoAspectRatio,
                    keepScreenOn = state.isPlaybackActive,
                    onViewportChanged = { _, _ ->
                        host.onEvent(
                            StreamPlayerEvent.ViewportChanged(
                                windowSize.width,
                                windowSize.height
                            )
                        )
                    },
                    modifier = Modifier.fillMaxSize(),
                    audioOnly = state.isAudioOnly,
                    ambience = ambience,
                    ambientSampling = state.phase == StreamPhase.PLAYING && !state.isAudioOnly &&
                        state.preferences.ambientLight
                )
                if (state.preferences.latencyOnVideo && panel != TvPlayerPanel.CONTROLS) {
                    StreamLatencyBadge(
                        latency = host.latencySeconds,
                        textSize = 14.sp,
                        dotSize = 8.dp,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(16.dp)
                    )
                }
                TvPlaybackStatus(state, login, onRetry = { host.onEvent(StreamPlayerEvent.Retry) })
            }
            if (chatMode == LandscapeChatMode.SIDE) {
                TvChatPanel(
                    channelLogin = login,
                    identity = identity,
                    callbacks = chatCallbacks,
                    scale = state.preferences.overlayScale,
                    backgroundAlpha = { SIDE_CHAT_ALPHA },
                    latency = host.latencySeconds,
                    showLatency = showLatency,
                    returnToLiveMs = returnToLiveMs,
                    focusRequester = chatFocus,
                    onLeave = { panel = TvPlayerPanel.NONE },
                    modifier = Modifier
                        .width(chatWidth)
                        .fillMaxHeight()
                )
            }
        }
        if (chatMode == LandscapeChatMode.OVERLAY) {
            TvChatPanel(
                channelLogin = login,
                identity = identity,
                callbacks = chatCallbacks,
                scale = state.preferences.overlayScale,
                backgroundAlpha = { overlayBackground },
                chromeAlpha = { overlayChrome },
                latency = host.latencySeconds,
                showLatency = showLatency,
                returnToLiveMs = returnToLiveMs,
                focusRequester = chatFocus,
                onLeave = { panel = TvPlayerPanel.NONE },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(chatWidth)
                    .fillMaxHeight()
                    .padding(vertical = TvDimens.ScreenVertical)
            )
        }
        AnimatedVisibility(
            visible = panel == TvPlayerPanel.CONTROLS || panel == TvPlayerPanel.CONFIRM,
            enter = fadeIn(tween(160)),
            exit = fadeOut(tween(200))
        ) {
            val live =
                chatState.liveStream?.takeIf { it.userLogin.equals(login, ignoreCase = true) }
            val channel =
                if (channelState.channel.login == login) channelState.channel else ChannelSnapshot(
                    login
                )
            val viewers = live?.let { BrowseFormatting.viewers(it.viewerCount) }
            val delayText = latency?.let {
                stringResource(Res.string.tv_delay_seconds, StreamFormatting.decimal(it, 1, '.'))
            }
            val prompts = tvControlPrompts(channel.displayName.ifBlank { channel.login })
            val summary =
                listOf(live?.gameName.orEmpty().ifBlank { channel.category }, viewers, delayText)
                    .filterNot { it.isNullOrBlank() }
                    .joinToString(" · ")
            TvPlayerControls(
                state = state,
                info = TvPlayerInfo(
                    channel = channel,
                    title = live?.title.orEmpty().ifBlank { channel.title },
                    summary = summary,
                    follow = channelState.follow,
                    canClip = identity.userId.isNotBlank(),
                    pinned = pinned
                ),
                actions = TvControlsActions(
                    onTogglePlayback = {
                        host.onEvent(StreamPlayerEvent.TogglePlayback)
                        interactions++
                    },
                    onToggleMute = {
                        host.onEvent(StreamPlayerEvent.ToggleMute)
                        interactions++
                    },
                    onQuality = { panel = TvPlayerPanel.QUALITY },
                    onCycleChat = {
                        host.onEvent(StreamPlayerEvent.CycleLandscapeChatMode)
                        interactions++
                    },
                    onToggleFollow = {
                        val following = channelState.follow == FollowStatus.FOLLOWING
                        ask(
                            TvControl.FOLLOW,
                            TvConfirmRequest(
                                title = if (following) prompts.unfollow else prompts.follow,
                                confirmLabel = if (following) prompts.unfollowAction else prompts.followAction
                            ) {
                                channelViewModel.sendEvent(StreamChannelEvent.ToggleFollow)
                                interactions++
                            }
                        )
                    },
                    onClip = {
                        ask(TvControl.CLIP, TvConfirmRequest(prompts.clip, prompts.clipAction, prompts.clipBody) {
                            panel = TvPlayerPanel.NONE
                            actions.onCreateClip(login, latency)
                        })
                    },
                    onChannel = {
                        panel = TvPlayerPanel.NONE
                        actions.onOpenChannel(login)
                    },
                    onTogglePin = {
                        ask(
                            TvControl.PIN,
                            TvConfirmRequest(
                                title = if (pinned) prompts.unpin else prompts.pin,
                                confirmLabel = if (pinned) prompts.unpinAction else prompts.pinAction
                            ) {
                                actions.onTogglePin(channel)
                                interactions++
                            }
                        )
                    },
                    onInteraction = { interactions++ }
                ),
                focusRequester = controlsFocus,
                endInset = if (chatMode == LandscapeChatMode.HIDDEN) 0.dp else chatWidth,
                focusTarget = controlsFocusTarget,
                interactive = panel == TvPlayerPanel.CONTROLS
            )
        }
        AnimatedVisibility(
            visible = panel == TvPlayerPanel.CHANNELS,
            enter = slideInHorizontally { -it } + fadeIn(),
            exit = slideOutHorizontally { -it } + fadeOut(),
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            TvChannelsPanel(
                streams = followed,
                currentLogin = login,
                needsSignIn = needsSignIn,
                onSelect = { stream ->
                    panel = TvPlayerPanel.NONE
                    actions.onSwitchStream(stream)
                },
                modifier = Modifier
            )
        }
        AnimatedVisibility(
            visible = panel == TvPlayerPanel.QUALITY,
            enter = slideInHorizontally { it } + fadeIn(),
            exit = slideOutHorizontally { it } + fadeOut(),
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            TvQualityPanel(
                state = state,
                onSelect = { selection ->
                    host.onEvent(StreamPlayerEvent.SelectQuality(selection))
                    panel = TvPlayerPanel.CONTROLS
                },
                onLowLatency = { host.onEvent(StreamPlayerEvent.SetLowLatency(it)) }
            )
        }
        confirm?.let { request ->
            TvConfirmDialog(
                request = request,
                onDismiss = {
                    confirm = null
                    if (panel == TvPlayerPanel.CONFIRM) panel = TvPlayerPanel.CONTROLS
                }
            )
        }
    }
}

@Composable
private fun BoxScope.TvPlaybackStatus(
    state: StreamPlayerUiState,
    login: String,
    onRetry: () -> Unit
) {
    when {
        state.needsAttention -> TvMessage(
            title = if (state.phase == StreamPhase.OFFLINE) {
                stringResource(Res.string.player_offline, login)
            } else {
                StreamFormatting.errorMessage(state.errorKind)
            },
            onRetry = onRetry,
            modifier = Modifier.align(Alignment.Center)
        )

        state.isBusy -> MorphingLoadingIndicator(
            size = 64.dp,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}
