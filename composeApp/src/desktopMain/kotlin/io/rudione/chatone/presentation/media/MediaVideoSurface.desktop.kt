package io.rudione.chatone.presentation.media

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.rudione.chatone.domain.media.MediaPlayback
import io.rudione.chatone.presentation.stream.VideoFrameSink

@Composable
actual fun MediaVideoSurface(playback: MediaPlayback?, modifier: Modifier, frameSink: VideoFrameSink?) {
    Box(modifier)
}
