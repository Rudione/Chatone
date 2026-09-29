package io.rudione.chatone.presentation.stream

import android.os.SystemClock
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DecoderCounters
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.source.LoadEventInfo
import androidx.media3.exoplayer.source.MediaLoadData
import kotlin.math.roundToInt

@UnstableApi
internal class StreamMetricsCollector(
    private val onVideoFormatChanged: () -> Unit
) : AnalyticsListener {

    @Volatile
    var droppedFrames: Int = 0
        private set

    @Volatile
    var downloadBitrateKbps: Int? = null
        private set

    private var smoothedKbps = 0.0
    private var lastRenderedFrames = -1
    private var lastFrameSampleMs = 0L
    private var framesPerSecond: Float? = null

    fun reset() {
        droppedFrames = 0
        downloadBitrateKbps = null
        smoothedKbps = 0.0
        lastRenderedFrames = -1
        lastFrameSampleMs = 0L
        framesPerSecond = null
    }

    fun sampleFramesPerSecond(counters: DecoderCounters?): Float? {
        if (counters == null) return null
        counters.ensureUpdated()
        val rendered = counters.renderedOutputBufferCount
        val now = SystemClock.elapsedRealtime()
        if (lastRenderedFrames in 0..rendered && lastFrameSampleMs > 0L) {
            val elapsed = now - lastFrameSampleMs
            if (elapsed >= MIN_FPS_WINDOW_MS) {
                framesPerSecond = ((rendered - lastRenderedFrames) * 1000f / elapsed * 10f).roundToInt() / 10f
                lastRenderedFrames = rendered
                lastFrameSampleMs = now
            }
        } else {
            lastRenderedFrames = rendered
            lastFrameSampleMs = now
        }
        return framesPerSecond
    }

    override fun onDroppedVideoFrames(eventTime: AnalyticsListener.EventTime, droppedFrames: Int, elapsedMs: Long) {
        this.droppedFrames += droppedFrames.coerceAtLeast(0)
    }

    override fun onLoadCompleted(
        eventTime: AnalyticsListener.EventTime,
        loadEventInfo: LoadEventInfo,
        mediaLoadData: MediaLoadData
    ) {
        if (mediaLoadData.dataType != C.DATA_TYPE_MEDIA) return
        if (mediaLoadData.mediaStartTimeMs == C.TIME_UNSET || mediaLoadData.mediaEndTimeMs == C.TIME_UNSET) return
        val durationMs = mediaLoadData.mediaEndTimeMs - mediaLoadData.mediaStartTimeMs
        if (durationMs <= 0L || loadEventInfo.bytesLoaded <= 0L) return
        val kbps = loadEventInfo.bytesLoaded * 8.0 / durationMs
        smoothedKbps = if (smoothedKbps <= 0.0) kbps else smoothedKbps * (1 - SMOOTHING) + kbps * SMOOTHING
        downloadBitrateKbps = smoothedKbps.roundToInt()
    }

    override fun onVideoInputFormatChanged(
        eventTime: AnalyticsListener.EventTime,
        format: Format,
        decoderReuseEvaluation: androidx.media3.exoplayer.DecoderReuseEvaluation?
    ) {
        onVideoFormatChanged()
    }

    private companion object {
        const val SMOOTHING = 0.35
        const val MIN_FPS_WINDOW_MS = 500L
    }
}
