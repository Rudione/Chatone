package io.rudione.chatone.presentation.stream

import android.content.Context
import android.os.Handler
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.Renderer
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.video.MediaCodecVideoRenderer
import androidx.media3.exoplayer.video.VideoRendererEventListener

@UnstableApi
internal class StreamRenderersFactory(context: Context) : DefaultRenderersFactory(context) {

    override fun buildVideoRenderers(
        context: Context,
        extensionRendererMode: Int,
        mediaCodecSelector: MediaCodecSelector,
        enableDecoderFallback: Boolean,
        eventHandler: Handler,
        eventListener: VideoRendererEventListener,
        allowedVideoJoiningTimeMs: Long,
        out: ArrayList<Renderer>
    ) {
        val builder = MediaCodecVideoRenderer.Builder(context)
            .setCodecAdapterFactory(codecAdapterFactory)
            .setMediaCodecSelector(mediaCodecSelector)
            .setAllowedJoiningTimeMs(allowedVideoJoiningTimeMs)
            .setEnableDecoderFallback(enableDecoderFallback)
            .setEventHandler(eventHandler)
            .setEventListener(eventListener)
            .setMaxDroppedFramesToNotify(MAX_DROPPED_VIDEO_FRAME_COUNT_TO_NOTIFY)
        out.add(SurfaceSafeVideoRenderer(builder))
    }
}

@UnstableApi
private class SurfaceSafeVideoRenderer(builder: Builder) : MediaCodecVideoRenderer(builder) {
    override fun codecNeedsSetOutputSurfaceWorkaround(name: String): Boolean =
        VideoCodecQuirks.reopensOnSurfaceChange(name) || super.codecNeedsSetOutputSurfaceWorkaround(name)
}

internal object VideoCodecQuirks {
    private val unsafeSurfaceSwitchPrefixes = listOf("OMX.Exynos.")

    fun reopensOnSurfaceChange(codecName: String): Boolean =
        unsafeSurfaceSwitchPrefixes.any { codecName.startsWith(it, ignoreCase = true) }
}
