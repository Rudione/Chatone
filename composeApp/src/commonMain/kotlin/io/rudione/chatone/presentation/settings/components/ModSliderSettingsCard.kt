package io.rudione.chatone.presentation.settings.components

import io.rudione.chatone.presentation.components.durationText
import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.mod_slider_action_ban
import chatone.composeapp.generated.resources.mod_slider_action_delete
import chatone.composeapp.generated.resources.mod_slider_add_step
import chatone.composeapp.generated.resources.mod_slider_ban_at_end
import chatone.composeapp.generated.resources.mod_slider_ban_at_end_help
import chatone.composeapp.generated.resources.mod_slider_delete_at_start
import chatone.composeapp.generated.resources.mod_slider_delete_at_start_help
import chatone.composeapp.generated.resources.mod_slider_enable_help
import chatone.composeapp.generated.resources.mod_slider_enable_title
import chatone.composeapp.generated.resources.mod_slider_own_step
import chatone.composeapp.generated.resources.mod_slider_preview_hint
import chatone.composeapp.generated.resources.mod_slider_preview_message
import chatone.composeapp.generated.resources.mod_slider_preview_result
import chatone.composeapp.generated.resources.mod_slider_preview_title
import chatone.composeapp.generated.resources.mod_slider_preview_user
import chatone.composeapp.generated.resources.mod_slider_quick_steps
import chatone.composeapp.generated.resources.mod_slider_reset_steps
import chatone.composeapp.generated.resources.mod_slider_scale_title
import chatone.composeapp.generated.resources.mod_slider_status_off
import chatone.composeapp.generated.resources.mod_slider_status_on
import chatone.composeapp.generated.resources.mod_slider_steps_help
import chatone.composeapp.generated.resources.mod_slider_steps_order
import chatone.composeapp.generated.resources.mod_slider_steps_title
import chatone.composeapp.generated.resources.mod_slider_travel_help
import chatone.composeapp.generated.resources.mod_slider_travel_title
import chatone.composeapp.generated.resources.mod_slider_travel_value
import chatone.composeapp.generated.resources.mod_slider_twitch_scale
import chatone.composeapp.generated.resources.mod_slider_twitch_scale_help
import org.jetbrains.compose.resources.stringResource
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.rudione.chatone.domain.model.DisplayMessage
import io.rudione.chatone.domain.model.ModSliderSettings
import io.rudione.chatone.domain.moderation.ModSliderAction
import io.rudione.chatone.domain.moderation.ModSliderDurations
import io.rudione.chatone.icons.lucide.ChevronRight
import io.rudione.chatone.icons.lucide.Lucide
import io.rudione.chatone.icons.lucide.Plus
import io.rudione.chatone.icons.lucide.RotateCcw
import io.rudione.chatone.icons.lucide.X
import io.rudione.chatone.presentation.chat.PrivMsgItem
import io.rudione.chatone.presentation.chat.modslider.ModSliderBinding
import io.rudione.chatone.presentation.chat.modslider.ModSliderGuard
import io.rudione.chatone.presentation.chat.modslider.ModSliderHost
import io.rudione.chatone.presentation.chat.modslider.rememberModSliderStyle
import io.rudione.chatone.presentation.components.ChatoneButton
import io.rudione.chatone.presentation.components.ChatoneButtonTone
import io.rudione.chatone.presentation.components.ChatoneChip
import io.rudione.chatone.presentation.components.ChatoneDurationField
import io.rudione.chatone.presentation.components.ChatoneFieldLabel
import io.rudione.chatone.presentation.components.SettingsCard
import io.rudione.chatone.presentation.components.rows.RowDivider
import io.rudione.chatone.presentation.components.rows.SliderRow
import io.rudione.chatone.presentation.components.rows.SwitchRow
import io.rudione.chatone.presentation.settings.SettingsEvent
import io.rudione.chatone.presentation.settings.SettingsState
import io.rudione.chatone.presentation.settings.settingsAnchor
import io.rudione.chatone.presentation.theme.ChatoneTheme
import io.rudione.chatone.util.chat.MessageToken

@Composable
internal fun ModSliderSettings(state: SettingsState, onEvent: (SettingsEvent) -> Unit) {
    val settings = state.modSlider
    val update: (ModSliderSettings) -> Unit = { onEvent(SettingsEvent.OnModSliderChanged(it)) }
    SliderEnableGroup(settings, update)
    AnimatedVisibility(
        visible = settings.enabled,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val custom = settings.customDurations(state.allModButtons, state.defaultTimeoutDuration)
            SliderScaleGroup(settings, custom, update)
            SliderTrackGroup(settings, update)
            SliderPreviewCard(settings, settings.durations(state.allModButtons, state.defaultTimeoutDuration))
        }
    }
}

