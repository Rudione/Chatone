package io.rudione.chatone.presentation.media

import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.media_close
import chatone.composeapp.generated.resources.media_mute
import chatone.composeapp.generated.resources.media_pause
import chatone.composeapp.generated.resources.media_play
import chatone.composeapp.generated.resources.media_unmute
import chatone.composeapp.generated.resources.media_volume
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.rudione.chatone.domain.media.MediaPlayback
import io.rudione.chatone.domain.media.PlaybackPhase
import io.rudione.chatone.icons.lucide.Lucide
import io.rudione.chatone.icons.lucide.Pause
import io.rudione.chatone.icons.lucide.Play
import io.rudione.chatone.icons.lucide.Volume2
import io.rudione.chatone.icons.lucide.VolumeX
import io.rudione.chatone.icons.lucide.X
import io.rudione.chatone.presentation.components.ChatoneSlider
import io.rudione.chatone.presentation.components.expressive.GlassIconButton
import io.rudione.chatone.presentation.components.expressive.expressiveGlass
import io.rudione.chatone.presentation.profile.formatDuration
import io.rudione.chatone.presentation.stream.ambient.StreamAmbience
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.jetbrains.compose.resources.stringResource

@Stable
class PlaybackClock internal constructor() {
    var positionMs by mutableLongStateOf(0L)
        private set
    var durationMs by mutableLongStateOf(0L)
        private set

    val fraction: Float get() = if (durationMs <= 0L) 0f else (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)

    internal fun update(positionMs: Long, durationMs: Long) {
        this.positionMs = positionMs
        this.durationMs = durationMs
    }
}

@Composable
internal fun rememberPlaybackClock(playback: MediaPlayback?, fallbackDurationMs: Long = 0L): PlaybackClock {
    val clock = remember(playback) { PlaybackClock() }
    val fallback by rememberUpdatedState(fallbackDurationMs)
    LaunchedEffect(playback, clock) {
        while (playback != null) {
            clock.update(playback.positionMs(), playback.durationMs().takeIf { it > 0L } ?: fallback)
            delay(CLOCK_TICK_MS)
        }
    }
    return clock
}

@Stable
class MediaChromeState {
    var visible by mutableStateOf(true)
        private set
    var touches by mutableIntStateOf(0)
        private set

    fun toggle() {
        visible = !visible
        touches++
    }

    fun poke() {
        visible = true
        touches++
    }

    fun hide() {
        visible = false
    }
}

@Composable
internal fun rememberMediaChromeState(): MediaChromeState = remember { MediaChromeState() }

@Composable
internal fun rememberPlaybackPlaying(playback: MediaPlayback?): Boolean {
    val playing by (playback?.playing ?: NotPlaying).collectAsState()
    return playing
}

@Immutable
class MediaVolume(val level: Float, val muted: Boolean)

