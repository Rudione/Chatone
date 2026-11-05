package io.rudione.chatone.presentation.media

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.clip_preview_processing
import chatone.composeapp.generated.resources.clip_trim_hint_remote
import chatone.composeapp.generated.resources.media_copy_link
import chatone.composeapp.generated.resources.media_length
import chatone.composeapp.generated.resources.media_preview_unavailable
import chatone.composeapp.generated.resources.media_publish
import chatone.composeapp.generated.resources.media_published
import chatone.composeapp.generated.resources.media_selection_hint
import chatone.composeapp.generated.resources.media_share
import chatone.composeapp.generated.resources.media_title_hint
import chatone.composeapp.generated.resources.media_title_label
import chatone.composeapp.generated.resources.media_watch_clip
import coil3.compose.AsyncImage
import io.rudione.chatone.domain.media.ClipTrim
import io.rudione.chatone.domain.media.MediaPlayback
import io.rudione.chatone.domain.media.PlaybackPhase
import io.rudione.chatone.icons.lucide.CircleCheck
import io.rudione.chatone.icons.lucide.Copy
import io.rudione.chatone.icons.lucide.Lucide
import io.rudione.chatone.icons.lucide.Play
import io.rudione.chatone.icons.lucide.Share2
import io.rudione.chatone.presentation.components.ChatoneTextField
import io.rudione.chatone.presentation.components.expressive.GlassIconButton
import io.rudione.chatone.presentation.components.expressive.HeroPillButton
import io.rudione.chatone.presentation.components.expressive.PillButton
import io.rudione.chatone.presentation.components.expressive.PillSegmented
import io.rudione.chatone.presentation.components.expressive.PillTone
import io.rudione.chatone.presentation.components.expressive.WavyLinearProgress
import io.rudione.chatone.presentation.profile.formatDuration
import io.rudione.chatone.presentation.stream.ambient.StreamAmbience
import io.rudione.chatone.presentation.stream.ambient.ambientHalo
import io.rudione.chatone.util.link.shareLink
import io.rudione.chatone.util.platform.currentFormFactor
import io.rudione.chatone.util.platform.setText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

private val PreviewShape = RoundedCornerShape(20.dp)
private val WideLayoutWidth = 700.dp
private val PRESETS = listOf(15f, 30f, 60f)
private val NoFrame: StateFlow<Boolean> = MutableStateFlow(false)
private val IdlePhase: StateFlow<PlaybackPhase> = MutableStateFlow(PlaybackPhase.IDLE)

@Stable
private class ClipPlayhead(initial: Float) {
    var seconds by mutableFloatStateOf(initial)
}

@Composable
private fun rememberClipPlayhead(playback: MediaPlayback?, start: Float): ClipPlayhead {
    val playhead = remember(playback) { ClipPlayhead(start) }
    val frameShown by (playback?.firstFrameShown ?: NoFrame).collectAsState()
    val latestStart by rememberUpdatedState(start)
    LaunchedEffect(playhead, frameShown) {
        while (isActive) {
            withFrameMillis { }
            playhead.seconds = if (frameShown && playback != null) {
                playback.positionMs() / 1000f
            } else {
                latestStart
            }
        }
    }
    return playhead
}

