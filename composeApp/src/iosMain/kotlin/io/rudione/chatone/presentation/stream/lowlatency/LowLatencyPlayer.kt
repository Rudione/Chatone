package io.rudione.chatone.presentation.stream.lowlatency

import platform.AVKit.AVPictureInPictureController
import platform.UIKit.UIView

enum class LowLatencyPlayerState { IDLE, READY, BUFFERING, PLAYING, ENDED }

class LowLatencyPlayerStats(
    val state: LowLatencyPlayerState,
    val latencyMs: Long,
    val bufferedMs: Long,
    val videoWidth: Int,
    val videoHeight: Int,
    val videoBitrate: Long,
    val bandwidthEstimate: Long,
    val decodedFrames: Long,
    val droppedFrames: Long,
    val failedFrames: Long,
    val qualityName: String?,
    val codecs: String?,
    val lowLatency: Boolean
)

class LowLatencyPlayerError(
    val status: Int,
    val reason: String,
    val source: String
)

interface LowLatencyPlayerListener {
    fun onStateChanged(state: LowLatencyPlayerState)
    fun onPlaybackChanged()
    fun onRebuffer()
    fun onNetworkUnavailable()
    fun onError(error: LowLatencyPlayerError)
}

interface LowLatencyPlayer {
    val view: UIView
    val version: String
    fun setListener(listener: LowLatencyPlayerListener?)
    fun load(url: String, lowLatency: Boolean)
    fun play()
    fun pause()
    fun setMuted(muted: Boolean)
    fun setPlaybackRate(rate: Float)
    fun selectQuality(name: String?, seamless: Boolean)
    fun limitVideoSize(width: Int, height: Int)
    fun stats(): LowLatencyPlayerStats
    fun pictureInPictureController(): AVPictureInPictureController?
    fun sampleFrame(width: Int, height: Int): IntArray?
    fun close()
}

interface LowLatencyPlayerProvider {
    fun createPlayer(): LowLatencyPlayer
}
