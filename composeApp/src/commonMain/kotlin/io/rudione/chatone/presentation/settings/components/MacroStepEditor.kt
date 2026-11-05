package io.rudione.chatone.presentation.settings.components

import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.cancel
import chatone.composeapp.generated.resources.mod_action
import chatone.composeapp.generated.resources.mod_add_step
import chatone.composeapp.generated.resources.mod_channel_to_raid
import chatone.composeapp.generated.resources.mod_clear_warning
import chatone.composeapp.generated.resources.mod_disable
import chatone.composeapp.generated.resources.mod_duration_minutes
import chatone.composeapp.generated.resources.mod_edit_step
import chatone.composeapp.generated.resources.mod_enable
import chatone.composeapp.generated.resources.mod_message_text
import chatone.composeapp.generated.resources.mod_message_to_pin
import chatone.composeapp.generated.resources.mod_repeat
import chatone.composeapp.generated.resources.mod_repeat_duplicate_hint
import chatone.composeapp.generated.resources.mod_repeat_times
import chatone.composeapp.generated.resources.mod_save_changes
import chatone.composeapp.generated.resources.mod_select_action_type
import chatone.composeapp.generated.resources.mod_slow_mode_seconds
import chatone.composeapp.generated.resources.mod_step_desc_clear
import chatone.composeapp.generated.resources.mod_step_desc_delay
import chatone.composeapp.generated.resources.mod_step_desc_emote_disable
import chatone.composeapp.generated.resources.mod_step_desc_emote_enable
import chatone.composeapp.generated.resources.mod_step_desc_follow_disable
import chatone.composeapp.generated.resources.mod_step_desc_follow_enable
import chatone.composeapp.generated.resources.mod_step_desc_insert
import chatone.composeapp.generated.resources.mod_step_desc_insert_multi
import chatone.composeapp.generated.resources.mod_step_desc_pin
import chatone.composeapp.generated.resources.mod_step_desc_r9k_disable
import chatone.composeapp.generated.resources.mod_step_desc_r9k_enable
import chatone.composeapp.generated.resources.mod_step_desc_raid
import chatone.composeapp.generated.resources.mod_step_desc_send
import chatone.composeapp.generated.resources.mod_step_desc_send_multi
import chatone.composeapp.generated.resources.mod_step_desc_slow_disable
import chatone.composeapp.generated.resources.mod_step_desc_slow_enable
import chatone.composeapp.generated.resources.mod_step_desc_sub_disable
import chatone.composeapp.generated.resources.mod_step_desc_sub_enable
import chatone.composeapp.generated.resources.mod_step_type_clear
import chatone.composeapp.generated.resources.mod_step_type_delay
import chatone.composeapp.generated.resources.mod_step_type_emote
import chatone.composeapp.generated.resources.mod_step_type_followers
import chatone.composeapp.generated.resources.mod_step_type_insert
import chatone.composeapp.generated.resources.mod_step_type_pin
import chatone.composeapp.generated.resources.mod_step_type_r9k
import chatone.composeapp.generated.resources.mod_step_type_raid
import chatone.composeapp.generated.resources.mod_step_type_send
import chatone.composeapp.generated.resources.mod_step_type_slow
import chatone.composeapp.generated.resources.mod_step_type_sub
import chatone.composeapp.generated.resources.mod_text_to_insert
import chatone.composeapp.generated.resources.mod_wait_seconds
import chatone.composeapp.generated.resources.unit_minutes_label
import chatone.composeapp.generated.resources.unit_seconds_label
import org.jetbrains.compose.resources.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import io.rudione.chatone.icons.material.Icons
import io.rudione.chatone.icons.material.automirrored.outlined.Send
import io.rudione.chatone.icons.material.outlined.DeleteSweep
import io.rudione.chatone.icons.material.outlined.EditNote
import io.rudione.chatone.icons.material.outlined.EmojiEmotions
import io.rudione.chatone.icons.material.outlined.FavoriteBorder
import io.rudione.chatone.icons.material.outlined.Fingerprint
import io.rudione.chatone.icons.material.outlined.HourglassEmpty
import io.rudione.chatone.icons.material.outlined.PushPin
import io.rudione.chatone.icons.material.outlined.RocketLaunch
import io.rudione.chatone.icons.material.outlined.Speed
import io.rudione.chatone.icons.material.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.rudione.chatone.domain.model.MacroStep
import io.rudione.chatone.presentation.components.ChatoneButton
import io.rudione.chatone.presentation.components.ChatoneButtonTone
import io.rudione.chatone.presentation.components.ChatoneChip
import io.rudione.chatone.presentation.components.ChatoneDialog
import io.rudione.chatone.presentation.components.ChatoneFieldLabel
import io.rudione.chatone.presentation.components.ChatoneListItem
import io.rudione.chatone.presentation.components.ChatoneNumberField
import io.rudione.chatone.presentation.components.ChatoneSegmentedControl
import io.rudione.chatone.presentation.components.ChatoneTextField

