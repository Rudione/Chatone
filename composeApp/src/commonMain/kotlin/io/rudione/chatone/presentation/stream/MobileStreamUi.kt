package io.rudione.chatone.presentation.stream

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import io.rudione.chatone.presentation.chat.ChatViewModel
import io.rudione.chatone.util.platform.DeviceFormFactor
import io.rudione.chatone.util.platform.currentFormFactor
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.koin.compose.viewmodel.koinViewModel

@Stable
class StreamWindowControls internal constructor(
    val orientation: StreamOrientationController,
    val pictureInPicture: PictureInPictureController
)

@Immutable
class MobileStreamUi internal constructor(
    val host: StreamPlayerHost,
    val controls: StreamWindowControls,
    val liveInfo: StreamLiveInfo?,
    val playerVisible: Boolean,
    val landscape: Boolean,
    private val playerContent: @Composable (Modifier, Boolean, StreamLiveInfo?, (@Composable BoxScope.() -> Unit)?) -> Unit
) {
    @Composable
    fun Player(modifier: Modifier, fullscreen: Boolean, overlay: (@Composable BoxScope.() -> Unit)? = null) {
        if (inPictureInPicture) {
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
    landscapeWindow: Boolean
): MobileStreamUi? {
    if (host == null) return null

    LaunchedEffect(host, channelLogin) {
        host.onEvent(StreamPlayerEvent.FollowChannel(channelLogin))
    }

    val openChannel by remember(host) {
        host.state.map { it.channelLogin }.distinctUntilChanged()
    }.collectAsState(host.state.value.channelLogin)
    val playerVisible = openChannel != null && channelLogin != null
    val isTablet = remember { currentFormFactor() == DeviceFormFactor.TABLET }
    val theater = if (isTablet) rememberTheaterLayoutController(landscapeWindow, openChannel != null) else null
    val orientation = theater ?: rememberStreamOrientationController(active = openChannel != null)
    val fullLayout = theater?.expanded ?: landscapeWindow
    val pictureInPicture = rememberPictureInPictureController()
    val controls = remember(orientation, pictureInPicture) { StreamWindowControls(orientation, pictureInPicture) }
    val liveInfo = rememberStreamLiveInfo(openChannel)
    val chatChannelLive = rememberStreamLiveInfo(channelLogin) != null
    val playerContent = remember(host, controls) {
        movableContentOf { playerModifier: Modifier, fullscreen: Boolean, info: StreamLiveInfo?, overlay: (@Composable BoxScope.() -> Unit)? ->
            StreamPlayerView(
                host = host,
                liveInfo = info,
                isFullscreen = fullscreen,
                pictureInPicture = controls.pictureInPicture,
                onToggleFullscreen = {
                    if (fullscreen) controls.orientation.exitLandscape() else controls.orientation.enterLandscape()
                },
                modifier = playerModifier,
                overlay = overlay
            )
        }
    }

    StreamWindowEffects(
        host = host,
        controls = controls,
        landscapeActive = playerVisible && fullLayout,
        chatChannelLogin = channelLogin,
        chatChannelLive = chatChannelLive
    )

    return MobileStreamUi(
        host = host,
        controls = controls,
        liveInfo = liveInfo,
        playerVisible = playerVisible,
        landscape = fullLayout,
        playerContent = playerContent
    )
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
private fun rememberTheaterLayoutController(landscapeWindow: Boolean, playerOpen: Boolean): TheaterLayoutController {
    val controller = remember { TheaterLayoutController(landscapeWindow) }
    LaunchedEffect(landscapeWindow, playerOpen) { controller.expanded = landscapeWindow }
    return controller
}

@Composable
private fun StreamWindowEffects(
    host: StreamPlayerHost,
    controls: StreamWindowControls,
    landscapeActive: Boolean,
    chatChannelLogin: String?,
    chatChannelLive: Boolean
) {
    val state by host.state.collectAsState()
    PictureInPictureAutoEnterEffect(
        controller = controls.pictureInPicture,
        enabled = state.isOpen && state.isPlaybackActive && state.preferences.autoPictureInPicture,
        aspectRatio = state.videoAspectRatio
    )
    StreamVisibilityEffect { visible -> host.onEvent(StreamPlayerEvent.AppVisibilityChanged(visible)) }
    StreamImmersiveEffect(enabled = state.isOpen && (landscapeActive || controls.pictureInPicture.isActive))
    StreamBackHandler(enabled = state.isOpen && !state.settingsVisible) {
        if (landscapeActive) controls.orientation.exitLandscape() else host.onEvent(StreamPlayerEvent.Dismiss)
    }
    LaunchedEffect(host, chatChannelLogin, chatChannelLive) {
        if (chatChannelLive) host.onEvent(StreamPlayerEvent.StreamWentLive)
    }
}

@Composable
private fun rememberStreamLiveInfo(channelLogin: String?): StreamLiveInfo? {
    val chatViewModel: ChatViewModel = koinViewModel()
    val liveInfoFlow = remember(chatViewModel, channelLogin) {
        chatViewModel.state
            .map { chat ->
                chat.liveStream
                    ?.takeIf { channelLogin != null && chat.channelLogin.equals(channelLogin, ignoreCase = true) }
                    ?.let { StreamLiveInfo(viewerCount = it.viewerCount, startedAt = it.startedAt) }
            }
            .distinctUntilChanged()
    }
    val liveInfo by liveInfoFlow.collectAsState(null)
    return liveInfo
}
