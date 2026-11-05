package io.rudione.chatone.presentation.settings.sections

import chatone.composeapp.generated.resources.settings_size_large
import chatone.composeapp.generated.resources.settings_size_medium
import chatone.composeapp.generated.resources.settings_size_small
import chatone.composeapp.generated.resources.settings_spacing_high
import chatone.composeapp.generated.resources.settings_spacing_low
import chatone.composeapp.generated.resources.settings_spacing_medium
import chatone.composeapp.generated.resources.settings_spacing_none
import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.chat_show_badges
import chatone.composeapp.generated.resources.chat_show_badges_desc
import chatone.composeapp.generated.resources.settings_alternate_row_bg
import chatone.composeapp.generated.resources.settings_alternate_row_bg_desc
import chatone.composeapp.generated.resources.settings_auto_translate_input
import chatone.composeapp.generated.resources.settings_auto_translate_input_desc
import chatone.composeapp.generated.resources.settings_chat_input_glow
import chatone.composeapp.generated.resources.settings_chat_input_glow_desc
import chatone.composeapp.generated.resources.settings_chat_input_group
import chatone.composeapp.generated.resources.settings_chat_scrollbar_width
import chatone.composeapp.generated.resources.settings_chat_top_bar_height
import chatone.composeapp.generated.resources.settings_close_on_mouse_leave
import chatone.composeapp.generated.resources.settings_close_on_mouse_leave_desc
import chatone.composeapp.generated.resources.settings_emote_size
import chatone.composeapp.generated.resources.settings_font_size
import chatone.composeapp.generated.resources.settings_hide_emoji_button
import chatone.composeapp.generated.resources.settings_hide_emoji_button_desc
import chatone.composeapp.generated.resources.settings_hide_placeholder
import chatone.composeapp.generated.resources.settings_hide_placeholder_desc
import chatone.composeapp.generated.resources.settings_history_group
import chatone.composeapp.generated.resources.settings_hub_group_chat_panel
import chatone.composeapp.generated.resources.settings_hub_group_message_look
import chatone.composeapp.generated.resources.settings_hub_group_mouse
import chatone.composeapp.generated.resources.settings_hub_group_row_decor
import chatone.composeapp.generated.resources.settings_mention_tabs
import chatone.composeapp.generated.resources.settings_mention_tabs_desc
import chatone.composeapp.generated.resources.settings_message_history_limit
import chatone.composeapp.generated.resources.settings_message_spacing
import chatone.composeapp.generated.resources.settings_messages
import chatone.composeapp.generated.resources.settings_messages_unit
import chatone.composeapp.generated.resources.settings_outgoing_translation_lang
import chatone.composeapp.generated.resources.settings_outgoing_translation_lang_desc
import chatone.composeapp.generated.resources.settings_pause_on_hover
import chatone.composeapp.generated.resources.settings_pause_on_hover_desc
import chatone.composeapp.generated.resources.settings_readable_nick_colors
import chatone.composeapp.generated.resources.settings_readable_nick_colors_desc
import chatone.composeapp.generated.resources.settings_repeated_window
import chatone.composeapp.generated.resources.settings_seconds_unit
import chatone.composeapp.generated.resources.settings_show_chat_header
import chatone.composeapp.generated.resources.settings_show_chat_header_desc
import chatone.composeapp.generated.resources.settings_show_deleted_messages
import chatone.composeapp.generated.resources.settings_show_deleted_messages_desc
import chatone.composeapp.generated.resources.settings_show_repeated_counter
import chatone.composeapp.generated.resources.settings_show_repeated_counter_desc
import chatone.composeapp.generated.resources.settings_show_timestamps_desc
import chatone.composeapp.generated.resources.settings_show_viewer_join_leave
import chatone.composeapp.generated.resources.settings_show_viewer_join_leave_desc
import chatone.composeapp.generated.resources.settings_smooth_chat
import chatone.composeapp.generated.resources.settings_smooth_chat_desc
import chatone.composeapp.generated.resources.settings_timestamp12h
import chatone.composeapp.generated.resources.settings_timestamp24h
import chatone.composeapp.generated.resources.settings_timestamp_format
import chatone.composeapp.generated.resources.settings_timestamps
import chatone.composeapp.generated.resources.settings_translation_lang
import chatone.composeapp.generated.resources.settings_translation_lang_desc
import org.jetbrains.compose.resources.stringResource
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
    SettingsGroup(stringResource(Res.string.settings_hub_group_message_look)) {
        ChatMessageScaleSettingRow()
        RowDivider()
        ListRow(
            stringResource(Res.string.settings_font_size),
            sizeLabel(state.fontSize.ordinal),
            SettingsState.FontSize.entries.map { sizeLabel(it.ordinal) }
        ) { vm.sendEvent(SettingsEvent.OnFontSizeChanged(SettingsState.FontSize.entries[it])) }
        RowDivider()
        ListRow(
            stringResource(Res.string.settings_emote_size),
            sizeLabel(state.emoteSize.ordinal),
            SettingsState.EmoteSize.entries.map { sizeLabel(it.ordinal) }
        ) { vm.sendEvent(SettingsEvent.OnEmoteSizeChanged(SettingsState.EmoteSize.entries[it])) }
        RowDivider()
        ListRow(
            stringResource(Res.string.settings_message_spacing),
            spacingLabel(state.messageSpacing.ordinal),
            SettingsState.MessageSpacing.entries.map { spacingLabel(it.ordinal) }
        ) { vm.sendEvent(SettingsEvent.OnMessageSpacingChanged(SettingsState.MessageSpacing.entries[it])) }
    }
}

