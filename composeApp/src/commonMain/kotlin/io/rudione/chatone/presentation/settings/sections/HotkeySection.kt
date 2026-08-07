package io.rudione.chatone.presentation.settings.sections

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import io.rudione.chatone.presentation.components.rows.DropdownRow
import io.rudione.chatone.presentation.components.rows.HotkeyRow
import io.rudione.chatone.presentation.components.rows.RowDivider
import io.rudione.chatone.presentation.components.rows.SwitchRow
import io.rudione.chatone.presentation.settings.PauseHotkeyMode
import io.rudione.chatone.presentation.settings.SettingsEvent
import io.rudione.chatone.presentation.settings.SettingsState
import io.rudione.chatone.presentation.settings.SettingsViewModel
import io.rudione.chatone.presentation.settings.components.SettingsGroup
import io.rudione.chatone.presentation.theme.i18n.LocalStrings
import io.rudione.chatone.util.system.HotkeyAction
import io.rudione.chatone.util.system.comboFor

internal fun LazyListScope.hotkeyLazyItems(state: SettingsState, vm: SettingsViewModel) {
    item { HotkeyCombinationsGroup(state, vm) }
    item {
        val s = LocalStrings.current
        SettingsGroup(s.settingsChatControls) {
            HotkeyRow(
                s.settingsPauseAutoScroll,
                s.settingsPauseAutoScrollDesc,
                state.pauseHotkey,
                allowMouseButtons = true
            ) {
                vm.sendEvent(SettingsEvent.OnPauseHotkeyChanged(it))
            }
            DropdownRow(
                label = s.settingsPauseMode,
                description = if (state.pauseHotkeyMode == PauseHotkeyMode.HOLD) s.settingsPauseModeHoldDesc else s.settingsPauseModeToggleDesc,
                options = PauseHotkeyMode.entries.map { it.name },
                selected = state.pauseHotkeyMode.ordinal
            ) { idx ->
                vm.sendEvent(SettingsEvent.OnPauseHotkeyModeChanged(PauseHotkeyMode.entries[idx]))
            }
        }
    }
    item {
        io.rudione.chatone.presentation.settings.components.ImageUploaderSection(
            state = state,
            onEvent = { vm.sendEvent(it) }
        )
    }
}

@Composable
internal fun HotkeyContent(state: SettingsState, vm: SettingsViewModel) {
    val s = LocalStrings.current
    HotkeyCombinationsGroup(state, vm)
    SettingsGroup(s.settingsChatControls) {
        HotkeyRow(
            s.settingsPauseAutoScroll,
            s.settingsPauseAutoScrollDesc,
            state.pauseHotkey,
            allowMouseButtons = true
        ) {
            vm.sendEvent(SettingsEvent.OnPauseHotkeyChanged(it))
        }
        DropdownRow(
            label = s.settingsPauseMode,
            description = if (state.pauseHotkeyMode == PauseHotkeyMode.HOLD) s.settingsPauseModeHoldDesc else s.settingsPauseModeToggleDesc,
            options = PauseHotkeyMode.entries.map { it.name },
            selected = state.pauseHotkeyMode.ordinal
        ) { idx ->
            vm.sendEvent(SettingsEvent.OnPauseHotkeyModeChanged(PauseHotkeyMode.entries[idx]))
        }
    }
    io.rudione.chatone.presentation.settings.components.ImageUploaderSection(
        state = state,
        onEvent = { vm.sendEvent(it) }
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun hotkeyActionLabel(
    action: HotkeyAction,
    s: io.rudione.chatone.presentation.theme.i18n.AppStrings
): String =
    when (action) {
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

@Composable
private fun HotkeyCombinationsGroup(state: SettingsState, vm: SettingsViewModel) {
    val s = LocalStrings.current
    SettingsGroup(s.hotkeysCombinationsTitle) {
        SwitchRow(
            s.settingsNavigationHidden,
            s.settingsNavigationHiddenDesc,
            state.navigationHidden
        ) {
            vm.sendEvent(SettingsEvent.OnNavigationHiddenChanged(it))
        }
        HotkeyAction.entries.forEach { action ->
            RowDivider()
            HotkeyRow(
                hotkeyActionLabel(action, s),
                "",
                state.hotkeys.comboFor(action)
            ) { combo -> vm.sendEvent(SettingsEvent.OnHotkeyChanged(action.id, combo)) }
        }
    }
}
