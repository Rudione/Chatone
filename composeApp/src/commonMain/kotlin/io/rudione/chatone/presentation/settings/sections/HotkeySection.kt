package io.rudione.chatone.presentation.settings.sections

import io.rudione.chatone.presentation.settings.PauseHotkeyMode
import io.rudione.chatone.presentation.settings.InlineImageMode
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

internal fun LazyListScope.hotkeyLazyItems(state: SettingsState, vm: SettingsViewModel) {
    item { HotkeyCombinationsGroup(state, vm) }
    item {
        val s = LocalStrings.current
        SettingsGroup(s.settingsChatControls) {
            HotkeyRow(s.settingsPauseAutoScroll, s.settingsPauseAutoScrollDesc, state.pauseHotkey) {
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
        val s = LocalStrings.current
        SettingsGroup(s.settingsImageLinks) {
            DropdownRow(
                label = s.settingsShowInlineImages,
                description = s.settingsShowInlineImagesDesc,
                options = listOf(s.on, s.off, s.blur),
                selected = state.showInlineImages.ordinal
            ) { idx ->
                vm.sendEvent(SettingsEvent.OnShowInlineImagesChanged(InlineImageMode.entries[idx]))
            }
            if (state.showInlineImages != InlineImageMode.OFF) {
                SliderRow(
                    label = s.settingsImageMaxHeight,
                    value = state.inlineImageMaxHeight.toFloat(),
                    valueRange = 50f..500f,
                    steps = 8,
                    valueLabel = s.settingsImageMaxHeightUnit.replace(
                        "{0}",
                        state.inlineImageMaxHeight.toString()
                    )
                ) { vm.sendEvent(SettingsEvent.OnInlineImageMaxHeightChanged(it.toInt())) }
            }
            SliderRow(
                label = s.settingsChatScrollbarWidth,
                value = state.chatScrollbarWidth.toFloat(),
                valueRange = 6f..32f,
                steps = 12,
                valueLabel = "${state.chatScrollbarWidth} dp"
            ) { vm.sendEvent(SettingsEvent.OnChatScrollbarWidthChanged(it.toInt())) }
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
        HotkeyRow(s.settingsPauseAutoScroll, s.settingsPauseAutoScrollDesc, state.pauseHotkey) {
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
    SettingsGroup(s.settingsImageLinks) {
        DropdownRow(
            label = s.settingsShowInlineImages,
            description = s.settingsShowInlineImagesDesc,
            options = listOf(s.on, s.off, s.blur),
            selected = state.showInlineImages.ordinal
        ) { idx ->
            vm.sendEvent(SettingsEvent.OnShowInlineImagesChanged(InlineImageMode.entries[idx]))
        }
        if (state.showInlineImages != InlineImageMode.OFF) {
            SliderRow(
                label = s.settingsImageMaxHeight,
                value = state.inlineImageMaxHeight.toFloat(),
                valueRange = 50f..500f,
                steps = 8,
                valueLabel = s.settingsImageMaxHeightUnit.replace(
                    "{0}",
                    state.inlineImageMaxHeight.toString()
                )
            ) { vm.sendEvent(SettingsEvent.OnInlineImageMaxHeightChanged(it.toInt())) }
        }
    }
    io.rudione.chatone.presentation.settings.components.ImageUploaderSection(
        state = state,
        onEvent = { vm.sendEvent(it) }
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun hotkeyActionLabel(action: HotkeyAction, s: io.rudione.chatone.presentation.theme.i18n.AppStrings): String =
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
        SwitchRow(s.settingsNavigationHidden, s.settingsNavigationHiddenDesc, state.navigationHidden) {
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
