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
import chatone.composeapp.generated.resources.hotkey_conflict
import chatone.composeapp.generated.resources.hotkeys_commands_empty
import chatone.composeapp.generated.resources.hotkeys_macros_empty
import chatone.composeapp.generated.resources.hotkeys_macros_title
import chatone.composeapp.generated.resources.hotkeys_commands_title
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
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.rudione.chatone.domain.model.Macro
import io.rudione.chatone.util.system.HotkeyBinding
import io.rudione.chatone.util.system.HotkeyBindings
import io.rudione.chatone.util.system.HotkeyOwner
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
    val bindings = rememberHotkeyBindings(state)
    SettingsPair(
        first = { HotkeyCombinationsGroup(state, vm, bindings) },
        second = { ChatControlsGroup(state, vm, bindings) }
    )
    SettingsPair(
        first = { MacroHotkeysGroup(state, vm, bindings) },
        second = { CommandHotkeysGroup(state, vm, bindings) }
    )
}

@Composable
private fun HotkeyCombinationsGroup(state: SettingsState, vm: SettingsViewModel, bindings: List<HotkeyBinding>) {
    SettingsGroup(stringResource(Res.string.hotkeys_combinations_title)) {
        HotkeyAction.entries.forEachIndexed { index, action ->
            if (index > 0) RowDivider()
            val label = hotkeyActionLabel(action)
            val combo = state.hotkeys.comboFor(action)
            HotkeyRow(
                title = label,
                subtitle = "",
                currentHotkey = combo,
                warning = conflictWarning(bindings, HotkeyBinding(HotkeyOwner.ACTION, action.id, combo, label))
            ) { newCombo ->
                vm.sendEvent(SettingsEvent.OnHotkeyChanged(action.id, newCombo))
            }
        }
    }
}

@Composable
private fun ChatControlsGroup(state: SettingsState, vm: SettingsViewModel, bindings: List<HotkeyBinding>) {
    SettingsGroup(stringResource(Res.string.settings_chat_controls)) {
        val pauseLabel = stringResource(Res.string.settings_pause_auto_scroll)
        HotkeyRow(
            pauseLabel,
            stringResource(Res.string.settings_pause_auto_scroll_desc),
            state.pauseHotkey,
            allowMouseButtons = true,
            warning = conflictWarning(bindings, HotkeyBinding(HotkeyOwner.PAUSE, PAUSE_ID, state.pauseHotkey, pauseLabel))
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

@Composable
private fun MacroHotkeysGroup(state: SettingsState, vm: SettingsViewModel, bindings: List<HotkeyBinding>) {
    SettingsGroup(stringResource(Res.string.hotkeys_macros_title)) {
        if (state.macros.isEmpty()) {
            EmptyHint(stringResource(Res.string.hotkeys_macros_empty))
            return@SettingsGroup
        }
        state.macros.forEachIndexed { index, macro ->
            if (index > 0) RowDivider()
            key(macro.id) {
                val label = macroLabel(macro)
                HotkeyRow(
                    title = label,
                    subtitle = "",
                    currentHotkey = macro.hotkey,
                    requireModifier = true,
                    warning = conflictWarning(bindings, HotkeyBinding(HotkeyOwner.MACRO, macro.id, macro.hotkey, label))
                ) { combo -> vm.sendEvent(SettingsEvent.OnUpdateMacro(macro.copy(hotkey = combo))) }
            }
        }
    }
}

@Composable
private fun CommandHotkeysGroup(state: SettingsState, vm: SettingsViewModel, bindings: List<HotkeyBinding>) {
    SettingsGroup(stringResource(Res.string.hotkeys_commands_title)) {
        if (state.chatCommands.isEmpty()) {
            EmptyHint(stringResource(Res.string.hotkeys_commands_empty))
            return@SettingsGroup
        }
        state.chatCommands.forEachIndexed { index, command ->
            if (index > 0) RowDivider()
            key(command.id) {
                HotkeyRow(
                    title = command.trigger,
                    subtitle = "",
                    currentHotkey = command.hotkey,
                    requireModifier = true,
                    warning = conflictWarning(bindings, HotkeyBinding(HotkeyOwner.COMMAND, command.id, command.hotkey, command.trigger))
                ) { combo -> vm.sendEvent(SettingsEvent.OnUpdateChatCommand(command.copy(hotkey = combo))) }
            }
        }
    }
}

@Composable
private fun EmptyHint(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
    )
}

@Composable
private fun conflictWarning(bindings: List<HotkeyBinding>, binding: HotkeyBinding): String? =
    hotkeyConflictLabel(bindings, binding)?.let { stringResource(Res.string.hotkey_conflict, it) }

private fun macroLabel(macro: Macro): String =
    listOf(macro.icon, macro.name).filter { it.isNotBlank() }.joinToString(" ")

private const val PAUSE_ID = "pause"

@Composable
internal fun rememberHotkeyBindings(state: SettingsState): List<HotkeyBinding> {
    val actionLabels = HotkeyAction.entries.map { hotkeyActionLabel(it) }
    val pauseLabel = stringResource(Res.string.settings_pause_auto_scroll)
    return remember(state.hotkeys, state.pauseHotkey, state.macros, state.chatCommands, actionLabels, pauseLabel) {
        buildList {
            HotkeyAction.entries.forEachIndexed { i, action ->
                add(HotkeyBinding(HotkeyOwner.ACTION, action.id, state.hotkeys.comboFor(action), actionLabels[i]))
            }
            add(HotkeyBinding(HotkeyOwner.PAUSE, PAUSE_ID, state.pauseHotkey, pauseLabel))
            state.macros.forEach { add(HotkeyBinding(HotkeyOwner.MACRO, it.id, it.hotkey, macroLabel(it))) }
            state.chatCommands.filter { it.enabled }.forEach {
                add(HotkeyBinding(HotkeyOwner.COMMAND, it.id, it.hotkey, it.trigger))
            }
        }
    }
}

internal fun hotkeyConflictLabel(bindings: List<HotkeyBinding>, binding: HotkeyBinding): String? {
    if (binding.combo.isBlank()) return null
    val others = HotkeyBindings.conflicts(bindings.filter { it.key != binding.key } + binding)[binding.key]
    return others?.takeIf { it.isNotEmpty() }?.joinToString(", ") { it.label }
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
