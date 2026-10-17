package io.rudione.chatone.presentation.settings.sections

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import io.rudione.chatone.presentation.chat.TranslationLanguages
import io.rudione.chatone.presentation.chat.zoom.ChatMessageScaleSettingRow
import io.rudione.chatone.presentation.components.rows.ListRow
import io.rudione.chatone.presentation.components.rows.RowDivider
import io.rudione.chatone.presentation.components.rows.SliderRow
import io.rudione.chatone.presentation.components.rows.SwitchRow
import io.rudione.chatone.presentation.settings.SettingsEvent
import io.rudione.chatone.presentation.settings.SettingsState
import io.rudione.chatone.presentation.settings.SettingsViewModel
import io.rudione.chatone.presentation.settings.components.SettingsGroup
import io.rudione.chatone.presentation.settings.components.SettingsPair
import io.rudione.chatone.presentation.theme.i18n.AppStrings
import io.rudione.chatone.presentation.theme.i18n.LocalStrings
import io.rudione.chatone.util.platform.rememberInputCapabilities

@Composable
internal fun ChatContent(state: SettingsState, vm: SettingsViewModel) {
    SettingsPair(
        first = { MessageLookGroup(state, vm) },
        second = { RowDecorGroup(state, vm) }
    )
    SettingsPair(
        first = { MessagesGroup(state, vm) },
        second = { ChatPanelGroup(state, vm) }
    )
    val pointer = rememberInputCapabilities().pointer
    SettingsPair(
        first = { ChatInputGroup(state, vm) },
        second = if (pointer) {
            { MouseGroup(state, vm) }
        } else null
    )
    SettingsPair(
        first = { HistoryGroup(state, vm) },
        second = { MentionTabsGroup(state, vm) }
    )
    TranslationGroup(state, vm)
}

@Composable
private fun MessageLookGroup(state: SettingsState, vm: SettingsViewModel) {
    val s = LocalStrings.current
    SettingsGroup(s.settingsHub.groupMessageLook) {
        ChatMessageScaleSettingRow()
        RowDivider()
        ListRow(
            s.settingsFontSize,
            s.sizeLabel(state.fontSize.ordinal),
            SettingsState.FontSize.entries.map { s.sizeLabel(it.ordinal) }
        ) { vm.sendEvent(SettingsEvent.OnFontSizeChanged(SettingsState.FontSize.entries[it])) }
        RowDivider()
        ListRow(
            s.settingsEmoteSize,
            s.sizeLabel(state.emoteSize.ordinal),
            SettingsState.EmoteSize.entries.map { s.sizeLabel(it.ordinal) }
        ) { vm.sendEvent(SettingsEvent.OnEmoteSizeChanged(SettingsState.EmoteSize.entries[it])) }
        RowDivider()
        ListRow(
            s.settingsMessageSpacing,
            s.spacingLabel(state.messageSpacing.ordinal),
            SettingsState.MessageSpacing.entries.map { s.spacingLabel(it.ordinal) }
        ) { vm.sendEvent(SettingsEvent.OnMessageSpacingChanged(SettingsState.MessageSpacing.entries[it])) }
    }
}

@Composable
private fun RowDecorGroup(state: SettingsState, vm: SettingsViewModel) {
    val s = LocalStrings.current
    SettingsGroup(s.settingsHub.groupRowDecor) {
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
                if (state.timestampFormat == SettingsState.TimestampFormat.H12) s.settingsTimestamp12h else s.settingsTimestamp24h,
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
        SwitchRow(s.chatShowBadges, s.chatShowBadgesDesc, state.showBadges) {
            vm.sendEvent(SettingsEvent.OnShowBadgesChanged(it))
        }
        RowDivider()
        SwitchRow(s.settingsAlternateRowBg, s.settingsAlternateRowBgDesc, state.alternateRowBackground) {
            vm.sendEvent(SettingsEvent.OnAlternateRowBackgroundChanged(it))
        }
        RowDivider()
        SwitchRow(s.settingsReadableNickColors, s.settingsReadableNickColorsDesc, state.readableNickColors) {
            vm.sendEvent(SettingsEvent.OnReadableNickColorsChanged(it))
        }
    }
}

@Composable
private fun MessagesGroup(state: SettingsState, vm: SettingsViewModel) {
    val s = LocalStrings.current
    SettingsGroup(s.settingsMessages) {
        SwitchRow(s.settingsShowDeletedMessages, s.settingsShowDeletedMessagesDesc, state.showDeletedMessages) {
            vm.sendEvent(SettingsEvent.OnShowDeletedChanged(it))
        }
        RowDivider()
        SwitchRow(s.settingsShowViewerJoinLeave, s.settingsShowViewerJoinLeaveDesc, state.showViewerJoinLeave) {
            vm.sendEvent(SettingsEvent.OnShowViewerJoinLeaveChanged(it))
        }
        RowDivider()
        SwitchRow(s.settingsSmoothChat, s.settingsSmoothChatDesc, state.smoothChatEnabled) {
            vm.sendEvent(SettingsEvent.OnSmoothChatEnabledChanged(it))
        }
        RowDivider()
        SwitchRow(s.settingsShowRepeatedCounter, s.settingsShowRepeatedCounterDesc, state.showRepeatedMessageCounter) {
            vm.sendEvent(SettingsEvent.OnShowRepeatedCounterChanged(it))
        }
        if (state.showRepeatedMessageCounter) {
            SliderRow(
                s.settingsRepeatedWindow, state.repeatedMessageWindow, 5f..120f, 11,
                s.settingsSecondsUnit.replace("{0}", state.repeatedMessageWindow.toString())
            ) { vm.sendEvent(SettingsEvent.OnRepeatedWindowChanged(it.toInt())) }
        }
    }
}

