package io.rudione.chatone.presentation.settings.components

import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.actions_add
import chatone.composeapp.generated.resources.actions_all_channels
import chatone.composeapp.generated.resources.actions_all_channels_badge
import chatone.composeapp.generated.resources.actions_automations
import chatone.composeapp.generated.resources.actions_automations_hint
import chatone.composeapp.generated.resources.actions_cancel
import chatone.composeapp.generated.resources.actions_edit
import chatone.composeapp.generated.resources.actions_empty
import chatone.composeapp.generated.resources.actions_field_channel
import chatone.composeapp.generated.resources.actions_field_cooldown
import chatone.composeapp.generated.resources.actions_field_interval
import chatone.composeapp.generated.resources.actions_field_keyword
import chatone.composeapp.generated.resources.actions_field_message
import chatone.composeapp.generated.resources.actions_hint
import chatone.composeapp.generated.resources.actions_kind_reply
import chatone.composeapp.generated.resources.actions_kind_sound
import chatone.composeapp.generated.resources.actions_kind_timer
import chatone.composeapp.generated.resources.actions_list_every
import chatone.composeapp.generated.resources.actions_list_sound
import chatone.composeapp.generated.resources.actions_mention_badge
import chatone.composeapp.generated.resources.actions_muted_phrase_placeholder
import chatone.composeapp.generated.resources.actions_muted_phrases
import chatone.composeapp.generated.resources.actions_muted_phrases_hint
import chatone.composeapp.generated.resources.actions_only_mention
import chatone.composeapp.generated.resources.actions_safety_note
import chatone.composeapp.generated.resources.actions_save
import chatone.composeapp.generated.resources.actions_unit_minutes
import chatone.composeapp.generated.resources.actions_unit_seconds
import org.jetbrains.compose.resources.stringResource
import io.rudione.chatone.presentation.components.expressive.HelpHint
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import io.rudione.chatone.icons.material.Icons
import io.rudione.chatone.icons.material.filled.Add
import io.rudione.chatone.icons.material.filled.Close
import io.rudione.chatone.icons.material.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.rudione.chatone.domain.model.AutomationKind
import io.rudione.chatone.domain.model.ChatAutomation
import io.rudione.chatone.domain.model.normalizeAutomationChannel
import io.rudione.chatone.presentation.components.ChatoneSwitch
import io.rudione.chatone.presentation.components.ExpressiveCheckbox
import io.rudione.chatone.presentation.settings.SettingsEvent
import io.rudione.chatone.presentation.settings.SettingsState
import kotlin.time.Clock
import io.rudione.chatone.presentation.components.ChatoneIconButton

@Composable
private fun ActionField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    numeric: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    BasicTextField(
        value = value,
        onValueChange = { v -> onValueChange(if (numeric) v.filter { it.isDigit() }.take(5) else v) },
        singleLine = true,
        interactionSource = interactionSource,
        textStyle = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        modifier = modifier
            .height(28.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.6f))
            .border(
                1.dp,
                if (isFocused) MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                RoundedCornerShape(9.dp)
            ),
        decorationBox = { inner ->
            Box(Modifier.fillMaxSize().padding(horizontal = 9.dp), contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) Text(
                    placeholder,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    maxLines = 1
                )
                inner()
            }
        }
    )
}

@Composable
private fun SectionCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.5f))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(9.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        content = content
    )
}

@Composable
private fun CardTitle(text: String, help: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f, fill = false)
        )
        HelpHint(title = text, text = help, size = 28.dp)
    }
}

