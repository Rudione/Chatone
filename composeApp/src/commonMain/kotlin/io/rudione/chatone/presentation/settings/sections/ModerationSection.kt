package io.rudione.chatone.presentation.settings.sections

import org.jetbrains.compose.resources.StringResource
import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.mod_default_timeout_duration
import chatone.composeapp.generated.resources.mod_local_automod
import chatone.composeapp.generated.resources.mod_local_automod_desc
import chatone.composeapp.generated.resources.mod_open_local_automod_editor
import chatone.composeapp.generated.resources.mod_saved_ban_reason_label
import chatone.composeapp.generated.resources.mod_saved_reasons_desc
import chatone.composeapp.generated.resources.mod_saved_reasons_title
import chatone.composeapp.generated.resources.mod_saved_timeout_reason_label
import chatone.composeapp.generated.resources.mod_slider_buttons_in_slider_mode
import chatone.composeapp.generated.resources.mod_slider_tab_buttons
import chatone.composeapp.generated.resources.mod_slider_tab_general
import chatone.composeapp.generated.resources.mod_slider_tab_macros
import chatone.composeapp.generated.resources.mod_slider_tab_slider
import org.jetbrains.compose.resources.stringResource
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
import io.rudione.chatone.util.system.isDesktopPlatform

@Composable
internal fun ModerationContent(state: SettingsState, onEvent: (SettingsEvent) -> Unit) {
    val page = LocalSettingsPage.current
    var localTab by remember { mutableStateOf(ModerationTab.GENERAL) }
    val tabs = ModerationTabs
    val selected = (page?.moderationTab ?: localTab).takeIf { it in tabs } ?: ModerationTab.GENERAL
    val select: (ModerationTab) -> Unit = { tab -> if (page != null) page.moderationTab = tab else localTab = tab }
    ExpressiveTabs(
        tabs = tabs,
        selected = selected,
        onSelect = select,
        label = { tab -> moderationTabLabel(tab) },
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
                        stringResource(Res.string.mod_slider_buttons_in_slider_mode),
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
    var showAutomod by remember { mutableStateOf(false) }
    if (showAutomod) {
        DetachedAutomodWindow(currentChannelLogin = null, onClose = { showAutomod = false })
    }
    SettingsPair(
        first = {
            SettingsCard(title = stringResource(Res.string.mod_default_timeout_duration)) {
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
            SettingsCard(title = stringResource(Res.string.mod_local_automod), help = stringResource(Res.string.mod_local_automod_desc)) {
                ChatoneButton(
                    text = stringResource(Res.string.mod_open_local_automod_editor),
                    onClick = { showAutomod = true },
                    tone = ChatoneButtonTone.Neutral,
                    icon = Lucide.Shield
                )
            }
        }
    )
    SettingsCard(title = stringResource(Res.string.mod_saved_reasons_title), help = stringResource(Res.string.mod_saved_reasons_desc)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            ChatoneTextField(
                value = state.savedTimeoutReason,
                onValueChange = { onEvent(SettingsEvent.OnSavedTimeoutReasonChanged(it)) },
                modifier = Modifier.weight(1f).settingsAnchor(stringResource(Res.string.mod_saved_timeout_reason_label)),
                label = stringResource(Res.string.mod_saved_timeout_reason_label),
                singleLine = true
            )
            ChatoneTextField(
                value = state.savedBanReason,
                onValueChange = { onEvent(SettingsEvent.OnSavedBanReasonChanged(it)) },
                modifier = Modifier.weight(1f).settingsAnchor(stringResource(Res.string.mod_saved_ban_reason_label)),
                label = stringResource(Res.string.mod_saved_ban_reason_label),
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

internal fun ModerationTab.labelRes(): StringResource = when (this) {
    ModerationTab.GENERAL -> Res.string.mod_slider_tab_general
    ModerationTab.BUTTONS -> Res.string.mod_slider_tab_buttons
    ModerationTab.SLIDER -> Res.string.mod_slider_tab_slider
    ModerationTab.MACROS -> Res.string.mod_slider_tab_macros
}


@Composable
internal fun moderationTabLabel(tab: ModerationTab): String = stringResource(tab.labelRes())
