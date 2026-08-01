package io.rudione.chatone.presentation.settings.sections

import io.rudione.chatone.presentation.settings.UiScaleRow
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.bell_filled
import chatone.composeapp.generated.resources.bell_outlined
import chatone.composeapp.generated.resources.chatbubbles
import chatone.composeapp.generated.resources.chatbubbles_outline
import chatone.composeapp.generated.resources.icon
import chatone.composeapp.generated.resources.images
import chatone.composeapp.generated.resources.images_outline
import chatone.composeapp.generated.resources.key_outline
import chatone.composeapp.generated.resources.ic_sword
import chatone.composeapp.generated.resources.sparkle_filled
import chatone.composeapp.generated.resources.keyboard_24_filled
import chatone.composeapp.generated.resources.keyboard_24_regular
import chatone.composeapp.generated.resources.musical_notes_outline
import chatone.composeapp.generated.resources.palette_fill_16
import chatone.composeapp.generated.resources.palette_stroke_12
import chatone.composeapp.generated.resources.panel_left_key_16_regular
import chatone.composeapp.generated.resources.person_filled
import chatone.composeapp.generated.resources.shield_filled
import chatone.composeapp.generated.resources.shield_outlined
import chatone.composeapp.generated.resources.star_filled
import chatone.composeapp.generated.resources.star_outlined
import chatone.composeapp.generated.resources.unfold_more
import chatone.composeapp.generated.resources.wallpaper_filled
import chatone.composeapp.generated.resources.wallpaper_outlined
import coil3.compose.AsyncImage
import io.rudione.chatone.domain.model.HighlightRule
import io.rudione.chatone.presentation.components.LiquidGlassSurface
import io.rudione.chatone.presentation.components.rows.HighlightedSettingsText
import io.rudione.chatone.presentation.components.rows.LocalSettingsSearch
import io.rudione.chatone.presentation.components.rows.RowDivider
import io.rudione.chatone.presentation.components.rows.SwitchRow
import io.rudione.chatone.presentation.components.rows.ListRow
import io.rudione.chatone.presentation.components.rows.DropdownRow
import io.rudione.chatone.presentation.components.rows.SliderRow
import io.rudione.chatone.presentation.components.rows.HotkeyRow
import io.rudione.chatone.presentation.settings.components.ModerationSettingsSection
import io.rudione.chatone.presentation.settings.theme_settings.ThemeSettingsScreen
import io.rudione.chatone.presentation.settings.theme_settings.ThinSlider
import io.rudione.chatone.presentation.theme.ChatoneTheme
import io.rudione.chatone.presentation.theme.CustomThemeManager
import io.rudione.chatone.presentation.theme.DEFAULT_ACCENT_INDEX
import io.rudione.chatone.presentation.theme.ExpressivePalettes
import io.rudione.chatone.presentation.theme.LocalCustomThemeManager
import io.rudione.chatone.presentation.theme.LocalWallpaperController
import androidx.compose.runtime.CompositionLocalProvider
import io.rudione.chatone.util.BuildConfig
import io.rudione.chatone.util.system.HotkeyAction
import io.rudione.chatone.util.system.comboFor
import io.rudione.chatone.util.media.NotificationSoundPlayer
import io.rudione.chatone.util.media.WallpaperLoader
import io.rudione.chatone.presentation.theme.i18n.AppLocale
import io.rudione.chatone.presentation.theme.i18n.AppStrings
import io.rudione.chatone.presentation.theme.i18n.LocalStrings
import io.rudione.chatone.presentation.settings.TitleBarMode
import io.rudione.chatone.presentation.settings.SettingsState
import io.rudione.chatone.presentation.settings.SettingsEvent
import io.rudione.chatone.presentation.settings.SettingsViewModel
import io.rudione.chatone.util.media.pickAudioFile
import io.rudione.chatone.util.media.pickImageFile
import io.rudione.chatone.util.font.pickFontFile
import io.rudione.chatone.util.font.resolveFontFamily
import io.rudione.chatone.util.font.listAvailableFontNames
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextDecoration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

