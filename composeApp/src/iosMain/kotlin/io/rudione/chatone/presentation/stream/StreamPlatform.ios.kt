package io.rudione.chatone.presentation.stream

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.rudione.chatone.domain.stream.StreamPlaybackEngine

@Composable
actual fun PlatformVideoView(engine: StreamPlaybackEngine?, keepScreenOn: Boolean, modifier: Modifier) {
    Box(modifier)
}

@Composable
actual fun StreamImmersiveEffect(enabled: Boolean) = Unit

@Composable
actual fun StreamVisibilityEffect(onVisibilityChanged: (Boolean) -> Unit) = Unit

@Composable
actual fun StreamBackHandler(enabled: Boolean, onBack: () -> Unit) = Unit

@Composable
actual fun rememberStreamOrientationController(active: Boolean): StreamOrientationController =
    NoOpStreamOrientationController

@Composable
actual fun rememberPictureInPictureController(): PictureInPictureController = UnsupportedPictureInPictureController

@Composable
actual fun PictureInPictureAutoEnterEffect(
    controller: PictureInPictureController,
    enabled: Boolean,
    aspectRatio: Float
) = Unit

private object NoOpStreamOrientationController : StreamOrientationController {
    override fun enterLandscape() = Unit
    override fun exitLandscape() = Unit
}

private object UnsupportedPictureInPictureController : PictureInPictureController {
    override val isSupported: Boolean = false
    override val isActive: Boolean = false
    override fun enter() = Unit
}
