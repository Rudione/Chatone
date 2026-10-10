package io.rudione.chatone.presentation.stream

import android.content.Context
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.SurfaceView
import android.view.TextureView
import android.view.View
import androidx.annotation.OptIn
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
import androidx.media3.common.util.UnstableApi
import io.rudione.chatone.domain.stream.StreamPlaybackEngine
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

@OptIn(UnstableApi::class)
@Composable
actual fun PlatformVideoView(
    engine: StreamPlaybackEngine?,
    keepScreenOn: Boolean,
    modifier: Modifier,
    surfaceKind: VideoSurfaceKind,
    frameSink: VideoFrameSink?
) {
    val androidEngine = engine as? AndroidStreamPlaybackEngine
    var output by remember { mutableStateOf<View?>(null) }

    AndroidView(
        factory = { context -> createVideoOutput(context, surfaceKind).also { output = it } },
        update = { view -> view.keepScreenOn = keepScreenOn },
        onRelease = { view -> view.keepScreenOn = false },
        modifier = modifier
    )

    val view = output
    DisposableEffect(androidEngine, view) {
        if (androidEngine != null && view != null) androidEngine.attachVideoOutput(view)
        onDispose {
            if (androidEngine != null && view != null) androidEngine.detachVideoOutput(view)
        }
    }

    val surfaceView = view as? SurfaceView
    if (surfaceView != null && frameSink != null) FrameSamplingEffect(surfaceView, frameSink)
}

private fun createVideoOutput(context: Context, kind: VideoSurfaceKind): View = when (kind) {
    VideoSurfaceKind.Direct -> SurfaceView(context)
    VideoSurfaceKind.Composited -> TextureView(context)
}

@Composable
private fun FrameSamplingEffect(view: SurfaceView, sink: VideoFrameSink) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(view, sink, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            val width = sink.sampleWidth
            val height = sink.sampleHeight
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val pixels = IntArray(width * height)
            while (true) {
                if (copyFrame(view, bitmap)) {
                    bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
                    sink.accept(pixels)
                }
                delay(sink.sampleIntervalMs)
            }
        }
    }
}

private suspend fun copyFrame(view: SurfaceView, bitmap: Bitmap): Boolean {
    val surface = view.holder.surface
    if (!view.isAttachedToWindow || view.width == 0 || view.height == 0 || surface == null || !surface.isValid) return false
    return suspendCancellableCoroutine { continuation ->
        runCatching {
            PixelCopy.request(
                view,
                bitmap,
                { result -> if (continuation.isActive) continuation.resume(result == PixelCopy.SUCCESS) },
                FrameCopyHandler
            )
        }.onFailure {
            if (continuation.isActive) continuation.resume(false)
        }
    }
}

private val FrameCopyHandler = Handler(Looper.getMainLooper())