@Composable
private fun RowDecorGroup(state: SettingsState, vm: SettingsViewModel) {
    SettingsGroup(stringResource(Res.string.settings_hub_group_row_decor)) {
        SwitchRow(
            stringResource(Res.string.settings_timestamps), stringResource(Res.string.settings_show_timestamps_desc),
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
                stringResource(Res.string.settings_timestamp_format),
                if (state.timestampFormat == SettingsState.TimestampFormat.H12) stringResource(Res.string.settings_timestamp12h) else stringResource(Res.string.settings_timestamp24h),
                listOf(stringResource(Res.string.settings_timestamp12h), stringResource(Res.string.settings_timestamp24h))
            ) {
                vm.sendEvent(
                    SettingsEvent.OnTimestampFormatChanged(
                        if (it == 0) SettingsState.TimestampFormat.H12 else SettingsState.TimestampFormat.H24
                    )
                )
            }
        }
        RowDivider()
        SwitchRow(stringResource(Res.string.chat_show_badges), stringResource(Res.string.chat_show_badges_desc), state.showBadges) {
            vm.sendEvent(SettingsEvent.OnShowBadgesChanged(it))
        }
        RowDivider()
        SwitchRow(stringResource(Res.string.settings_alternate_row_bg), stringResource(Res.string.settings_alternate_row_bg_desc), state.alternateRowBackground) {
            vm.sendEvent(SettingsEvent.OnAlternateRowBackgroundChanged(it))
        }
        RowDivider()
        SwitchRow(stringResource(Res.string.settings_readable_nick_colors), stringResource(Res.string.settings_readable_nick_colors_desc), state.readableNickColors) {
            vm.sendEvent(SettingsEvent.OnReadableNickColorsChanged(it))
        }
    }
}

@Composable
private fun MessagesGroup(state: SettingsState, vm: SettingsViewModel) {
    SettingsGroup(stringResource(Res.string.settings_messages)) {
        SwitchRow(stringResource(Res.string.settings_show_deleted_messages), stringResource(Res.string.settings_show_deleted_messages_desc), state.showDeletedMessages) {
            vm.sendEvent(SettingsEvent.OnShowDeletedChanged(it))
        }
        RowDivider()
        SwitchRow(stringResource(Res.string.settings_show_viewer_join_leave), stringResource(Res.string.settings_show_viewer_join_leave_desc), state.showViewerJoinLeave) {
            vm.sendEvent(SettingsEvent.OnShowViewerJoinLeaveChanged(it))
        }
        RowDivider()
        SwitchRow(stringResource(Res.string.settings_smooth_chat), stringResource(Res.string.settings_smooth_chat_desc), state.smoothChatEnabled) {
            vm.sendEvent(SettingsEvent.OnSmoothChatEnabledChanged(it))
        }
        RowDivider()
        SwitchRow(stringResource(Res.string.settings_show_repeated_counter), stringResource(Res.string.settings_show_repeated_counter_desc), state.showRepeatedMessageCounter) {
            vm.sendEvent(SettingsEvent.OnShowRepeatedCounterChanged(it))
        }
        if (state.showRepeatedMessageCounter) {
            SliderRow(
                stringResource(Res.string.settings_repeated_window), state.repeatedMessageWindow, 5f..120f, 11,
                stringResource(Res.string.settings_seconds_unit, state.repeatedMessageWindow)
            ) { vm.sendEvent(SettingsEvent.OnRepeatedWindowChanged(it.toInt())) }
        }
    }
}

@Composable
private fun ChatPanelGroup(state: SettingsState, vm: SettingsViewModel) {
    SettingsGroup(stringResource(Res.string.settings_hub_group_chat_panel)) {
        SwitchRow(stringResource(Res.string.settings_show_chat_header), stringResource(Res.string.settings_show_chat_header_desc), state.showChatHeader) {
            vm.sendEvent(SettingsEvent.OnShowChatHeaderChanged(it))
        }
        SliderRow(
            label = stringResource(Res.string.settings_chat_scrollbar_width),
            value = state.chatScrollbarWidth.toFloat(),
            valueRange = 6f..32f,
            steps = 12,
            valueLabel = "${state.chatScrollbarWidth} dp"
        ) { vm.sendEvent(SettingsEvent.OnChatScrollbarWidthChanged(it.toInt())) }
        SliderRow(
            label = stringResource(Res.string.settings_chat_top_bar_height),
            value = state.chatTopBarHeight.toFloat(),
            valueRange = 28f..64f,
            steps = 17,
            valueLabel = "${state.chatTopBarHeight} dp"
        ) { vm.sendEvent(SettingsEvent.OnChatTopBarHeightChanged(it.toInt())) }
    }
}

