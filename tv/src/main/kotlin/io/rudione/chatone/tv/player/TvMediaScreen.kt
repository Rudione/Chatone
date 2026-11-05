package io.rudione.chatone.tv.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.media_load_failed
import chatone.composeapp.generated.resources.media_pause
import chatone.composeapp.generated.resources.media_play
import chatone.composeapp.generated.resources.tv_seek_back
import chatone.composeapp.generated.resources.tv_seek_forward
import io.rudione.chatone.domain.media.MediaKind
import io.rudione.chatone.domain.media.MediaPlayback
import io.rudione.chatone.domain.media.PlaybackPhase
import io.rudione.chatone.icons.lucide.ChevronLeft
import io.rudione.chatone.icons.lucide.ChevronRight
import io.rudione.chatone.icons.lucide.Lucide
import io.rudione.chatone.icons.lucide.Pause
import io.rudione.chatone.icons.lucide.Play
import io.rudione.chatone.presentation.components.SystemBackHandler
import io.rudione.chatone.presentation.components.expressive.MorphingLoadingIndicator
import io.rudione.chatone.presentation.components.rememberScopedViewModelStoreOwner
import io.rudione.chatone.presentation.media.MediaVideoSurface
import io.rudione.chatone.presentation.media.MediaViewerEvent
import io.rudione.chatone.presentation.media.MediaViewerViewModel
import io.rudione.chatone.presentation.media.ViewerLoad
import io.rudione.chatone.tv.components.TvButtonTone
import io.rudione.chatone.tv.components.TvIconButton
import io.rudione.chatone.tv.components.TvMessage
import io.rudione.chatone.tv.theme.TvDimens
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

private const val SEEK_STEP_MS = 10_000L
private const val CONTROLS_HIDE_MS = 4_000L
private const val CLOCK_TICK_MS = 500L

@Composable
fun TvMediaScreen(kind: MediaKind, id: String, onClose: () -> Unit, modifier: Modifier = Modifier) {
    val session = rememberScopedViewModelStoreOwner()
    val viewModel: MediaViewerViewModel = koinViewModel(viewModelStoreOwner = session) {
        parametersOf(kind, id, 0)
    }
    val state by viewModel.state.collectAsState()
    SystemBackHandler(onBack = onClose)
    Box(modifier
        .fillMaxSize()
        .background(Color.Black)) {
        when (state.load) {
            ViewerLoad.FAILED, ViewerLoad.MISSING -> TvMessage(
                stringResource(Res.string.media_load_failed),
                modifier = Modifier.align(Alignment.Center),
                onRetry = { viewModel.sendEvent(MediaViewerEvent.Retry) }
            )

            ViewerLoad.LOADING -> MorphingLoadingIndicator(
                size = 64.dp,
                modifier = Modifier.align(Alignment.Center)
            )

            ViewerLoad.READY -> TvMediaPlayer(
                viewModel.playback,
                state.details?.title.orEmpty(),
                kind
            )
        }
    }
}

@Composable
private fun TvMediaPlayer(playback: MediaPlayback?, title: String, kind: MediaKind) {
    if (playback == null) return
    val phase by playback.phase.collectAsState()
    val playing by playback.playing.collectAsState()
    var controls by remember { mutableStateOf(true) }
    var touches by remember { mutableIntStateOf(0) }
    var progress by remember { mutableLongStateOf(0L) }
    val rootFocus = remember { FocusRequester() }
    val buttonFocus = remember { FocusRequester() }
    val seek: (Long) -> Unit = { delta ->
        playback.seekTo(
            (playback.positionMs() + delta).coerceIn(
                0L,
                playback.durationMs().coerceAtLeast(0L)
            )
        )
        controls = true
        touches++
    }
    LaunchedEffect(playback) {
        while (true) {
            val duration = playback.durationMs().coerceAtLeast(1L)
            progress = playback.positionMs() * 1000L / duration
            delay(CLOCK_TICK_MS)
        }
    }
    LaunchedEffect(controls, touches, playing) {
        if (controls && playing) {
            delay(CONTROLS_HIDE_MS)
            controls = false
        }
    }
    LaunchedEffect(controls) {
        runCatching { if (controls) buttonFocus.requestFocus() else rootFocus.requestFocus() }
    }
    Box(
        Modifier
            .fillMaxSize()
            .focusRequester(rootFocus)
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.MediaPlayPause, Key.MediaPlay, Key.MediaPause -> {
                        playback.setPlaying(!playing)
                        true
                    }

                    Key.MediaFastForward -> {
                        seek(SEEK_STEP_MS)
                        true
                    }

                    Key.MediaRewind -> {
                        seek(-SEEK_STEP_MS)
                        true
                    }

                    else -> false
                }
            }
            .onKeyEvent { event ->
                if (controls || event.type != KeyEventType.KeyDown) return@onKeyEvent false
                when (event.key) {
                    Key.DirectionLeft -> seek(-SEEK_STEP_MS)
                    Key.DirectionRight -> seek(SEEK_STEP_MS)
                    Key.DirectionCenter, Key.Enter, Key.NumPadEnter, Key.DirectionUp, Key.DirectionDown -> controls =
                        true

                    else -> return@onKeyEvent false
                }
                true
            }
            .focusable()
    ) {
        MediaVideoSurface(playback, Modifier.fillMaxSize())
        if (phase == PlaybackPhase.LOADING) {
            MorphingLoadingIndicator(size = 64.dp, modifier = Modifier.align(Alignment.Center))
        }
        AnimatedVisibility(visible = controls, enter = fadeIn(), exit = fadeOut()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.5f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.85f)
                        )
                    )
                    .padding(
                        horizontal = TvDimens.ScreenHorizontal,
                        vertical = TvDimens.ScreenVertical
                    ),
                verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.Bottom)
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                TvProgressLine({ progress / 1000f })
                Row(
                    Modifier.focusGroup(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (kind == MediaKind.VIDEO) {
                        TvIconButton(
                            Lucide.ChevronLeft,
                            stringResource(Res.string.tv_seek_back),
                            { seek(-SEEK_STEP_MS) })
                    }
                    TvIconButton(
                        if (playing) Lucide.Pause else Lucide.Play,
                        stringResource(if (playing) Res.string.media_pause else Res.string.media_play),
                        onClick = {
                            playback.setPlaying(!playing)
                            touches++
                        },
                        tone = TvButtonTone.Primary,
                        size = 56.dp,
                        modifier = Modifier.focusRequester(buttonFocus)
                    )
                    if (kind == MediaKind.VIDEO) {
                        TvIconButton(
                            Lucide.ChevronRight,
                            stringResource(Res.string.tv_seek_forward),
                            { seek(SEEK_STEP_MS) })
                    }
                }
            }
        }
    }
}

@Composable
private fun TvProgressLine(fraction: () -> Float) {
    val track = Color.White.copy(alpha = 0.3f)
    val fill = MaterialTheme.colorScheme.primary
    Box(Modifier
        .fillMaxWidth()
        .height(4.dp)
        .clip(CircleShape)
        .background(track)) {
        Box(
            Modifier
                .fillMaxWidth(fraction().coerceIn(0f, 1f))
                .height(4.dp)
                .background(fill)
        )
    }
}
