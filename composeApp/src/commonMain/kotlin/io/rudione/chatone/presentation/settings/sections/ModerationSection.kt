package io.rudione.chatone.presentation.settings.sections

import androidx.compose.animation.togetherWith
import androidx.compose.animation.fadeOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.AnimatedContent
import io.rudione.chatone.presentation.components.expressive.ExpressiveTabs
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.rudione.chatone.icons.lucide.Lucide
import io.rudione.chatone.icons.lucide.Shield
import io.rudione.chatone.presentation.automod.DetachedAutomodWindow
import io.rudione.chatone.presentation.components.ChatoneButton
import io.rudione.chatone.presentation.components.ChatoneButtonTone
import io.rudione.chatone.presentation.components.ChatoneChip
import io.rudione.chatone.presentation.components.ChatoneTextField
import io.rudione.chatone.presentation.components.SettingsCard
import io.rudione.chatone.presentation.settings.LocalSettingsPage
import io.rudione.chatone.presentation.settings.ModerationTab
import io.rudione.chatone.presentation.settings.SettingsEvent
import io.rudione.chatone.presentation.settings.SettingsState
import io.rudione.chatone.presentation.settings.components.MacrosSection
import io.rudione.chatone.presentation.settings.components.ModActionButtonsSection
import io.rudione.chatone.presentation.settings.components.ModSliderSettings
import io.rudione.chatone.presentation.settings.components.SettingsPair
import io.rudione.chatone.presentation.settings.settingsAnchor
import io.rudione.chatone.presentation.theme.i18n.LocalStrings
import io.rudione.chatone.presentation.theme.i18n.ModSliderStrings
import io.rudione.chatone.util.system.isDesktopPlatform

@Composable
internal fun ModerationContent(state: SettingsState, onEvent: (SettingsEvent) -> Unit) {
    val strings = LocalStrings.current.modSlider
    val page = LocalSettingsPage.current
    var localTab by remember { mutableStateOf(ModerationTab.GENERAL) }
    val tabs = ModerationTabs
    val selected = (page?.moderationTab ?: localTab).takeIf { it in tabs } ?: ModerationTab.GENERAL
    val select: (ModerationTab) -> Unit = { tab -> if (page != null) page.moderationTab = tab else localTab = tab }
    ExpressiveTabs(
        tabs = tabs,
        selected = selected,
        onSelect = select,
        label = { tab -> moderationTabLabel(tab, strings) },
        modifier = Modifier.padding(bottom = 10.dp)
    )
    AnimatedContent(
        targetState = selected,
        transitionSpec = {
            (fadeIn(tween(TAB_FADE_IN_MS, delayMillis = TAB_FADE_DELAY_MS)) togetherWith fadeOut(tween(TAB_FADE_OUT_MS)))
                .using(SizeTransform(clip = false) { _, _ -> tween(TAB_RESIZE_MS, easing = FastOutSlowInEasing) })
        },
        label = "moderationTab"
    ) { tab ->
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            when (tab) {
            ModerationTab.GENERAL -> ModerationGeneral(state, onEvent)
            ModerationTab.BUTTONS -> {
                if (state.modSlider.enabled && isDesktopPlatform) {
                    Text(
                        strings.buttonsInSliderMode,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 6.dp)
                    )
                }
                ModActionButtonsSection(state = state, onEvent = onEvent)
            }
            ModerationTab.SLIDER -> ModSliderSettings(state = state, onEvent = onEvent)
            ModerationTab.MACROS -> MacrosSection(state = state, onEvent = onEvent)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ModerationGeneral(state: SettingsState, onEvent: (SettingsEvent) -> Unit) {
    val s = LocalStrings.current
    var showAutomod by remember { mutableStateOf(false) }
    if (showAutomod) {
        DetachedAutomodWindow(currentChannelLogin = null, onClose = { showAutomod = false })
    }
    SettingsPair(
        first = {
            SettingsCard(title = s.modDefaultTimeoutDuration) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    DEFAULT_TIMEOUT_OPTIONS.forEach { (seconds, label) ->
                        ChatoneChip(
                            label = label,
                            onClick = { onEvent(SettingsEvent.OnDefaultTimeoutChanged(seconds)) },
                            selected = state.defaultTimeoutDuration == seconds
                        )
                    }
                }
            }
        },
        second = {
            SettingsCard(title = s.modLocalAutomod, help = s.modLocalAutomodDesc) {
                ChatoneButton(
                    text = s.modOpenLocalAutomodEditor,
                    onClick = { showAutomod = true },
                    tone = ChatoneButtonTone.Neutral,
                    icon = Lucide.Shield
                )
            }
        }
    )
    SettingsCard(title = s.modSavedReasonsTitle, help = s.modSavedReasonsDesc) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            ChatoneTextField(
                value = state.savedTimeoutReason,
                onValueChange = { onEvent(SettingsEvent.OnSavedTimeoutReasonChanged(it)) },
                modifier = Modifier.weight(1f).settingsAnchor(s.modSavedTimeoutReasonLabel),
                label = s.modSavedTimeoutReasonLabel,
                singleLine = true
            )
            ChatoneTextField(
                value = state.savedBanReason,
                onValueChange = { onEvent(SettingsEvent.OnSavedBanReasonChanged(it)) },
                modifier = Modifier.weight(1f).settingsAnchor(s.modSavedBanReasonLabel),
                label = s.modSavedBanReasonLabel,
                singleLine = true
            )
        }
    }
}

private const val TAB_FADE_IN_MS = 220
private const val TAB_FADE_DELAY_MS = 60
private const val TAB_FADE_OUT_MS = 120
private const val TAB_RESIZE_MS = 260

private val DEFAULT_TIMEOUT_OPTIONS = listOf(
    60 to "1m", 300 to "5m", 600 to "10m", 1800 to "30m", 3600 to "1h", 86400 to "1d"
)

private val ModerationTabs = ModerationTab.entries.filter { it != ModerationTab.SLIDER || isDesktopPlatform }

internal fun moderationTabLabel(tab: ModerationTab, strings: ModSliderStrings): String = when (tab) {
    ModerationTab.GENERAL -> strings.tabGeneral
    ModerationTab.BUTTONS -> strings.tabButtons
    ModerationTab.SLIDER -> strings.tabSlider
    ModerationTab.MACROS -> strings.tabMacros
}
