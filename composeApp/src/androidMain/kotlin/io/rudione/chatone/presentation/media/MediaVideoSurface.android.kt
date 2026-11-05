package io.rudione.chatone.presentation.media

import android.view.TextureView
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import io.rudione.chatone.domain.media.MediaPlayback

@Composable
actual fun MediaVideoSurface(playback: MediaPlayback?, modifier: Modifier) {
    val player = (playback as? AndroidMediaPlayback)?.player
    if (player == null) {
        Box(modifier)
        return
    }
    var view by remember { mutableStateOf<TextureView?>(null) }
    AndroidView(
        factory = { context -> TextureView(context).also { view = it } },
        update = { it.keepScreenOn = true },
        onRelease = { it.keepScreenOn = false },
        modifier = modifier
    )
    val texture = view
    DisposableEffect(player, texture) {
        if (texture != null) player.setVideoTextureView(texture)
        onDispose { if (texture != null) player.clearVideoTextureView(texture) }
    }
}