@Composable
internal fun MediaPlayerFrame(
    playback: MediaPlayback?,
    clock: PlaybackClock,
    chrome: MediaChromeState,
    volume: MediaVolume,
    onToggleMute: () -> Unit,
    onVolume: (Float) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    ambience: StreamAmbience? = null
) {
    val phase by (playback?.phase ?: IdlePhase).collectAsState()
    val playing = rememberPlaybackPlaying(playback)
    var scrubbing by remember { mutableStateOf<Float?>(null) }
    val controlsShown = chrome.visible || !playing
    LaunchedEffect(chrome.visible, playing, chrome.touches, scrubbing) {
        if (chrome.visible && playing && scrubbing == null) {
            delay(CONTROLS_HIDE_MS)
            chrome.hide()
        }
    }
    Box(
        modifier.background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = chrome::toggle
            )
    ) {
        MediaVideoSurface(playback, Modifier.fillMaxSize(), frameSink = ambience?.takeIf { playing })
        if (phase == PlaybackPhase.LOADING) {
            CircularProgressIndicator(
                Modifier.size(34.dp).align(Alignment.Center),
                color = Color.White,
                strokeWidth = 3.dp
            )
        }
        AnimatedVisibility(
            visible = !controlsShown,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
        ) {
            PlaybackProgressLine(clock, Modifier.fillMaxWidth().height(PROGRESS_LINE_HEIGHT))
        }
        AnimatedVisibility(
            visible = controlsShown,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.45f),
                        0.3f to Color.Transparent,
                        0.65f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.6f)
                    )
                )
            ) {
                GlassIconButton(
                    Lucide.X,
                    stringResource(Res.string.media_close),
                    onClose,
                    Modifier.align(Alignment.TopStart).padding(8.dp)
                        .expressiveGlass(CircleShape, elevation = 2.dp),
                    size = 40.dp
                )
                if (phase != PlaybackPhase.LOADING) {
                    GlassIconButton(
                        icon = if (playing) Lucide.Pause else Lucide.Play,
                        contentDescription = if (playing) stringResource(Res.string.media_pause) else stringResource(
                            Res.string.media_play
                        ),
                        onClick = {
                            playback?.setPlaying(!playing)
                            chrome.poke()
                        },
                        modifier = Modifier.align(Alignment.Center)
                            .expressiveGlass(CircleShape, elevation = 4.dp),
                        size = 60.dp
                    )
                }
                Column(
                    Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    SeekBar(
                        clock = clock,
                        scrubbing = scrubbing,
                        onScrub = {
                            scrubbing = it
                            chrome.poke()
                        },
                        onScrubFinished = {
                            scrubbing?.let { fraction -> playback?.seekTo((fraction * clock.durationMs).toLong()) }
                            scrubbing = null
                        }
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PlaybackTime(clock, scrubbing, Modifier.weight(1f))
                        Box(
                            Modifier.size(36.dp).clip(CircleShape)
                                .clickable {
                                    onToggleMute()
                                    chrome.poke()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (volume.muted) Lucide.VolumeX else Lucide.Volume2,
                                if (volume.muted) stringResource(Res.string.media_unmute) else stringResource(
                                    Res.string.media_mute
                                ),
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        val volumeLabel = stringResource(Res.string.media_volume)
                        ChatoneSlider(
                            value = if (volume.muted) 0f else volume.level,
                            onValueChange = {
                                onVolume(it)
                                chrome.poke()
                            },
                            trackHeight = 3.dp,
                            knobSize = 11.dp,
                            inactive = OnVideoTrack,
                            modifier = Modifier.width(VOLUME_SLIDER_WIDTH)
                                .semantics { contentDescription = volumeLabel }
                        )
                        Box(Modifier.width(4.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SeekBar(
    clock: PlaybackClock,
    scrubbing: Float?,
    onScrub: (Float) -> Unit,
    onScrubFinished: () -> Unit
) {
    ChatoneSlider(
        value = scrubbing ?: clock.fraction,
        onValueChange = onScrub,
        onValueChangeFinished = onScrubFinished,
        enabled = clock.durationMs > 0L,
        knobSize = 14.dp,
        inactive = OnVideoTrack,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun PlaybackTime(clock: PlaybackClock, scrubbing: Float?, modifier: Modifier = Modifier) {
    val shown = scrubbing?.let { (it * clock.durationMs).toLong() } ?: clock.positionMs
    Text(
        "${formatDuration((shown / 1000).toInt())} / ${formatDuration((clock.durationMs / 1000).toInt())}",
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = Color.White,
        modifier = modifier
    )
}

@Composable
private fun PlaybackProgressLine(clock: PlaybackClock, modifier: Modifier = Modifier) {
    val accent = MaterialTheme.colorScheme.primary
    Spacer(
        modifier.drawBehind {
            drawRect(OnVideoTrack)
            val fraction = clock.fraction
            if (fraction > 0f) drawRect(accent, size = Size(size.width * fraction, size.height))
        }
    )
}

private val IdlePhase: StateFlow<PlaybackPhase> = MutableStateFlow(PlaybackPhase.ERROR)
private val NotPlaying: StateFlow<Boolean> = MutableStateFlow(false)
private const val CLOCK_TICK_MS = 250L
private const val CONTROLS_HIDE_MS = 3_000L
private val PROGRESS_LINE_HEIGHT = 2.5.dp
private val OnVideoTrack = Color.White.copy(alpha = 0.28f)
private val VOLUME_SLIDER_WIDTH = 84.dp