@Composable
private fun ClipPreview(
    state: ClipEditorState,
    playback: MediaPlayback?,
    ambience: StreamAmbience,
    modifier: Modifier = Modifier
) {
    val darkBackground = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val phase by (playback?.phase ?: IdlePhase).collectAsState()
    val frameShown by (playback?.firstFrameShown ?: NoFrame).collectAsState()
    val draft = state.draft
    Box(modifier.ambientHalo(ambience, darkBackground, spread = 36.dp)) {
        Box(Modifier.fillMaxSize().clip(PreviewShape).background(Color.Black)) {
            if (draft != null) {
                MediaVideoSurface(playback, Modifier.fillMaxSize())
            } else {
                Text(
                    stringResource(Res.string.media_preview_unavailable),
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.Center).padding(16.dp)
                )
            }
            AnimatedVisibility(
                visible = draft != null && !frameShown,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(Modifier.fillMaxSize().background(Color.Black)) {
                    if (draft != null && draft.thumbnailUrl.isNotEmpty()) {
                        AsyncImage(
                            model = draft.thumbnailUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            alpha = 0.6f,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Column(
                        Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            stringResource(Res.string.clip_preview_processing),
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge,
                            textAlign = TextAlign.Center
                        )
                        if (phase != PlaybackPhase.READY) {
                            WavyLinearProgress(
                                color = Color.White,
                                trackColor = Color.White.copy(alpha = 0.25f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun EditingPane(
    state: ClipEditorState,
    viewModel: ClipEditorViewModel,
    ambience: StreamAmbience
) {
    val playhead = rememberClipPlayhead(viewModel.playback, state.start)
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val previewModifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)
        if (maxWidth >= WideLayoutWidth) {
            Row(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Column(Modifier.weight(0.6f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    ClipPreview(state, viewModel.playback, ambience, previewModifier)
                    ClipTimelineSection(state, playhead, viewModel)
                }
                Column(Modifier.weight(0.4f)) { ClipPublishForm(state, viewModel) }
            }
        } else {
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ClipPreview(state, viewModel.playback, ambience, previewModifier)
                ClipTimelineSection(state, playhead, viewModel)
                ClipPublishForm(state, viewModel)
            }
        }
    }
}

@Composable
private fun ClipTimelineSection(
    state: ClipEditorState,
    playhead: ClipPlayhead,
    viewModel: ClipEditorViewModel
) {
    val remoteControl = remember { currentFormFactor().isLeanBack }
    val presets = remember(state.totalSeconds) {
        PRESETS.filter { it <= state.totalSeconds }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ClipTrimTimeline(
            totalSeconds = state.totalSeconds,
            start = state.start,
            end = state.end,
            playhead = { ClipTrim.playhead(playhead.seconds, state.range) },
            onTrimStart = { viewModel.sendEvent(ClipEditorEvent.BeginTrim) },
            onRangeChange = { a, b, at -> viewModel.sendEvent(ClipEditorEvent.SetRange(a, b, at)) },
            onTrimEnd = { viewModel.sendEvent(ClipEditorEvent.EndTrim) },
            onSeek = { viewModel.sendEvent(ClipEditorEvent.Seek(it)) }
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                formatDuration(state.start.toInt()),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                stringResource(Res.string.media_length, state.length.toInt()),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                textAlign = TextAlign.Center
            )
            Text(
                formatDuration(state.end.toInt()),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            stringResource(if (remoteControl) Res.string.clip_trim_hint_remote else Res.string.media_selection_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (presets.size > 1) {
            PillSegmented(
                options = presets.map { it to stringResource(Res.string.media_length, it.toInt()) },
                selected = state.length,
                onSelect = { viewModel.sendEvent(ClipEditorEvent.Preset(it)) },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
private fun ClipPublishForm(state: ClipEditorState, viewModel: ClipEditorViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ChatoneTextField(
            value = state.title,
            onValueChange = { viewModel.sendEvent(ClipEditorEvent.EditTitle(it)) },
            modifier = Modifier.fillMaxWidth(),
            label = stringResource(Res.string.media_title_label),
            hint = state.failure?.let { failureText(it) }
                ?: stringResource(Res.string.media_title_hint),
            isError = state.failure != null
        )
        HeroPillButton(
            text = stringResource(Res.string.media_publish),
            onClick = { viewModel.sendEvent(ClipEditorEvent.Publish) },
            enabled = state.canPublish,
            loading = state.phase == ClipEditorPhase.PUBLISHING,
            height = 54.dp,
            modifier = Modifier.fillMaxWidth()
        )
        AnimatedVisibility(
            visible = state.phase == ClipEditorPhase.PUBLISHING,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            WavyLinearProgress(Modifier.padding(horizontal = 24.dp))
        }
    }
}

@Composable
internal fun PublishedPane(
    slug: String,
    title: String,
    thumbnail: String,
    ambience: StreamAmbience,
    navigation: ClipEditorNavigation
) {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val url = "https://clips.twitch.tv/$slug"
    val darkBackground = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val canShare = remember { !currentFormFactor().isLeanBack }
    Centered {
        val pop = remember { Animatable(0.5f) }
        LaunchedEffect(Unit) {
            pop.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMediumLow))
        }
        Icon(
            Lucide.CircleCheck,
            null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(56.dp).graphicsLayer { scaleX = pop.value; scaleY = pop.value }
        )
        Text(
            stringResource(Res.string.media_published),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        if (thumbnail.isNotEmpty()) {
            AsyncImage(
                model = thumbnail,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .widthIn(max = 460.dp)
                    .fillMaxWidth(0.82f)
                    .aspectRatio(16f / 9f)
                    .ambientHalo(ambience, darkBackground, spread = 32.dp)
                    .clip(PreviewShape)
            )
        }
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        HeroPillButton(
            stringResource(Res.string.media_watch_clip),
            onClick = { navigation.onWatch(slug) },
            icon = Lucide.Play,
            height = 52.dp
        )
        if (canShare) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.widthIn(max = 360.dp)
            ) {
                PillButton(
                    stringResource(Res.string.media_copy_link),
                    onClick = { scope.launch { clipboard.setText(url) } },
                    icon = Lucide.Copy,
                    tone = PillTone.Tonal,
                    height = 44.dp,
                    modifier = Modifier.weight(1f, fill = false)
                )
                GlassIconButton(
                    Lucide.Share2,
                    stringResource(Res.string.media_share),
                    onClick = { shareLink(url, title) },
                    modifier = Modifier.background(
                        MaterialTheme.colorScheme.secondaryContainer,
                        CircleShape
                    )
                )
            }
        }
    }
}
