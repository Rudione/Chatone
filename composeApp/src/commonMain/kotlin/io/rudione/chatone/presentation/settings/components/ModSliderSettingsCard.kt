package io.rudione.chatone.presentation.settings.components

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
import io.rudione.chatone.presentation.theme.i18n.LocalStrings
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
    val strings = LocalStrings.current.modSlider
    val scheme = MaterialTheme.colorScheme
    SettingsGroup {
        SwitchRow(strings.enableTitle, strings.enableHelp, settings.enabled) { update(settings.copy(enabled = it)) }
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
                if (settings.enabled) strings.statusOn else strings.statusOff,
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
    val strings = LocalStrings.current.modSlider
    SettingsGroup(strings.scaleTitle) {
        SwitchRow(strings.twitchScale, strings.twitchScaleHelp, settings.twitchScale) {
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
    val s = LocalStrings.current
    val strings = s.modSlider
    val extra = ChatoneTheme.extraColors
    var draft by remember { mutableIntStateOf(DEFAULT_DRAFT_SECONDS) }
    val full = steps.size >= ModSliderSettings.MAX_DURATIONS
    Column(
        modifier = Modifier
            .settingsAnchor(strings.stepsTitle)
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            strings.stepsTitle,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
        ChatoneFieldLabel(strings.stepsOrder)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            itemVerticalAlignment = Alignment.CenterVertically
        ) {
            if (settings.deleteAtStart) {
                StepTag(strings.actionDelete, extra.modDelete, onRemove = null)
                StepArrow()
            }
            steps.forEachIndexed { index, seconds ->
                StepTag(ModSliderDurations.format(seconds), extra.modTimeout) {
                    update(settings.withDurations(steps - seconds))
                }
                if (index < steps.lastIndex || settings.banAtEnd) StepArrow()
            }
            if (settings.banAtEnd) StepTag(strings.actionBan, extra.modBan, onRemove = null)
        }
        ChatoneFieldLabel(strings.quickSteps)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            QUICK_STEPS.forEach { seconds ->
                val included = seconds in steps
                ChatoneChip(
                    label = ModSliderDurations.format(seconds),
                    onClick = { update(settings.withDurations(if (included) steps - seconds else steps + seconds)) },
                    selected = included,
                    enabled = included || !full,
                    accent = extra.modTimeout,
                    height = 26.dp
                )
            }
        }
        ChatoneFieldLabel(strings.ownStep)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ChatoneDurationField(
                totalSeconds = draft,
                onValueChange = { draft = it },
                maxSeconds = ModSliderDurations.MAX_SECONDS,
                modifier = Modifier.width(200.dp)
            )
            ChatoneButton(
                text = strings.addStep,
                onClick = { update(settings.withDurations(steps + draft)) },
                enabled = draft !in steps && !full,
                tone = ChatoneButtonTone.Neutral,
                icon = Lucide.Plus
            )
            Spacer(Modifier.weight(1f))
            if (settings.hasCustomDurations) {
                ChatoneButton(
                    text = strings.resetSteps,
                    onClick = { update(settings.copy(timeoutSeconds = null)) },
                    tone = ChatoneButtonTone.Neutral,
                    icon = Lucide.RotateCcw
                )
            }
        }
        Text(strings.stepsHelp, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    val strings = LocalStrings.current.modSlider
    SettingsGroup(strings.travelTitle) {
        SwitchRow(strings.deleteAtStart, strings.deleteAtStartHelp, settings.deleteAtStart) {
            update(settings.copy(deleteAtStart = it))
        }
        RowDivider()
        SwitchRow(strings.banAtEnd, strings.banAtEndHelp, settings.banAtEnd) {
            update(settings.copy(banAtEnd = it))
        }
        RowDivider()
        SliderRow(
            label = strings.travelTitle,
            value = settings.travelPercent.toFloat(),
            valueRange = ModSliderSettings.MIN_TRAVEL_PERCENT.toFloat()..ModSliderSettings.MAX_TRAVEL_PERCENT.toFloat(),
            steps = (ModSliderSettings.MAX_TRAVEL_PERCENT - ModSliderSettings.MIN_TRAVEL_PERCENT) / ModSliderSettings.TRAVEL_STEP_PERCENT - 1,
            valueLabel = strings.travelValue.replace("{0}", settings.travelPercent.toString())
        ) { value ->
            val step = ModSliderSettings.TRAVEL_STEP_PERCENT
            update(settings.copy(travelPercent = ((value + step / 2f) / step).toInt() * step))
        }
        Text(
            strings.travelHelp,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 8.dp)
        )
    }
}

@Composable
private fun SliderPreviewCard(settings: ModSliderSettings, durations: List<Int>) {
    val strings = LocalStrings.current.modSlider
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
    val message = remember(strings) { previewMessage(strings.previewUser, strings.previewMessage) }
    var lastAction by remember { mutableStateOf<ModSliderAction?>(null) }
    ModSliderGuard(host)
    SettingsCard(title = strings.previewTitle, help = strings.previewHint) {
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
            lastAction?.let { strings.previewResult.replace("{0}", style.labelOf(it)) } ?: strings.previewHint,
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
