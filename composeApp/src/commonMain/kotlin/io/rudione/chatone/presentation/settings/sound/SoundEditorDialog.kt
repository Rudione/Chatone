package io.rudione.chatone.presentation.settings.sound

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.cancel
import chatone.composeapp.generated.resources.delete
import chatone.composeapp.generated.resources.save
import chatone.composeapp.generated.resources.sound_custom_fallback
import chatone.composeapp.generated.resources.sound_editor_auto_trim
import chatone.composeapp.generated.resources.sound_editor_failed
import chatone.composeapp.generated.resources.sound_editor_fade_in
import chatone.composeapp.generated.resources.sound_editor_fade_out
import chatone.composeapp.generated.resources.sound_editor_gain
import chatone.composeapp.generated.resources.sound_editor_hold_delete
import chatone.composeapp.generated.resources.sound_editor_max
import chatone.composeapp.generated.resources.sound_editor_ms
import chatone.composeapp.generated.resources.sound_editor_name
import chatone.composeapp.generated.resources.sound_editor_normalize
import chatone.composeapp.generated.resources.sound_editor_normalize_desc
import chatone.composeapp.generated.resources.sound_editor_reset
import chatone.composeapp.generated.resources.sound_editor_source
import chatone.composeapp.generated.resources.sound_editor_title
import chatone.composeapp.generated.resources.sound_editor_trim_hint
import chatone.composeapp.generated.resources.sound_play
import chatone.composeapp.generated.resources.sound_stop
import io.rudione.chatone.domain.sound.SoundLimits
import io.rudione.chatone.icons.lucide.Lucide
import io.rudione.chatone.icons.lucide.Play
import io.rudione.chatone.icons.lucide.RotateCcw
import io.rudione.chatone.icons.lucide.Scissors
import io.rudione.chatone.icons.lucide.Square
import io.rudione.chatone.icons.lucide.Trash2
import io.rudione.chatone.icons.lucide.WandSparkles
import io.rudione.chatone.presentation.components.ChatoneDialog
import io.rudione.chatone.presentation.components.ChatoneSlider
import io.rudione.chatone.presentation.components.ChatoneTextField
import io.rudione.chatone.presentation.components.expressive.HoldToConfirmButton
import io.rudione.chatone.presentation.components.expressive.MorphingLoadingIndicator
import io.rudione.chatone.presentation.components.expressive.PillButton
import io.rudione.chatone.presentation.components.expressive.PillTone
import io.rudione.chatone.util.system.isDesktopPlatform
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun SoundEditorDialog(
    editor: SoundEditorState,
    playing: SoundPlayback?,
    volume: Float,
    onEvent: (MentionSoundEvent) -> Unit
) {
    val ready = !editor.loading && !editor.failed
    ChatoneDialog(
        onDismissRequest = { onEvent(MentionSoundEvent.CloseEditor) },
        title = stringResource(Res.string.sound_editor_title),
        subtitle = if (ready) stringResource(Res.string.sound_editor_source, secondsValue(editor.sourceDurationMs)) else null,
        maxWidth = 560.dp
    ) {
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .heightIn(max = if (isDesktopPlatform) 560.dp else Dp.Unspecified)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when {
                editor.loading -> Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
                    MorphingLoadingIndicator()
                }
                editor.failed -> Text(
                    stringResource(Res.string.sound_editor_failed),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
                else -> EditorBody(editor, playing, volume, onEvent)
            }
        }
        EditorFooter(editor = editor, ready = ready, onEvent = onEvent)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditorBody(
    editor: SoundEditorState,
    playing: SoundPlayback?,
    volume: Float,
    onEvent: (MentionSoundEvent) -> Unit
) {
    val edit = editor.edit
    val length = (edit.endMs - edit.startMs).coerceAtLeast(0L)
    val previewing = playing?.id == MentionSoundViewModel.PREVIEW_ID
    val progress by rememberPlaybackProgress(playing, MentionSoundViewModel.PREVIEW_ID)
    ChatoneTextField(
        value = editor.name,
        onValueChange = { onEvent(MentionSoundEvent.EditorRenamed(it)) },
        label = stringResource(Res.string.sound_editor_name),
        placeholder = stringResource(Res.string.sound_custom_fallback)
    )
    AnimatedVisibility(visible = editor.trimHint, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.14f))
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Lucide.Scissors, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(Res.string.sound_editor_trim_hint), style = MaterialTheme.typography.bodySmall)
        }
    }
    SoundTrimmer(
        peaks = editor.peaks,
        durationMs = editor.sourceDurationMs,
        edit = edit,
        progress = { progress },
        onChange = { onEvent(MentionSoundEvent.EditorChanged(it)) },
        modifier = Modifier.fillMaxWidth().height(if (isDesktopPlatform) 112.dp else 132.dp)
    )
    TimeRow(startMs = edit.startMs, lengthMs = length, endMs = edit.endMs)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PillButton(
            text = stringResource(if (previewing) Res.string.sound_stop else Res.string.sound_play),
            onClick = {
                if (previewing) onEvent(MentionSoundEvent.StopPreview)
                else onEvent(MentionSoundEvent.EditorPreview(volume.coerceAtLeast(0.05f)))
            },
            icon = if (previewing) Lucide.Square else Lucide.Play,
            tone = PillTone.Tonal,
            height = 40.dp
        )
        PillButton(
            text = stringResource(Res.string.sound_editor_auto_trim),
            onClick = { onEvent(MentionSoundEvent.EditorAutoTrim) },
            icon = Lucide.WandSparkles,
            tone = PillTone.Ghost,
            height = 40.dp
        )
        PillButton(
            text = stringResource(Res.string.sound_editor_reset),
            onClick = { onEvent(MentionSoundEvent.EditorReset) },
            icon = Lucide.RotateCcw,
            tone = PillTone.Ghost,
            height = 40.dp
        )
    }
    val maxFade = minOf(SoundLimits.MAX_FADE_MS.toLong(), length / 2).toInt().coerceAtLeast(0)
    EditorSlider(
        label = stringResource(Res.string.sound_editor_fade_in),
        value = edit.fadeInMs.toFloat(),
        range = 0f..maxFade.toFloat().coerceAtLeast(1f),
        valueLabel = stringResource(Res.string.sound_editor_ms, edit.fadeInMs),
        onChange = { onEvent(MentionSoundEvent.EditorChanged(edit.copy(fadeInMs = it.toInt()))) }
    )
    EditorSlider(
        label = stringResource(Res.string.sound_editor_fade_out),
        value = edit.fadeOutMs.toFloat(),
        range = 0f..maxFade.toFloat().coerceAtLeast(1f),
        valueLabel = stringResource(Res.string.sound_editor_ms, edit.fadeOutMs),
        onChange = { onEvent(MentionSoundEvent.EditorChanged(edit.copy(fadeOutMs = it.toInt()))) }
    )
    EditorSlider(
        label = stringResource(Res.string.sound_editor_gain),
        value = edit.gain,
        range = 0.1f..2f,
        valueLabel = "${(edit.gain * 100).toInt()}%",
        onChange = { onEvent(MentionSoundEvent.EditorChanged(edit.copy(gain = it))) }
    )
    SoundSwitchRow(
        title = stringResource(Res.string.sound_editor_normalize),
        help = stringResource(Res.string.sound_editor_normalize_desc),
        checked = edit.normalize,
        onCheckedChange = { onEvent(MentionSoundEvent.EditorChanged(edit.copy(normalize = it))) }
    )
}

