package io.rudione.chatone.presentation.settings.sound

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.sound_advanced_title
import chatone.composeapp.generated.resources.sound_channels_add
import chatone.composeapp.generated.resources.sound_channels_empty
import chatone.composeapp.generated.resources.sound_channels_help
import chatone.composeapp.generated.resources.sound_channels_placeholder
import chatone.composeapp.generated.resources.sound_channels_remove
import chatone.composeapp.generated.resources.sound_channels_title
import chatone.composeapp.generated.resources.sound_cooldown
import chatone.composeapp.generated.resources.sound_cooldown_help
import chatone.composeapp.generated.resources.sound_cooldown_off
import chatone.composeapp.generated.resources.sound_media_stream
import chatone.composeapp.generated.resources.sound_media_stream_desc
import chatone.composeapp.generated.resources.sound_play
import chatone.composeapp.generated.resources.sound_seconds
import io.rudione.chatone.data.sound.mediaStreamChoiceSupported
import io.rudione.chatone.domain.sound.BuiltInSound
import io.rudione.chatone.domain.sound.MentionSoundIds
import io.rudione.chatone.icons.lucide.ChevronsUpDown
import io.rudione.chatone.icons.lucide.Lucide
import io.rudione.chatone.icons.lucide.Play
import io.rudione.chatone.icons.lucide.Plus
import io.rudione.chatone.icons.lucide.VolumeX
import io.rudione.chatone.icons.lucide.X
import io.rudione.chatone.presentation.components.ChatoneDropdownMenu
import io.rudione.chatone.presentation.components.ChatoneSwitch
import io.rudione.chatone.presentation.components.ChatoneTextField
import io.rudione.chatone.presentation.components.expressive.HelpHint
import io.rudione.chatone.presentation.components.expressive.PillSegmented
import io.rudione.chatone.presentation.components.rows.HighlightedSettingsText
import io.rudione.chatone.presentation.settings.components.settingsCard
import io.rudione.chatone.presentation.settings.settingsAnchor
import io.rudione.chatone.presentation.theme.ChatoneIndication
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ChannelSoundsCard(
    state: MentionSoundUiState,
    volume: Float,
    onEvent: (MentionSoundEvent) -> Unit
) {
    val title = stringResource(Res.string.sound_channels_title)
    var input by remember { mutableStateOf("") }
    val submit = {
        if (input.isNotBlank()) {
            onEvent(MentionSoundEvent.AddChannel(input))
            input = ""
        }
    }
    Column(
        modifier = Modifier
            .settingsAnchor(title)
            .fillMaxWidth()
            .settingsCard()
            .padding(CardPadding)
            .animateContentSize(spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        CardHeader(title = title, help = stringResource(Res.string.sound_channels_help))
        Row(verticalAlignment = Alignment.CenterVertically) {
            ChatoneTextField(
                value = input,
                onValueChange = { input = it.take(MAX_INPUT) },
                placeholder = stringResource(Res.string.sound_channels_placeholder),
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit() })
            )
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = if (input.isBlank()) 0.10f else 1f))
                    .clickable(enabled = input.isNotBlank(), role = Role.Button, onClick = submit),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Lucide.Plus,
                    contentDescription = stringResource(Res.string.sound_channels_add),
                    tint = if (input.isBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        val entries = state.config.channelSounds.entries.sortedBy { it.key }
        if (entries.isEmpty()) {
            Text(
                stringResource(Res.string.sound_channels_empty),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        entries.forEachIndexed { index, (login, soundId) ->
            key(login) {
                ChannelSoundRow(login = login, soundId = soundId, index = index, state = state, volume = volume, onEvent = onEvent)
            }
        }
    }
}

@Composable
private fun ChannelSoundRow(
    login: String,
    soundId: String,
    index: Int,
    state: MentionSoundUiState,
    volume: Float,
    onEvent: (MentionSoundEvent) -> Unit
) {
    val appear = rememberAppear(index)
    var expanded by remember { mutableStateOf(false) }
    val accent = avatarColor(login)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = appear.value
                translationX = (1f - appear.value) * -18.dp.toPx()
            }
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.045f))
            .padding(start = 6.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(28.dp).clip(CircleShape).background(accent.copy(alpha = 0.22f)),
            contentAlignment = Alignment.Center
        ) {
            Text(login.take(1).uppercase(), style = MaterialTheme.typography.labelLarge, color = accent, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(8.dp))
        Text(
            login,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Box {
            Row(
                modifier = Modifier
                    .widthIn(max = 150.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                    .clickable(interactionSource = null, indication = ChatoneIndication, role = Role.DropdownList) { expanded = true }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (soundId == MentionSoundIds.MUTE) {
                    Icon(Lucide.VolumeX, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(4.dp))
                }
                Text(
                    soundName(soundId, state),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(Modifier.width(4.dp))
                Icon(Lucide.ChevronsUpDown, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(13.dp))
            }
            ChatoneDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                val options = BuiltInSound.entries.map { it.id } + state.library.map { it.id } + MentionSoundIds.MUTE
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(soundName(option, state), fontWeight = if (option == soundId) FontWeight.SemiBold else FontWeight.Normal) },
                        onClick = {
                            expanded = false
                            onEvent(MentionSoundEvent.SetChannelSound(login, option))
                            if (option != MentionSoundIds.MUTE) onEvent(MentionSoundEvent.Preview(option, volume.coerceAtLeast(0.05f)))
                        }
                    )
                }
            }
        }
        if (soundId != MentionSoundIds.MUTE) {
            MiniIconButton(Lucide.Play, stringResource(Res.string.sound_play), { onEvent(MentionSoundEvent.Preview(soundId, volume.coerceAtLeast(0.05f))) })
        }
        MiniIconButton(Lucide.X, stringResource(Res.string.sound_channels_remove), { onEvent(MentionSoundEvent.RemoveChannel(login)) })
    }
}

