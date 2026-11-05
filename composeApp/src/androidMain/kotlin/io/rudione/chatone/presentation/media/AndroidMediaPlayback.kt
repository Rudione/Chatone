package io.rudione.chatone.presentation.media

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import io.rudione.chatone.domain.media.MediaPlayback
import io.rudione.chatone.domain.media.MediaPlaybackFactory
import io.rudione.chatone.domain.media.MediaSource
import io.rudione.chatone.domain.media.PlaybackPhase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AndroidMediaPlaybackFactory(private val context: Context) : MediaPlaybackFactory {
    override val isSupported: Boolean = true

    override fun create(): MediaPlayback = AndroidMediaPlayback(context.applicationContext)
}

@OptIn(UnstableApi::class)
class AndroidMediaPlayback(context: Context) : MediaPlayback {

    internal val player: ExoPlayer = ExoPlayer.Builder(context)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .build(),
            true
        )
        .setHandleAudioBecomingNoisy(true)
        .build()

    private val _phase = MutableStateFlow(PlaybackPhase.IDLE)
    override val phase: StateFlow<PlaybackPhase> = _phase.asStateFlow()

    private val _playing = MutableStateFlow(false)
    override val playing: StateFlow<Boolean> = _playing.asStateFlow()

    private val _firstFrameShown = MutableStateFlow(false)
    override val firstFrameShown: StateFlow<Boolean> = _firstFrameShown.asStateFlow()

    private val listener = object : Player.Listener {
        override fun onPlaybackStateChanged(state: Int) {
            _phase.value = when (state) {
                Player.STATE_BUFFERING -> PlaybackPhase.LOADING
                Player.STATE_READY -> PlaybackPhase.READY
                Player.STATE_ENDED -> PlaybackPhase.ENDED
                else -> PlaybackPhase.IDLE
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _playing.value = isPlaying
        }

        override fun onPlayerError(error: PlaybackException) {
            _phase.value = PlaybackPhase.ERROR
        }

        override fun onRenderedFirstFrame() {
            _firstFrameShown.value = true
        }
    }

    init {
        player.addListener(listener)
    }

    override fun load(source: MediaSource, startMs: Long) {
        val item = MediaItem.Builder()
            .setUri(source.url)
            .apply { if (source.isStream) setMimeType(MimeTypes.APPLICATION_M3U8) }
            .build()
        val wasPlaying = player.playWhenReady || player.mediaItemCount == 0
        _firstFrameShown.value = false
        player.setMediaItem(item, startMs.coerceAtLeast(0))
        player.prepare()
        player.playWhenReady = wasPlaying
        _phase.value = PlaybackPhase.LOADING
    }

    override fun setPlaying(playing: Boolean) {
        if (playing && player.playbackState == Player.STATE_ENDED) player.seekTo(0)
        player.playWhenReady = playing
    }

    override fun seekTo(positionMs: Long) {
        player.setSeekParameters(SeekParameters.DEFAULT)
        player.seekTo(positionMs.coerceAtLeast(0))
    }

    override fun scrubTo(positionMs: Long) {
        player.setSeekParameters(SeekParameters.CLOSEST_SYNC)
        player.seekTo(positionMs.coerceAtLeast(0))
    }

    override fun setVolume(volume: Float) {
        player.volume = volume.coerceIn(0f, 1f)
    }

    override fun setSpeed(speed: Float) {
        player.playbackParameters = PlaybackParameters(speed.coerceIn(0.25f, 2f))
    }

    override fun setLooping(looping: Boolean) {
        player.repeatMode = if (looping) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
    }

    override fun positionMs(): Long = player.currentPosition

    override fun durationMs(): Long = player.duration.takeIf { it != C.TIME_UNSET } ?: 0L

    override fun release() {
        player.removeListener(listener)
        player.release()
    }
}
