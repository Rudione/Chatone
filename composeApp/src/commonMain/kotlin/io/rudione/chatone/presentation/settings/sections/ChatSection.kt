package io.rudione.chatone.presentation.settings.sections

import io.rudione.chatone.presentation.settings.MentionTabsSettingsGroup
import io.rudione.chatone.presentation.settings.TranslationSettingsGroup
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

internal fun LazyListScope.chatLazyItems(state: SettingsState, vm: SettingsViewModel) {
    item {
        val s = LocalStrings.current
        SettingsGroup(s.settingsMessages) {
            SwitchRow(
                s.settingsTimestamps, s.settingsShowTimestampsDesc,
                state.timestampFormat != SettingsState.TimestampFormat.OFF
            ) { enabled ->
                vm.sendEvent(
                    SettingsEvent.OnTimestampFormatChanged(
                        if (enabled) SettingsState.TimestampFormat.H24 else SettingsState.TimestampFormat.OFF
                    )
                )
            }
            if (state.timestampFormat != SettingsState.TimestampFormat.OFF) {
                RowDivider()
                ListRow(
                    s.settingsTimestampFormat,
                    when (state.timestampFormat) {
                        SettingsState.TimestampFormat.H12 -> s.settingsTimestamp12h
                        SettingsState.TimestampFormat.H24 -> s.settingsTimestamp24h
                        else -> s.settingsTimestampOff
                    },
                    listOf(s.settingsTimestamp12h, s.settingsTimestamp24h)
                ) {
                    vm.sendEvent(
                        SettingsEvent.OnTimestampFormatChanged(
                            if (it == 0) SettingsState.TimestampFormat.H12 else SettingsState.TimestampFormat.H24
                        )
                    )
                }
            }
            RowDivider()
            SwitchRow(
                s.settingsShowChatHeader,
                s.settingsShowChatHeaderDesc,
                state.showChatHeader
            ) {
                vm.sendEvent(SettingsEvent.OnShowChatHeaderChanged(it))
            }
            RowDivider()
            SwitchRow(s.chatShowBadges, s.chatShowBadgesDesc, state.showBadges) {
                vm.sendEvent(SettingsEvent.OnShowBadgesChanged(it))
            }
            RowDivider()
            SwitchRow(
                s.settingsShowDeletedMessages,
                s.settingsShowDeletedMessagesDesc,
                state.showDeletedMessages
            ) {
                vm.sendEvent(SettingsEvent.OnShowDeletedChanged(it))
            }
            RowDivider()
            SwitchRow(
                s.settingsShowViewerJoinLeave,
                s.settingsShowViewerJoinLeaveDesc,
                state.showViewerJoinLeave
            ) {
                vm.sendEvent(SettingsEvent.OnShowViewerJoinLeaveChanged(it))
            }
            RowDivider()
            SwitchRow(
                s.settingsSmoothChat,
                s.settingsSmoothChatDesc,
                state.smoothChatEnabled
            ) {
                vm.sendEvent(SettingsEvent.OnSmoothChatEnabledChanged(it))
            }
            RowDivider()
            SwitchRow(
                s.settingsAlternateRowBg,
                s.settingsAlternateRowBgDesc,
                state.alternateRowBackground
            ) {
                vm.sendEvent(SettingsEvent.OnAlternateRowBackgroundChanged(it))
            }
        }
    }
    item {
        val s = LocalStrings.current
        SettingsGroup(s.settingsAutoScroll) {
            SwitchRow(
                s.settingsPauseOnHover,
                s.settingsPauseOnHoverDesc,
                state.pauseOnHover
            ) {
                vm.sendEvent(SettingsEvent.OnPauseOnHoverChanged(it))
            }
        }
    }
    item {
        val s = LocalStrings.current
        SettingsGroup(s.settingsEmotePicker) {
            SwitchRow(
                s.settingsCloseOnMouseLeave,
                s.settingsCloseOnMouseLeaveDesc,
                state.closeEmotePickerOnMouseLeave
            ) {
                vm.sendEvent(SettingsEvent.OnCloseEmotePickerOnMouseLeaveChanged(it))
            }
        }
    }
    item {
        val s = LocalStrings.current
        SettingsGroup(s.settingsHistoryGroup) {
            SliderRow(
                s.settingsMessageHistoryLimit, state.scrollbackLimit, 100f..2000f, 18,
                s.settingsMessagesUnit.replace("{0}", state.scrollbackLimit.toString())
            ) {
                vm.sendEvent(SettingsEvent.OnScrollbackLimitChanged(it.toInt()))
            }
        }
    }
    item {
        val s = LocalStrings.current
        SettingsGroup(s.settingsChatInputGroup) {
            SwitchRow(
                s.settingsHidePlaceholder, s.settingsHidePlaceholderDesc,
                state.hideChatInputPlaceholder
            ) { vm.sendEvent(SettingsEvent.OnHideChatPlaceholderChanged(it)) }
            RowDivider()
            SwitchRow(
                s.settingsHideEmojiButton, s.settingsHideEmojiButtonDesc,
                state.hideEmojiButton
            ) { vm.sendEvent(SettingsEvent.OnHideEmojiButtonChanged(it)) }
            RowDivider()
            SwitchRow(
                s.settingsChatInputGlow, s.settingsChatInputGlowDesc,
                state.chatInputEventGlow
            ) { vm.sendEvent(SettingsEvent.OnChatInputGlowChanged(it)) }
            RowDivider()
            SwitchRow(
                s.settingsShowRepeatedCounter, s.settingsShowRepeatedCounterDesc,
                state.showRepeatedMessageCounter
            ) { vm.sendEvent(SettingsEvent.OnShowRepeatedCounterChanged(it)) }
            if (state.showRepeatedMessageCounter) {
                RowDivider()
                SliderRow(
                    s.settingsRepeatedWindow, state.repeatedMessageWindow, 5f..120f, 11,
                    s.settingsSecondsUnit.replace("{0}", state.repeatedMessageWindow.toString())
                ) { vm.sendEvent(SettingsEvent.OnRepeatedWindowChanged(it.toInt())) }
            }
        }
    }
    item { MentionTabsSettingsGroup(state, vm) }
    item { TranslationSettingsGroup(state, vm) }
}

