package io.rudione.chatone.presentation.media

import android.graphics.Bitmap
import android.view.TextureView
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import io.rudione.chatone.domain.media.MediaPlayback
import io.rudione.chatone.presentation.stream.VideoFrameSink
import kotlinx.coroutines.delay

@Composable
actual fun MediaVideoSurface(playback: MediaPlayback?, modifier: Modifier, frameSink: VideoFrameSink?) {
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
    if (texture != null && frameSink != null) TextureSamplingEffect(texture, frameSink)
}

@Composable
private fun TextureSamplingEffect(view: TextureView, sink: VideoFrameSink) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(view, sink, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            val width = sink.sampleWidth
            val height = sink.sampleHeight
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val pixels = IntArray(width * height)
            try {
                while (true) {
                    if (view.isAvailable && view.width > 0 && view.height > 0) {
                        view.getBitmap(bitmap)
                        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
                        sink.accept(pixels)
                    }
                    delay(sink.sampleIntervalMs)
                }
            } finally {
                bitmap.recycle()
            }
        }
    }
}
