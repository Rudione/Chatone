package io.rudione.chatone.presentation.stream

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import io.rudione.chatone.domain.stream.StreamPlaybackEngine

enum class VideoSurfaceKind { Direct, Composited }

@Stable
interface VideoFrameSink {
    val sampleWidth: Int
    val sampleHeight: Int
    val sampleIntervalMs: Long
    fun accept(argb: IntArray)
}

@Composable
expect fun PlatformVideoView(
    engine: StreamPlaybackEngine?,
    keepScreenOn: Boolean,
    modifier: Modifier = Modifier,
    surfaceKind: VideoSurfaceKind = VideoSurfaceKind.Direct,
    frameSink: VideoFrameSink? = null
)

@Composable
expect fun StreamImmersiveEffect(enabled: Boolean)

@Composable
expect fun StreamVisibilityEffect(onVisibilityChanged: (Boolean) -> Unit)

@Composable
expect fun StreamBackHandler(enabled: Boolean, onBack: () -> Unit)

@Stable
interface StreamOrientationController {
    fun enterLandscape()
    fun exitLandscape()
}

@Composable
expect fun rememberStreamOrientationController(active: Boolean): StreamOrientationController

@Stable
interface PictureInPictureController {
    val isSupported: Boolean
    val isActive: Boolean
    fun enter()
}

@Composable
expect fun rememberPictureInPictureController(): PictureInPictureController

@Immutable
data class PictureInPicturePlayback(val playing: Boolean, val muted: Boolean)

@Composable
expect fun PictureInPictureAutoEnterEffect(
    controller: PictureInPictureController,
    enabled: Boolean,
    aspectRatio: Float,
    playback: PictureInPicturePlayback,
    onTogglePlayback: () -> Unit,
    onToggleMute: () -> Unit
)
