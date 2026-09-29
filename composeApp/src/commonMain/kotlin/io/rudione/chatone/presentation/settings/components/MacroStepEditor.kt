package io.rudione.chatone.presentation.settings.components

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
import io.rudione.chatone.presentation.theme.i18n.AppStrings
import io.rudione.chatone.presentation.theme.i18n.LocalStrings

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

internal fun MacroStepKind.label(s: AppStrings): String = when (this) {
    MacroStepKind.SEND -> s.modStepTypeSend
    MacroStepKind.INSERT -> s.modStepTypeInsert
    MacroStepKind.SUB -> s.modStepTypeSub
    MacroStepKind.EMOTE -> s.modStepTypeEmote
    MacroStepKind.SLOW -> s.modStepTypeSlow
    MacroStepKind.FOLLOWERS -> s.modStepTypeFollowers
    MacroStepKind.R9K -> s.modStepTypeR9k
    MacroStepKind.RAID -> s.modStepTypeRaid
    MacroStepKind.PIN -> s.modStepTypePin
    MacroStepKind.DELAY -> s.modStepTypeDelay
    MacroStepKind.CLEAR -> s.modStepTypeClear
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

internal fun macroStepDescription(step: MacroStep, s: AppStrings): String = when (step) {
    is MacroStep.SendMessage -> if (step.repeatCount > 1) {
        s.modStepDescSendMulti.replace("{0}", step.repeatCount.toString()).replace("{1}", step.text)
    } else {
        s.modStepDescSend.replace("{0}", step.text)
    }
    is MacroStep.InsertText -> if (step.repeatCount > 1) {
        s.modStepDescInsertMulti.replace("{0}", step.repeatCount.toString()).replace("{1}", step.text)
    } else {
        s.modStepDescInsert.replace("{0}", step.text)
    }
    is MacroStep.SubMode -> if (step.enable) s.modStepDescSubEnable else s.modStepDescSubDisable
    is MacroStep.EmoteMode -> if (step.enable) s.modStepDescEmoteEnable else s.modStepDescEmoteDisable
    is MacroStep.SlowMode -> if (step.enable) {
        s.modStepDescSlowEnable.replace("{0}", step.seconds.toString())
    } else {
        s.modStepDescSlowDisable
    }
    is MacroStep.FollowerMode -> if (step.enable) {
        s.modStepDescFollowEnable.replace("{0}", step.minutes.toString())
    } else {
        s.modStepDescFollowDisable
    }
    is MacroStep.R9KMode -> if (step.enable) s.modStepDescR9kEnable else s.modStepDescR9kDisable
    is MacroStep.StartRaid -> s.modStepDescRaid.replace("{0}", step.targetLogin)
    is MacroStep.PinMessage -> s.modStepDescPin.replace("{0}", step.message)
    is MacroStep.Delay -> s.modStepDescDelay.replace("{0}", (step.seconds * step.repeatCount.coerceAtLeast(1)).toString())
    is MacroStep.ClearChat -> s.modStepDescClear
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
    val s = LocalStrings.current
    var kind by remember { mutableStateOf(initialStep?.let(MacroStepKind::of)) }
    var form by remember { mutableStateOf(MacroStepForm.from(initialStep)) }
    val draft = kind?.let(form::toStep)

    ChatoneDialog(
        onDismissRequest = onDismiss,
        title = if (initialStep == null) s.modAddStep else s.modEditStep,
        maxWidth = 520.dp,
        actions = {
            ChatoneButton(text = s.cancel, onClick = onDismiss, tone = ChatoneButtonTone.Neutral)
            ChatoneButton(
                text = if (initialStep == null) s.modAddStep else s.modSaveChanges,
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
                    label = option.label(s),
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
                s.modSelectActionType,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        } else {
            MacroStepFields(kind = selectedKind, form = form, onFormChange = { form = it })
        }

        if (draft != null) {
            ChatoneListItem(leading = { MacroStepBadge(MacroStepKind.of(draft)) }) {
                Text(
                    macroStepDescription(draft, s),
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
    val s = LocalStrings.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        when (kind) {
            MacroStepKind.SEND, MacroStepKind.INSERT -> {
                ChatoneTextField(
                    value = form.text,
                    onValueChange = { onFormChange(form.copy(text = it)) },
                    label = if (kind == MacroStepKind.SEND) s.modMessageText else s.modTextToInsert,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                RepeatField(form = form, onFormChange = onFormChange)
                if (kind == MacroStepKind.SEND && form.repeatCount > 1) {
                    Text(
                        s.modRepeatDuplicateHint,
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
                        label = s.modSlowModeSeconds,
                        range = SLOW_MODE_SECONDS_RANGE,
                        suffix = s.unitSecondsLabel,
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
                        label = s.modDurationMinutes,
                        range = FOLLOWER_MINUTES_RANGE,
                        suffix = s.unitMinutesLabel,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            MacroStepKind.RAID -> ChatoneTextField(
                value = form.raidTarget,
                onValueChange = { onFormChange(form.copy(raidTarget = it)) },
                label = s.modChannelToRaid,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            MacroStepKind.PIN -> ChatoneTextField(
                value = form.pinMessage,
                onValueChange = { onFormChange(form.copy(pinMessage = it)) },
                label = s.modMessageToPin,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            MacroStepKind.DELAY -> {
                ChatoneNumberField(
                    value = form.delaySeconds,
                    onValueChange = { onFormChange(form.copy(delaySeconds = it)) },
                    label = s.modWaitSeconds,
                    range = DELAY_SECONDS_RANGE,
                    suffix = s.unitSecondsLabel,
                    modifier = Modifier.fillMaxWidth()
                )
                RepeatField(form = form, onFormChange = onFormChange)
            }

            MacroStepKind.CLEAR -> Text(
                s.modClearWarning,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun ToggleField(form: MacroStepForm, onFormChange: (MacroStepForm) -> Unit) {
    val s = LocalStrings.current
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        ChatoneFieldLabel(s.modAction)
        ChatoneSegmentedControl(
            options = listOf(true to s.modEnable, false to s.modDisable),
            selected = form.enabled,
            onSelect = { onFormChange(form.copy(enabled = it)) }
        )
    }
}

@Composable
private fun RepeatField(form: MacroStepForm, onFormChange: (MacroStepForm) -> Unit) {
    val s = LocalStrings.current
    ChatoneNumberField(
        value = form.repeatCount,
        onValueChange = { onFormChange(form.copy(repeatCount = it)) },
        label = s.modRepeat,
        range = REPEAT_RANGE,
        suffix = s.modRepeatTimes,
        modifier = Modifier.width(180.dp)
    )
}
