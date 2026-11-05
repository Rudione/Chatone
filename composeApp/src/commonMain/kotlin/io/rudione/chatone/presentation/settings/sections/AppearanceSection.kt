package io.rudione.chatone.presentation.settings.sections

import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.settings_always_on_top
import chatone.composeapp.generated.resources.settings_always_on_top_all_windows
import chatone.composeapp.generated.resources.settings_always_on_top_browser
import chatone.composeapp.generated.resources.settings_always_on_top_desc
import chatone.composeapp.generated.resources.settings_always_on_top_except_system
import chatone.composeapp.generated.resources.settings_always_on_top_mode
import chatone.composeapp.generated.resources.settings_always_on_top_mode_desc
import chatone.composeapp.generated.resources.settings_both
import chatone.composeapp.generated.resources.settings_channel_navigation
import chatone.composeapp.generated.resources.settings_hide_sidebar
import chatone.composeapp.generated.resources.settings_hide_sidebar_desc
import chatone.composeapp.generated.resources.settings_hub_group_interface
import chatone.composeapp.generated.resources.settings_language
import chatone.composeapp.generated.resources.settings_language_desc
import chatone.composeapp.generated.resources.settings_mini_rail
import chatone.composeapp.generated.resources.settings_navigation_hidden
import chatone.composeapp.generated.resources.settings_navigation_hidden_desc
import chatone.composeapp.generated.resources.settings_tab_bar
import chatone.composeapp.generated.resources.settings_theme
import chatone.composeapp.generated.resources.settings_title_bar_adaptive
import chatone.composeapp.generated.resources.settings_title_bar_dark
import chatone.composeapp.generated.resources.settings_title_bar_light
import chatone.composeapp.generated.resources.settings_title_bar_mode
import chatone.composeapp.generated.resources.settings_title_bar_mode_desc
import chatone.composeapp.generated.resources.settings_title_bar_system
import chatone.composeapp.generated.resources.settings_ui_scale
import chatone.composeapp.generated.resources.settings_window
import org.jetbrains.compose.resources.stringResource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.rudione.chatone.presentation.components.ChatoneSlider
import io.rudione.chatone.presentation.components.rows.HighlightedSettingsText
import io.rudione.chatone.presentation.components.rows.SettingsRowMetrics
import io.rudione.chatone.presentation.settings.settingsAnchor
import io.rudione.chatone.presentation.components.rows.DropdownRow
import io.rudione.chatone.presentation.components.rows.ListRow
import io.rudione.chatone.presentation.components.rows.RowDivider
import io.rudione.chatone.presentation.components.rows.SwitchRow
import io.rudione.chatone.presentation.settings.AlwaysOnTopMode
import io.rudione.chatone.presentation.settings.SettingsEvent
import io.rudione.chatone.presentation.settings.SettingsState
import io.rudione.chatone.presentation.settings.SettingsViewModel
import io.rudione.chatone.presentation.settings.TitleBarMode
import io.rudione.chatone.presentation.settings.components.AccentColorPaletteRow
import io.rudione.chatone.presentation.settings.components.FontSettingsCard
import io.rudione.chatone.presentation.settings.components.SettingsGroup
import io.rudione.chatone.presentation.settings.components.SettingsPair
import io.rudione.chatone.presentation.theme.DEFAULT_ACCENT_INDEX
import io.rudione.chatone.presentation.theme.i18n.AppLocale
import io.rudione.chatone.util.platform.DeviceFormFactor
import io.rudione.chatone.util.platform.currentFormFactor
import io.rudione.chatone.util.system.isDesktopPlatform

@Composable
internal fun AppearanceContent(
    state: SettingsState,
    vm: SettingsViewModel,
    onOpenThemeCreator: (seedColor: Int?) -> Unit
) {
    SettingsGroup(stringResource(Res.string.settings_theme)) {
        AccentColorPaletteRow(
            selectedIndex = state.accentColorIndex,
            onSelect = { vm.sendEvent(SettingsEvent.OnAccentColorChanged(it)) },
            state = state,
            onOpenThemeCreator = onOpenThemeCreator,
            onReset = {
                vm.sendEvent(SettingsEvent.OnAccentColorChanged(DEFAULT_ACCENT_INDEX))
                vm.sendEvent(SettingsEvent.OnCustomThemeApplied(null))
            }
        )
    }
    InterfaceGroup(state, vm)
    SettingsPair(
        first = { FontSettingsCard(state = state, vm = vm) },
        second = if (isDesktopPlatform || isPhoneFormFactor) {
            { WindowSettingsGroup(state, vm) }
        } else null
    )
}

