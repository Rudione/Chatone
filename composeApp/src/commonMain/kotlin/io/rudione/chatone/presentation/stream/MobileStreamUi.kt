package io.rudione.chatone.presentation.stream

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.SideEffect
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import io.rudione.chatone.presentation.chat.ChatViewModel
import io.rudione.chatone.presentation.stream.ambient.StreamAmbience
import io.rudione.chatone.util.platform.DeviceFormFactor
import io.rudione.chatone.util.platform.currentFormFactor
import io.rudione.chatone.data.repository.StreamerModeController
import io.rudione.chatone.domain.stream.StreamMediaInfo
import io.rudione.chatone.presentation.theme.i18n.LocalStrings
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Stable
class StreamWindowControls internal constructor(
    val orientation: StreamOrientationController,
    val pictureInPicture: PictureInPictureController,
    val background: AppBackgroundController
) {
    fun leaveApp(intoPictureInPicture: Boolean) {
        if (intoPictureInPicture && pictureInPicture.enter()) return
        background.moveToBackground()
    }
}

@Immutable
class MobileStreamUi internal constructor(
    val host: StreamPlayerHost,
    val controls: StreamWindowControls,
    val liveInfo: StreamLiveInfo?,
    val playerVisible: Boolean,
    val landscape: Boolean,
    val floating: FloatingStreamPlayerHost,
    val ambience: StreamAmbience,
    private val playerContent: @Composable (Modifier, Boolean, StreamLiveInfo?, (@Composable BoxScope.() -> Unit)?) -> Unit
) {
    @Composable
    fun Player(
        modifier: Modifier,
        fullscreen: Boolean,
        overlay: (@Composable BoxScope.() -> Unit)? = null
    ) {
        if (inPictureInPicture || floating.isShowing) {
            Box(modifier.background(Color.Black))
        } else {
            playerContent(modifier, fullscreen, liveInfo, overlay)
        }
    }

    @Composable
    fun PictureInPicturePlayer() {
        StreamPictureInPictureView(host = host, modifier = Modifier.fillMaxSize())
    }

    val showLandscapeLayout: Boolean get() = playerVisible && landscape
    val showPortraitPlayer: Boolean get() = playerVisible && !landscape
    val inPictureInPicture: Boolean get() = playerVisible && controls.pictureInPicture.isActive
}

@Composable
fun rememberMobileStreamUi(
    host: StreamPlayerHost?,
    channelLogin: String?,
    landscapeWindow: Boolean,
    onNavigateBack: (() -> Unit)? = null
): MobileStreamUi? {
    if (host == null) return null
    val navigateBack by rememberUpdatedState(onNavigateBack)

    SideEffect(host, channelLogin) {
        host.onEvent(StreamPlayerEvent.FollowChannel(channelLogin))
    }

    val openChannel by remember(host) {
        host.state.map { it.channelLogin }.distinctUntilChanged()
    }.collectAsState(host.state.value.channelLogin)
    val playerVisible = openChannel != null && channelLogin != null
    val isTablet = remember { currentFormFactor() == DeviceFormFactor.TABLET }
    val theater = if (isTablet) rememberTheaterLayoutController(
        landscapeWindow,
        openChannel != null
    ) else null
    val orientation = theater ?: rememberStreamOrientationController(active = openChannel != null)
    val fullLayout = theater?.expanded ?: landscapeWindow
    val pictureInPicture = rememberPictureInPictureController()
    val background = rememberAppBackgroundController()
    val controls = remember(orientation, pictureInPicture, background) {
        StreamWindowControls(
            orientation,
            pictureInPicture,
            background
        )
    }
    val floating =
        remember(host, pictureInPicture) { FloatingStreamPlayerHost(host, pictureInPicture) }
    val ambience = rememberStreamAmbience(host)
    val liveInfo = rememberStreamLiveInfo(openChannel)
    val chatChannelLive = rememberStreamLiveInfo(channelLogin) != null
    val playerContent = remember(host, controls, ambience) {
        movableContentOf {
            playerModifier: Modifier,
            fullscreen: Boolean,
            info: StreamLiveInfo?,
            overlay: (@Composable BoxScope.() -> Unit)? ->
            StreamPlayerView(
                host = host,
                liveInfo = info,
                isFullscreen = fullscreen,
                pictureInPicture = controls.pictureInPicture,
                onToggleFullscreen = {
                    if (fullscreen)
                        controls.orientation.exitLandscape()
                    else controls.orientation.enterLandscape()
                },
                onBack = navigateBack,
                modifier = playerModifier,
                ambience = ambience,
                overlay = overlay
            )
        }
    }

    StreamMediaInfoEffect(host = host, channelLogin = openChannel)
    StreamWindowEffects(
        host = host,
        controls = controls,
        landscapeActive = playerVisible && fullLayout,
        chatChannelLogin = channelLogin,
        chatChannelLive = chatChannelLive,
        onNavigateBack = navigateBack
    )

    return MobileStreamUi(
        host = host,
        controls = controls,
        liveInfo = liveInfo,
        playerVisible = playerVisible,
        landscape = fullLayout,
        floating = floating,
        ambience = ambience,
        playerContent = playerContent
    )
}

