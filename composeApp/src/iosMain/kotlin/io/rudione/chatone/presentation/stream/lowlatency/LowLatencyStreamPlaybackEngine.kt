package io.rudione.chatone.presentation.stream.lowlatency

import io.github.aakira.napier.Napier
import io.rudione.chatone.data.remote.stream.TwitchAdBreakFilter
import io.rudione.chatone.domain.stream.FrameSamplingBudget
import io.rudione.chatone.domain.stream.PlaybackMetrics
import io.rudione.chatone.domain.stream.PlaybackStatus
import io.rudione.chatone.domain.stream.StreamAdBreakSource
import io.rudione.chatone.domain.stream.StreamErrorKind
import io.rudione.chatone.domain.stream.StreamManifest
import io.rudione.chatone.domain.stream.StreamMediaInfo
import io.rudione.chatone.domain.stream.StreamPhase
import io.rudione.chatone.domain.stream.StreamPlaybackEngine
import io.rudione.chatone.domain.stream.StreamQualitySelection
import io.rudione.chatone.domain.stream.StreamVariant
import io.rudione.chatone.presentation.stream.IosPlaybackAudioSession
import io.rudione.chatone.presentation.stream.IosRemoteControls
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import platform.AVKit.AVPictureInPictureController
import platform.UIKit.UIScreen
import kotlin.concurrent.Volatile
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.time.Clock
import kotlin.time.TimeSource

