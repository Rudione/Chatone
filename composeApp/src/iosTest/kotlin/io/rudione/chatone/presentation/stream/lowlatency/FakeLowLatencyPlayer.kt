package io.rudione.chatone.presentation.stream.lowlatency

import platform.AVKit.AVPictureInPictureController
import platform.UIKit.UIView

internal class FakeLowLatencyPlayer : LowLatencyPlayer {
    val calls = ArrayList<String>()
    var loadedUrl: String? = null
    var state = LowLatencyPlayerState.IDLE
    var latencyMs = 0L
    var bufferedMs = 0L
    var decodedFrames = 0L
    var failedFrames = 0L
    var qualityName: String? = null
    var videoWidth = 0
    var videoHeight = 0
    var closed = false
    private var listener: LowLatencyPlayerListener? = null

    override val view: UIView by lazy { UIView() }

    override val version: String = "test"

    override fun setListener(listener: LowLatencyPlayerListener?) {
        this.listener = listener
    }

    override fun load(url: String, lowLatency: Boolean) {
        loadedUrl = url
        calls += "load(lowLatency=$lowLatency)"
        state = LowLatencyPlayerState.IDLE
    }

    override fun play() {
        calls += "play"
    }

    override fun pause() {
        calls += "pause"
    }

    override fun setMuted(muted: Boolean) {
        calls += "muted($muted)"
    }

    override fun setPlaybackRate(rate: Float) {
        calls += "rate($rate)"
    }

    override fun selectQuality(name: String?, seamless: Boolean) {
        calls += "quality($name, seamless=$seamless)"
    }

    override fun limitVideoSize(width: Int, height: Int) = Unit

    override fun stats(): LowLatencyPlayerStats = LowLatencyPlayerStats(
        state = state,
        latencyMs = latencyMs,
        bufferedMs = bufferedMs,
        videoWidth = videoWidth,
        videoHeight = videoHeight,
        videoBitrate = 0L,
        bandwidthEstimate = 0L,
        decodedFrames = decodedFrames,
        droppedFrames = 0L,
        failedFrames = failedFrames,
        qualityName = qualityName,
        codecs = null,
        lowLatency = true
    )

    override fun pictureInPictureController(): AVPictureInPictureController? = null

    override fun sampleFrame(width: Int, height: Int): IntArray? = null

    override fun close() {
        closed = true
    }

    fun emit(next: LowLatencyPlayerState) {
        state = next
        listener?.onStateChanged(next)
    }

    fun startPlaying() {
        emit(LowLatencyPlayerState.READY)
        emit(LowLatencyPlayerState.BUFFERING)
        emit(LowLatencyPlayerState.PLAYING)
    }

    fun fail(status: Int, reason: String, source: String = "MediaPlaylist") {
        state = LowLatencyPlayerState.IDLE
        listener?.onError(LowLatencyPlayerError(status, reason, source))
    }

    fun rebuffer() {
        listener?.onRebuffer()
    }

    fun loseNetwork() {
        listener?.onNetworkUnavailable()
    }
}