import io.rudione.chatone.presentation.settings.components.SettingsGroup
import io.rudione.chatone.presentation.settings.components.SettingsSurface
import io.rudione.chatone.presentation.settings.components.NotificationGroupCard
import io.rudione.chatone.presentation.settings.components.CustomSoundCard
import io.rudione.chatone.presentation.settings.components.BackgroundCard
import io.rudione.chatone.presentation.settings.components.AccentColorPaletteRow
import io.rudione.chatone.presentation.settings.components.HighlightRuleCardFor
import io.rudione.chatone.presentation.settings.components.HightlightRuleCard
import io.rudione.chatone.presentation.settings.components.FontSettingsCard

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
                state.fontSize.name.lowercase().replaceFirstChar { it.uppercase() },
                SettingsState.FontSize.entries.map {
                    it.name.lowercase().replaceFirstChar { c -> c.uppercase() }
                }
            ) { vm.sendEvent(SettingsEvent.OnFontSizeChanged(SettingsState.FontSize.entries[it])) }
            UiScaleRow(state.uiScale) { vm.sendEvent(SettingsEvent.OnUiScaleChanged(it)) }
            RowDivider()
            ListRow(
                s.settingsEmoteSize,
                state.emoteSize.name.lowercase().replaceFirstChar { it.uppercase() },
                SettingsState.EmoteSize.entries.map {
                    it.name.lowercase().replaceFirstChar { c -> c.uppercase() }
                }
            ) { vm.sendEvent(SettingsEvent.OnEmoteSizeChanged(SettingsState.EmoteSize.entries[it])) }
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
            ListRow(
                s.settingsMessageSpacing,
                state.messageSpacing.name.lowercase().replaceFirstChar { it.uppercase() },
                listOf("None", "Low", "Medium", "High")
            ) { vm.sendEvent(SettingsEvent.OnMessageSpacingChanged(SettingsState.MessageSpacing.entries[it])) }
        }
    }
    item {
        FontSettingsCard(state = state, vm = vm)
    }
    item {
        val s = LocalStrings.current
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
    item {
        val s = LocalStrings.current
        SettingsGroup(s.settingsWindow) {
            SwitchRow(s.settingsAlwaysOnTop, s.settingsAlwaysOnTopDesc, state.alwaysOnTop) {
                vm.sendEvent(SettingsEvent.OnAlwaysOnTopChanged(it))
            }
            RowDivider()
            val titleBarOptions = listOf(
                s.settingsTitleBarDark,
                s.settingsTitleBarLight,
                s.settingsTitleBarAdaptive,
                s.settingsTitleBarSystem
            )
            val titleBarModes = listOf(TitleBarMode.DARK, TitleBarMode.LIGHT, TitleBarMode.ADAPTIVE, TitleBarMode.SYSTEM)
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
            s.settingsFontSize, state.fontSize.name.lowercase().replaceFirstChar { it.uppercase() },
            SettingsState.FontSize.entries.map {
                it.name.lowercase().replaceFirstChar { c -> c.uppercase() }
            }
        ) { vm.sendEvent(SettingsEvent.OnFontSizeChanged(SettingsState.FontSize.entries[it])) }
        UiScaleRow(state.uiScale) { vm.sendEvent(SettingsEvent.OnUiScaleChanged(it)) }
        RowDivider()
        ListRow(
            s.settingsEmoteSize,
            state.emoteSize.name.lowercase().replaceFirstChar { it.uppercase() },
            SettingsState.EmoteSize.entries.map {
                it.name.lowercase().replaceFirstChar { c -> c.uppercase() }
            }
        ) { vm.sendEvent(SettingsEvent.OnEmoteSizeChanged(SettingsState.EmoteSize.entries[it])) }
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
        RowDivider()
        val titleBarOptions = listOf(
            s.settingsTitleBarDark,
            s.settingsTitleBarLight,
            s.settingsTitleBarAdaptive,
            s.settingsTitleBarSystem
        )
        val titleBarModes = listOf(TitleBarMode.DARK, TitleBarMode.LIGHT, TitleBarMode.ADAPTIVE, TitleBarMode.SYSTEM)
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
