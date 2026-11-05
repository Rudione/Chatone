package io.rudione.chatone.presentation.media

import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.media_action_failed
import chatone.composeapp.generated.resources.media_ban_curator
import chatone.composeapp.generated.resources.media_ban_curator_confirm
import chatone.composeapp.generated.resources.media_banned
import chatone.composeapp.generated.resources.media_cancel
import chatone.composeapp.generated.resources.media_close
import chatone.composeapp.generated.resources.media_confirm
import chatone.composeapp.generated.resources.media_delete_clip
import chatone.composeapp.generated.resources.media_delete_clip_confirm
import chatone.composeapp.generated.resources.media_deleted
import chatone.composeapp.generated.resources.media_load_failed
import chatone.composeapp.generated.resources.media_missing
import chatone.composeapp.generated.resources.media_retry
import org.jetbrains.compose.resources.stringResource
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import io.rudione.chatone.domain.media.MediaDetails
import io.rudione.chatone.domain.media.MediaKind
import io.rudione.chatone.presentation.components.SystemBackHandler
import io.rudione.chatone.presentation.components.expressive.expressiveGlass
import kotlinx.coroutines.delay
import io.rudione.chatone.presentation.components.rememberScopedViewModelStoreOwner
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Immutable
class MediaViewerNavigation(
    val onClose: () -> Unit,
    val onOpenProfile: (String) -> Unit,
    val onOpenUrl: (String) -> Unit,
    val onClipVideo: ((MediaDetails, Float) -> Unit)? = null
)

internal enum class PendingAction { DELETE, BAN }