@Composable
private fun InterfaceGroup(state: SettingsState, vm: SettingsViewModel) {
    SettingsGroup(stringResource(Res.string.settings_hub_group_interface)) {
        UiScaleRow(state.uiScale) { vm.sendEvent(SettingsEvent.OnUiScaleChanged(it)) }
        RowDivider()
        val locales = AppLocale.all
        DropdownRow(
            label = stringResource(Res.string.settings_language),
            description = stringResource(Res.string.settings_language_desc),
            options = locales.map { it.displayName },
            selected = locales.indexOfFirst { it.code == state.language }.coerceAtLeast(0),
            onSelected = { vm.sendEvent(SettingsEvent.OnLanguageChanged(locales[it].code)) }
        )
        if (!isPhoneFormFactor) {
            RowDivider()
            ListRow(
                stringResource(Res.string.settings_channel_navigation),
                when (state.channelNavigation) {
                    SettingsState.ChannelNavigation.TAB_BAR -> stringResource(Res.string.settings_tab_bar)
                    SettingsState.ChannelNavigation.MINI_RAIL -> stringResource(Res.string.settings_mini_rail)
                    SettingsState.ChannelNavigation.BOTH -> stringResource(Res.string.settings_both)
                },
                listOf(stringResource(Res.string.settings_tab_bar), stringResource(Res.string.settings_mini_rail), stringResource(Res.string.settings_both))
            ) { vm.sendEvent(SettingsEvent.OnChannelNavigationChanged(SettingsState.ChannelNavigation.entries[it])) }
            RowDivider()
            SwitchRow(stringResource(Res.string.settings_navigation_hidden), stringResource(Res.string.settings_navigation_hidden_desc), state.navigationHidden) {
                vm.sendEvent(SettingsEvent.OnNavigationHiddenChanged(it))
            }
        }
    }
}

@Composable
private fun WindowSettingsGroup(state: SettingsState, vm: SettingsViewModel) {
    SettingsGroup(stringResource(Res.string.settings_window)) {
        if (isDesktopPlatform) {
            SwitchRow(stringResource(Res.string.settings_always_on_top), stringResource(Res.string.settings_always_on_top_desc), state.alwaysOnTop) {
                vm.sendEvent(SettingsEvent.OnAlwaysOnTopChanged(it))
            }
            if (state.alwaysOnTop) {
                RowDivider()
                val modes = AlwaysOnTopMode.entries
                DropdownRow(
                    label = stringResource(Res.string.settings_always_on_top_mode),
                    description = stringResource(Res.string.settings_always_on_top_mode_desc),
                    options = modes.map {
                        when (it) {
                            AlwaysOnTopMode.EXCEPT_SYSTEM -> stringResource(Res.string.settings_always_on_top_except_system)
                            AlwaysOnTopMode.ALL_WINDOWS -> stringResource(Res.string.settings_always_on_top_all_windows)
                            AlwaysOnTopMode.BROWSER -> stringResource(Res.string.settings_always_on_top_browser)
                        }
                    },
                    selected = modes.indexOf(state.alwaysOnTopMode).coerceAtLeast(0),
                    onSelected = { vm.sendEvent(SettingsEvent.OnAlwaysOnTopModeChanged(modes[it])) }
                )
            }
            RowDivider()
        }
        SwitchRow(stringResource(Res.string.settings_hide_sidebar), stringResource(Res.string.settings_hide_sidebar_desc), state.hideSidebar) {
            vm.sendEvent(SettingsEvent.OnHideSidebarChanged(it))
        }
        if (isDesktopPlatform) {
            RowDivider()
            val titleBarModes = listOf(TitleBarMode.DARK, TitleBarMode.LIGHT, TitleBarMode.ADAPTIVE, TitleBarMode.SYSTEM)
            DropdownRow(
                label = stringResource(Res.string.settings_title_bar_mode),
                description = stringResource(Res.string.settings_title_bar_mode_desc),
                options = listOf(
                    stringResource(Res.string.settings_title_bar_dark),
                    stringResource(Res.string.settings_title_bar_light),
                    stringResource(Res.string.settings_title_bar_adaptive),
                    stringResource(Res.string.settings_title_bar_system)
                ),
                selected = titleBarModes.indexOf(state.titleBarMode).coerceAtLeast(0),
                onSelected = { vm.sendEvent(SettingsEvent.OnTitleBarModeChanged(titleBarModes[it])) }
            )
        }
    }
}

@Composable
private fun UiScaleRow(currentScale: Float, onScaleChanged: (Float) -> Unit) {
    val title = stringResource(Res.string.settings_ui_scale)
    Row(
        modifier = Modifier
            .settingsAnchor(title)
            .fillMaxWidth()
            .padding(horizontal = SettingsRowMetrics.horizontal, vertical = SettingsRowMetrics.vertical),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            HighlightedSettingsText(title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            HighlightedSettingsText(
                "${(currentScale * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(12.dp))
        ChatoneSlider(
            value = currentScale,
            onValueChange = onScaleChanged,
            valueRange = 0.7f..2.0f,
            steps = 12,
            modifier = Modifier.width(180.dp)
        )
    }
}

internal val isPhoneFormFactor: Boolean by lazy { currentFormFactor() == DeviceFormFactor.PHONE }
