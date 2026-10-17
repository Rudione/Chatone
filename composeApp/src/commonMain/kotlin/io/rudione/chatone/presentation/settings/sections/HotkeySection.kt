package io.rudione.chatone.presentation.settings.sections

import androidx.compose.runtime.Composable
import io.rudione.chatone.presentation.components.rows.DropdownRow
import io.rudione.chatone.presentation.components.rows.HotkeyRow
import io.rudione.chatone.presentation.components.rows.RowDivider
import io.rudione.chatone.presentation.components.rows.SwitchRow
import io.rudione.chatone.presentation.settings.PauseHotkeyMode
import io.rudione.chatone.presentation.settings.SettingsEvent
import io.rudione.chatone.presentation.settings.SettingsState
import io.rudione.chatone.presentation.settings.SettingsViewModel
import io.rudione.chatone.presentation.settings.components.SettingsGroup
import io.rudione.chatone.presentation.settings.components.SettingsPair
import io.rudione.chatone.presentation.theme.i18n.AppStrings
import io.rudione.chatone.presentation.theme.i18n.LocalStrings
import io.rudione.chatone.util.system.HotkeyAction
import io.rudione.chatone.util.system.comboFor

@Composable
internal fun HotkeyContent(state: SettingsState, vm: SettingsViewModel) {
    SettingsPair(
        first = { HotkeyCombinationsGroup(state, vm) },
        second = { ChatControlsGroup(state, vm) }
    )
}

@Composable
private fun HotkeyCombinationsGroup(state: SettingsState, vm: SettingsViewModel) {
    val s = LocalStrings.current
    SettingsGroup(s.hotkeysCombinationsTitle) {
        HotkeyAction.entries.forEachIndexed { index, action ->
            if (index > 0) RowDivider()
            HotkeyRow(hotkeyActionLabel(action, s), "", state.hotkeys.comboFor(action)) { combo ->
                vm.sendEvent(SettingsEvent.OnHotkeyChanged(action.id, combo))
            }
        }
    }
}

@Composable
private fun ChatControlsGroup(state: SettingsState, vm: SettingsViewModel) {
    val s = LocalStrings.current
    SettingsGroup(s.settingsChatControls) {
        HotkeyRow(
            s.settingsPauseAutoScroll,
            s.settingsPauseAutoScrollDesc,
            state.pauseHotkey,
            allowMouseButtons = true
        ) { vm.sendEvent(SettingsEvent.OnPauseHotkeyChanged(it)) }
        RowDivider()
        DropdownRow(
            label = s.settingsPauseMode,
            description = if (state.pauseHotkeyMode == PauseHotkeyMode.HOLD) s.settingsPauseModeHoldDesc else s.settingsPauseModeToggleDesc,
            options = PauseHotkeyMode.entries.map { it.name },
            selected = state.pauseHotkeyMode.ordinal
        ) { idx -> vm.sendEvent(SettingsEvent.OnPauseHotkeyModeChanged(PauseHotkeyMode.entries[idx])) }
        RowDivider()
        SwitchRow(s.settingsBlockScroll, s.settingsBlockScrollDesc, state.disableScrollOnAlt) {
            vm.sendEvent(SettingsEvent.OnDisableScrollOnAltChanged(it))
        }
    }
}

internal fun hotkeyActionLabel(action: HotkeyAction, s: AppStrings): String = when (action) {
    HotkeyAction.TOGGLE_NAVIGATION -> s.hotkeyToggleNavigation
    HotkeyAction.TOGGLE_SIDEBAR -> s.hotkeyToggleSidebar
    HotkeyAction.TOGGLE_MENTIONS -> s.hotkeyToggleMentions
    HotkeyAction.ADD_CHANNEL -> s.hotkeyAddChannel
    HotkeyAction.OPEN_SETTINGS -> s.hotkeyOpenSettings
    HotkeyAction.NEXT_CHANNEL -> s.hotkeyNextChannel
    HotkeyAction.PREV_CHANNEL -> s.hotkeyPrevChannel
    HotkeyAction.CLOSE_CHANNEL -> s.hotkeyCloseChannel
    HotkeyAction.TOGGLE_WHISPERS -> s.hotkeyToggleWhispers
}
