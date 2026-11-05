package io.rudione.chatone.domain.media

import kotlinx.coroutines.flow.StateFlow

enum class PlaybackPhase { IDLE, LOADING, READY, ENDED, ERROR }

interface MediaPlayback {
    val phase: StateFlow<PlaybackPhase>
    val playing: StateFlow<Boolean>
    val firstFrameShown: StateFlow<Boolean>

    fun load(source: MediaSource, startMs: Long)

    fun setPlaying(playing: Boolean)

    fun seekTo(positionMs: Long)

    fun scrubTo(positionMs: Long)

    fun setVolume(volume: Float)

    fun setSpeed(speed: Float)

    fun setLooping(looping: Boolean)

    fun positionMs(): Long

    fun durationMs(): Long

    fun release()
}

interface MediaPlaybackFactory {
    val isSupported: Boolean

    fun create(): MediaPlayback?
}

object UnsupportedMediaPlaybackFactory : MediaPlaybackFactory {
    override val isSupported: Boolean = false

    override fun create(): MediaPlayback? = null
}
