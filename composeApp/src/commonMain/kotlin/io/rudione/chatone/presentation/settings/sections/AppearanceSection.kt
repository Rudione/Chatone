package io.rudione.chatone.presentation.settings.sections

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import io.rudione.chatone.presentation.components.rows.DropdownRow
import io.rudione.chatone.presentation.components.rows.ListRow
import io.rudione.chatone.presentation.components.rows.RowDivider
import io.rudione.chatone.presentation.components.rows.SwitchRow
import io.rudione.chatone.presentation.settings.SettingsEvent
import io.rudione.chatone.presentation.settings.SettingsState
import io.rudione.chatone.presentation.settings.SettingsViewModel
import io.rudione.chatone.presentation.settings.TitleBarMode
import io.rudione.chatone.presentation.settings.UiScaleRow
import io.rudione.chatone.presentation.settings.components.AccentColorPaletteRow
import io.rudione.chatone.presentation.settings.components.FontSettingsCard
import io.rudione.chatone.presentation.settings.components.SettingsGroup
import io.rudione.chatone.presentation.settings.components.SettingsPair
import io.rudione.chatone.presentation.theme.DEFAULT_ACCENT_INDEX
import io.rudione.chatone.presentation.theme.i18n.AppLocale
import io.rudione.chatone.presentation.theme.i18n.AppStrings
import io.rudione.chatone.presentation.theme.i18n.LocalStrings
import io.rudione.chatone.util.platform.DeviceFormFactor
import io.rudione.chatone.util.platform.currentFormFactor
import io.rudione.chatone.util.system.isDesktopPlatform

internal fun LazyListScope.appearanceLazyItems(
    state: SettingsState,
    onThemeChanged: (Boolean) -> Unit,
    vm: SettingsViewModel,
    onOpenThemeCreator: (seedColor: Int?) -> Unit = {},
) {
    item {
        val s = LocalStrings.current
        SettingsGroup(s.settingsTheme) {
            RowDivider()

            AccentColorPaletteRow(
                selectedIndex = state.accentColorIndex,
                state = state,
                onOpenThemeCreator = onOpenThemeCreator,
                onReset = {
                    vm.sendEvent(SettingsEvent.OnAccentColorChanged(DEFAULT_ACCENT_INDEX))
                    vm.sendEvent(SettingsEvent.OnCustomThemeApplied(null))
                },
                onSelect = { vm.sendEvent(SettingsEvent.OnAccentColorChanged(it)) }
            )
        }
    }

    item {
        val s = LocalStrings.current
        SettingsGroup(s.settingsDisplay) {
            ListRow(
                s.settingsFontSize,
                s.sizeLabel(state.fontSize.ordinal),
                SettingsState.FontSize.entries.map { s.sizeLabel(it.ordinal) }
            ) { vm.sendEvent(SettingsEvent.OnFontSizeChanged(SettingsState.FontSize.entries[it])) }
            UiScaleRow(state.uiScale) { vm.sendEvent(SettingsEvent.OnUiScaleChanged(it)) }
            RowDivider()
            ListRow(
                s.settingsEmoteSize,
                s.sizeLabel(state.emoteSize.ordinal),
                SettingsState.EmoteSize.entries.map { s.sizeLabel(it.ordinal) }
            ) { vm.sendEvent(SettingsEvent.OnEmoteSizeChanged(SettingsState.EmoteSize.entries[it])) }
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
            }
            RowDivider()
            ListRow(
                s.settingsMessageSpacing,
                s.spacingLabel(state.messageSpacing.ordinal),
                SettingsState.MessageSpacing.entries.map { s.spacingLabel(it.ordinal) }
            ) { vm.sendEvent(SettingsEvent.OnMessageSpacingChanged(SettingsState.MessageSpacing.entries[it])) }
        }
    }
    item {
        val s = LocalStrings.current
        SettingsPair(
            first = { FontSettingsCard(state = state, vm = vm) },
            second = {
                SettingsGroup(s.settingsLinks) {
                    ListRow(
                        s.settingsOpenLinks,
                        when (state.linkOpenMode) {
                            SettingsState.LinkOpenMode.DEFAULT -> s.settingsDefaultBrowser
                            SettingsState.LinkOpenMode.INCOGNITO -> s.settingsIncognitoMode
                        },
                        listOf(s.settingsDefaultBrowser, s.settingsIncognitoMode)
                    ) {
                        vm.sendEvent(SettingsEvent.OnLinkOpenModeChanged(SettingsState.LinkOpenMode.entries[it]))
                    }
                }
            }
        )
    }
    item {
        val s = LocalStrings.current
        SettingsGroup(s.settingsWindow) {
            SwitchRow(s.settingsAlwaysOnTop, s.settingsAlwaysOnTopDesc, state.alwaysOnTop) {
                vm.sendEvent(SettingsEvent.OnAlwaysOnTopChanged(it))
            }
            if (isDesktopPlatform) {
                RowDivider()
                SwitchRow(s.settingsHideSidebar, s.settingsHideSidebarDesc, state.hideSidebar) {
                    vm.sendEvent(SettingsEvent.OnHideSidebarChanged(it))
                }
            }
            RowDivider()
            val titleBarOptions = listOf(
                s.settingsTitleBarDark,
                s.settingsTitleBarLight,
                s.settingsTitleBarAdaptive,
                s.settingsTitleBarSystem
            )
            val titleBarModes = listOf(
                TitleBarMode.DARK,
                TitleBarMode.LIGHT,
                TitleBarMode.ADAPTIVE,
                TitleBarMode.SYSTEM
            )
            DropdownRow(
                label = s.settingsTitleBarMode,
                description = s.settingsTitleBarModeDesc,
                options = titleBarOptions,
                selected = titleBarModes.indexOf(state.titleBarMode).coerceAtLeast(0),
                onSelected = { vm.sendEvent(SettingsEvent.OnTitleBarModeChanged(titleBarModes[it])) }
            )
            RowDivider()
            run {
                val locales = AppLocale.all
                val selectedIdx =
                    locales.indexOfFirst { it.code == state.language }.coerceAtLeast(0)
                DropdownRow(
                    label = s.settingsLanguage,
                    description = s.settingsLanguageDesc,
                    options = locales.map { it.displayName },
                    selected = selectedIdx,
                    onSelected = { vm.sendEvent(SettingsEvent.OnLanguageChanged(locales[it].code)) }
                )
            }
            RowDivider()
            SwitchRow(
                s.settingsBlockScroll,
                s.settingsBlockScrollDesc,
                state.disableScrollOnAlt
            ) {
                vm.sendEvent(SettingsEvent.OnDisableScrollOnAltChanged(it))
            }
        }
    }
}

