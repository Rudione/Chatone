package io.rudione.chatone.presentation.media

import kotlinx.cinterop.readValue
import io.rudione.chatone.domain.media.MediaPlayback
import io.rudione.chatone.domain.media.MediaPlaybackFactory
import io.rudione.chatone.domain.media.MediaSource
import io.rudione.chatone.domain.media.PlaybackPhase
import io.rudione.chatone.presentation.stream.IosPlaybackAudioSession
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerItem
import platform.AVFoundation.AVPlayerItemDidPlayToEndTimeNotification
import platform.AVFoundation.AVPlayerItemStatusFailed
import platform.AVFoundation.AVPlayerItemStatusReadyToPlay
import platform.AVFoundation.AVPlayerTimeControlStatusPlaying
import platform.AVFoundation.currentItem
import platform.AVFoundation.currentTime
import platform.AVFoundation.duration
import platform.AVFoundation.pause
import platform.AVFoundation.play
import platform.AVFoundation.rate
import platform.AVFoundation.replaceCurrentItemWithPlayerItem
import platform.AVFoundation.seekToTime
import platform.AVFoundation.timeControlStatus
import platform.AVFoundation.volume
import platform.CoreMedia.CMTimeGetSeconds
import platform.CoreMedia.CMTimeMake
import platform.CoreMedia.CMTimeMakeWithSeconds
import platform.CoreMedia.kCMTimeZero
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSURL
import platform.darwin.NSObjectProtocol

internal class IosMediaPlaybackFactory(private val audioSession: IosPlaybackAudioSession) : MediaPlaybackFactory {
    override val isSupported: Boolean = true

    override fun create(): MediaPlayback = IosMediaPlayback(audioSession)
}

@OptIn(ExperimentalForeignApi::class)
internal class IosMediaPlayback(private val audioSession: IosPlaybackAudioSession) : MediaPlayback {

    val player: AVPlayer = AVPlayer()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val _phase = MutableStateFlow(PlaybackPhase.IDLE)
    override val phase: StateFlow<PlaybackPhase> = _phase.asStateFlow()
    private val _playing = MutableStateFlow(false)
    override val playing: StateFlow<Boolean> = _playing.asStateFlow()
    private val _firstFrameShown = MutableStateFlow(false)
    override val firstFrameShown: StateFlow<Boolean> = _firstFrameShown.asStateFlow()

    private var wantsPlay = true
    private var looping = false
    private var speed = 1f
    private var ended = false
    private var endObserver: NSObjectProtocol? = null
    private var sessionHeld = false

    init {
        scope.launch {
            while (isActive) {
                refresh()
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    override fun load(source: MediaSource, startMs: Long) {
        val url = NSURL.URLWithString(source.url) ?: run {
            _phase.value = PlaybackPhase.ERROR
            return
        }
        if (!sessionHeld) {
            audioSession.activate()
            sessionHeld = true
        }
        val item = AVPlayerItem.playerItemWithURL(url)
        observeEnd(item)
        ended = false
        _firstFrameShown.value = false
        _phase.value = PlaybackPhase.LOADING
        player.replaceCurrentItemWithPlayerItem(item)
        if (startMs > 0) player.seekToTime(CMTimeMake(startMs, MS_TIMESCALE))
        if (wantsPlay) play()
    }

    override fun setPlaying(playing: Boolean) {
        wantsPlay = playing
        if (playing) {
            if (ended) {
                ended = false
                player.seekToTime(kCMTimeZero.readValue())
            }
            play()
        } else {
            player.pause()
        }
        refresh()
    }

    override fun seekTo(positionMs: Long) {
        ended = false
        player.seekToTime(CMTimeMake(positionMs.coerceAtLeast(0), MS_TIMESCALE), toleranceBefore = kCMTimeZero.readValue(), toleranceAfter = kCMTimeZero.readValue())
    }

    override fun scrubTo(positionMs: Long) {
        ended = false
        player.seekToTime(CMTimeMakeWithSeconds(positionMs.coerceAtLeast(0) / 1000.0, MS_TIMESCALE))
    }

    override fun setVolume(volume: Float) {
        player.volume = volume.coerceIn(0f, 1f)
    }

    override fun setSpeed(speed: Float) {
        this.speed = speed.coerceIn(MIN_SPEED, MAX_SPEED)
        if (player.timeControlStatus == AVPlayerTimeControlStatusPlaying) player.rate = this.speed
    }

    override fun setLooping(looping: Boolean) {
        this.looping = looping
    }

    override fun positionMs(): Long = seconds(CMTimeGetSeconds(player.currentTime()))

    override fun durationMs(): Long = player.currentItem?.let { seconds(CMTimeGetSeconds(it.duration)) } ?: 0L

    override fun release() {
        scope.cancel()
        endObserver?.let(NSNotificationCenter.defaultCenter::removeObserver)
        endObserver = null
        player.pause()
        player.replaceCurrentItemWithPlayerItem(null)
        if (sessionHeld) audioSession.deactivate()
        sessionHeld = false
    }

    private fun play() {
        player.play()
        player.rate = speed
    }

    private fun observeEnd(item: AVPlayerItem) {
        endObserver?.let(NSNotificationCenter.defaultCenter::removeObserver)
        endObserver = NSNotificationCenter.defaultCenter.addObserverForName(
            AVPlayerItemDidPlayToEndTimeNotification,
            item,
            NSOperationQueue.mainQueue
        ) { _ ->
            if (looping && wantsPlay) {
                player.seekToTime(kCMTimeZero.readValue())
                play()
            } else {
                ended = true
                refresh()
            }
        }
    }

    private fun refresh() {
        val item = player.currentItem
        val isPlaying = player.timeControlStatus == AVPlayerTimeControlStatusPlaying
        _playing.value = isPlaying
        if (isPlaying && positionMs() > 0) _firstFrameShown.value = true
        _phase.value = when {
            item == null -> PlaybackPhase.IDLE
            item.status == AVPlayerItemStatusFailed -> PlaybackPhase.ERROR
            ended -> PlaybackPhase.ENDED
            item.status == AVPlayerItemStatusReadyToPlay && (isPlaying || !wantsPlay) -> PlaybackPhase.READY
            else -> PlaybackPhase.LOADING
        }
    }

    private fun seconds(value: Double): Long = if (value.isFinite() && value > 0) (value * 1000).toLong() else 0L

    private companion object {
        const val POLL_INTERVAL_MS = 200L
        const val MS_TIMESCALE = 1000
        const val MIN_SPEED = 0.25f
        const val MAX_SPEED = 2f
    }
}
