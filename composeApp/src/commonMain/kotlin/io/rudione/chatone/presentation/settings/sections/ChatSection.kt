package io.rudione.chatone.presentation.settings.sections

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import io.rudione.chatone.presentation.components.rows.DropdownRow
import io.rudione.chatone.presentation.components.rows.ListRow
import io.rudione.chatone.presentation.components.rows.RowDivider
import io.rudione.chatone.presentation.components.rows.SliderRow
import io.rudione.chatone.presentation.components.rows.SwitchRow
import io.rudione.chatone.presentation.settings.InlineImageMode
import io.rudione.chatone.presentation.settings.MentionTabsSettingsGroup
import io.rudione.chatone.presentation.settings.SettingsEvent
import io.rudione.chatone.presentation.settings.SettingsState
import io.rudione.chatone.presentation.settings.SettingsViewModel
import io.rudione.chatone.presentation.settings.TranslationSettingsGroup
import io.rudione.chatone.presentation.settings.components.SettingsGroup
import io.rudione.chatone.presentation.theme.i18n.LocalStrings

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
                label = s.settingsClipPreviewWidth,
                value = state.clipPreviewWidth.toFloat(),
                valueRange = 90f..320f,
                steps = 22,
                valueLabel = "${state.clipPreviewWidth} dp"
            ) { vm.sendEvent(SettingsEvent.OnClipPreviewWidthChanged(it.toInt())) }
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
            label = s.settingsClipPreviewWidth,
            value = state.clipPreviewWidth.toFloat(),
            valueRange = 90f..320f,
            steps = 22,
            valueLabel = "${state.clipPreviewWidth} dp"
        ) { vm.sendEvent(SettingsEvent.OnClipPreviewWidthChanged(it.toInt())) }
        SliderRow(
            label = s.settingsChatScrollbarWidth,
            value = state.chatScrollbarWidth.toFloat(),
            valueRange = 6f..32f,
            steps = 12,
            valueLabel = "${state.chatScrollbarWidth} dp"
        ) { vm.sendEvent(SettingsEvent.OnChatScrollbarWidthChanged(it.toInt())) }
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