@Composable
private fun TimeRow(startMs: Long, lengthMs: Long, endMs: Long) {
    val capped = lengthMs >= SoundLimits.MAX_CLIP_MS
    val accent by animateColorAsState(
        if (capped) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
        tween(220),
        label = "lengthAccent"
    )
    val pulse by animateFloatAsState(if (capped) 1.06f else 1f, spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessMedium), label = "lengthPulse")
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(secondsValue(startMs), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.weight(1f))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                secondsLabel(lengthMs),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = accent,
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = pulse
                        scaleY = pulse
                    }
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.14f))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            )
            AnimatedVisibility(visible = capped, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                Text(stringResource(Res.string.sound_editor_max), style = MaterialTheme.typography.labelSmall, color = accent)
            }
        }
        Spacer(Modifier.weight(1f))
        Text(secondsValue(endMs), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun EditorSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    valueLabel: String,
    onChange: (Float) -> Unit
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text(valueLabel, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
        }
        ChatoneSlider(value = value.coerceIn(range.start, range.endInclusive), onValueChange = onChange, valueRange = range)
    }
}

@Composable
private fun EditorFooter(editor: SoundEditorState, ready: Boolean, onEvent: (MentionSoundEvent) -> Unit) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val narrow = maxWidth < 430.dp
        val delete: @Composable (Modifier) -> Unit = { modifier ->
            HoldToConfirmButton(
                text = stringResource(Res.string.delete),
                holdingText = stringResource(Res.string.sound_editor_hold_delete),
                onConfirmed = { onEvent(MentionSoundEvent.EditorDelete) },
                icon = Lucide.Trash2,
                height = 44.dp,
                modifier = modifier
            )
        }
        val actions: @Composable (Modifier) -> Unit = { modifier ->
            Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                PillButton(
                    text = stringResource(Res.string.cancel),
                    onClick = { onEvent(MentionSoundEvent.CloseEditor) },
                    tone = PillTone.Ghost,
                    height = 44.dp
                )
                PillButton(
                    text = stringResource(Res.string.save),
                    onClick = { onEvent(MentionSoundEvent.EditorSave) },
                    enabled = ready,
                    loading = editor.saving,
                    height = 44.dp
                )
            }
        }
        if (narrow) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                actions(Modifier.fillMaxWidth())
                delete(Modifier.fillMaxWidth())
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                delete(Modifier)
                Spacer(Modifier.weight(1f))
                actions(Modifier)
            }
        }
    }
}