private val SLOW_MODE_SECONDS_RANGE = 3..120
private val FOLLOWER_MINUTES_RANGE = 0..129_600
private val DELAY_SECONDS_RANGE = 1..3_600
private val REPEAT_RANGE = 1..100

internal enum class MacroStepKind(val icon: ImageVector) {
    SEND(Icons.AutoMirrored.Outlined.Send),
    INSERT(Icons.Outlined.EditNote),
    SUB(Icons.Outlined.Star),
    EMOTE(Icons.Outlined.EmojiEmotions),
    SLOW(Icons.Outlined.Speed),
    FOLLOWERS(Icons.Outlined.FavoriteBorder),
    R9K(Icons.Outlined.Fingerprint),
    RAID(Icons.Outlined.RocketLaunch),
    PIN(Icons.Outlined.PushPin),
    DELAY(Icons.Outlined.HourglassEmpty),
    CLEAR(Icons.Outlined.DeleteSweep);

    companion object {
        fun of(step: MacroStep): MacroStepKind = when (step) {
            is MacroStep.SendMessage -> SEND
            is MacroStep.InsertText -> INSERT
            is MacroStep.SubMode -> SUB
            is MacroStep.EmoteMode -> EMOTE
            is MacroStep.SlowMode -> SLOW
            is MacroStep.FollowerMode -> FOLLOWERS
            is MacroStep.R9KMode -> R9K
            is MacroStep.StartRaid -> RAID
            is MacroStep.PinMessage -> PIN
            is MacroStep.Delay -> DELAY
            is MacroStep.ClearChat -> CLEAR
        }
    }
}

@Composable
internal fun MacroStepKind.label(): String = when (this) {
    MacroStepKind.SEND -> stringResource(Res.string.mod_step_type_send)
    MacroStepKind.INSERT -> stringResource(Res.string.mod_step_type_insert)
    MacroStepKind.SUB -> stringResource(Res.string.mod_step_type_sub)
    MacroStepKind.EMOTE -> stringResource(Res.string.mod_step_type_emote)
    MacroStepKind.SLOW -> stringResource(Res.string.mod_step_type_slow)
    MacroStepKind.FOLLOWERS -> stringResource(Res.string.mod_step_type_followers)
    MacroStepKind.R9K -> stringResource(Res.string.mod_step_type_r9k)
    MacroStepKind.RAID -> stringResource(Res.string.mod_step_type_raid)
    MacroStepKind.PIN -> stringResource(Res.string.mod_step_type_pin)
    MacroStepKind.DELAY -> stringResource(Res.string.mod_step_type_delay)
    MacroStepKind.CLEAR -> stringResource(Res.string.mod_step_type_clear)
}

internal data class MacroStepForm(
    val text: String = "",
    val enabled: Boolean = true,
    val slowSeconds: Int = 30,
    val followerMinutes: Int = 10,
    val raidTarget: String = "",
    val pinMessage: String = "",
    val delaySeconds: Int = 5,
    val repeatCount: Int = 1
) {
    fun toStep(kind: MacroStepKind): MacroStep? {
        val repeat = repeatCount.coerceAtLeast(1)
        return when (kind) {
            MacroStepKind.SEND -> text.takeIf { it.isNotBlank() }?.let { MacroStep.SendMessage(it, repeat) }
            MacroStepKind.INSERT -> text.takeIf { it.isNotBlank() }?.let { MacroStep.InsertText(it, repeat) }
            MacroStepKind.SUB -> MacroStep.SubMode(enabled)
            MacroStepKind.EMOTE -> MacroStep.EmoteMode(enabled)
            MacroStepKind.SLOW -> MacroStep.SlowMode(enabled, slowSeconds.coerceAtLeast(1))
            MacroStepKind.FOLLOWERS -> MacroStep.FollowerMode(enabled, followerMinutes.coerceAtLeast(0))
            MacroStepKind.R9K -> MacroStep.R9KMode(enabled)
            MacroStepKind.RAID -> raidTarget.trim().takeIf { it.isNotEmpty() }?.let { MacroStep.StartRaid(it) }
            MacroStepKind.PIN -> pinMessage.takeIf { it.isNotBlank() }?.let { MacroStep.PinMessage(it) }
            MacroStepKind.DELAY -> delaySeconds.takeIf { it > 0 }?.let { MacroStep.Delay(it, repeat) }
            MacroStepKind.CLEAR -> MacroStep.ClearChat()
        }
    }

    companion object {
        fun from(step: MacroStep?): MacroStepForm = when (step) {
            is MacroStep.SendMessage -> MacroStepForm(text = step.text, repeatCount = step.repeatCount)
            is MacroStep.InsertText -> MacroStepForm(text = step.text, repeatCount = step.repeatCount)
            is MacroStep.SubMode -> MacroStepForm(enabled = step.enable)
            is MacroStep.EmoteMode -> MacroStepForm(enabled = step.enable)
            is MacroStep.SlowMode -> MacroStepForm(enabled = step.enable, slowSeconds = step.seconds)
            is MacroStep.FollowerMode -> MacroStepForm(enabled = step.enable, followerMinutes = step.minutes)
            is MacroStep.R9KMode -> MacroStepForm(enabled = step.enable)
            is MacroStep.StartRaid -> MacroStepForm(raidTarget = step.targetLogin)
            is MacroStep.PinMessage -> MacroStepForm(pinMessage = step.message)
            is MacroStep.Delay -> MacroStepForm(delaySeconds = step.seconds, repeatCount = step.repeatCount)
            is MacroStep.ClearChat, null -> MacroStepForm()
        }
    }
}

