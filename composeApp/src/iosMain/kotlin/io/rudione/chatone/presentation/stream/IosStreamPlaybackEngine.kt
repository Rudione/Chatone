package io.rudione.chatone.presentation.stream

import platform.AVFoundation.resourceLoader
import platform.AVFoundation.preferredForwardBufferDuration
import platform.AVFoundation.configuredTimeOffsetFromLive
import platform.AVFoundation.automaticallyPreservesTimeOffsetFromLive
import platform.AVFoundation.muted
import platform.AVFoundation.preferredMaximumResolution
import platform.AVFoundation.AVAudioTimePitchAlgorithmTimeDomain
import platform.AVFoundation.audioTimePitchAlgorithm
import platform.AVFoundation.rate
import platform.AVFoundation.setRate
import platform.CoreGraphics.CGSizeMake
import platform.UIKit.UIScreen
import platform.AVFoundation.currentDate
import io.rudione.chatone.data.remote.stream.LiveStartHint
import io.rudione.chatone.data.remote.stream.TwitchAdBreakFilter
import io.rudione.chatone.data.remote.stream.TwitchHlsPlaylist
import io.rudione.chatone.domain.stream.PlaybackMetrics
import io.rudione.chatone.domain.stream.PlaybackStatus
import io.rudione.chatone.domain.stream.StreamAdBreakSource
import io.rudione.chatone.domain.stream.StreamErrorKind
import io.rudione.chatone.domain.stream.StreamManifest
import io.rudione.chatone.domain.stream.StreamMediaInfo
import io.rudione.chatone.domain.stream.StreamPhase
import io.rudione.chatone.domain.stream.StreamPlaybackEngine
import io.rudione.chatone.domain.stream.StreamPlaybackEngineFactory
import io.rudione.chatone.domain.stream.StreamQualitySelection
import io.rudione.chatone.domain.stream.StreamVariant
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import platform.AVFoundation.AVErrorDecodeFailed
import platform.AVFoundation.AVErrorDecoderNotFound
import platform.AVFoundation.AVErrorFormatUnsupported
import platform.AVFoundation.AVFoundationErrorDomain
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerItem
import platform.AVFoundation.AVPlayerItemAccessLogEvent
import platform.AVFoundation.AVPlayerItemDidPlayToEndTimeNotification
import platform.AVFoundation.AVPlayerItemStatusFailed
import platform.AVFoundation.AVPlayerTimeControlStatusPlaying
import platform.AVFoundation.AVPlayerTimeControlStatusWaitingToPlayAtSpecifiedRate
import platform.AVFoundation.AVURLAsset
import platform.AVFoundation.accessLog
import platform.AVFoundation.currentItem
import platform.AVFoundation.currentTime
import platform.AVFoundation.loadedTimeRanges
import platform.AVFoundation.pause
import platform.AVFoundation.play
import platform.AVFoundation.presentationSize
import platform.AVFoundation.replaceCurrentItemWithPlayerItem
import platform.AVFoundation.seekToTime
import platform.AVFoundation.seekableTimeRanges
import platform.AVFoundation.timeControlStatus
import platform.CoreMedia.CMTimeGetSeconds
import platform.CoreMedia.CMTimeMakeWithSeconds
import platform.CoreMedia.CMTimeRangeGetEnd
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSURLErrorDomain
import platform.Foundation.NSValue
import platform.Foundation.timeIntervalSince1970
import platform.darwin.NSObjectProtocol
import platform.darwin.dispatch_get_main_queue
import platform.AVFoundation.CMTimeRangeValue
import kotlin.math.abs
import kotlin.time.Clock

internal class IosStreamPlaybackEngineFactory(
    private val adBreakSource: StreamAdBreakSource,
    private val audioSession: IosPlaybackAudioSession
) : StreamPlaybackEngineFactory {
    override val isSupported: Boolean = true

    override val supportedCodecs: String = SUPPORTED_CODECS

    override fun create(): StreamPlaybackEngine = IosStreamPlaybackEngine(adBreakSource, SUPPORTED_CODECS, audioSession)

    private companion object {
        const val SUPPORTED_CODECS = "h264"
    }
}

