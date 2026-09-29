package io.rudione.chatone.presentation.stream

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import io.rudione.chatone.domain.stream.StreamPlaybackEngine

@Composable
expect fun PlatformVideoView(
    engine: StreamPlaybackEngine?,
    keepScreenOn: Boolean,
    modifier: Modifier = Modifier
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

@Composable
expect fun PictureInPictureAutoEnterEffect(
    controller: PictureInPictureController,
    enabled: Boolean,
    aspectRatio: Float
)