@Composable
fun MediaViewerScreen(
    kind: MediaKind,
    id: String,
    startSeconds: Int,
    navigation: MediaViewerNavigation,
    modifier: Modifier = Modifier
) {
    val mediaDeletedText = stringResource(Res.string.media_deleted)
    val mediaBannedText = stringResource(Res.string.media_banned)
    val mediaActionFailedText = stringResource(Res.string.media_action_failed)
    val session = rememberScopedViewModelStoreOwner()
    val viewModel: MediaViewerViewModel =
        koinViewModel(viewModelStoreOwner = session, key = "media_${kind.name}_$id") {
            parametersOf(kind, id, startSeconds)
        }
    val state by viewModel.state.collectAsState()
    val clock = rememberPlaybackClock(viewModel.playback)
    val chrome = rememberMediaChromeState()
    val playing = rememberPlaybackPlaying(viewModel.playback)
    val navigationBars = WindowInsets.navigationBars.asPaddingValues()
    var notice by remember { mutableStateOf<String?>(null) }
    var pending by remember { mutableStateOf<PendingAction?>(null) }
    SystemBackHandler(onBack = navigation.onClose)

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            notice = when (effect) {
                MediaViewerEffect.Deleted -> mediaDeletedText
                MediaViewerEffect.Banned -> mediaBannedText
                MediaViewerEffect.ActionFailed -> mediaActionFailedText
            }
        }
    }
    LaunchedEffect(notice) {
        if (notice != null) {
            delay(2_600)
            notice = null
        }
    }
    val positionSeconds = (clock.positionMs / 1000).toInt()
    LaunchedEffect(positionSeconds / REPLAY_PROGRESS_STEP) {
        viewModel.sendEvent(
            MediaViewerEvent.Progress(
                positionSeconds
            )
        )
    }

    Surface(modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        BoxWithConstraints(
            Modifier.fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
        ) {
            val wide = maxWidth >= WIDE_LAYOUT
            val details = state.details
            when {
                state.load == ViewerLoad.LOADING -> Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 2.5.dp)
                }

                details == null || state.load != ViewerLoad.READY -> Column(
                    Modifier.fillMaxSize(),
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        if (state.load == ViewerLoad.MISSING) stringResource(Res.string.media_missing) else stringResource(
                            Res.string.media_load_failed
                        ), style = MaterialTheme.typography.bodyLarge
                    )
                    Row {
                        TextButton(onClick = { viewModel.sendEvent(MediaViewerEvent.Retry) }) {
                            Text(
                                stringResource(Res.string.media_retry)
                            )
                        }
                        TextButton(onClick = navigation.onClose) { Text(stringResource(Res.string.media_close)) }
                    }
                }

                else -> {
                    val hasReplay = details.replayVideoId != null
                    val onClip = navigation.onClipVideo
                        ?.takeIf { details.kind == MediaKind.VIDEO }
                        ?.let { clip ->
                            {
                                viewModel.playback?.setPlaying(false)
                                clip(details, clock.positionMs / 1000f)
                            }
                        }
                    val frame: @Composable (Modifier) -> Unit = { frameModifier ->
                        MediaPlayerFrame(
                            playback = viewModel.playback,
                            clock = clock,
                            chrome = chrome,
                            volume = MediaVolume(state.volume, state.muted),
                            onToggleMute = { viewModel.sendEvent(MediaViewerEvent.ToggleMute) },
                            onVolume = { viewModel.sendEvent(MediaViewerEvent.SetVolume(it)) },
                            onClose = navigation.onClose,
                            modifier = frameModifier
                        )
                    }
                    val header: @Composable (Modifier) -> Unit = { headerModifier ->
                        MediaInfoHeader(
                            state = state,
                            details = details,
                            onEvent = viewModel::sendEvent,
                            onAsk = { pending = it },
                            onNotice = { notice = it },
                            onClip = onClip,
                            navigation = navigation,
                            modifier = headerModifier.pointerInput(chrome) {
                                awaitEachGesture {
                                    awaitFirstDown(requireUnconsumed = false)
                                    chrome.poke()
                                }
                            }
                        )
                    }
                    val chat: @Composable (Modifier) -> Unit = { chatModifier ->
                        if (hasReplay) {
                            MediaReplayChat(
                                entries = state.replayMessages,
                                nowSeconds = details.replayOffsetSeconds + positionSeconds,
                                hasClock = state.replayHasClock,
                                contentPadding = PaddingValues(bottom = navigationBars.calculateBottomPadding() + 8.dp),
                                modifier = chatModifier
                            )
                        }
                    }
                    if (wide) {
                        Row(Modifier.fillMaxSize()) {
                            Box(
                                Modifier.weight(0.64f).fillMaxHeight(),
                                contentAlignment = Alignment.Center
                            ) {
                                frame(Modifier.fillMaxWidth().aspectRatio(16f / 9f))
                            }
                            Column(Modifier.weight(0.36f).fillMaxHeight()) {
                                header(Modifier.fillMaxWidth())
                                chat(Modifier.weight(1f).fillMaxWidth())
                            }
                        }
                    } else {
                        Column(Modifier.fillMaxSize()) {
                            frame(Modifier.fillMaxWidth().aspectRatio(16f / 9f))
                            AnimatedVisibility(
                                visible = !hasReplay || chrome.visible || !playing,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                header(Modifier.fillMaxWidth())
                            }
                            chat(Modifier.weight(1f).fillMaxWidth())
                        }
                    }
                }
            }
            AnimatedVisibility(
                visible = notice != null,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.navigationBars).padding(bottom = 18.dp)
            ) {
                Text(
                    notice.orEmpty(),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.expressiveGlass(CircleShape)
                        .padding(horizontal = 18.dp, vertical = 10.dp)
                )
            }
        }
    }

    val action = pending
    val curatorName = state.details?.curator?.displayName.orEmpty()
    if (action != null) {
        AlertDialog(
            onDismissRequest = { pending = null },
            title = {
                Text(
                    if (action == PendingAction.DELETE) stringResource(Res.string.media_delete_clip) else stringResource(
                        Res.string.media_ban_curator
                    )
                )
            },
            text = {
                Text(
                    if (action == PendingAction.DELETE) stringResource(Res.string.media_delete_clip_confirm) else stringResource(
                        Res.string.media_ban_curator_confirm,
                        curatorName
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    pending = null
                    viewModel.sendEvent(if (action == PendingAction.DELETE) MediaViewerEvent.DeleteClip else MediaViewerEvent.BanCurator)
                }) {
                    Text(
                        stringResource(Res.string.media_confirm),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    pending = null
                }) { Text(stringResource(Res.string.media_cancel)) }
            }
        )
    }
}

private val WIDE_LAYOUT = 720.dp
private const val REPLAY_PROGRESS_STEP = 5
