package io.rudione.chatone.presentation.stream

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Base64
import android.view.SurfaceView
import android.view.TextureView
import android.view.View
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaLibraryInfo
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSourceBitmapLoader
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.session.MediaSession
import io.github.aakira.napier.Napier
import io.rudione.chatone.data.remote.stream.LiveStartHint
import io.rudione.chatone.data.remote.stream.TwitchAdBreakFilter
import io.rudione.chatone.domain.stream.LiveEdgeGuard
import io.rudione.chatone.domain.stream.LiveSpeedPolicy
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient

@UnstableApi
internal class AndroidStreamPlaybackEngine(
    context: Context,
    httpClient: OkHttpClient,
    private val adBreakSource: StreamAdBreakSource,
    private val supportedCodecs: String,
    capacityMemory: NetworkCapacityMemory,
    private val sessions: StreamMediaSessions
) : StreamPlaybackEngine {

    private val appContext = context.applicationContext
    private val autoQuality = AutoQualityController(appContext, capacityMemory)
    private val bandwidthMeter = StreamBandwidthMeter(autoQuality)
    private val dataSourceFactory = RestrictedStreamDataSourceFactory(OkHttpDataSource.Factory(httpClient))
    private val metricsCollector = StreamMetricsCollector(onVideoFormatChanged = ::publish)
    private val window = Timeline.Window()
    private val speedControl = TwitchLiveSpeedControl()
    private val liveEdgeGuard = LiveEdgeGuard()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val liveEdgeCheck = Runnable(::checkLiveEdge)
    private var liveEdgeCheckScheduled = false

    private val _status = MutableStateFlow(PlaybackStatus())
    override val status: StateFlow<PlaybackStatus> = _status.asStateFlow()

    private var manifest: StreamManifest? = null
    private var selection: StreamQualitySelection = StreamQualitySelection.Auto
    private var lowLatency = true
    private var playlistParser: TwitchPlaylistParserFactory? = null
    private var playerError: StreamErrorKind? = null
    private var released = false
    private var mediaInfo: StreamMediaInfo? = null
    private val outputs = ArrayList<View>(2)
    private val trackSelector = DefaultTrackSelector(appContext, AutoQualityTrackSelectionFactory(autoQuality))

    val player: ExoPlayer = ExoPlayer.Builder(appContext, StreamRenderersFactory(appContext))
        .setBandwidthMeter(bandwidthMeter)
        .setTrackSelector(trackSelector)
        .setLoadControl(
            DefaultLoadControl.Builder()
                .setBufferDurationsMs(MIN_BUFFER_MS, MAX_BUFFER_MS, START_BUFFER_MS, REBUFFER_MS)
                .setPrioritizeTimeOverSizeThresholds(true)
                .build()
        )
        .setLivePlaybackSpeedControl(speedControl)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .build(),
            true
        )
        .setHandleAudioBecomingNoisy(true)
        .setWakeMode(C.WAKE_MODE_NETWORK)
        .setUsePlatformDiagnostics(false)
        .build()

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            if (events.contains(Player.EVENT_IS_PLAYING_CHANGED)) updateLiveEdgeCheck()
            publish()
        }

        override fun onPlayerError(error: PlaybackException) {
            handleError(error)
        }
    }

    init {
        player.addListener(listener)
        player.addAnalyticsListener(metricsCollector)
    }

    private val mediaSession: MediaSession = sessions.open(
        player = player,
        artworkLoader = DataSourceBitmapLoader.Builder(appContext).setDataSourceFactory(dataSourceFactory).build()
    )

    override fun load(manifest: StreamManifest, selection: StreamQualitySelection, lowLatency: Boolean) {
        if (released) return
        this.manifest = manifest
        this.selection = selection
        this.lowLatency = lowLatency
        playerError = null
        metricsCollector.reset()
        liveEdgeGuard.reset()
        autoQuality.startSession()
        val clockOffsetMs = manifest.clockOffsetMs
        speedControl.clockOffsetUs = clockOffsetMs * 1_000L
        val startLatencyMs = targetLatencyMs(lowLatency)
        val audioVariant = manifest.audioOnlyVariant.takeIf { selection == StreamQualitySelection.AudioOnly }
        val adBreakFilter = TwitchAdBreakFilter(manifest, supportedCodecs, adBreakSource)
        val parserFactory = TwitchPlaylistParserFactory(
            lowLatency = lowLatency,
            liveStart = { LiveStartHint(System.currentTimeMillis() + clockOffsetMs, startLatencyMs) }
        ) { url, playlist ->
            if (adBreakFilter.needsFilter(url, playlist)) {
                runBlocking { adBreakFilter.filter(url, playlist) }
            } else {
                playlist
            }
        }
        playlistParser = parserFactory
        val mediaItem = MediaItem.Builder()
            .setUri(audioVariant?.url ?: inlinePlaylistUri(manifest.playlist))
            .setMimeType(MimeTypes.APPLICATION_M3U8)
            .setLiveConfiguration(liveConfiguration(lowLatency))
            .setMediaMetadata(metadataFor(manifest.channelLogin))
            .build()
        val mediaSource = HlsMediaSource.Factory(dataSourceFactory)
            .setPlaylistParserFactory(parserFactory)
            .setAllowChunklessPreparation(true)
            .createMediaSource(mediaItem)
        applyTrackParameters()
        player.setMediaSource(mediaSource)
        player.prepare()
        player.playWhenReady = true
        publish()
    }

    override fun selectQuality(selection: StreamQualitySelection) {
        val current = manifest ?: return
        val switchesSource = (selection == StreamQualitySelection.AudioOnly) != (this.selection == StreamQualitySelection.AudioOnly)
        this.selection = selection
        if (switchesSource) {
            load(current, selection, lowLatency)
        } else {
            applyTrackParameters()
        }
    }

    override fun setPlaying(playing: Boolean) {
        if (released) return
        player.playWhenReady = playing
        publish()
    }

    override fun setMuted(muted: Boolean) {
        if (released) return
        player.volume = if (muted) 0f else 1f
        publish()
    }

    override fun seekToLive() {
        if (released) return
        if (player.isCurrentMediaItemLive) player.seekToDefaultPosition()
    }

    override fun setMediaInfo(info: StreamMediaInfo) {
        if (released || info == mediaInfo) return
        mediaInfo = info
        val login = manifest?.channelLogin ?: return
        val item = player.currentMediaItem ?: return
        player.replaceMediaItem(player.currentMediaItemIndex, item.buildUpon().setMediaMetadata(metadataFor(login)).build())
    }

    override fun metrics(): PlaybackMetrics? {
        if (released || manifest == null) return null
        val videoFormat = player.videoFormat
        val videoSize = player.videoSize
        return PlaybackMetrics(
            downloadWidth = videoFormat?.width?.takeIf { it > 0 } ?: 0,
            downloadHeight = videoFormat?.height?.takeIf { it > 0 } ?: 0,
            renderWidth = videoSize.width,
            renderHeight = videoSize.height,
            downloadBitrateKbps = metricsCollector.downloadBitrateKbps,
            bandwidthEstimateKbps = bandwidthMeter.bitrateEstimate.takeIf { it > 0 }?.let { (it / 1000).toInt() },
            framesPerSecond = metricsCollector.sampleFramesPerSecond(player.videoDecoderCounters),
            droppedFrames = metricsCollector.droppedFrames,
            bufferMs = player.totalBufferedDuration.coerceAtLeast(0L),
            playbackWallClockMs = playbackWallClockMs(),
            codecs = listOfNotNull(videoFormat?.codecs, player.audioFormat?.codecs)
                .joinToString(",")
                .ifEmpty { NOT_AVAILABLE },
            lowLatencyActive = lowLatency && playlistParser?.isPrefetchActive == true,
            renderSurface = RENDER_SURFACE,
            backendVersion = "Media3 ${MediaLibraryInfo.VERSION}"
        )
    }

    override fun release() {
        if (released) return
        released = true
        mainHandler.removeCallbacks(liveEdgeCheck)
        liveEdgeCheckScheduled = false
        sessions.close(mediaSession)
        outputs.clear()
        player.removeListener(listener)
        player.removeAnalyticsListener(metricsCollector)
        player.release()
        manifest = null
        _status.value = PlaybackStatus()
    }

    fun attachVideoOutput(view: View) {
        if (released) return
        outputs.remove(view)
        outputs.add(view)
        render(view)
    }

    fun detachVideoOutput(view: View) {
        if (released) return
        val wasCurrent = outputs.lastOrNull() === view
        outputs.remove(view)
        if (!wasCurrent) return
        when (view) {
            is SurfaceView -> player.clearVideoSurfaceView(view)
            is TextureView -> player.clearVideoTextureView(view)
        }
        outputs.lastOrNull()?.let(::render)
    }

    private fun render(view: View) {
        when (view) {
            is SurfaceView -> player.setVideoSurfaceView(view)
            is TextureView -> player.setVideoTextureView(view)
        }
    }

    private fun updateLiveEdgeCheck() {
        if (released) return
        val shouldRun = player.isPlaying
        if (shouldRun == liveEdgeCheckScheduled) return
        liveEdgeCheckScheduled = shouldRun
        if (shouldRun) {
            mainHandler.postDelayed(liveEdgeCheck, LIVE_EDGE_CHECK_INTERVAL_MS)
        } else {
            mainHandler.removeCallbacks(liveEdgeCheck)
            liveEdgeGuard.reset()
        }
    }

    private fun checkLiveEdge() {
        if (released || !liveEdgeCheckScheduled) return
        val offsetMs = player.currentLiveOffset
        if (offsetMs != C.TIME_UNSET && player.isCurrentMediaItemLive) {
            val liveOffsetUs = offsetMs * 1_000L + speedControl.clockOffsetUs
            val targetUs = speedControl.targetUs
            val bufferedUs = player.totalBufferedDuration * 1_000L
            if (liveEdgeGuard.shouldJump(liveOffsetUs, targetUs, bufferedUs, SystemClock.elapsedRealtime())) {
                Napier.i("Jumping to live edge, lag ${liveOffsetUs / 1_000}ms over target ${targetUs.takeIf { it != LiveSpeedPolicy.UNSET }?.div(1_000)}ms", tag = TAG)
                player.seekToDefaultPosition()
            }
        }
        mainHandler.postDelayed(liveEdgeCheck, LIVE_EDGE_CHECK_INTERVAL_MS)
    }

    private fun playbackWallClockMs(): Long? {
        val timeline = player.currentTimeline
        if (timeline.isEmpty) return null
        timeline.getWindow(player.currentMediaItemIndex, window)
        if (!window.isLive() || window.windowStartTimeMs == C.TIME_UNSET) return null
        return window.windowStartTimeMs + player.currentPosition
    }

    private fun applyTrackParameters() {
        if (released) return
        val fixed = selection is StreamQualitySelection.Fixed
        autoQuality.pin(pinnedVariant())
        val builder = trackSelector.buildUponParameters()
            .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, selection == StreamQualitySelection.AudioOnly)
            .setAllowVideoMixedMimeTypeAdaptiveness(fixed)
        if (fixed) builder.clearViewportSizeConstraints() else builder.setViewportSizeToPhysicalDisplaySize(appContext, true)
        trackSelector.setParameters(builder)
    }

    private fun pinnedVariant(): StreamVariant? {
        val fixed = selection as? StreamQualitySelection.Fixed ?: return null
        return manifest?.variants?.firstOrNull { it.groupId == fixed.groupId && !it.isAudioOnly }
    }

    private fun handleError(error: PlaybackException) {
        if (error.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW) {
            player.seekToDefaultPosition()
            player.prepare()
            return
        }
        val kind = errorKindOf(error)
        Napier.w("Playback error ${error.errorCodeName} -> $kind", tag = TAG)
        if (kind == null) {
            publish(forcedPhase = StreamPhase.ENDED)
            return
        }
        playerError = kind
        publish()
    }

    private fun errorKindOf(error: PlaybackException): StreamErrorKind? {
        val httpStatus = generateSequence<Throwable>(error) { it.cause }
            .filterIsInstance<HttpDataSource.InvalidResponseCodeException>()
            .firstOrNull()
            ?.responseCode
        return when {
            httpStatus == HTTP_NOT_FOUND || httpStatus == HTTP_GONE -> null
            httpStatus == HTTP_FORBIDDEN -> StreamErrorKind.UNKNOWN
            error.errorCode in NETWORK_ERRORS -> StreamErrorKind.NETWORK
            error.errorCode in CODEC_ERRORS -> StreamErrorKind.UNSUPPORTED_CODEC
            error.errorCode == PlaybackException.ERROR_CODE_IO_NO_PERMISSION -> StreamErrorKind.FORBIDDEN
            else -> StreamErrorKind.UNKNOWN
        }
    }

    private fun publish(forcedPhase: StreamPhase? = null) {
        if (released) return
        val phase = forcedPhase ?: when {
            playerError != null -> StreamPhase.ERROR
            else -> when (player.playbackState) {
                Player.STATE_IDLE -> StreamPhase.IDLE
                Player.STATE_ENDED -> StreamPhase.ENDED
                Player.STATE_BUFFERING -> if (player.playWhenReady) StreamPhase.BUFFERING else StreamPhase.PAUSED
                else -> when {
                    !player.playWhenReady -> StreamPhase.PAUSED
                    player.playbackSuppressionReason != Player.PLAYBACK_SUPPRESSION_REASON_NONE -> StreamPhase.PAUSED
                    player.isPlaying -> StreamPhase.PLAYING
                    else -> StreamPhase.BUFFERING
                }
            }
        }
        val videoFormat = player.videoFormat
        _status.value = PlaybackStatus(
            phase = phase,
            errorKind = if (phase == StreamPhase.ERROR) playerError else null,
            isMuted = player.volume == 0f,
            videoWidth = player.videoSize.width,
            videoHeight = player.videoSize.height,
            activeBandwidth = videoFormat?.let { format ->
                listOf(format.peakBitrate, format.averageBitrate, format.bitrate).firstOrNull { it > 0 }?.toLong()
            },
            activeHeight = videoFormat?.height?.takeIf { it > 0 },
            audioOnly = selection == StreamQualitySelection.AudioOnly
        )
    }

    private fun inlinePlaylistUri(playlist: String): String =
        "data:${MimeTypes.APPLICATION_M3U8};base64," +
            Base64.encodeToString(playlist.encodeToByteArray(), Base64.NO_WRAP)

    private fun liveConfiguration(lowLatency: Boolean): MediaItem.LiveConfiguration =
        MediaItem.LiveConfiguration.Builder()
            .setTargetOffsetMs(targetLatencyMs(lowLatency))
            .setMinOffsetMs(if (lowLatency) LOW_LATENCY_MIN_MS else NORMAL_LATENCY_MIN_MS)
            .setMaxOffsetMs(if (lowLatency) LOW_LATENCY_MAX_MS else NORMAL_LATENCY_MAX_MS)
            .setMinPlaybackSpeed(MIN_LIVE_SPEED)
            .setMaxPlaybackSpeed(MAX_LIVE_SPEED)
            .build()

    private fun metadataFor(channelLogin: String): MediaMetadata {
        val info = mediaInfo?.takeIf { it.channelLogin.equals(channelLogin, ignoreCase = true) }
        val channel = info?.channelName?.takeIf { it.isNotBlank() } ?: channelLogin
        return MediaMetadata.Builder()
            .setTitle(info?.title?.takeIf { it.isNotBlank() } ?: channel)
            .setArtist(channel)
            .setArtworkUri(info?.artworkUrl?.let(Uri::parse))
            .setMediaType(MediaMetadata.MEDIA_TYPE_VIDEO)
            .build()
    }

    private fun targetLatencyMs(lowLatency: Boolean): Long =
        if (lowLatency) LOW_LATENCY_TARGET_MS else NORMAL_LATENCY_TARGET_MS

    private companion object {
        const val TAG = "StreamEngine"
        const val MIN_BUFFER_MS = 4_000
        const val MAX_BUFFER_MS = 12_000
        const val START_BUFFER_MS = 500
        const val REBUFFER_MS = 1_000
        const val LOW_LATENCY_TARGET_MS = 1_600L
        const val LOW_LATENCY_MIN_MS = 1_000L
        const val LOW_LATENCY_MAX_MS = 6_000L
        const val NORMAL_LATENCY_TARGET_MS = 5_000L
        const val NORMAL_LATENCY_MIN_MS = 3_000L
        const val NORMAL_LATENCY_MAX_MS = 12_000L
        const val MIN_LIVE_SPEED = 0.95f
        const val MAX_LIVE_SPEED = 1.1f
        const val LIVE_EDGE_CHECK_INTERVAL_MS = 1_000L
        const val HTTP_FORBIDDEN = 403
        const val HTTP_NOT_FOUND = 404
        const val HTTP_GONE = 410
        const val RENDER_SURFACE = "SurfaceView"
        const val NOT_AVAILABLE = "—"
        val NETWORK_ERRORS = setOf(
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
            PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
            PlaybackException.ERROR_CODE_IO_UNSPECIFIED,
            PlaybackException.ERROR_CODE_TIMEOUT
        )
        val CODEC_ERRORS = setOf(
            PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
            PlaybackException.ERROR_CODE_DECODER_QUERY_FAILED,
            PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED,
            PlaybackException.ERROR_CODE_DECODING_FORMAT_EXCEEDS_CAPABILITIES
        )
    }
}