@Composable
internal fun macroStepDescription(step: MacroStep): String = when (step) {
    is MacroStep.SendMessage -> if (step.repeatCount > 1) {
        stringResource(Res.string.mod_step_desc_send_multi, step.repeatCount, (step.text))
    } else {
        stringResource(Res.string.mod_step_desc_send, (step.text))
    }
    is MacroStep.InsertText -> if (step.repeatCount > 1) {
        stringResource(Res.string.mod_step_desc_insert_multi, step.repeatCount, (step.text))
    } else {
        stringResource(Res.string.mod_step_desc_insert, (step.text))
    }
    is MacroStep.SubMode -> if (step.enable) stringResource(Res.string.mod_step_desc_sub_enable) else stringResource(Res.string.mod_step_desc_sub_disable)
    is MacroStep.EmoteMode -> if (step.enable) stringResource(Res.string.mod_step_desc_emote_enable) else stringResource(Res.string.mod_step_desc_emote_disable)
    is MacroStep.SlowMode -> if (step.enable) {
        stringResource(Res.string.mod_step_desc_slow_enable, step.seconds)
    } else {
        stringResource(Res.string.mod_step_desc_slow_disable)
    }
    is MacroStep.FollowerMode -> if (step.enable) {
        stringResource(Res.string.mod_step_desc_follow_enable, step.minutes)
    } else {
        stringResource(Res.string.mod_step_desc_follow_disable)
    }
    is MacroStep.R9KMode -> if (step.enable) stringResource(Res.string.mod_step_desc_r9k_enable) else stringResource(Res.string.mod_step_desc_r9k_disable)
    is MacroStep.StartRaid -> stringResource(Res.string.mod_step_desc_raid, (step.targetLogin))
    is MacroStep.PinMessage -> stringResource(Res.string.mod_step_desc_pin, (step.message))
    is MacroStep.Delay -> stringResource(Res.string.mod_step_desc_delay, (step.seconds * step.repeatCount.coerceAtLeast(1)))
    is MacroStep.ClearChat -> stringResource(Res.string.mod_step_desc_clear)
}