@Composable
private fun SliderEnableGroup(settings: ModSliderSettings, update: (ModSliderSettings) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    SettingsGroup {
        SwitchRow(stringResource(Res.string.mod_slider_enable_title), stringResource(Res.string.mod_slider_enable_help), settings.enabled) { update(settings.copy(enabled = it)) }
        Row(
            modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (settings.enabled) scheme.primary else scheme.outline)
            )
            Text(
                if (settings.enabled) stringResource(Res.string.mod_slider_status_on) else stringResource(Res.string.mod_slider_status_off),
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SliderScaleGroup(
    settings: ModSliderSettings,
    custom: List<Int>,
    update: (ModSliderSettings) -> Unit
) {
    SettingsGroup(stringResource(Res.string.mod_slider_scale_title)) {
        SwitchRow(stringResource(Res.string.mod_slider_twitch_scale), stringResource(Res.string.mod_slider_twitch_scale_help), settings.twitchScale) {
            update(settings.copy(twitchScale = it))
        }
        AnimatedVisibility(
            visible = !settings.twitchScale,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column {
                RowDivider()
                CustomStepsEditor(settings, custom, update)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CustomStepsEditor(
    settings: ModSliderSettings,
    steps: List<Int>,
    update: (ModSliderSettings) -> Unit
) {
    val extra = ChatoneTheme.extraColors
    var draft by remember { mutableIntStateOf(DEFAULT_DRAFT_SECONDS) }
    val full = steps.size >= ModSliderSettings.MAX_DURATIONS
    Column(
        modifier = Modifier
            .settingsAnchor(stringResource(Res.string.mod_slider_steps_title))
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            stringResource(Res.string.mod_slider_steps_title),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
        ChatoneFieldLabel(stringResource(Res.string.mod_slider_steps_order))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            itemVerticalAlignment = Alignment.CenterVertically
        ) {
            if (settings.deleteAtStart) {
                StepTag(stringResource(Res.string.mod_slider_action_delete), extra.modDelete, onRemove = null)
                StepArrow()
            }
            steps.forEachIndexed { index, seconds ->
                StepTag(durationText(seconds), extra.modTimeout) {
                    update(settings.withDurations(steps - seconds))
                }
                if (index < steps.lastIndex || settings.banAtEnd) StepArrow()
            }
            if (settings.banAtEnd) StepTag(stringResource(Res.string.mod_slider_action_ban), extra.modBan, onRemove = null)
        }
        ChatoneFieldLabel(stringResource(Res.string.mod_slider_quick_steps))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            QUICK_STEPS.forEach { seconds ->
                val included = seconds in steps
                ChatoneChip(
                    label = durationText(seconds),
                    onClick = { update(settings.withDurations(if (included) steps - seconds else steps + seconds)) },
                    selected = included,
                    enabled = included || !full,
                    accent = extra.modTimeout,
                    height = 26.dp
                )
            }
        }
        ChatoneFieldLabel(stringResource(Res.string.mod_slider_own_step))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ChatoneDurationField(
                totalSeconds = draft,
                onValueChange = { draft = it },
                maxSeconds = ModSliderDurations.MAX_SECONDS,
                modifier = Modifier.width(200.dp)
            )
            ChatoneButton(
                text = stringResource(Res.string.mod_slider_add_step),
                onClick = { update(settings.withDurations(steps + draft)) },
                enabled = draft !in steps && !full,
                tone = ChatoneButtonTone.Neutral,
                icon = Lucide.Plus
            )
            Spacer(Modifier.weight(1f))
            if (settings.hasCustomDurations) {
                ChatoneButton(
                    text = stringResource(Res.string.mod_slider_reset_steps),
                    onClick = { update(settings.copy(timeoutSeconds = null)) },
                    tone = ChatoneButtonTone.Neutral,
                    icon = Lucide.RotateCcw
                )
            }
        }
        Text(stringResource(Res.string.mod_slider_steps_help), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun StepTag(label: String, color: Color, onRemove: (() -> Unit)?) {
    val shape = RoundedCornerShape(8.dp)
    Row(
        modifier = Modifier
            .height(26.dp)
            .clip(shape)
            .background(color.copy(alpha = 0.14f))
            .border(1.dp, color.copy(alpha = 0.4f), shape)
            .then(if (onRemove != null) Modifier.clickable(onClick = onRemove) else Modifier)
            .padding(start = 8.dp, end = if (onRemove != null) 5.dp else 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = color)
        if (onRemove != null) Icon(Lucide.X, contentDescription = null, tint = color, modifier = Modifier.size(12.dp))
    }
}

@Composable
private fun StepArrow() {
    Icon(
        Lucide.ChevronRight,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.size(14.dp)
    )
}

@Composable
private fun SliderTrackGroup(settings: ModSliderSettings, update: (ModSliderSettings) -> Unit) {
    SettingsGroup(stringResource(Res.string.mod_slider_travel_title)) {
        SwitchRow(stringResource(Res.string.mod_slider_delete_at_start), stringResource(Res.string.mod_slider_delete_at_start_help), settings.deleteAtStart) {
            update(settings.copy(deleteAtStart = it))
        }
        RowDivider()
        SwitchRow(stringResource(Res.string.mod_slider_ban_at_end), stringResource(Res.string.mod_slider_ban_at_end_help), settings.banAtEnd) {
            update(settings.copy(banAtEnd = it))
        }
        RowDivider()
        SliderRow(
            label = stringResource(Res.string.mod_slider_travel_title),
            value = settings.travelPercent.toFloat(),
            valueRange = ModSliderSettings.MIN_TRAVEL_PERCENT.toFloat()..ModSliderSettings.MAX_TRAVEL_PERCENT.toFloat(),
            steps = (ModSliderSettings.MAX_TRAVEL_PERCENT - ModSliderSettings.MIN_TRAVEL_PERCENT) / ModSliderSettings.TRAVEL_STEP_PERCENT - 1,
            valueLabel = stringResource(Res.string.mod_slider_travel_value, settings.travelPercent)
        ) { value ->
            val step = ModSliderSettings.TRAVEL_STEP_PERCENT
            update(settings.copy(travelPercent = ((value + step / 2f) / step).toInt() * step))
        }
        Text(
            stringResource(Res.string.mod_slider_travel_help),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 8.dp)
        )
    }
}

@Composable
private fun SliderPreviewCard(settings: ModSliderSettings, durations: List<Int>) {
    val modSliderPreviewUserText = stringResource(Res.string.mod_slider_preview_user)
    val modSliderPreviewMessageText = stringResource(Res.string.mod_slider_preview_message)
    val host = remember { ModSliderHost() }
    val style = rememberModSliderStyle()
    val binding = remember(settings, durations, style) {
        ModSliderBinding(
            host = host,
            style = style,
            durations = durations,
            deleteAtStart = settings.deleteAtStart,
            banAtEnd = settings.banAtEnd,
            travelFraction = settings.travelFraction
        )
    }
    val message = remember(modSliderPreviewUserText, modSliderPreviewMessageText) { previewMessage(modSliderPreviewUserText, modSliderPreviewMessageText) }
    var lastAction by remember { mutableStateOf<ModSliderAction?>(null) }
    ModSliderGuard(host)
    SettingsCard(title = stringResource(Res.string.mod_slider_preview_title), help = stringResource(Res.string.mod_slider_preview_hint)) {
        Column(modifier = Modifier.fillMaxWidth()) {
            PrivMsgItem(
                message = message,
                showModActions = true,
                actorCanModerate = true,
                isMod = true,
                modSlider = binding,
                onDelete = { lastAction = ModSliderAction.Delete },
                onCustomTimeout = { lastAction = ModSliderAction.Timeout(it) },
                onBan = { lastAction = ModSliderAction.Ban },
                onUnban = { lastAction = ModSliderAction.Unban }
            )
        }
        Text(
            lastAction?.let { stringResource(Res.string.mod_slider_preview_result, style.labelOf(it)) } ?: stringResource(Res.string.mod_slider_preview_hint),
            style = MaterialTheme.typography.labelSmall,
            color = lastAction?.let { style.colorOf(it) } ?: MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun previewMessage(user: String, text: String) = DisplayMessage.PrivMsg(
    id = "mod_slider_preview",
    timestamp = PREVIEW_TIMESTAMP,
    channel = "preview",
    userId = "mod_slider_preview_user",
    username = user,
    displayName = user,
    tokens = listOf(MessageToken.Text(text)),
    color = PREVIEW_COLOR,
    badges = emptyList(),
    isModerator = false,
    isSubscriber = false,
    isVip = false,
    isBroadcaster = false,
    isMention = false,
    isAction = false
)

private val QUICK_STEPS = listOf(
    10, 30, ModSliderDurations.MINUTE, 5 * ModSliderDurations.MINUTE, 10 * ModSliderDurations.MINUTE,
    30 * ModSliderDurations.MINUTE, ModSliderDurations.HOUR, 6 * ModSliderDurations.HOUR, 12 * ModSliderDurations.HOUR,
    ModSliderDurations.DAY, 3 * ModSliderDurations.DAY, 7 * ModSliderDurations.DAY, 14 * ModSliderDurations.DAY
)
private const val DEFAULT_DRAFT_SECONDS = 1_800
private const val PREVIEW_TIMESTAMP = 1_760_000_000_000L
private const val PREVIEW_COLOR = "#5B9DFF"