@Composable
private fun avatarColor(login: String) = when ((login.hashCode() and 0x7fffffff) % 3) {
    0 -> MaterialTheme.colorScheme.primary
    1 -> MaterialTheme.colorScheme.tertiary
    else -> MaterialTheme.colorScheme.secondary
}

@Composable
internal fun SoundAdvancedCard(state: MentionSoundUiState, onEvent: (MentionSoundEvent) -> Unit) {
    val title = stringResource(Res.string.sound_advanced_title)
    val cooldownTitle = stringResource(Res.string.sound_cooldown)
    val off = stringResource(Res.string.sound_cooldown_off)
    val options = MentionSoundViewModel.cooldownOptions.map { ms ->
        ms to if (ms == 0L) off else stringResource(Res.string.sound_seconds, (ms / 1000).toString())
    }
    val selected = MentionSoundViewModel.cooldownOptions.minBy { kotlin.math.abs(it - state.config.cooldownMs) }
    Column(
        modifier = Modifier
            .settingsAnchor(title)
            .fillMaxWidth()
            .settingsCard()
            .padding(CardPadding),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        CardHeader(title = title)
        Column(modifier = Modifier.settingsAnchor(cooldownTitle), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                HighlightedSettingsText(
                    cooldownTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f, fill = false)
                )
                HelpHint(title = cooldownTitle, text = stringResource(Res.string.sound_cooldown_help), size = 24.dp)
            }
            PillSegmented(
                options = options,
                selected = selected,
                onSelect = { onEvent(MentionSoundEvent.SetCooldown(it)) }
            )
        }
        if (mediaStreamChoiceSupported) {
            SoundSwitchRow(
                title = stringResource(Res.string.sound_media_stream),
                help = stringResource(Res.string.sound_media_stream_desc),
                checked = state.config.mediaStream,
                onCheckedChange = { onEvent(MentionSoundEvent.SetMediaStream(it)) }
            )
        }
    }
}

@Composable
internal fun SoundSwitchRow(title: String, help: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .settingsAnchor(title)
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(role = Role.Switch) { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            HighlightedSettingsText(
                title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f, fill = false)
            )
            HelpHint(title = title, text = help, size = 24.dp)
        }
        Spacer(Modifier.width(10.dp))
        ChatoneSwitch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

private const val MAX_INPUT = 120
