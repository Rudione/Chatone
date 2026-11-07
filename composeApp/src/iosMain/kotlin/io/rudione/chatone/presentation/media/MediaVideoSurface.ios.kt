package io.rudione.chatone.presentation.media

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import io.rudione.chatone.domain.media.MediaPlayback
import io.rudione.chatone.presentation.stream.KeepScreenOnEffect
import io.rudione.chatone.presentation.stream.PlayerLayerView
import io.rudione.chatone.presentation.stream.VideoFrameSink

@Composable
actual fun MediaVideoSurface(playback: MediaPlayback?, modifier: Modifier, frameSink: VideoFrameSink?) {
    val player = (playback as? IosMediaPlayback)?.player
    if (player == null) {
        Box(modifier)
        return
    }
    KeepScreenOnEffect(true)
    UIKitView(
        factory = { PlayerLayerView() },
        modifier = modifier,
        update = { it.attach(player) },
        onRelease = { it.dispose() }
    )
}
