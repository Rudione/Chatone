package io.rudione.chatone.presentation.settings.sections

import org.jetbrains.compose.resources.StringResource
import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.hotkey_add_channel
import chatone.composeapp.generated.resources.hotkey_close_channel
import chatone.composeapp.generated.resources.hotkey_next_channel
import chatone.composeapp.generated.resources.hotkey_open_settings
import chatone.composeapp.generated.resources.hotkey_prev_channel
import chatone.composeapp.generated.resources.hotkey_toggle_mentions
import chatone.composeapp.generated.resources.hotkey_toggle_navigation
import chatone.composeapp.generated.resources.hotkey_toggle_sidebar
import chatone.composeapp.generated.resources.hotkey_toggle_whispers
import chatone.composeapp.generated.resources.hotkeys_combinations_title
import chatone.composeapp.generated.resources.settings_block_scroll
import chatone.composeapp.generated.resources.settings_block_scroll_desc
import chatone.composeapp.generated.resources.settings_chat_controls
import chatone.composeapp.generated.resources.settings_pause_auto_scroll
import chatone.composeapp.generated.resources.settings_pause_auto_scroll_desc
import chatone.composeapp.generated.resources.settings_pause_mode
import chatone.composeapp.generated.resources.settings_pause_mode_hold_desc
import chatone.composeapp.generated.resources.settings_pause_mode_toggle_desc
import org.jetbrains.compose.resources.stringResource
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
    SettingsGroup(stringResource(Res.string.hotkeys_combinations_title)) {
        HotkeyAction.entries.forEachIndexed { index, action ->
            if (index > 0) RowDivider()
            HotkeyRow(hotkeyActionLabel(action), "", state.hotkeys.comboFor(action)) { combo ->
                vm.sendEvent(SettingsEvent.OnHotkeyChanged(action.id, combo))
            }
        }
    }
}

@Composable
private fun ChatControlsGroup(state: SettingsState, vm: SettingsViewModel) {
    SettingsGroup(stringResource(Res.string.settings_chat_controls)) {
        HotkeyRow(
            stringResource(Res.string.settings_pause_auto_scroll),
            stringResource(Res.string.settings_pause_auto_scroll_desc),
            state.pauseHotkey,
            allowMouseButtons = true
        ) { vm.sendEvent(SettingsEvent.OnPauseHotkeyChanged(it)) }
        RowDivider()
        DropdownRow(
            label = stringResource(Res.string.settings_pause_mode),
            description = if (state.pauseHotkeyMode == PauseHotkeyMode.HOLD) stringResource(Res.string.settings_pause_mode_hold_desc) else stringResource(Res.string.settings_pause_mode_toggle_desc),
            options = PauseHotkeyMode.entries.map { it.name },
            selected = state.pauseHotkeyMode.ordinal
        ) { idx -> vm.sendEvent(SettingsEvent.OnPauseHotkeyModeChanged(PauseHotkeyMode.entries[idx])) }
        RowDivider()
        SwitchRow(stringResource(Res.string.settings_block_scroll), stringResource(Res.string.settings_block_scroll_desc), state.disableScrollOnAlt) {
            vm.sendEvent(SettingsEvent.OnDisableScrollOnAltChanged(it))
        }
    }
}

internal fun HotkeyAction.labelRes(): StringResource = when (this) {
    HotkeyAction.TOGGLE_NAVIGATION -> Res.string.hotkey_toggle_navigation
    HotkeyAction.TOGGLE_SIDEBAR -> Res.string.hotkey_toggle_sidebar
    HotkeyAction.TOGGLE_MENTIONS -> Res.string.hotkey_toggle_mentions
    HotkeyAction.ADD_CHANNEL -> Res.string.hotkey_add_channel
    HotkeyAction.OPEN_SETTINGS -> Res.string.hotkey_open_settings
    HotkeyAction.NEXT_CHANNEL -> Res.string.hotkey_next_channel
    HotkeyAction.PREV_CHANNEL -> Res.string.hotkey_prev_channel
    HotkeyAction.CLOSE_CHANNEL -> Res.string.hotkey_close_channel
    HotkeyAction.TOGGLE_WHISPERS -> Res.string.hotkey_toggle_whispers
}


@Composable
internal fun hotkeyActionLabel(action: HotkeyAction): String = stringResource(action.labelRes())