@Composable
private fun rememberStreamAmbience(host: StreamPlayerHost): StreamAmbience {
    val ambience = remember(host) { StreamAmbience() }
    LaunchedEffect(ambience) { ambience.animate() }
    LaunchedEffect(host, ambience) {
        host.state
            .map { it.isOpen && !it.needsAttention }
            .distinctUntilChanged()
            .collect { showing -> if (!showing) ambience.fadeOut() }
    }
    return ambience
}

@Stable
private class TheaterLayoutController(expandedInitially: Boolean) : StreamOrientationController {
    var expanded by mutableStateOf(expandedInitially)

    override fun enterLandscape() {
        expanded = true
    }

    override fun exitLandscape() {
        expanded = false
    }
}

@Composable
private fun rememberTheaterLayoutController(
    landscapeWindow: Boolean,
    playerOpen: Boolean
): TheaterLayoutController {
    val controller = remember { TheaterLayoutController(landscapeWindow) }
    SideEffect(landscapeWindow, playerOpen) { controller.expanded = landscapeWindow }
    return controller
}

@Composable
private fun StreamWindowEffects(
    host: StreamPlayerHost,
    controls: StreamWindowControls,
    landscapeActive: Boolean,
    chatChannelLogin: String?,
    chatChannelLive: Boolean,
    onNavigateBack: (() -> Unit)?
) {
    val state by host.state.collectAsState()
    PictureInPictureAutoEnterEffect(
        controller = controls.pictureInPicture,
        enabled = state.isOpen && state.isPlaybackActive && state.preferences.autoPictureInPicture,
        aspectRatio = state.videoAspectRatio,
        playback = PictureInPicturePlayback(playing = state.isPlaybackActive, muted = state.isMuted),
        onTogglePlayback = { host.onEvent(StreamPlayerEvent.TogglePlayback) },
        onToggleMute = { host.onEvent(StreamPlayerEvent.ToggleMute) }
    )
    StreamVisibilityEffect { visible -> host.onEvent(StreamPlayerEvent.AppVisibilityChanged(visible)) }
    PictureInPictureDismissEffect(controls.pictureInPicture) {
        host.onEvent(StreamPlayerEvent.PictureInPictureDismissed)
    }
    StreamImmersiveEffect(enabled = state.isOpen && (landscapeActive || controls.pictureInPicture.isActive))
    val pictureInPictureOnLeave = state.isOpen && state.isPlaybackActive && state.preferences.autoPictureInPicture
    StreamBackHandler(enabled = state.isOpen && !state.settingsVisible) {
        when {
            landscapeActive -> controls.orientation.exitLandscape()
            onNavigateBack != null -> onNavigateBack()
            else -> controls.leaveApp(intoPictureInPicture = pictureInPictureOnLeave)
        }
    }
    SideEffect(host, chatChannelLogin, chatChannelLive) {
        if (chatChannelLive) host.onEvent(StreamPlayerEvent.StreamWentLive)
    }
}

@Composable
private fun StreamMediaInfoEffect(host: StreamPlayerHost, channelLogin: String?) {
    if (channelLogin == null) return
    val chatViewModel: ChatViewModel = koinViewModel()
    val streamerMode: StreamerModeController = koinInject()
    val strings = LocalStrings.current
    LaunchedEffect(host, chatViewModel, streamerMode, channelLogin, strings) {
        combine(chatViewModel.state, streamerMode.state) { chat, mode ->
            val live = chat.liveStream?.takeIf { chat.channelLogin.equals(channelLogin, ignoreCase = true) }
                ?: return@combine null
            val hideNames = mode.enabled && mode.options.hideChannelNames
            val hideThumbnail = mode.enabled && mode.options.hideThumbnails
            StreamMediaInfo(
                channelLogin = channelLogin,
                title = if (hideNames) strings.player.live else live.title,
                channelName = if (hideNames) APP_NAME else chat.channelDisplayName.ifBlank { live.userName },
                artworkUrl = live.thumbnailUrl
                    .takeUnless { hideThumbnail || it.isBlank() }
                    ?.replace("{width}", ARTWORK_WIDTH)
                    ?.replace("{height}", ARTWORK_HEIGHT)
            )
        }
            .filterNotNull()
            .distinctUntilChanged()
            .collect { host.onEvent(StreamPlayerEvent.SetMediaInfo(it)) }
    }
}

private const val APP_NAME = "Chatone"
private const val ARTWORK_WIDTH = "640"
private const val ARTWORK_HEIGHT = "360"

@Composable
private fun rememberStreamLiveInfo(channelLogin: String?): StreamLiveInfo? {
    val chatViewModel: ChatViewModel = koinViewModel()
    val liveInfoFlow = remember(chatViewModel, channelLogin) {
        chatViewModel.state
            .map { chat ->
                chat.liveStream
                    ?.takeIf {
                        channelLogin != null && chat.channelLogin.equals(
                            channelLogin,
                            ignoreCase = true
                        )
                    }
                    ?.let { StreamLiveInfo(viewerCount = it.viewerCount, startedAt = it.startedAt) }
            }
            .distinctUntilChanged()
    }
    val liveInfo by liveInfoFlow.collectAsState(null)
    return liveInfo
}