@Composable
internal fun MacroStepBadge(kind: MacroStepKind, modifier: Modifier = Modifier) {
    val accent = MaterialTheme.colorScheme.primary
    Box(
        modifier = modifier
            .size(24.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(accent.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(kind.icon, contentDescription = null, tint = accent, modifier = Modifier.size(14.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun MacroStepDialog(
    initialStep: MacroStep?,
    onConfirm: (MacroStep) -> Unit,
    onDismiss: () -> Unit
) {
    var kind by remember { mutableStateOf(initialStep?.let(MacroStepKind::of)) }
    var form by remember { mutableStateOf(MacroStepForm.from(initialStep)) }
    val draft = kind?.let(form::toStep)

    ChatoneDialog(
        onDismissRequest = onDismiss,
        title = if (initialStep == null) stringResource(Res.string.mod_add_step) else stringResource(Res.string.mod_edit_step),
        maxWidth = 520.dp,
        actions = {
            ChatoneButton(text = stringResource(Res.string.cancel), onClick = onDismiss, tone = ChatoneButtonTone.Neutral)
            ChatoneButton(
                text = if (initialStep == null) stringResource(Res.string.mod_add_step) else stringResource(Res.string.mod_save_changes),
                onClick = { draft?.let(onConfirm) },
                enabled = draft != null
            )
        }
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            MacroStepKind.entries.forEach { option ->
                ChatoneChip(
                    label = option.label(),
                    onClick = { kind = option },
                    selected = kind == option,
                    leadingIcon = option.icon,
                    height = 26.dp
                )
            }
        }

        val selectedKind = kind
        if (selectedKind == null) {
            Text(
                stringResource(Res.string.mod_select_action_type),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        } else {
            MacroStepFields(kind = selectedKind, form = form, onFormChange = { form = it })
        }

        if (draft != null) {
            ChatoneListItem(leading = { MacroStepBadge(MacroStepKind.of(draft)) }) {
                Text(
                    macroStepDescription(draft),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun MacroStepFields(
    kind: MacroStepKind,
    form: MacroStepForm,
    onFormChange: (MacroStepForm) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        when (kind) {
            MacroStepKind.SEND, MacroStepKind.INSERT -> {
                ChatoneTextField(
                    value = form.text,
                    onValueChange = { onFormChange(form.copy(text = it)) },
                    label = if (kind == MacroStepKind.SEND) stringResource(Res.string.mod_message_text) else stringResource(Res.string.mod_text_to_insert),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                RepeatField(form = form, onFormChange = onFormChange)
                if (kind == MacroStepKind.SEND && form.repeatCount > 1) {
                    Text(
                        stringResource(Res.string.mod_repeat_duplicate_hint),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            MacroStepKind.SUB, MacroStepKind.EMOTE, MacroStepKind.R9K ->
                ToggleField(form = form, onFormChange = onFormChange)

            MacroStepKind.SLOW -> {
                ToggleField(form = form, onFormChange = onFormChange)
                if (form.enabled) {
                    ChatoneNumberField(
                        value = form.slowSeconds,
                        onValueChange = { onFormChange(form.copy(slowSeconds = it)) },
                        label = stringResource(Res.string.mod_slow_mode_seconds),
                        range = SLOW_MODE_SECONDS_RANGE,
                        suffix = stringResource(Res.string.unit_seconds_label),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            MacroStepKind.FOLLOWERS -> {
                ToggleField(form = form, onFormChange = onFormChange)
                if (form.enabled) {
                    ChatoneNumberField(
                        value = form.followerMinutes,
                        onValueChange = { onFormChange(form.copy(followerMinutes = it)) },
                        label = stringResource(Res.string.mod_duration_minutes),
                        range = FOLLOWER_MINUTES_RANGE,
                        suffix = stringResource(Res.string.unit_minutes_label),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            MacroStepKind.RAID -> ChatoneTextField(
                value = form.raidTarget,
                onValueChange = { onFormChange(form.copy(raidTarget = it)) },
                label = stringResource(Res.string.mod_channel_to_raid),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            MacroStepKind.PIN -> ChatoneTextField(
                value = form.pinMessage,
                onValueChange = { onFormChange(form.copy(pinMessage = it)) },
                label = stringResource(Res.string.mod_message_to_pin),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            MacroStepKind.DELAY -> {
                ChatoneNumberField(
                    value = form.delaySeconds,
                    onValueChange = { onFormChange(form.copy(delaySeconds = it)) },
                    label = stringResource(Res.string.mod_wait_seconds),
                    range = DELAY_SECONDS_RANGE,
                    suffix = stringResource(Res.string.unit_seconds_label),
                    modifier = Modifier.fillMaxWidth()
                )
                RepeatField(form = form, onFormChange = onFormChange)
            }

            MacroStepKind.CLEAR -> Text(
                stringResource(Res.string.mod_clear_warning),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun ToggleField(form: MacroStepForm, onFormChange: (MacroStepForm) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        ChatoneFieldLabel(stringResource(Res.string.mod_action))
        ChatoneSegmentedControl(
            options = listOf(true to stringResource(Res.string.mod_enable), false to stringResource(Res.string.mod_disable)),
            selected = form.enabled,
            onSelect = { onFormChange(form.copy(enabled = it)) }
        )
    }
}

@Composable
private fun RepeatField(form: MacroStepForm, onFormChange: (MacroStepForm) -> Unit) {
    ChatoneNumberField(
        value = form.repeatCount,
        onValueChange = { onFormChange(form.copy(repeatCount = it)) },
        label = stringResource(Res.string.mod_repeat),
        range = REPEAT_RANGE,
        suffix = stringResource(Res.string.mod_repeat_times),
        modifier = Modifier.width(180.dp)
    )
}
