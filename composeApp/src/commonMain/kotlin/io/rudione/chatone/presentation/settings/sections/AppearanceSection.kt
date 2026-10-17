package io.rudione.chatone.presentation.settings.sections

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
import io.rudione.chatone.presentation.theme.i18n.LocalStrings
import io.rudione.chatone.util.platform.DeviceFormFactor
import io.rudione.chatone.util.platform.currentFormFactor
import io.rudione.chatone.util.system.isDesktopPlatform

@Composable
internal fun AppearanceContent(
    state: SettingsState,
    vm: SettingsViewModel,
    onOpenThemeCreator: (seedColor: Int?) -> Unit
) {
    val s = LocalStrings.current
    SettingsGroup(s.settingsTheme) {
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
    val s = LocalStrings.current
    SettingsGroup(s.settingsHub.groupInterface) {
        UiScaleRow(state.uiScale) { vm.sendEvent(SettingsEvent.OnUiScaleChanged(it)) }
        RowDivider()
        val locales = AppLocale.all
        DropdownRow(
            label = s.settingsLanguage,
            description = s.settingsLanguageDesc,
            options = locales.map { it.displayName },
            selected = locales.indexOfFirst { it.code == state.language }.coerceAtLeast(0),
            onSelected = { vm.sendEvent(SettingsEvent.OnLanguageChanged(locales[it].code)) }
        )
        if (!isPhoneFormFactor) {
            RowDivider()
            ListRow(
                s.settingsChannelNavigation,
                when (state.channelNavigation) {
                    SettingsState.ChannelNavigation.TAB_BAR -> s.settingsTabBar
                    SettingsState.ChannelNavigation.MINI_RAIL -> s.settingsMiniRail
                    SettingsState.ChannelNavigation.BOTH -> s.settingsBoth
                },
                listOf(s.settingsTabBar, s.settingsMiniRail, s.settingsBoth)
            ) { vm.sendEvent(SettingsEvent.OnChannelNavigationChanged(SettingsState.ChannelNavigation.entries[it])) }
            RowDivider()
            SwitchRow(s.settingsNavigationHidden, s.settingsNavigationHiddenDesc, state.navigationHidden) {
                vm.sendEvent(SettingsEvent.OnNavigationHiddenChanged(it))
            }
        }
    }
}

@Composable
private fun WindowSettingsGroup(state: SettingsState, vm: SettingsViewModel) {
    val s = LocalStrings.current
    SettingsGroup(s.settingsWindow) {
        if (isDesktopPlatform) {
            SwitchRow(s.settingsAlwaysOnTop, s.settingsAlwaysOnTopDesc, state.alwaysOnTop) {
                vm.sendEvent(SettingsEvent.OnAlwaysOnTopChanged(it))
            }
            if (state.alwaysOnTop) {
                RowDivider()
                val modes = AlwaysOnTopMode.entries
                DropdownRow(
                    label = s.settingsAlwaysOnTopMode,
                    description = s.settingsAlwaysOnTopModeDesc,
                    options = modes.map {
                        when (it) {
                            AlwaysOnTopMode.EXCEPT_SYSTEM -> s.settingsAlwaysOnTopExceptSystem
                            AlwaysOnTopMode.ALL_WINDOWS -> s.settingsAlwaysOnTopAllWindows
                            AlwaysOnTopMode.BROWSER -> s.settingsAlwaysOnTopBrowser
                        }
                    },
                    selected = modes.indexOf(state.alwaysOnTopMode).coerceAtLeast(0),
                    onSelected = { vm.sendEvent(SettingsEvent.OnAlwaysOnTopModeChanged(modes[it])) }
                )
            }
            RowDivider()
        }
        SwitchRow(s.settingsHideSidebar, s.settingsHideSidebarDesc, state.hideSidebar) {
            vm.sendEvent(SettingsEvent.OnHideSidebarChanged(it))
        }
        if (isDesktopPlatform) {
            RowDivider()
            val titleBarModes = listOf(TitleBarMode.DARK, TitleBarMode.LIGHT, TitleBarMode.ADAPTIVE, TitleBarMode.SYSTEM)
            DropdownRow(
                label = s.settingsTitleBarMode,
                description = s.settingsTitleBarModeDesc,
                options = listOf(
                    s.settingsTitleBarDark,
                    s.settingsTitleBarLight,
                    s.settingsTitleBarAdaptive,
                    s.settingsTitleBarSystem
                ),
                selected = titleBarModes.indexOf(state.titleBarMode).coerceAtLeast(0),
                onSelected = { vm.sendEvent(SettingsEvent.OnTitleBarModeChanged(titleBarModes[it])) }
            )
        }
    }
}

@Composable
private fun UiScaleRow(currentScale: Float, onScaleChanged: (Float) -> Unit) {
    val title = LocalStrings.current.settingsUiScale
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
