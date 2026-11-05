package io.rudione.chatone.presentation.media

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.rudione.chatone.domain.media.MediaPlayback

@Composable
expect fun MediaVideoSurface(playback: MediaPlayback?, modifier: Modifier = Modifier)