@Composable
private fun ChatPanelGroup(state: SettingsState, vm: SettingsViewModel) {
    val s = LocalStrings.current
    SettingsGroup(s.settingsHub.groupChatPanel) {
        SwitchRow(s.settingsShowChatHeader, s.settingsShowChatHeaderDesc, state.showChatHeader) {
            vm.sendEvent(SettingsEvent.OnShowChatHeaderChanged(it))
        }
        SliderRow(
            label = s.settingsChatScrollbarWidth,
            value = state.chatScrollbarWidth.toFloat(),
            valueRange = 6f..32f,
            steps = 12,
            valueLabel = "${state.chatScrollbarWidth} dp"
        ) { vm.sendEvent(SettingsEvent.OnChatScrollbarWidthChanged(it.toInt())) }
        SliderRow(
            label = s.settingsChatTopBarHeight,
            value = state.chatTopBarHeight.toFloat(),
            valueRange = 28f..64f,
            steps = 17,
            valueLabel = "${state.chatTopBarHeight} dp"
        ) { vm.sendEvent(SettingsEvent.OnChatTopBarHeightChanged(it.toInt())) }
    }
}

@Composable
private fun ChatInputGroup(state: SettingsState, vm: SettingsViewModel) {
    val s = LocalStrings.current
    SettingsGroup(s.settingsChatInputGroup) {
        SwitchRow(s.settingsHidePlaceholder, s.settingsHidePlaceholderDesc, state.hideChatInputPlaceholder) {
            vm.sendEvent(SettingsEvent.OnHideChatPlaceholderChanged(it))
        }
        RowDivider()
        SwitchRow(s.settingsHideEmojiButton, s.settingsHideEmojiButtonDesc, state.hideEmojiButton) {
            vm.sendEvent(SettingsEvent.OnHideEmojiButtonChanged(it))
        }
        RowDivider()
        SwitchRow(s.settingsChatInputGlow, s.settingsChatInputGlowDesc, state.chatInputEventGlow) {
            vm.sendEvent(SettingsEvent.OnChatInputGlowChanged(it))
        }
    }
}

@Composable
private fun MouseGroup(state: SettingsState, vm: SettingsViewModel) {
    val s = LocalStrings.current
    SettingsGroup(s.settingsHub.groupMouse) {
        SwitchRow(s.settingsPauseOnHover, s.settingsPauseOnHoverDesc, state.pauseOnHover) {
            vm.sendEvent(SettingsEvent.OnPauseOnHoverChanged(it))
        }
        RowDivider()
        SwitchRow(s.settingsCloseOnMouseLeave, s.settingsCloseOnMouseLeaveDesc, state.closeEmotePickerOnMouseLeave) {
            vm.sendEvent(SettingsEvent.OnCloseEmotePickerOnMouseLeaveChanged(it))
        }
    }
}

@Composable
private fun HistoryGroup(state: SettingsState, vm: SettingsViewModel) {
    val s = LocalStrings.current
    SettingsGroup(s.settingsHistoryGroup) {
        SliderRow(
            s.settingsMessageHistoryLimit, state.scrollbackLimit, 100f..2000f, 18,
            s.settingsMessagesUnit.replace("{0}", state.scrollbackLimit.toString())
        ) { vm.sendEvent(SettingsEvent.OnScrollbackLimitChanged(it.toInt())) }
    }
}

@Composable
private fun MentionTabsGroup(state: SettingsState, vm: SettingsViewModel) {
    val s = LocalStrings.current
    SettingsGroup(s.settingsMentionTabs) {
        SwitchRow(s.settingsMentionTabs, s.settingsMentionTabsDesc, state.mentionTabsEnabled) {
            vm.sendEvent(SettingsEvent.OnMentionTabsChanged(it))
        }
    }
}

@Composable
private fun TranslationGroup(state: SettingsState, vm: SettingsViewModel) {
    val s = LocalStrings.current
    val langs = TranslationLanguages
    val currentName = langs.firstOrNull { it.first == state.translationTargetLang }?.second ?: state.translationTargetLang
    SettingsGroup(s.settingsTranslationLang) {
        ListRow(s.settingsTranslationLangDesc, currentName, langs.map { it.second }) { idx ->
            vm.sendEvent(SettingsEvent.OnTranslationLangChanged(langs[idx].first))
        }
        RowDivider()
        SwitchRow(s.settingsAutoTranslateInput, s.settingsAutoTranslateInputDesc, state.autoTranslateInput) {
            vm.sendEvent(SettingsEvent.OnAutoTranslateInputChanged(it))
        }
        AnimatedVisibility(
            visible = state.autoTranslateInput,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            val outgoingName = langs.firstOrNull { it.first == state.outgoingTranslationLang }?.second
                ?: state.outgoingTranslationLang
            ListRow(
                title = s.settingsOutgoingTranslationLang,
                value = outgoingName,
                options = langs.map { it.second },
                help = s.settingsOutgoingTranslationLangDesc
            ) { idx -> vm.sendEvent(SettingsEvent.OnOutgoingTranslationLangChanged(langs[idx].first)) }
        }
    }
}

internal fun AppStrings.sizeLabel(index: Int): String =
    listOf(settingsSizeSmall, settingsSizeMedium, settingsSizeLarge).getOrElse(index) { settingsSizeMedium }

private fun AppStrings.spacingLabel(index: Int): String =
    listOf(settingsSpacingNone, settingsSpacingLow, settingsSpacingMedium, settingsSpacingHigh)
        .getOrElse(index) { settingsSpacingLow }
