package io.rudione.chatone.presentation.stream

import android.content.Context
import android.util.Base64
import android.view.SurfaceView
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaLibraryInfo
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLivePlaybackSpeedControl
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter
import io.github.aakira.napier.Napier
import io.rudione.chatone.data.remote.stream.TwitchAdBreakFilter
import io.rudione.chatone.domain.stream.PlaybackMetrics
import io.rudione.chatone.domain.stream.PlaybackStatus
import io.rudione.chatone.domain.stream.StreamAdBreakSource
import io.rudione.chatone.domain.stream.StreamErrorKind
import io.rudione.chatone.domain.stream.StreamManifest
import io.rudione.chatone.domain.stream.StreamPhase
import io.rudione.chatone.domain.stream.StreamPlaybackEngine
import io.rudione.chatone.domain.stream.StreamQualitySelection
import io.rudione.chatone.domain.stream.StreamVariant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import kotlin.math.abs

@UnstableApi
internal class AndroidStreamPlaybackEngine(
    context: Context,
    httpClient: OkHttpClient,
    private val adBreakSource: StreamAdBreakSource,
    private val supportedCodecs: String
) : StreamPlaybackEngine {

    private val appContext = context.applicationContext
    private val bandwidthMeter = DefaultBandwidthMeter.getSingletonInstance(appContext)
    private val dataSourceFactory = RestrictedStreamDataSourceFactory(OkHttpDataSource.Factory(httpClient))
    private val metricsCollector = StreamMetricsCollector(onVideoFormatChanged = ::publish)
    private val window = Timeline.Window()

    private val _status = MutableStateFlow(PlaybackStatus())
    override val status: StateFlow<PlaybackStatus> = _status.asStateFlow()

    private var manifest: StreamManifest? = null
    private var selection: StreamQualitySelection = StreamQualitySelection.Auto
    private var lowLatency = true
    private var playlistParser: TwitchPlaylistParserFactory? = null
    private var playerError: StreamErrorKind? = null
    private var released = false
    private val surfaces = ArrayList<SurfaceView>(2)

    val player: ExoPlayer = ExoPlayer.Builder(appContext)
        .setBandwidthMeter(bandwidthMeter)
        .setLoadControl(
            DefaultLoadControl.Builder()
                .setBufferDurationsMs(MIN_BUFFER_MS, MAX_BUFFER_MS, START_BUFFER_MS, REBUFFER_MS)
                .setPrioritizeTimeOverSizeThresholds(true)
                .build()
        )
        .setLivePlaybackSpeedControl(
            DefaultLivePlaybackSpeedControl.Builder()
                .setFallbackMinPlaybackSpeed(MIN_LIVE_SPEED)
                .setFallbackMaxPlaybackSpeed(MAX_LIVE_SPEED)
                .setProportionalControlFactor(LIVE_SPEED_CONTROL_FACTOR)
                .setTargetLiveOffsetIncrementOnRebufferMs(REBUFFER_OFFSET_INCREMENT_MS)
                .build()
        )
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .build(),
            true
        )
        .setHandleAudioBecomingNoisy(true)
        .setUsePlatformDiagnostics(false)
        .build()

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            if (events.contains(Player.EVENT_TRACKS_CHANGED)) applyPendingOverride()
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

    override fun load(manifest: StreamManifest, selection: StreamQualitySelection, lowLatency: Boolean) {
        if (released) return
        this.manifest = manifest
        this.selection = selection
        this.lowLatency = lowLatency
        playerError = null
        metricsCollector.reset()
        val audioVariant = manifest.audioOnlyVariant.takeIf { selection == StreamQualitySelection.AudioOnly }
        val adBreakFilter = TwitchAdBreakFilter(manifest, supportedCodecs, adBreakSource)
        val parserFactory = TwitchPlaylistParserFactory(lowLatency) { url, playlist ->
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
            .build()
        val mediaSource = HlsMediaSource.Factory(dataSourceFactory)
            .setPlaylistParserFactory(parserFactory)
            .setAllowChunklessPreparation(true)
            .createMediaSource(mediaItem)
        applyTrackParameters(resolveOverride = false)
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
        surfaces.clear()
        player.removeListener(listener)
        player.removeAnalyticsListener(metricsCollector)
        player.release()
        manifest = null
        _status.value = PlaybackStatus()
    }

    fun attachSurface(view: SurfaceView) {
        if (released) return
        surfaces.remove(view)
        surfaces.add(view)
        player.setVideoSurfaceView(view)
    }

    fun detachSurface(view: SurfaceView) {
        if (released) return
        val wasCurrent = surfaces.lastOrNull() === view
        surfaces.remove(view)
        if (!wasCurrent) return
        player.clearVideoSurfaceView(view)
        surfaces.lastOrNull()?.let(player::setVideoSurfaceView)
    }

    private fun playbackWallClockMs(): Long? {
        val timeline = player.currentTimeline
        if (timeline.isEmpty) return null
        timeline.getWindow(player.currentMediaItemIndex, window)
        if (!window.isLive() || window.windowStartTimeMs == C.TIME_UNSET) return null
        return window.windowStartTimeMs + player.currentPosition
    }

    private fun applyTrackParameters(resolveOverride: Boolean = true) {
        if (released) return
        val builder = player.trackSelectionParameters.buildUpon()
            .clearOverridesOfType(C.TRACK_TYPE_VIDEO)
            .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, selection == StreamQualitySelection.AudioOnly)
        if (resolveOverride) {
            val requested = selection as? StreamQualitySelection.Fixed
            val variant = requested?.let { fixed -> manifest?.variants?.firstOrNull { it.groupId == fixed.groupId } }
            overrideFor(variant)?.let(builder::addOverride)
        }
        player.trackSelectionParameters = builder.build()
    }

    private fun applyPendingOverride() {
        if (selection !is StreamQualitySelection.Fixed) return
        val currentGroups = player.currentTracks.groups.map { it.mediaTrackGroup }
        val hasLiveOverride = player.trackSelectionParameters.overrides.any { (group, override) ->
            override.type == C.TRACK_TYPE_VIDEO && group in currentGroups
        }
        if (!hasLiveOverride) applyTrackParameters()
    }

    private fun overrideFor(variant: StreamVariant?): TrackSelectionOverride? {
        if (variant == null || variant.isAudioOnly) return null
        var best: TrackSelectionOverride? = null
        var bestScore = Long.MAX_VALUE
        for (group in player.currentTracks.groups) {
            if (group.type != C.TRACK_TYPE_VIDEO) continue
            for (index in 0 until group.length) {
                if (!group.isTrackSupported(index)) continue
                val score = matchScore(group.getTrackFormat(index), variant) ?: continue
                if (score < bestScore) {
                    bestScore = score
                    best = TrackSelectionOverride(group.mediaTrackGroup, index)
                }
            }
        }
        return best
    }

    private fun matchScore(format: Format, variant: StreamVariant): Long? {
        if (variant.height > 0 && format.height > 0 && format.height != variant.height) return null
        val bitrate = listOf(format.peakBitrate, format.averageBitrate, format.bitrate).firstOrNull { it > 0 }
            ?: return Long.MAX_VALUE - 1
        val frameRatePenalty = if (variant.frameRate > 0f && format.frameRate > 0f) {
            (abs(format.frameRate - variant.frameRate) * FRAME_RATE_WEIGHT).toLong()
        } else 0L
        return abs(bitrate - variant.bandwidth) + frameRatePenalty
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
            activeHeight = videoFormat?.height?.takeIf { it > 0 }
        )
    }

    private fun inlinePlaylistUri(playlist: String): String =
        "data:${MimeTypes.APPLICATION_M3U8};base64," +
            Base64.encodeToString(playlist.encodeToByteArray(), Base64.NO_WRAP)

    private fun liveConfiguration(lowLatency: Boolean): MediaItem.LiveConfiguration =
        MediaItem.LiveConfiguration.Builder()
            .setTargetOffsetMs(if (lowLatency) LOW_LATENCY_TARGET_MS else NORMAL_LATENCY_TARGET_MS)
            .setMinOffsetMs(if (lowLatency) LOW_LATENCY_MIN_MS else NORMAL_LATENCY_MIN_MS)
            .setMaxOffsetMs(MAX_LIVE_OFFSET_MS)
            .setMinPlaybackSpeed(MIN_LIVE_SPEED)
            .setMaxPlaybackSpeed(MAX_LIVE_SPEED)
            .build()

    private companion object {
        const val TAG = "StreamEngine"
        const val MIN_BUFFER_MS = 4_000
        const val MAX_BUFFER_MS = 12_000
        const val START_BUFFER_MS = 500
        const val REBUFFER_MS = 1_000
        const val LOW_LATENCY_TARGET_MS = 1_800L
        const val LOW_LATENCY_MIN_MS = 1_500L
        const val NORMAL_LATENCY_TARGET_MS = 5_000L
        const val NORMAL_LATENCY_MIN_MS = 3_000L
        const val MAX_LIVE_OFFSET_MS = 30_000L
        const val MIN_LIVE_SPEED = 0.96f
        const val MAX_LIVE_SPEED = 1.06f
        const val LIVE_SPEED_CONTROL_FACTOR = 0.15f
        const val REBUFFER_OFFSET_INCREMENT_MS = 250L
        const val FRAME_RATE_WEIGHT = 10_000f
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