@Composable
internal fun AppearanceContent(
    state: SettingsState,
    onThemeChanged: (Boolean) -> Unit,
    vm: SettingsViewModel,
    onOpenThemeCreator: (seedColor: Int?) -> Unit = {}
) {
    val s = LocalStrings.current
    SettingsGroup(s.settingsTheme) {
        RowDivider()
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

    SettingsGroup(s.settingsDisplay) {
        ListRow(
            s.settingsFontSize, s.sizeLabel(state.fontSize.ordinal),
            SettingsState.FontSize.entries.map { s.sizeLabel(it.ordinal) }
        ) { vm.sendEvent(SettingsEvent.OnFontSizeChanged(SettingsState.FontSize.entries[it])) }
        UiScaleRow(state.uiScale) { vm.sendEvent(SettingsEvent.OnUiScaleChanged(it)) }
        RowDivider()
        ListRow(
            s.settingsEmoteSize,
            s.sizeLabel(state.emoteSize.ordinal),
            SettingsState.EmoteSize.entries.map { s.sizeLabel(it.ordinal) }
        ) { vm.sendEvent(SettingsEvent.OnEmoteSizeChanged(SettingsState.EmoteSize.entries[it])) }
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
        }
    }
    FontSettingsCard(state = state, vm = vm)
    SettingsGroup(s.settingsLinks) {
        ListRow(
            s.settingsOpenLinks,
            when (state.linkOpenMode) {
                SettingsState.LinkOpenMode.DEFAULT -> s.settingsDefaultBrowser
                SettingsState.LinkOpenMode.INCOGNITO -> s.settingsIncognitoMode
            },
            listOf(s.settingsDefaultBrowser, s.settingsIncognitoMode)
        ) { vm.sendEvent(SettingsEvent.OnLinkOpenModeChanged(SettingsState.LinkOpenMode.entries[it])) }
    }
    SettingsGroup(s.settingsWindow) {
        SwitchRow(s.settingsAlwaysOnTop, s.settingsAlwaysOnTopDesc, state.alwaysOnTop) {
            vm.sendEvent(SettingsEvent.OnAlwaysOnTopChanged(it))
        }
        if (isDesktopPlatform) {
            RowDivider()
            SwitchRow(s.settingsHideSidebar, s.settingsHideSidebarDesc, state.hideSidebar) {
                vm.sendEvent(SettingsEvent.OnHideSidebarChanged(it))
            }
        }
        RowDivider()
        val titleBarOptions = listOf(
            s.settingsTitleBarDark,
            s.settingsTitleBarLight,
            s.settingsTitleBarAdaptive,
            s.settingsTitleBarSystem
        )
        val titleBarModes = listOf(
            TitleBarMode.DARK,
            TitleBarMode.LIGHT,
            TitleBarMode.ADAPTIVE,
            TitleBarMode.SYSTEM
        )
        DropdownRow(
            label = s.settingsTitleBarMode,
            description = s.settingsTitleBarModeDesc,
            options = titleBarOptions,
            selected = titleBarModes.indexOf(state.titleBarMode).coerceAtLeast(0),
            onSelected = { vm.sendEvent(SettingsEvent.OnTitleBarModeChanged(titleBarModes[it])) }
        )
        RowDivider()
        run {
            val locales = AppLocale.all
            val selectedIdx = locales.indexOfFirst { it.code == state.language }.coerceAtLeast(0)
            DropdownRow(
                label = s.settingsLanguage,
                description = s.settingsLanguageDesc,
                options = locales.map { it.displayName },
                selected = selectedIdx,
                onSelected = { vm.sendEvent(SettingsEvent.OnLanguageChanged(locales[it].code)) }
            )
        }
        RowDivider()
        SwitchRow(
            s.settingsBlockScroll,
            s.settingsBlockScrollDesc,
            state.disableScrollOnAlt
        ) {
            vm.sendEvent(SettingsEvent.OnDisableScrollOnAltChanged(it))
        }
    }
}

private val isPhoneFormFactor: Boolean by lazy { currentFormFactor() == DeviceFormFactor.PHONE }

private fun AppStrings.sizeLabel(index: Int): String =
    listOf(
        settingsSizeSmall,
        settingsSizeMedium,
        settingsSizeLarge
    ).getOrElse(index) { settingsSizeMedium }

private fun AppStrings.spacingLabel(index: Int): String =
    listOf(
        settingsSpacingNone,
        settingsSpacingLow,
        settingsSpacingMedium,
        settingsSpacingHigh
    ).getOrElse(index) { settingsSpacingLow }