@Composable
fun ActionsSection(
    state: SettingsState,
    onEvent: (SettingsEvent) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
        SectionCard {
            CardTitle(
                stringResource(Res.string.actions_automations),
                listOf(stringResource(Res.string.actions_hint), stringResource(Res.string.actions_automations_hint), stringResource(Res.string.actions_safety_note)).joinToString(" ")
            )

            var editingId by remember { mutableStateOf<String?>(null) }
            var kind by remember { mutableStateOf(AutomationKind.TIMED_MESSAGE) }
            var channel by remember { mutableStateOf("") }
            var allChannels by remember { mutableStateOf(false) }
            var message by remember { mutableStateOf("") }
            var keyword by remember { mutableStateOf("") }
            var interval by remember { mutableStateOf("15") }
            var cooldown by remember { mutableStateOf("60") }
            var onlyMention by remember { mutableStateOf(false) }

            fun resetForm() {
                editingId = null
                channel = ""
                allChannels = false
                message = ""
                keyword = ""
                interval = "15"
                cooldown = "60"
                onlyMention = false
            }

            if (state.automations.isEmpty()) {
                Text(
                    stringResource(Res.string.actions_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
            state.automations.forEach { auto ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (editingId == auto.id) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                            else Color.Transparent
                        )
                        .padding(horizontal = 2.dp)
                ) {
                    val kindLabel = when (auto.kind) {
                        AutomationKind.TIMED_MESSAGE -> stringResource(Res.string.actions_kind_timer)
                        AutomationKind.AUTO_REPLY -> stringResource(Res.string.actions_kind_reply)
                        AutomationKind.KEYWORD_SOUND -> stringResource(Res.string.actions_kind_sound)
                    }
                    Text(
                        kindLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                    Text(
                        buildString {
                            if (auto.channelLogin == "*") append("[${stringResource(Res.string.actions_all_channels_badge)}] ")
                            else append("#${auto.channelLogin} ")
                            when (auto.kind) {
                                AutomationKind.TIMED_MESSAGE ->
                                    append(
                                        stringResource(Res.string.actions_list_every, auto.effectiveIntervalMinutes, auto.message)
                                    )
                                AutomationKind.AUTO_REPLY -> {
                                    if (auto.onlyWhenMentioned) append("[${stringResource(Res.string.actions_mention_badge)}] ")
                                    if (auto.keyword.isNotBlank()) append("«${auto.keyword}» → ${auto.message}")
                                    else append("→ ${auto.message}")
                                }
                                AutomationKind.KEYWORD_SOUND ->
                                    append(stringResource(Res.string.actions_list_sound, (auto.keyword)))
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    ChatoneSwitch(
                        checked = auto.enabled,
                        onCheckedChange = { onEvent(SettingsEvent.OnToggleAutomation(auto.id, it)) }
                    )
                    ChatoneIconButton(
                        onClick = {
                            editingId = auto.id
                            kind = auto.kind
                            allChannels = auto.channelLogin == "*"
                            channel = if (auto.channelLogin == "*") "" else auto.channelLogin
                            message = auto.message
                            keyword = auto.keyword
                            interval = auto.effectiveIntervalMinutes.toString()
                            cooldown = auto.effectiveCooldownSeconds.toString()
                            onlyMention = auto.onlyWhenMentioned
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Filled.Edit, stringResource(Res.string.actions_edit),
                            modifier = Modifier.size(13.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                        )
                    }
                    ChatoneIconButton(
                        onClick = {
                            if (editingId == auto.id) resetForm()
                            onEvent(SettingsEvent.OnRemoveAutomation(auto.id))
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Filled.Close, null,
                            modifier = Modifier.size(13.dp),
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                @Composable
                fun kindChip(k: AutomationKind, label: String) {
                    val selected = kind == k
                    Text(
                        label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                                else Color.Transparent
                            )
                            .border(
                                1.dp,
                                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { kind = k }
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                kindChip(AutomationKind.TIMED_MESSAGE, stringResource(Res.string.actions_kind_timer))
                kindChip(AutomationKind.AUTO_REPLY, stringResource(Res.string.actions_kind_reply))
                kindChip(AutomationKind.KEYWORD_SOUND, stringResource(Res.string.actions_kind_sound))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f).alpha(if (allChannels) 0.4f else 1f)) {
                    ActionField(
                        channel, { channel = it.lowercase().trim() },
                        stringResource(Res.string.actions_field_channel), Modifier.fillMaxWidth()
                    )
                }
                if (kind == AutomationKind.TIMED_MESSAGE) {
                    ActionField(interval, { interval = it }, stringResource(Res.string.actions_field_interval), Modifier.width(64.dp), numeric = true)
                } else {
                    ActionField(cooldown, { cooldown = it }, stringResource(Res.string.actions_field_cooldown), Modifier.width(64.dp), numeric = true)
                }
                Text(
                    if (kind == AutomationKind.TIMED_MESSAGE) stringResource(Res.string.actions_unit_minutes) else stringResource(Res.string.actions_unit_seconds),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .align(Alignment.End)
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { allChannels = !allChannels }
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(
                    stringResource(Res.string.actions_all_channels),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (allChannels) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
                ExpressiveCheckbox(
                    checked = allChannels,
                    onCheckedChange = { allChannels = it },
                    boxSize = 14
                )
            }
            if (kind != AutomationKind.TIMED_MESSAGE) {
                ActionField(keyword, { keyword = it }, stringResource(Res.string.actions_field_keyword), Modifier.fillMaxWidth())
            }
            if (kind == AutomationKind.AUTO_REPLY) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onlyMention = !onlyMention }
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    ExpressiveCheckbox(
                        checked = onlyMention,
                        onCheckedChange = { onlyMention = it },
                        boxSize = 14
                    )
                    Text(
                        stringResource(Res.string.actions_only_mention),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (onlyMention) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }
            if (kind != AutomationKind.KEYWORD_SOUND) {
                ActionField(message, { message = it }, stringResource(Res.string.actions_field_message), Modifier.fillMaxWidth())
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalButton(
                    onClick = {
                        val valid = (allChannels || normalizeAutomationChannel(channel).isNotEmpty()) && when (kind) {
                            AutomationKind.TIMED_MESSAGE -> message.isNotBlank()
                            AutomationKind.AUTO_REPLY -> message.isNotBlank() && (keyword.isNotBlank() || onlyMention)
                            AutomationKind.KEYWORD_SOUND -> keyword.isNotBlank()
                        }
                        if (!valid) return@FilledTonalButton
                        val existing = state.automations.firstOrNull { it.id == editingId }
                        val result = ChatAutomation(
                            id = existing?.id ?: "auto_${Clock.System.now().toEpochMilliseconds()}",
                            enabled = existing?.enabled ?: true,
                            kind = kind,
                            channelLogin = if (allChannels) ChatAutomation.ALL_CHANNELS else normalizeAutomationChannel(channel),
                            message = message.trim(),
                            keyword = keyword.trim(),
                            intervalMinutes = interval.toIntOrNull()
                                ?.coerceIn(ChatAutomation.MIN_TIMER_MINUTES, ChatAutomation.MAX_TIMER_MINUTES) ?: 15,
                            cooldownSeconds = cooldown.toIntOrNull()?.coerceIn(
                                if (kind == AutomationKind.KEYWORD_SOUND) ChatAutomation.MIN_SOUND_COOLDOWN_SECONDS
                                else ChatAutomation.MIN_REPLY_COOLDOWN_SECONDS,
                                ChatAutomation.MAX_COOLDOWN_SECONDS
                            ) ?: 60,
                            onlyWhenMentioned = kind == AutomationKind.AUTO_REPLY && onlyMention
                        )
                        if (existing != null) {
                            onEvent(SettingsEvent.OnUpdateAutomation(result))
                            resetForm()
                        } else {
                            onEvent(SettingsEvent.OnAddAutomation(result))
                            message = ""; keyword = ""
                        }
                    },
                    modifier = Modifier.height(28.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    if (editingId == null) {
                        Icon(Icons.Filled.Add, null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                    }
                    Text(
                        if (editingId != null) stringResource(Res.string.actions_save) else stringResource(Res.string.actions_add),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                if (editingId != null) {
                    TextButton(
                        onClick = { resetForm() },
                        modifier = Modifier.height(28.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp)
                    ) {
                        Text(stringResource(Res.string.actions_cancel), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        SectionCard {
            CardTitle(stringResource(Res.string.actions_muted_phrases), stringResource(Res.string.actions_muted_phrases_hint))
            state.mutedPhrases.forEach { phrase ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        phrase,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    ChatoneIconButton(
                        onClick = { onEvent(SettingsEvent.OnRemoveMutedPhrase(phrase)) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Filled.Close, null,
                            modifier = Modifier.size(13.dp),
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                        )
                    }
                }
            }
            var newPhrase by remember { mutableStateOf("") }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                ActionField(newPhrase, { newPhrase = it }, stringResource(Res.string.actions_muted_phrase_placeholder), Modifier.weight(1f))
                FilledTonalButton(
                    onClick = {
                        if (newPhrase.isNotBlank()) {
                            onEvent(SettingsEvent.OnAddMutedPhrase(newPhrase))
                            newPhrase = ""
                        }
                    },
                    enabled = newPhrase.isNotBlank(),
                    modifier = Modifier.height(28.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp)
                ) {
                    Text(stringResource(Res.string.actions_add), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