@OptIn(ExperimentalForeignApi::class)
internal class IosStreamPlaybackEngine(
    private val adBreakSource: StreamAdBreakSource,
    private val supportedCodecs: String,
    private val audioSession: IosPlaybackAudioSession
) : StreamPlaybackEngine {

    val player: AVPlayer = AVPlayer()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val _status = MutableStateFlow(PlaybackStatus())
    override val status: StateFlow<PlaybackStatus> = _status.asStateFlow()

    private var manifest: StreamManifest? = null
    private var selection: StreamQualitySelection = StreamQualitySelection.Auto
    private var lowLatency = true
    private var wantsPlay = true
    private var ended = false
    private var mediaHttpStatus = 0
    private var loader: HlsPlaylistLoader? = null
    private var poller: Job? = null
    private var released = false
    private var endObserver: NSObjectProtocol? = null
    private var remoteControls: IosRemoteControls? = null
    private val frameSampler = VideoFrameSampler()
    private val rateControl = IosLiveRateControl()
    private var wasPlaying = false
    private var pausedAtMs: Long? = null
    private var lastPositionSeconds = Double.NaN
    private var positionStuckSinceMs: Long? = null
    private var lastFreezeReloadMs = Long.MIN_VALUE

    override fun load(manifest: StreamManifest, selection: StreamQualitySelection, lowLatency: Boolean) {
        if (released) return
        this.manifest = manifest
        this.selection = selection
        this.lowLatency = lowLatency
        ended = false
        mediaHttpStatus = 0
        audioSession.activate()
        loader?.close()
        val filter = TwitchAdBreakFilter(manifest, supportedCodecs, adBreakSource)
        val startLatencyMs = targetLatencyMs()
        if (lowLatency) rateControl.enable(CATCH_UP_TARGET_MS) else rateControl.disable()
        wasPlaying = false
        pausedAtMs = null
        lastPositionSeconds = Double.NaN
        positionStuckSinceMs = null
        val playlistLoader = HlsPlaylistLoader(
            masterPlaylist = manifest.playlist,
            transform = { url, playlist ->
                val clean = if (filter.needsFilter(url, playlist)) filter.filter(url, playlist) else playlist
                val hint = LiveStartHint(nowMs() + manifest.clockOffsetMs, startLatencyMs)
                TwitchHlsPlaylist.rewriteMediaPlaylist(clean, expandPrefetch = lowLatency, liveStart = hint) ?: clean
            },
            onMediaStatus = { status -> mediaHttpStatus = status }
        )
        loader = playlistLoader
        val url = sourceVariant()?.url?.let(playlistLoader::mediaUrl) ?: playlistLoader.masterUrl
        val asset = AVURLAsset.URLAssetWithURL(url, options = null)
        asset.resourceLoader.setDelegate(playlistLoader, queue = dispatch_get_main_queue())
        val item = AVPlayerItem.playerItemWithAsset(asset).apply {
            preferredForwardBufferDuration = if (lowLatency) LOW_LATENCY_BUFFER_SECONDS else NORMAL_BUFFER_SECONDS
            if (!lowLatency) configuredTimeOffsetFromLive = CMTimeMakeWithSeconds(startLatencyMs / 1000.0, TIMESCALE)
            automaticallyPreservesTimeOffsetFromLive = true
            audioTimePitchAlgorithm = AVAudioTimePitchAlgorithmTimeDomain
            preferredMaximumResolution = screenVideoCap()
        }
        observeEnd(item)
        frameSampler.attach(item)
        player.replaceCurrentItemWithPlayerItem(item)
        wantsPlay = true
        player.play()
        ensurePolling()
        publish()
    }

    override fun selectQuality(selection: StreamQualitySelection) {
        val current = manifest ?: return
        if (selection == this.selection) return
        load(current, selection, lowLatency)
    }

    override fun setPlaying(playing: Boolean) {
        if (released) return
        wantsPlay = playing
        if (playing) {
            val fellBehind = lowLatency && pausedAtMs?.let { nowMs() - it > RESUME_RELOAD_MS } == true
            pausedAtMs = null
            if (ended || fellBehind) manifest?.let { load(it, selection, lowLatency) } else player.play()
        } else {
            pausedAtMs = pausedAtMs ?: nowMs()
            player.pause()
        }
        publish()
    }

    override fun setMuted(muted: Boolean) {
        if (released) return
        player.muted = muted
        publish()
    }

    override fun seekToLive() {
        if (released) return
        val item = player.currentItem ?: return
        if (lowLatency) {
            val latency = latencyMs(item, nowMs()) ?: return
            if (latency > CATCH_UP_TARGET_MS + LIVE_EDGE_TOLERANCE_SECONDS * 1000) manifest?.let { load(it, selection, true) }
            return
        }
        val range = (item.seekableTimeRanges.lastOrNull() as? NSValue)?.CMTimeRangeValue ?: return
        val end = CMTimeGetSeconds(CMTimeRangeGetEnd(range))
        if (!end.isFinite() || CMTimeGetSeconds(player.currentTime()) >= end - LIVE_EDGE_TOLERANCE_SECONDS) return
        player.seekToTime(CMTimeMakeWithSeconds(end, TIMESCALE))
    }

    fun sampleFrame(width: Int, height: Int): IntArray? =
        if (released || selection == StreamQualitySelection.AudioOnly) null else frameSampler.sample(width, height)

    override fun setMediaInfo(info: StreamMediaInfo) {
        if (released) return
        val controls = remoteControls ?: IosRemoteControls(
            onPlay = { setPlaying(true) },
            onPause = { setPlaying(false) }
        ).also { remoteControls = it }
        controls.show(info)
    }

    override fun metrics(): PlaybackMetrics? {
        if (released || manifest == null) return null
        val item = player.currentItem ?: return null
        val size = item.presentationSize.useContents { width.toInt() to height.toInt() }
        val event = item.accessLog()?.events?.lastOrNull() as? AVPlayerItemAccessLogEvent
        return PlaybackMetrics(
            downloadWidth = size.first,
            downloadHeight = size.second,
            renderWidth = size.first,
            renderHeight = size.second,
            downloadBitrateKbps = event?.indicatedBitrate?.takeIf { it > 0 }?.let { (it / 1000).toInt() },
            bandwidthEstimateKbps = event?.observedBitrate?.takeIf { it > 0 }?.let { (it / 1000).toInt() },
            framesPerSecond = null,
            droppedFrames = event?.numberOfDroppedVideoFrames?.toInt()?.coerceAtLeast(0) ?: 0,
            bufferMs = bufferedMs(item),
            playbackWallClockMs = item.currentDate()?.let { (it.timeIntervalSince1970 * 1000).toLong() },
            codecs = activeVariant()?.codecs?.ifEmpty { null } ?: NOT_AVAILABLE,
            lowLatencyActive = lowLatency,
            renderSurface = RENDER_SURFACE,
            backendVersion = BACKEND
        )
    }

    override fun release() {
        if (released) return
        released = true
        poller?.cancel()
        scope.cancel()
        endObserver?.let(NSNotificationCenter.defaultCenter::removeObserver)
        endObserver = null
        remoteControls?.clear()
        remoteControls = null
        player.pause()
        player.replaceCurrentItemWithPlayerItem(null)
        loader?.close()
        loader = null
        manifest = null
        audioSession.deactivate()
        _status.value = PlaybackStatus()
    }

    private fun observeEnd(item: AVPlayerItem) {
        endObserver?.let(NSNotificationCenter.defaultCenter::removeObserver)
        endObserver = NSNotificationCenter.defaultCenter.addObserverForName(
            AVPlayerItemDidPlayToEndTimeNotification,
            item,
            NSOperationQueue.mainQueue
        ) { _ ->
            ended = true
            publish()
        }
    }

    private fun ensurePolling() {
        if (poller?.isActive == true) return
        poller = scope.launch {
            while (isActive) {
                publish()
                steerTowardsLive()
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    private fun steerTowardsLive() {
        val item = player.currentItem ?: return
        val phase = _status.value.phase
        val now = nowMs()
        if (phase == StreamPhase.BUFFERING && wasPlaying && wantsPlay) rateControl.onRebuffer(now)
        wasPlaying = phase == StreamPhase.PLAYING
        if (phase != StreamPhase.PLAYING || !wantsPlay) {
            positionStuckSinceMs = null
            return
        }
        if (isFrozen(now)) {
            lastFreezeReloadMs = now
            manifest?.let { load(it, selection, lowLatency) }
            return
        }
        val latency = latencyMs(item, now) ?: return
        if (rateControl.shouldReload(latency, now)) {
            manifest?.let { load(it, selection, lowLatency) }
            return
        }
        val rate = rateControl.rate(latency, bufferedMs(item), now)
        if (abs(player.rate - rate) > RATE_EPSILON) player.rate = rate
    }

    private fun isFrozen(now: Long): Boolean {
        val position = CMTimeGetSeconds(player.currentTime())
        val moved = !lastPositionSeconds.isFinite() || abs(position - lastPositionSeconds) > FROZEN_EPSILON_SECONDS
        lastPositionSeconds = position
        if (moved) {
            positionStuckSinceMs = null
            return false
        }
        val since = positionStuckSinceMs ?: now.also { positionStuckSinceMs = it }
        return now - since >= FREEZE_RELOAD_MS && now - lastFreezeReloadMs >= FREEZE_RELOAD_COOLDOWN_MS
    }

    private fun latencyMs(item: AVPlayerItem, now: Long): Long? {
        val wallClock = item.currentDate() ?: return null
        return now + (manifest?.clockOffsetMs ?: 0L) - (wallClock.timeIntervalSince1970 * 1000).toLong()
    }

    private fun publish() {
        if (released) return
        val item = player.currentItem
        val phase = when {
            ended || mediaHttpStatus in ENDED_STATUSES -> StreamPhase.ENDED
            item == null -> StreamPhase.IDLE
            item.status == AVPlayerItemStatusFailed || item.error != null -> StreamPhase.ERROR
            !wantsPlay -> StreamPhase.PAUSED
            player.timeControlStatus == AVPlayerTimeControlStatusPlaying -> StreamPhase.PLAYING
            player.timeControlStatus == AVPlayerTimeControlStatusWaitingToPlayAtSpecifiedRate -> StreamPhase.BUFFERING
            else -> StreamPhase.BUFFERING
        }
        val size = item?.presentationSize?.useContents { width.toInt() to height.toInt() } ?: (0 to 0)
        val event = item?.accessLog()?.events?.lastOrNull() as? AVPlayerItemAccessLogEvent
        _status.value = PlaybackStatus(
            phase = phase,
            errorKind = if (phase == StreamPhase.ERROR) errorKind(item) else null,
            isMuted = player.muted,
            videoWidth = size.first,
            videoHeight = size.second,
            activeBandwidth = event?.indicatedBitrate?.takeIf { it > 0 }?.toLong(),
            activeHeight = size.second.takeIf { it > 0 },
            audioOnly = selection == StreamQualitySelection.AudioOnly
        )
    }

    private fun errorKind(item: AVPlayerItem?): StreamErrorKind {
        if (mediaHttpStatus == HTTP_FORBIDDEN) return StreamErrorKind.UNKNOWN
        val error = item?.error ?: return StreamErrorKind.UNKNOWN
        return when {
            error.domain == NSURLErrorDomain -> StreamErrorKind.NETWORK
            error.domain == AVFoundationErrorDomain && error.code in CODEC_ERRORS -> StreamErrorKind.UNSUPPORTED_CODEC
            else -> StreamErrorKind.UNKNOWN
        }
    }

    private fun bufferedMs(item: AVPlayerItem): Long {
        val now = CMTimeGetSeconds(player.currentTime())
        val end = item.loadedTimeRanges.mapNotNull { (it as? NSValue)?.CMTimeRangeValue }
            .map { CMTimeGetSeconds(CMTimeRangeGetEnd(it)) }
            .filter { it.isFinite() }
            .maxOrNull() ?: return 0L
        return ((end - now) * 1000).toLong().coerceAtLeast(0L)
    }

    private fun sourceVariant(): StreamVariant? = when (val current = selection) {
        StreamQualitySelection.Auto -> null
        StreamQualitySelection.AudioOnly -> manifest?.audioOnlyVariant
        is StreamQualitySelection.Fixed -> manifest?.variants?.firstOrNull { it.groupId == current.groupId && !it.isAudioOnly }
    }

    private fun activeVariant(): StreamVariant? {
        sourceVariant()?.let { return it }
        val height = player.currentItem?.presentationSize?.useContents { height.toInt() } ?: return null
        return manifest?.videoVariants?.firstOrNull { it.height == height }
    }

    private fun targetLatencyMs(): Long = if (lowLatency) LOW_LATENCY_TARGET_MS else NORMAL_LATENCY_TARGET_MS

    private fun nowMs(): Long = Clock.System.now().toEpochMilliseconds()

    private companion object {
        const val POLL_INTERVAL_MS = 250L
        const val TIMESCALE = 600
        const val LOW_LATENCY_TARGET_MS = 4_000L
        const val CATCH_UP_TARGET_MS = 5_000L
        const val RATE_EPSILON = 0.001f
        const val RESUME_RELOAD_MS = 3_000L
        const val FREEZE_RELOAD_MS = 2_500L
        const val FREEZE_RELOAD_COOLDOWN_MS = 15_000L
        const val FROZEN_EPSILON_SECONDS = 0.01
        const val NORMAL_LATENCY_TARGET_MS = 10_000L
        const val LIVE_EDGE_TOLERANCE_SECONDS = 1.0
        const val LOW_LATENCY_BUFFER_SECONDS = 2.0
        const val NORMAL_BUFFER_SECONDS = 6.0
        const val HTTP_FORBIDDEN = 403
        val ENDED_STATUSES = setOf(404, 410)
        val CODEC_ERRORS = setOf(AVErrorDecodeFailed, AVErrorDecoderNotFound, AVErrorFormatUnsupported)
        const val RENDER_SURFACE = "AVPlayerLayer"
        const val BACKEND = "AVFoundation"
        const val NOT_AVAILABLE = "—"
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun screenVideoCap() = UIScreen.mainScreen.nativeBounds.useContents {
    CGSizeMake(maxOf(size.width, size.height), minOf(size.width, size.height))
}
