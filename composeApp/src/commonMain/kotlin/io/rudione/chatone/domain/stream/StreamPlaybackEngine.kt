package io.rudione.chatone.domain.stream

import kotlinx.coroutines.flow.StateFlow

interface StreamPlaybackEngine {
    val status: StateFlow<PlaybackStatus>

    fun load(manifest: StreamManifest, selection: StreamQualitySelection, lowLatency: Boolean)

    fun selectQuality(selection: StreamQualitySelection)

    fun setPlaying(playing: Boolean)

    fun setMuted(muted: Boolean)

    fun seekToLive()

    fun metrics(): PlaybackMetrics?

    fun release()
}

sealed interface StreamManifestResult {
    class Ready(val manifest: StreamManifest) : StreamManifestResult
    data object Offline : StreamManifestResult
    data class Failed(val kind: StreamErrorKind) : StreamManifestResult
}

interface StreamManifestSource {
    suspend fun fetchManifest(channelLogin: String, supportedCodecs: String): StreamManifestResult
}

interface StreamAdBreakSource {
    suspend fun adFreeMediaPlaylist(channelLogin: String, variant: StreamVariant, supportedCodecs: String): String?
}

interface StreamPlaybackEngineFactory {
    val isSupported: Boolean

    val supportedCodecs: String

    fun create(): StreamPlaybackEngine
}

object UnsupportedStreamPlaybackEngineFactory : StreamPlaybackEngineFactory {
    override val isSupported: Boolean = false

    override val supportedCodecs: String = "h264"

    override fun create(): StreamPlaybackEngine =
        throw UnsupportedOperationException("Stream playback is not available on this platform")
}