@Composable
private fun ChatInputGroup(state: SettingsState, vm: SettingsViewModel) {
    SettingsGroup(stringResource(Res.string.settings_chat_input_group)) {
        SwitchRow(stringResource(Res.string.settings_hide_placeholder), stringResource(Res.string.settings_hide_placeholder_desc), state.hideChatInputPlaceholder) {
            vm.sendEvent(SettingsEvent.OnHideChatPlaceholderChanged(it))
        }
        RowDivider()
        SwitchRow(stringResource(Res.string.settings_hide_emoji_button), stringResource(Res.string.settings_hide_emoji_button_desc), state.hideEmojiButton) {
            vm.sendEvent(SettingsEvent.OnHideEmojiButtonChanged(it))
        }
        RowDivider()
        SwitchRow(stringResource(Res.string.settings_chat_input_glow), stringResource(Res.string.settings_chat_input_glow_desc), state.chatInputEventGlow) {
            vm.sendEvent(SettingsEvent.OnChatInputGlowChanged(it))
        }
    }
}

@Composable
private fun MouseGroup(state: SettingsState, vm: SettingsViewModel) {
    SettingsGroup(stringResource(Res.string.settings_hub_group_mouse)) {
        SwitchRow(stringResource(Res.string.settings_pause_on_hover), stringResource(Res.string.settings_pause_on_hover_desc), state.pauseOnHover) {
            vm.sendEvent(SettingsEvent.OnPauseOnHoverChanged(it))
        }
        RowDivider()
        SwitchRow(stringResource(Res.string.settings_close_on_mouse_leave), stringResource(Res.string.settings_close_on_mouse_leave_desc), state.closeEmotePickerOnMouseLeave) {
            vm.sendEvent(SettingsEvent.OnCloseEmotePickerOnMouseLeaveChanged(it))
        }
    }
}

@Composable
private fun HistoryGroup(state: SettingsState, vm: SettingsViewModel) {
    SettingsGroup(stringResource(Res.string.settings_history_group)) {
        SliderRow(
            stringResource(Res.string.settings_message_history_limit), state.scrollbackLimit, 100f..2000f, 18,
            stringResource(Res.string.settings_messages_unit, state.scrollbackLimit)
        ) { vm.sendEvent(SettingsEvent.OnScrollbackLimitChanged(it.toInt())) }
    }
}

@Composable
private fun MentionTabsGroup(state: SettingsState, vm: SettingsViewModel) {
    SettingsGroup(stringResource(Res.string.settings_mention_tabs)) {
        SwitchRow(stringResource(Res.string.settings_mention_tabs), stringResource(Res.string.settings_mention_tabs_desc), state.mentionTabsEnabled) {
            vm.sendEvent(SettingsEvent.OnMentionTabsChanged(it))
        }
    }
}

@Composable
private fun TranslationGroup(state: SettingsState, vm: SettingsViewModel) {
    val langs = TranslationLanguages
    val currentName = langs.firstOrNull { it.first == state.translationTargetLang }?.second ?: state.translationTargetLang
    SettingsGroup(stringResource(Res.string.settings_translation_lang)) {
        ListRow(stringResource(Res.string.settings_translation_lang_desc), currentName, langs.map { it.second }) { idx ->
            vm.sendEvent(SettingsEvent.OnTranslationLangChanged(langs[idx].first))
        }
        RowDivider()
        SwitchRow(stringResource(Res.string.settings_auto_translate_input), stringResource(Res.string.settings_auto_translate_input_desc), state.autoTranslateInput) {
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
                title = stringResource(Res.string.settings_outgoing_translation_lang),
                value = outgoingName,
                options = langs.map { it.second },
                help = stringResource(Res.string.settings_outgoing_translation_lang_desc)
            ) { idx -> vm.sendEvent(SettingsEvent.OnOutgoingTranslationLangChanged(langs[idx].first)) }
        }
    }
}

@Composable
internal fun sizeLabel(index: Int): String = stringResource(
    listOf(Res.string.settings_size_small, Res.string.settings_size_medium, Res.string.settings_size_large).getOrElse(index) { Res.string.settings_size_medium })

@Composable
private fun spacingLabel(index: Int): String = stringResource(
    listOf(Res.string.settings_spacing_none, Res.string.settings_spacing_low, Res.string.settings_spacing_medium, Res.string.settings_spacing_high)
        .getOrElse(index) { Res.string.settings_spacing_low })

