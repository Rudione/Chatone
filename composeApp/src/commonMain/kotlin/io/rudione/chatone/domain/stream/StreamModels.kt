package io.rudione.chatone.domain.stream

import androidx.compose.runtime.Immutable

@Immutable
data class StreamVariant(
    val groupId: String,
    val name: String,
    val url: String,
    val bandwidth: Long,
    val width: Int,
    val height: Int,
    val frameRate: Float,
    val codecs: String
) {
    val hasVideoCodec: Boolean
        get() = codecs.split(',').any { codec -> VIDEO_CODEC_PREFIXES.any { codec.trim().startsWith(it) } }

    val isAudioOnly: Boolean get() = groupId == AUDIO_ONLY_GROUP || (height <= 0 && !hasVideoCodec)

    val isSource: Boolean get() = groupId == SOURCE_GROUP || name.contains(SOURCE_MARKER, ignoreCase = true)

    val displayName: String get() = name.replace(SOURCE_MARKER, "", ignoreCase = true).trim()

    override fun toString(): String = "StreamVariant($groupId, ${width}x$height@$frameRate, $bandwidth)"

    companion object {
        const val AUDIO_ONLY_GROUP = "audio_only"
        const val SOURCE_GROUP = "chunked"
        private const val SOURCE_MARKER = "(source)"
        private val VIDEO_CODEC_PREFIXES = listOf("avc", "hev", "hvc", "av01")
    }
}

@Immutable
data class TwitchServingInfo(
    val servingId: String? = null,
    val broadcastId: String? = null,
    val transcodeStack: String? = null,
    val serverTimeMs: Long? = null,
    val streamStartedAtMs: Long? = null
)

@Immutable
class StreamManifest(
    val channelLogin: String,
    val playlist: String,
    val variants: List<StreamVariant>,
    val serving: TwitchServingInfo,
    val playSessionId: String,
    val clockOffsetMs: Long
) {
    val videoVariants: List<StreamVariant> get() = variants.filterNot { it.isAudioOnly }

    val audioOnlyVariant: StreamVariant? get() = variants.firstOrNull { it.isAudioOnly }

    override fun toString(): String = "StreamManifest($channelLogin, variants=${variants.size})"
}

sealed interface StreamQualitySelection {
    data object Auto : StreamQualitySelection
    data object AudioOnly : StreamQualitySelection
    data class Fixed(val groupId: String) : StreamQualitySelection
}

enum class LandscapeChatMode { SIDE, OVERLAY, HIDDEN }

enum class StreamVideoScale { FIT, FILL }

enum class StreamPhase { IDLE, LOADING, BUFFERING, PLAYING, PAUSED, OFFLINE, ENDED, ERROR }

enum class StreamErrorKind { NETWORK, FORBIDDEN, GEOBLOCKED, SUBSCRIBERS_ONLY, UNSUPPORTED_CODEC, NOT_FOUND, UNKNOWN }

@Immutable
data class OverlayChatBounds(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float
) {
    fun sanitized(): OverlayChatBounds {
        val w = width.finiteOr(DEFAULT.width).coerceIn(MIN_WIDTH, 1f)
        val h = height.finiteOr(DEFAULT.height).coerceIn(MIN_HEIGHT, 1f)
        return OverlayChatBounds(
            x = x.finiteOr(DEFAULT.x).coerceIn(0f, 1f - w),
            y = y.finiteOr(DEFAULT.y).coerceIn(0f, 1f - h),
            width = w,
            height = h
        )
    }

    private fun Float.finiteOr(fallback: Float): Float = if (isFinite()) this else fallback

    companion object {
        const val MIN_WIDTH = 0.18f
        const val MIN_HEIGHT = 0.3f
        val DEFAULT = OverlayChatBounds(x = 0.64f, y = 0.17f, width = 0.34f, height = 0.66f)
    }
}

@Immutable
data class StreamPlayerPreferences(
    val preferredQuality: String = QUALITY_AUTO,
    val lowLatency: Boolean = true,
    val landscapeChatMode: LandscapeChatMode = LandscapeChatMode.SIDE,
    val videoScale: StreamVideoScale = StreamVideoScale.FIT,
    val sideChatFraction: Float = DEFAULT_SIDE_CHAT_FRACTION,
    val overlayBounds: OverlayChatBounds = OverlayChatBounds.DEFAULT,
    val overlayOpacity: Float = DEFAULT_OVERLAY_OPACITY,
    val overlayMessageOpacity: Float = DEFAULT_OVERLAY_MESSAGE_OPACITY,
    val overlayLocked: Boolean = false,
    val autoPictureInPicture: Boolean = true,
    val autoOpen: Boolean = true
) {
    companion object {
        const val QUALITY_AUTO = "auto"
        const val QUALITY_AUDIO_ONLY = StreamVariant.AUDIO_ONLY_GROUP
        const val DEFAULT_SIDE_CHAT_FRACTION = 0.32f
        const val MIN_SIDE_CHAT_FRACTION = 0.22f
        const val MAX_SIDE_CHAT_FRACTION = 0.6f
        const val DEFAULT_OVERLAY_OPACITY = 0.55f
        const val MIN_OVERLAY_OPACITY = 0.0f
        const val MAX_OVERLAY_OPACITY = 0.9f
        const val DEFAULT_OVERLAY_MESSAGE_OPACITY = 0.8f
        const val MIN_OVERLAY_MESSAGE_OPACITY = 0.3f
        const val MAX_OVERLAY_MESSAGE_OPACITY = 1f
    }
}

@Immutable
data class PlaybackStatus(
    val phase: StreamPhase = StreamPhase.IDLE,
    val errorKind: StreamErrorKind? = null,
    val isMuted: Boolean = false,
    val videoWidth: Int = 0,
    val videoHeight: Int = 0,
    val activeBandwidth: Long? = null,
    val activeHeight: Int? = null
)

@Immutable
data class PlaybackMetrics(
    val downloadWidth: Int,
    val downloadHeight: Int,
    val renderWidth: Int,
    val renderHeight: Int,
    val downloadBitrateKbps: Int?,
    val bandwidthEstimateKbps: Int?,
    val framesPerSecond: Float?,
    val droppedFrames: Int,
    val bufferMs: Long,
    val playbackWallClockMs: Long?,
    val codecs: String,
    val lowLatencyActive: Boolean,
    val renderSurface: String,
    val backendVersion: String
)

@Immutable
data class StreamStats(
    val downloadResolution: String,
    val renderResolution: String,
    val viewportResolution: String,
    val downloadBitrateKbps: Int?,
    val bandwidthEstimateKbps: Int?,
    val framesPerSecond: Float?,
    val droppedFrames: Int,
    val bufferSeconds: Float,
    val latencySeconds: Float?,
    val codecs: String,
    val protocol: String,
    val lowLatency: Boolean,
    val renderSurface: String,
    val backendVersion: String,
    val playSessionId: String,
    val servingId: String?
)