@Composable
internal fun ChatContent(state: SettingsState, vm: SettingsViewModel) {
    val s = LocalStrings.current
    SettingsGroup(s.settingsMessages) {
        SwitchRow(
            s.settingsTimestamps, s.settingsShowTimestampsDesc,
            state.timestampFormat != SettingsState.TimestampFormat.OFF
        ) { enabled ->
            vm.sendEvent(
                SettingsEvent.OnTimestampFormatChanged(
                    if (enabled) SettingsState.TimestampFormat.H24 else SettingsState.TimestampFormat.OFF
                )
            )
        }
        if (state.timestampFormat != SettingsState.TimestampFormat.OFF) {
            RowDivider()
            ListRow(
                s.settingsTimestampFormat,
                when (state.timestampFormat) {
                    SettingsState.TimestampFormat.H12 -> s.settingsTimestamp12h
                    SettingsState.TimestampFormat.H24 -> s.settingsTimestamp24h
                    else -> s.settingsTimestampOff
                },
                listOf(s.settingsTimestamp12h, s.settingsTimestamp24h)
            ) {
                vm.sendEvent(
                    SettingsEvent.OnTimestampFormatChanged(
                        if (it == 0) SettingsState.TimestampFormat.H12 else SettingsState.TimestampFormat.H24
                    )
                )
            }
        }
        RowDivider()
        SwitchRow(
            s.settingsShowChatHeader,
            s.settingsShowChatHeaderDesc,
            state.showChatHeader
        ) {
            vm.sendEvent(SettingsEvent.OnShowChatHeaderChanged(it))
        }
        RowDivider()
        SwitchRow(s.chatShowBadges, s.chatShowBadgesDesc, state.showBadges) {
            vm.sendEvent(SettingsEvent.OnShowBadgesChanged(it))
        }
        RowDivider()
        SwitchRow(
            s.settingsShowDeletedMessages,
            s.settingsShowDeletedMessagesDesc,
            state.showDeletedMessages
        ) {
            vm.sendEvent(SettingsEvent.OnShowDeletedChanged(it))
        }
        RowDivider()
        SwitchRow(
            s.settingsShowViewerJoinLeave,
            s.settingsShowViewerJoinLeaveDesc,
            state.showViewerJoinLeave
        ) {
            vm.sendEvent(SettingsEvent.OnShowViewerJoinLeaveChanged(it))
        }
        RowDivider()
        SwitchRow(
            s.settingsSmoothChat,
            s.settingsSmoothChatDesc,
            state.smoothChatEnabled
        ) {
            vm.sendEvent(SettingsEvent.OnSmoothChatEnabledChanged(it))
        }
        RowDivider()
        SwitchRow(
            s.settingsAlternateRowBg,
            s.settingsAlternateRowBgDesc,
            state.alternateRowBackground
        ) {
            vm.sendEvent(SettingsEvent.OnAlternateRowBackgroundChanged(it))
        }
    }
    SettingsGroup(s.settingsAutoScroll) {
        SwitchRow(
            s.settingsPauseOnHover,
            s.settingsPauseOnHoverDesc,
            state.pauseOnHover
        ) {
            vm.sendEvent(SettingsEvent.OnPauseOnHoverChanged(it))
        }
    }
    SettingsGroup(s.settingsEmotePicker) {
        SwitchRow(
            s.settingsCloseOnMouseLeave,
            s.settingsCloseOnMouseLeaveDesc,
            state.closeEmotePickerOnMouseLeave
        ) {
            vm.sendEvent(SettingsEvent.OnCloseEmotePickerOnMouseLeaveChanged(it))
        }
    }
    SettingsGroup(s.settingsHistoryGroup) {
        SliderRow(
            s.settingsMessageHistoryLimit, state.scrollbackLimit, 100f..2000f, 18,
            s.settingsMessagesUnit.replace("{0}", state.scrollbackLimit.toString())
        ) {
            vm.sendEvent(SettingsEvent.OnScrollbackLimitChanged(it.toInt()))
        }
    }
    MentionTabsSettingsGroup(state, vm)
    TranslationSettingsGroup(state, vm)
}
