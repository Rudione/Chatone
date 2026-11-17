package io.rudione.chatone.presentation.settings.sound

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.settings_enable_mention_sound_desc
import chatone.composeapp.generated.resources.settings_mention_sound
import chatone.composeapp.generated.resources.settings_volume
import io.rudione.chatone.icons.lucide.Lucide
import io.rudione.chatone.icons.lucide.Volume1
import io.rudione.chatone.icons.lucide.Volume2
import io.rudione.chatone.icons.lucide.VolumeX
import io.rudione.chatone.presentation.components.ChatoneSlider
import io.rudione.chatone.presentation.components.ChatoneSwitch
import io.rudione.chatone.presentation.components.rows.HighlightedSettingsText
import io.rudione.chatone.presentation.settings.SettingsEvent
import io.rudione.chatone.presentation.settings.SettingsState
import io.rudione.chatone.presentation.settings.components.SettingsPair
import io.rudione.chatone.presentation.settings.components.settingsCard
import io.rudione.chatone.presentation.settings.settingsAnchor
import io.rudione.chatone.util.media.pickAudioFile
import io.rudione.chatone.util.system.isDesktopPlatform
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun MentionSoundSection(
    settings: SettingsState,
    onSettingsEvent: (SettingsEvent) -> Unit,
    viewModel: MentionSoundViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val onEvent = viewModel::onEvent
    val scope = rememberCoroutineScope()
    val volume = settings.mentionSoundVolume
    val pick: () -> Unit = {
        scope.launch { pickAudioFile()?.let { onEvent(MentionSoundEvent.Import(listOf(it))) } }
    }
    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
        MentionSoundContent(state, settings, volume, onSettingsEvent, onEvent, pick)
        state.editor?.let { editor ->
            SoundEditorDialog(editor = editor, playing = state.playing, volume = volume, onEvent = onEvent)
        }
    }
}

@Composable
private fun MentionSoundContent(
    state: MentionSoundUiState,
    settings: SettingsState,
    volume: Float,
    onSettingsEvent: (SettingsEvent) -> Unit,
    onEvent: (MentionSoundEvent) -> Unit,
    pick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(SectionGap)) {
        SoundHeroCard(
            state = state,
            enabled = settings.mentionSoundEnabled,
            volume = volume,
            onEnabledChange = { onSettingsEvent(SettingsEvent.OnMentionSoundChanged(it)) },
            onVolumeChange = { onSettingsEvent(SettingsEvent.OnMentionSoundVolumeChanged(it)) },
            onEvent = onEvent
        )
        AnimatedVisibility(
            visible = settings.mentionSoundEnabled,
            enter = expandVertically(spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(tween(220)),
            exit = shrinkVertically(spring(stiffness = Spring.StiffnessMedium)) + fadeOut(tween(140))
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(SectionGap)) {
                SoundLibraryCard(state = state, volume = volume, onEvent = onEvent, onPick = pick)
                SettingsPair(
                    first = { ChannelSoundsCard(state = state, volume = volume, onEvent = onEvent) },
                    second = { SoundAdvancedCard(state = state, onEvent = onEvent) }
                )
            }
        }
    }
}

@Composable
private fun SoundHeroCard(
    state: MentionSoundUiState,
    enabled: Boolean,
    volume: Float,
    onEnabledChange: (Boolean) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onEvent: (MentionSoundEvent) -> Unit
) {
    val selected = state.config.selectedId
    val playing = state.playing?.id == selected
    val progress by rememberPlaybackProgress(state.playing, selected)
    val aura by animateFloatAsState(if (playing) 1f else 0f, spring(stiffness = Spring.StiffnessVeryLow), label = "heroAura")
    val glow = MaterialTheme.colorScheme.primary
    val title = stringResource(Res.string.settings_mention_sound)
    Column(
        modifier = Modifier
            .settingsAnchor(title)
            .fillMaxWidth()
            .settingsCard()
            .drawBehind {
                val radius = size.maxDimension * (0.55f + 0.25f * aura)
                drawRect(
                    Brush.radialGradient(
                        listOf(glow.copy(alpha = 0.10f + 0.12f * aura), glow.copy(alpha = 0f)),
                        center = Offset(size.width * 0.12f, size.height * 0.2f),
                        radius = radius
                    )
                )
            }
            .padding(horizontal = CardPadding, vertical = CardPadding),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ScallopPlayButton(
                playing = playing,
                enabled = enabled,
                onClick = {
                    if (playing) onEvent(MentionSoundEvent.StopPreview)
                    else onEvent(MentionSoundEvent.Preview(selected, volume.coerceAtLeast(0.05f)))
                },
                size = if (isDesktopPlatform) 60.dp else 68.dp
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                HighlightedSettingsText(
                    title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 0.6.sp,
                    fontWeight = FontWeight.SemiBold
                )
                AnimatedContent(
                    targetState = selected,
                    transitionSpec = {
                        (slideInVertically(spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow)) { it / 2 } + fadeIn()) togetherWith
                            (slideOutVertically { -it / 2 } + fadeOut(tween(120)))
                    },
                    label = "heroName"
                ) { id ->
                    Text(
                        soundName(id, state),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    stringResource(Res.string.settings_enable_mention_sound_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(10.dp))
            ChatoneSwitch(checked = enabled, onCheckedChange = onEnabledChange)
        }
        AnimatedVisibility(
            visible = enabled,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SoundWaveform(
                    peaks = state.info(selected)?.peaks.orEmpty(),
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(if (isDesktopPlatform) 44.dp else 52.dp),
                    barCount = if (isDesktopPlatform) 56 else 44
                )
                VolumeRow(volume = volume, onChange = onVolumeChange, onCommit = {
                    onEvent(MentionSoundEvent.Preview(selected, volume))
                })
            }
        }
    }
}

@Composable
private fun VolumeRow(volume: Float, onChange: (Float) -> Unit, onCommit: () -> Unit) {
    val label = stringResource(Res.string.settings_volume)
    Row(
        modifier = Modifier.settingsAnchor(label).fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AnimatedContent(
            targetState = when {
                volume <= 0.001f -> 0
                volume < 0.5f -> 1
                else -> 2
            },
            transitionSpec = { fadeIn(tween(160)) togetherWith fadeOut(tween(120)) },
            label = "volumeIcon"
        ) { level ->
            Icon(
                when (level) {
                    0 -> Lucide.VolumeX
                    1 -> Lucide.Volume1
                    else -> Lucide.Volume2
                },
                contentDescription = label,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(Modifier.width(10.dp))
        ChatoneSlider(
            value = volume,
            onValueChange = onChange,
            onValueChangeFinished = onCommit,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            "${(volume * 100).toInt()}%",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(44.dp)
        )
    }
}

internal val SectionGap = if (isDesktopPlatform) 10.dp else 12.dp
internal val CardPadding = if (isDesktopPlatform) 14.dp else 16.dp