internal class LowLatencyStreamPlaybackEngine(
    private val player: LowLatencyPlayer,
    private val gateway: StreamPlaylistGateway,
    private val adBreakSource: StreamAdBreakSource,
    private val supportedCodecs: String,
    private val audioSession: IosPlaybackAudioSession,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
    private val nowMs: () -> Long = { Clock.System.now().toEpochMilliseconds() }
) : StreamPlaybackEngine, LowLatencyPlayerListener {

    private class UpstreamHealth {
        @Volatile
        var status = 0
    }

    val surfaces = VideoSurfaceStack(player)

    private val _status = MutableStateFlow(PlaybackStatus())
    override val status: StateFlow<PlaybackStatus> = _status.asStateFlow()

    private val governor = LiveLatencyGovernor()
    private val decodeGuard = VideoDecodeGuard()
    private val samplingBudget = FrameSamplingBudget()
    private var manifest: StreamManifest? = null
    private var selection: StreamQualitySelection = StreamQualitySelection.Auto
    private var lowLatency = true
    private var session: PlaylistSession? = null
    private var health = UpstreamHealth()
    private var wantsPlay = true
    private var muted = false
    private var sourceReady = false
    private var ended = false
    private var failure: StreamErrorKind? = null
    private var networkInterrupted = false
    private var rate = NORMAL_RATE
    private var pausedByEngineAtMs = 0L
    private var waitingSinceMs: Long? = null
    private var decodedFramesAtMs = 0L
    private var decodedFrames = 0L
    private var poller: Job? = null
    private var audioSessionHeld = false
    private var remoteControls: IosRemoteControls? = null
    private var released = false

    init {
        player.setListener(this)
    }

    override fun load(manifest: StreamManifest, selection: StreamQualitySelection, lowLatency: Boolean) {
        if (released) return
        if (this.manifest?.channelLogin != manifest.channelLogin) decodeGuard.forget()
        samplingBudget.reset()
        this.manifest = manifest
        this.selection = selection
        this.lowLatency = lowLatency
        holdAudioSession()
        session?.close()
        val upstream = UpstreamHealth()
        val adFilter = TwitchAdBreakFilter(manifest, supportedCodecs, adBreakSource)
        val opened = gateway.open(
            PlaylistSessionSpec(
                masterPlaylist = manifest.playlist,
                serverTimeMs = { nowMs() + manifest.clockOffsetMs },
                transform = { url, playlist ->
                    if (adFilter.needsFilter(url, playlist)) adFilter.filter(url, playlist) else playlist
                },
                onMediaStatus = { status -> upstream.status = status }
            )
        )
        session = opened
        health = upstream
        sourceReady = false
        ended = false
        failure = if (opened == null) StreamErrorKind.UNKNOWN else null
        wantsPlay = true
        networkInterrupted = false
        waitingSinceMs = null
        decodedFramesAtMs = 0L
        governor.configure(if (lowLatency) LiveLatencyGovernor.LOW_LATENCY else LiveLatencyGovernor.NORMAL)
        applyRate(NORMAL_RATE)
        if (opened != null) {
            screenVideoLimit().let { (width, height) -> player.limitVideoSize(width, height) }
            player.load(opened.masterUrl, lowLatency)
        }
        ensurePolling()
        publish()
    }

    override fun selectQuality(selection: StreamQualitySelection) {
        if (released || manifest == null || selection == this.selection) return
        this.selection = selection
        if (sourceReady) applyQuality(seamless = wantsPlay)
        publish()
    }

    override fun setPlaying(playing: Boolean) {
        if (released) return
        wantsPlay = playing
        when {
            !playing -> pausePlayer()
            needsReload() -> manifest?.let { load(it, selection, lowLatency) }
            sourceReady -> player.play()
        }
        remoteControls?.setPlaying(playing)
        publish()
    }

    override fun setMuted(muted: Boolean) {
        if (released) return
        this.muted = muted
        player.setMuted(muted)
        publish()
    }

    override fun seekToLive() {
        if (released || !wantsPlay || needsReload()) return
        val stats = player.stats()
        if (stats.state != LowLatencyPlayerState.PLAYING) return
        if (stats.latencyMs > governor.targetMs + LIVE_EDGE_TOLERANCE_MS) rejoinLive()
    }

    override fun setMediaInfo(info: StreamMediaInfo) {
        if (released) return
        val controls = remoteControls ?: IosRemoteControls(
            onPlay = { setPlaying(true) },
            onPause = { setPlaying(false) }
        ).also { remoteControls = it }
        controls.show(info)
        controls.setPlaying(wantsPlay)
    }

    override fun metrics(): PlaybackMetrics? {
        if (released) return null
        val current = manifest ?: return null
        val stats = player.stats()
        val variant = activeVariant(stats)
        val now = nowMs()
        return PlaybackMetrics(
            downloadWidth = variant?.width?.takeIf { it > 0 } ?: stats.videoWidth,
            downloadHeight = variant?.height?.takeIf { it > 0 } ?: stats.videoHeight,
            renderWidth = stats.videoWidth,
            renderHeight = stats.videoHeight,
            downloadBitrateKbps = stats.videoBitrate.takeIf { it > 0 }?.let { (it / 1000).toInt() },
            bandwidthEstimateKbps = stats.bandwidthEstimate.takeIf { it > 0 }?.let { (it / 1000).toInt() },
            framesPerSecond = framesPerSecond(stats, now),
            droppedFrames = stats.droppedFrames.coerceIn(0L, Int.MAX_VALUE.toLong()).toInt(),
            bufferMs = stats.bufferedMs.coerceAtLeast(0L),
            playbackWallClockMs = stats.latencyMs
                .takeIf { it > 0 && stats.state == LowLatencyPlayerState.PLAYING }
                ?.let { now + current.clockOffsetMs - it },
            codecs = stats.codecs?.ifEmpty { null } ?: variant?.codecs?.ifEmpty { null } ?: NOT_AVAILABLE,
            lowLatencyActive = stats.lowLatency,
            renderSurface = RENDER_SURFACE,
            backendVersion = "$BACKEND ${player.version}"
        )
    }

    override fun release() {
        if (released) return
        released = true
        poller?.cancel()
        remoteControls?.clear()
        remoteControls = null
        surfaces.dispose()
        player.setListener(null)
        player.close()
        session?.close()
        session = null
        manifest = null
        if (audioSessionHeld) audioSession.deactivate()
        audioSessionHeld = false
        _status.value = PlaybackStatus()
    }

    fun sampleFrame(width: Int, height: Int): IntArray? {
        if (released || selection == StreamQualitySelection.AudioOnly) return null
        val started = TimeSource.Monotonic.markNow()
        val frame = player.sampleFrame(width, height)
        samplingBudget.onSampled(started.elapsedNow().inWholeMicroseconds / MICROS_PER_MILLI)
        return frame
    }

    fun frameSamplingIntervalMs(minIntervalMs: Long): Long = samplingBudget.intervalMs(minIntervalMs)

    fun pictureInPictureController(): AVPictureInPictureController? =
        if (released) null else player.pictureInPictureController()

    override fun onStateChanged(state: LowLatencyPlayerState) {
        if (released) return
        when (state) {
            LowLatencyPlayerState.READY -> {
                sourceReady = true
                applyQuality(seamless = false)
                if (wantsPlay) player.play()
            }

            LowLatencyPlayerState.ENDED -> ended = true
            LowLatencyPlayerState.IDLE -> if (pausedOutsideEngine()) adoptIntent(playing = false)
            LowLatencyPlayerState.PLAYING -> {
                networkInterrupted = false
                if (resumedOutsideEngine()) adoptIntent(playing = true)
            }

            LowLatencyPlayerState.BUFFERING -> Unit
        }
        publish()
    }

    override fun onPlaybackChanged() = publish()

    override fun onRebuffer() {
        if (released) return
        governor.onRebuffer(nowMs())
    }

    override fun onNetworkUnavailable() {
        if (released) return
        networkInterrupted = true
    }

    override fun onError(error: LowLatencyPlayerError) {
        if (released) return
        if (error.status in ENDED_STATUSES) ended = true else failure = errorKind(error)
        Napier.w("Playback error ${error.reason}/${error.source} ${error.status} -> ${failure ?: StreamPhase.ENDED}", tag = TAG)
        if (failure == StreamErrorKind.UNSUPPORTED_CODEC && reloadOnDecodableQuality()) return
        publish()
    }

    private fun reloadOnDecodableQuality(): Boolean {
        val current = manifest ?: return false
        if (selection == StreamQualitySelection.AudioOnly) return false
        val quality = player.stats().qualityName ?: return false
        decodeGuard.reject(quality)
        val replacement = decodeGuard.replacement(null, videoQualityNames()) ?: return false
        Napier.w("Decoder cannot play $quality, reloading on $replacement", tag = TAG)
        failure = null
        scope.launch { if (!released) load(current, selection, lowLatency) }
        return true
    }

    private fun pausedOutsideEngine(): Boolean =
        sourceReady && wantsPlay && !networkInterrupted && failure == null && !ended &&
            nowMs() - pausedByEngineAtMs > INTENT_SETTLE_MS

    private fun resumedOutsideEngine(): Boolean =
        !wantsPlay && nowMs() - pausedByEngineAtMs > INTENT_SETTLE_MS

    private fun adoptIntent(playing: Boolean) {
        wantsPlay = playing
        remoteControls?.setPlaying(playing)
    }

    private fun pausePlayer() {
        pausedByEngineAtMs = nowMs()
        player.pause()
        applyRate(NORMAL_RATE)
        governor.onPlaybackInterrupted()
    }

    private fun rejoinLive() {
        pausePlayer()
        player.play()
    }

    private fun needsReload(): Boolean = ended || failure != null || session?.isServing != true

    private fun applyQuality(seamless: Boolean) = player.selectQuality(qualityName(), seamless)

    private fun qualityName(): String? {
        val wanted = when (val current = selection) {
            StreamQualitySelection.Auto -> null
            StreamQualitySelection.AudioOnly -> return manifest?.audioOnlyVariant?.name
            is StreamQualitySelection.Fixed ->
                manifest?.variants?.firstOrNull { it.groupId == current.groupId && !it.isAudioOnly }?.name
        }
        return if (decodeGuard.hasRejections) decodeGuard.replacement(wanted, videoQualityNames()) else wanted
    }

    private fun videoQualityNames(): List<String> =
        manifest?.videoVariants.orEmpty().sortedByDescending { it.bandwidth }.map { it.name }

    private fun applyRate(target: Float) {
        if (abs(rate - target) < RATE_EPSILON) return
        rate = target
        player.setPlaybackRate(target)
    }

    private fun ensurePolling() {
        if (poller?.isActive == true) return
        poller = scope.launch {
            while (isActive) {
                delay(POLL_INTERVAL_MS)
                val stats = player.stats()
                val now = nowMs()
                watchForStall(stats, now)
                watchDecoding(stats, now)
                steerTowardsLive(stats, now)
                publish(stats)
            }
        }
    }

    private fun watchForStall(stats: LowLatencyPlayerStats, now: Long) {
        val waiting = wantsPlay && failure == null && !ended && manifest != null &&
            stats.state != LowLatencyPlayerState.PLAYING
        if (!waiting) {
            waitingSinceMs = null
            return
        }
        val since = waitingSinceMs ?: now.also { waitingSinceMs = it }
        if (now - since < STALL_TIMEOUT_MS) return
        waitingSinceMs = null
        failure = StreamErrorKind.NETWORK
    }

    private fun watchDecoding(stats: LowLatencyPlayerStats, now: Long) {
        val watched = stats.qualityName.takeIf {
            stats.state == LowLatencyPlayerState.PLAYING && selection != StreamQualitySelection.AudioOnly
        }
        val undecodable = decodeGuard.observe(watched, stats.failedFrames, stats.decodedFrames, now) ?: return
        val replacement = decodeGuard.replacement(null, videoQualityNames())
        Napier.w("Frames of $undecodable cannot be decoded, switching to ${replacement ?: "nothing"}", tag = TAG)
        if (replacement == null) {
            failure = StreamErrorKind.UNSUPPORTED_CODEC
            return
        }
        applyQuality(seamless = false)
        decodeGuard.switched(now)
    }

    private fun steerTowardsLive(stats: LowLatencyPlayerStats, now: Long) {
        if (!wantsPlay || stats.state != LowLatencyPlayerState.PLAYING || stats.latencyMs <= 0) {
            governor.onPlaybackInterrupted()
            return
        }
        if (governor.shouldRejoin(stats.latencyMs, now)) {
            Napier.i("Rejoining live edge, latency ${stats.latencyMs}ms over target ${governor.targetMs}ms", tag = TAG)
            rejoinLive()
            return
        }
        applyRate(governor.rate(stats.latencyMs, stats.bufferedMs, now))
    }

    private fun publish(stats: LowLatencyPlayerStats = player.stats()) {
        if (released) return
        val phase = when {
            ended || health.status in ENDED_STATUSES -> StreamPhase.ENDED
            failure != null -> StreamPhase.ERROR
            manifest == null -> StreamPhase.IDLE
            !wantsPlay -> StreamPhase.PAUSED
            stats.state == LowLatencyPlayerState.PLAYING -> StreamPhase.PLAYING
            else -> StreamPhase.BUFFERING
        }
        val variant = activeVariant(stats)
        _status.value = PlaybackStatus(
            phase = phase,
            errorKind = failure.takeIf { phase == StreamPhase.ERROR },
            isMuted = muted,
            videoWidth = stats.videoWidth,
            videoHeight = stats.videoHeight,
            activeBandwidth = variant?.bandwidth?.takeIf { it > 0 } ?: stats.videoBitrate.takeIf { it > 0 },
            activeHeight = (variant?.height ?: stats.videoHeight).takeIf { it > 0 },
            audioOnly = selection == StreamQualitySelection.AudioOnly
        )
    }

    private fun activeVariant(stats: LowLatencyPlayerStats): StreamVariant? {
        val name = stats.qualityName ?: return null
        return manifest?.variants?.firstOrNull { it.name == name }
    }

    private fun framesPerSecond(stats: LowLatencyPlayerStats, now: Long): Float? {
        val elapsedMs = now - decodedFramesAtMs
        val frames = stats.decodedFrames - decodedFrames
        val known = decodedFramesAtMs > 0L
        decodedFramesAtMs = now
        decodedFrames = stats.decodedFrames
        if (!known || elapsedMs <= 0L || frames < 0L) return null
        return (frames * 10_000f / elapsedMs).roundToInt() / 10f
    }

    private fun errorKind(error: LowLatencyPlayerError): StreamErrorKind = when {
        error.status == HTTP_FORBIDDEN || health.status == HTTP_FORBIDDEN -> StreamErrorKind.UNKNOWN
        error.reason in NETWORK_REASONS -> StreamErrorKind.NETWORK
        error.reason == REASON_NOT_SUPPORTED || error.source == SOURCE_DECODE -> StreamErrorKind.UNSUPPORTED_CODEC
        else -> StreamErrorKind.UNKNOWN
    }

    private fun holdAudioSession() {
        if (audioSessionHeld) return
        audioSessionHeld = true
        audioSession.activate()
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun screenVideoLimit(): Pair<Int, Int> = UIScreen.mainScreen.nativeBounds.useContents {
        maxOf(size.width, size.height).toInt() to minOf(size.width, size.height).toInt()
    }

    private companion object {
        const val TAG = "StreamEngine"
        const val POLL_INTERVAL_MS = 500L
        const val STALL_TIMEOUT_MS = 12_000L
        const val LIVE_EDGE_TOLERANCE_MS = 1_500L
        const val INTENT_SETTLE_MS = 1_500L
        const val NORMAL_RATE = 1f
        const val RATE_EPSILON = 0.001f
        const val MICROS_PER_MILLI = 1_000f
        const val HTTP_FORBIDDEN = 403
        val ENDED_STATUSES = setOf(404, 410)
        val NETWORK_REASONS = setOf("ErrorNetwork", "ErrorNetworkIO", "ErrorTimeout")
        const val REASON_NOT_SUPPORTED = "ErrorNotSupported"
        const val SOURCE_DECODE = "Decode"
        const val RENDER_SURFACE = "IVSPlayerLayer"
        const val BACKEND = "Amazon IVS"
        const val NOT_AVAILABLE = "—"
    }
}
