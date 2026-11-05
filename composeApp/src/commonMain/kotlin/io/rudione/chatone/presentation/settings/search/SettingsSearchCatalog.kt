package io.rudione.chatone.presentation.settings.search

import chatone.composeapp.generated.resources.mod_slider_ban_at_end
import chatone.composeapp.generated.resources.mod_slider_ban_at_end_help
import chatone.composeapp.generated.resources.mod_slider_delete_at_start
import chatone.composeapp.generated.resources.mod_slider_delete_at_start_help
import chatone.composeapp.generated.resources.mod_slider_enable_help
import chatone.composeapp.generated.resources.mod_slider_enable_title
import chatone.composeapp.generated.resources.mod_slider_preview_hint
import chatone.composeapp.generated.resources.mod_slider_preview_title
import chatone.composeapp.generated.resources.mod_slider_scale_title
import chatone.composeapp.generated.resources.mod_slider_steps_help
import chatone.composeapp.generated.resources.mod_slider_steps_title
import chatone.composeapp.generated.resources.mod_slider_travel_help
import chatone.composeapp.generated.resources.mod_slider_travel_title
import chatone.composeapp.generated.resources.mod_slider_twitch_scale
import chatone.composeapp.generated.resources.mod_slider_twitch_scale_help
import chatone.composeapp.generated.resources.screen_chat_backdrop_help
import chatone.composeapp.generated.resources.screen_chat_backdrop_opacity_title
import chatone.composeapp.generated.resources.screen_chat_backdrop_title
import chatone.composeapp.generated.resources.screen_chat_background_title
import chatone.composeapp.generated.resources.screen_chat_font_size_title
import chatone.composeapp.generated.resources.screen_chat_group_look
import chatone.composeapp.generated.resources.screen_chat_group_messages
import chatone.composeapp.generated.resources.screen_chat_group_window
import chatone.composeapp.generated.resources.screen_chat_hide_delay_title
import chatone.composeapp.generated.resources.screen_chat_keep_dock_help
import chatone.composeapp.generated.resources.screen_chat_keep_dock_title
import chatone.composeapp.generated.resources.screen_chat_layer_help
import chatone.composeapp.generated.resources.screen_chat_layer_title
import chatone.composeapp.generated.resources.screen_chat_locked_help
import chatone.composeapp.generated.resources.screen_chat_locked_title
import chatone.composeapp.generated.resources.screen_chat_moderation_help
import chatone.composeapp.generated.resources.screen_chat_moderation_title
import chatone.composeapp.generated.resources.screen_chat_over_full_screen_help
import chatone.composeapp.generated.resources.screen_chat_over_full_screen_title
import chatone.composeapp.generated.resources.screen_chat_reply_above_help
import chatone.composeapp.generated.resources.screen_chat_reply_above_title
import chatone.composeapp.generated.resources.screen_chat_scale_title
import chatone.composeapp.generated.resources.screen_chat_settings_help
import chatone.composeapp.generated.resources.screen_chat_settings_title
import chatone.composeapp.generated.resources.screen_chat_text_title
import chatone.composeapp.generated.resources.settings_hub_group_chat_panel
import chatone.composeapp.generated.resources.settings_hub_group_interface
import chatone.composeapp.generated.resources.settings_hub_group_links_media
import chatone.composeapp.generated.resources.settings_hub_group_message_look
import chatone.composeapp.generated.resources.settings_hub_group_mouse
import chatone.composeapp.generated.resources.settings_hub_group_row_decor
import chatone.composeapp.generated.resources.settings_hub_highlight_built_in
import chatone.composeapp.generated.resources.settings_hub_highlight_custom
import chatone.composeapp.generated.resources.settings_hub_uploader_behaviour_group
import chatone.composeapp.generated.resources.settings_hub_uploader_fields_help
import chatone.composeapp.generated.resources.settings_hub_uploader_fields_title
import chatone.composeapp.generated.resources.settings_hub_uploader_link_help
import chatone.composeapp.generated.resources.settings_hub_uploader_service_group
import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.accounts_add
import chatone.composeapp.generated.resources.accounts_proxy_desc
import chatone.composeapp.generated.resources.accounts_proxy_title
import chatone.composeapp.generated.resources.accounts_title
import chatone.composeapp.generated.resources.actions_automations
import chatone.composeapp.generated.resources.actions_automations_hint
import chatone.composeapp.generated.resources.actions_hint
import chatone.composeapp.generated.resources.actions_kind_reply
import chatone.composeapp.generated.resources.actions_kind_sound
import chatone.composeapp.generated.resources.actions_kind_timer
import chatone.composeapp.generated.resources.actions_muted_phrases
import chatone.composeapp.generated.resources.actions_muted_phrases_hint
import chatone.composeapp.generated.resources.actions_safety_note
import chatone.composeapp.generated.resources.ai_active_hours
import chatone.composeapp.generated.resources.ai_auto_mod_title
import chatone.composeapp.generated.resources.ai_config_title
import chatone.composeapp.generated.resources.ai_enable
import chatone.composeapp.generated.resources.ai_enable_desc
import chatone.composeapp.generated.resources.ai_monitor_desc
import chatone.composeapp.generated.resources.ai_monitor_title
import chatone.composeapp.generated.resources.ai_sensitivity
import chatone.composeapp.generated.resources.blocked_users
import chatone.composeapp.generated.resources.blocked_users_desc
import chatone.composeapp.generated.resources.blocked_users_show_messages
import chatone.composeapp.generated.resources.chat_show_badges
import chatone.composeapp.generated.resources.chat_show_badges_desc
import chatone.composeapp.generated.resources.ext_rights_title
import chatone.composeapp.generated.resources.highlight_rule_channel_points
import chatone.composeapp.generated.resources.highlight_rule_first_message
import chatone.composeapp.generated.resources.highlight_rule_search_match
import chatone.composeapp.generated.resources.highlight_rule_subscriptions
import chatone.composeapp.generated.resources.highlight_rule_username
import chatone.composeapp.generated.resources.highlight_rule_whispers
import chatone.composeapp.generated.resources.hotkeys_combinations_title
import chatone.composeapp.generated.resources.mod_add_timeout_button
import chatone.composeapp.generated.resources.mod_button_color_hint
import chatone.composeapp.generated.resources.mod_button_colors
import chatone.composeapp.generated.resources.mod_buttons_on_hover
import chatone.composeapp.generated.resources.mod_buttons_on_hover_desc
import chatone.composeapp.generated.resources.mod_create_macro
import chatone.composeapp.generated.resources.mod_default_timeout_duration
import chatone.composeapp.generated.resources.mod_drag_reorder_hint
import chatone.composeapp.generated.resources.mod_local_automod
import chatone.composeapp.generated.resources.mod_local_automod_desc
import chatone.composeapp.generated.resources.mod_macros
import chatone.composeapp.generated.resources.mod_macros_desc
import chatone.composeapp.generated.resources.mod_mod_action_buttons
import chatone.composeapp.generated.resources.mod_saved_ban_reason_label
import chatone.composeapp.generated.resources.mod_saved_reasons_desc
import chatone.composeapp.generated.resources.mod_saved_reasons_title
import chatone.composeapp.generated.resources.mod_saved_timeout_reason_label
import chatone.composeapp.generated.resources.mod_slider_buttons_in_chat
import chatone.composeapp.generated.resources.mod_slider_buttons_in_chat_help
import chatone.composeapp.generated.resources.settings_account
import chatone.composeapp.generated.resources.settings_add_rule
import chatone.composeapp.generated.resources.settings_alternate_row_bg
import chatone.composeapp.generated.resources.settings_alternate_row_bg_desc
import chatone.composeapp.generated.resources.settings_always_on_top
import chatone.composeapp.generated.resources.settings_always_on_top_desc
import chatone.composeapp.generated.resources.settings_always_on_top_mode
import chatone.composeapp.generated.resources.settings_always_on_top_mode_desc
import chatone.composeapp.generated.resources.settings_app_info
import chatone.composeapp.generated.resources.settings_auto_translate_input
import chatone.composeapp.generated.resources.settings_auto_translate_input_desc
import chatone.composeapp.generated.resources.settings_backup_desc
import chatone.composeapp.generated.resources.settings_backup_title
import chatone.composeapp.generated.resources.settings_block_scroll
import chatone.composeapp.generated.resources.settings_block_scroll_desc
import chatone.composeapp.generated.resources.settings_channel_navigation
import chatone.composeapp.generated.resources.settings_chat_background_image
import chatone.composeapp.generated.resources.settings_chat_controls
import chatone.composeapp.generated.resources.settings_chat_input_glow
import chatone.composeapp.generated.resources.settings_chat_input_glow_desc
import chatone.composeapp.generated.resources.settings_chat_input_group
import chatone.composeapp.generated.resources.settings_chat_message_scale
import chatone.composeapp.generated.resources.settings_chat_message_scale_hint
import chatone.composeapp.generated.resources.settings_chat_scrollbar_width
import chatone.composeapp.generated.resources.settings_chat_top_bar_height
import chatone.composeapp.generated.resources.settings_clear_cache
import chatone.composeapp.generated.resources.settings_clear_cache_desc
import chatone.composeapp.generated.resources.settings_clip_preview_width
import chatone.composeapp.generated.resources.settings_close_on_mouse_leave
import chatone.composeapp.generated.resources.settings_close_on_mouse_leave_desc
import chatone.composeapp.generated.resources.settings_commands_add
import chatone.composeapp.generated.resources.settings_commands_desc
import chatone.composeapp.generated.resources.settings_commands_title
import chatone.composeapp.generated.resources.settings_custom_sound
import chatone.composeapp.generated.resources.settings_emote_size
import chatone.composeapp.generated.resources.settings_enable_mention_sound
import chatone.composeapp.generated.resources.settings_enable_mention_sound_desc
import chatone.composeapp.generated.resources.settings_font_family
import chatone.composeapp.generated.resources.settings_font_italic
import chatone.composeapp.generated.resources.settings_font_size
import chatone.composeapp.generated.resources.settings_font_strikethrough
import chatone.composeapp.generated.resources.settings_font_underline
import chatone.composeapp.generated.resources.settings_giphy_api_key
import chatone.composeapp.generated.resources.settings_giphy_api_key_desc
import chatone.composeapp.generated.resources.settings_hide_emoji_button
import chatone.composeapp.generated.resources.settings_hide_emoji_button_desc
import chatone.composeapp.generated.resources.settings_hide_placeholder
import chatone.composeapp.generated.resources.settings_hide_placeholder_desc
import chatone.composeapp.generated.resources.settings_hide_sidebar
import chatone.composeapp.generated.resources.settings_hide_sidebar_desc
import chatone.composeapp.generated.resources.settings_highlight_rules_hint
import chatone.composeapp.generated.resources.settings_history_group
import chatone.composeapp.generated.resources.settings_image_max_height
import chatone.composeapp.generated.resources.settings_language
import chatone.composeapp.generated.resources.settings_language_desc
import chatone.composeapp.generated.resources.settings_logout
import chatone.composeapp.generated.resources.settings_logout_desc
import chatone.composeapp.generated.resources.settings_mention_alerts
import chatone.composeapp.generated.resources.settings_mention_alerts_desc
import chatone.composeapp.generated.resources.settings_mention_sound
import chatone.composeapp.generated.resources.settings_mention_tabs
import chatone.composeapp.generated.resources.settings_mention_tabs_desc
import chatone.composeapp.generated.resources.settings_message_history_limit
import chatone.composeapp.generated.resources.settings_message_spacing
import chatone.composeapp.generated.resources.settings_messages
import chatone.composeapp.generated.resources.settings_navigation_hidden
import chatone.composeapp.generated.resources.settings_navigation_hidden_desc
import chatone.composeapp.generated.resources.settings_open_links
import chatone.composeapp.generated.resources.settings_outgoing_translation_lang
import chatone.composeapp.generated.resources.settings_outgoing_translation_lang_desc
import chatone.composeapp.generated.resources.settings_overlay_opacity
import chatone.composeapp.generated.resources.settings_pause_auto_scroll
import chatone.composeapp.generated.resources.settings_pause_auto_scroll_desc
import chatone.composeapp.generated.resources.settings_pause_mode
import chatone.composeapp.generated.resources.settings_pause_on_hover
import chatone.composeapp.generated.resources.settings_pause_on_hover_desc
import chatone.composeapp.generated.resources.settings_readable_nick_colors
import chatone.composeapp.generated.resources.settings_readable_nick_colors_desc
import chatone.composeapp.generated.resources.settings_repeated_window
import chatone.composeapp.generated.resources.settings_set_custom
import chatone.composeapp.generated.resources.settings_show_chat_gifs
import chatone.composeapp.generated.resources.settings_show_chat_gifs_desc
import chatone.composeapp.generated.resources.settings_show_chat_header
import chatone.composeapp.generated.resources.settings_show_chat_header_desc
import chatone.composeapp.generated.resources.settings_show_deleted_messages
import chatone.composeapp.generated.resources.settings_show_deleted_messages_desc
import chatone.composeapp.generated.resources.settings_show_inline_images
import chatone.composeapp.generated.resources.settings_show_inline_images_desc
import chatone.composeapp.generated.resources.settings_show_repeated_counter
import chatone.composeapp.generated.resources.settings_show_repeated_counter_desc
import chatone.composeapp.generated.resources.settings_show_timestamps_desc
import chatone.composeapp.generated.resources.settings_show_viewer_join_leave
import chatone.composeapp.generated.resources.settings_show_viewer_join_leave_desc
import chatone.composeapp.generated.resources.settings_smooth_chat
import chatone.composeapp.generated.resources.settings_smooth_chat_desc
import chatone.composeapp.generated.resources.settings_sound_supported
import chatone.composeapp.generated.resources.settings_telegram
import chatone.composeapp.generated.resources.settings_theme
import chatone.composeapp.generated.resources.settings_timestamp_format
import chatone.composeapp.generated.resources.settings_timestamps
import chatone.composeapp.generated.resources.settings_title_bar_mode
import chatone.composeapp.generated.resources.settings_title_bar_mode_desc
import chatone.composeapp.generated.resources.settings_translation_lang
import chatone.composeapp.generated.resources.settings_translation_lang_desc
import chatone.composeapp.generated.resources.settings_typography
import chatone.composeapp.generated.resources.settings_ui_scale
import chatone.composeapp.generated.resources.settings_version
import chatone.composeapp.generated.resources.settings_volume
import chatone.composeapp.generated.resources.settings_window
import chatone.composeapp.generated.resources.streamer_hide_mod
import chatone.composeapp.generated.resources.streamer_hide_thumbs
import chatone.composeapp.generated.resources.streamer_mask_channels
import chatone.composeapp.generated.resources.streamer_mask_tokens
import chatone.composeapp.generated.resources.streamer_mode_hide_data
import chatone.composeapp.generated.resources.streamer_mode_obs
import chatone.composeapp.generated.resources.streamer_mode_title
import chatone.composeapp.generated.resources.streamer_suppress_notif
import chatone.composeapp.generated.resources.theme_accent_color
import chatone.composeapp.generated.resources.theme_accent_color_hint
import chatone.composeapp.generated.resources.uploader_ask_on_upload
import chatone.composeapp.generated.resources.uploader_check_status
import chatone.composeapp.generated.resources.uploader_deletion_link
import chatone.composeapp.generated.resources.uploader_desc
import chatone.composeapp.generated.resources.uploader_enable
import chatone.composeapp.generated.resources.uploader_extra_headers
import chatone.composeapp.generated.resources.uploader_form_field
import chatone.composeapp.generated.resources.uploader_image_link
import chatone.composeapp.generated.resources.uploader_request_url
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import io.rudione.chatone.presentation.settings.ModerationTab
import io.rudione.chatone.presentation.settings.SettingsSection
import io.rudione.chatone.presentation.settings.isAvailable
import io.rudione.chatone.presentation.settings.sections.labelRes
import io.rudione.chatone.util.platform.InputCapabilities
import io.rudione.chatone.util.system.HotkeyAction

internal class CatalogText(
    val title: String,
    val group: String?,
    val description: String?,
    val sectionLabel: String
)

internal class SettingsSearchEntry(
    val section: SettingsSection,
    val tab: ModerationTab?,
    val tabTitle: String?,
    val groupTitle: String?,
    text: CatalogText,
    keywords: String
) {
    val title: String = text.title
    val group: String? = text.group
    val description: String? = text.description
    val anchor: String get() = title
    val fallbackAnchor: String? get() = group
    val id: String = "${section.name}:${tab?.name.orEmpty()}:$title:${group.orEmpty()}"

    val document = SearchDocument(
        buildList {
            add(SearchField(text.title, TITLE_WEIGHT))
            if (keywords.isNotBlank()) add(SearchField(keywords, KEYWORD_WEIGHT))
            text.group?.let { add(SearchField(it, GROUP_WEIGHT)) }
            text.description?.let { add(SearchField(it, DESCRIPTION_WEIGHT)) }
            add(SearchField(text.sectionLabel, SECTION_WEIGHT))
        }
    )

    private companion object {
        const val TITLE_WEIGHT = 3f
        const val KEYWORD_WEIGHT = 2f
        const val GROUP_WEIGHT = 1.4f
        const val DESCRIPTION_WEIGHT = 1f
        const val SECTION_WEIGHT = 0.9f
    }
}

internal data class SettingsSubsection(
    val section: SettingsSection,
    val tab: ModerationTab?,
    val title: String,
    val anchor: String?
)

internal fun settingsSubsections(catalog: List<SettingsSearchEntry>): Map<SettingsSection, List<SettingsSubsection>> =
    catalog
        .mapNotNull { entry ->
            val tab = entry.tab
            when {
                tab != null -> SettingsSubsection(entry.section, tab, entry.tabTitle.orEmpty(), anchor = null)
                entry.groupTitle != null -> SettingsSubsection(entry.section, null, entry.groupTitle, anchor = entry.groupTitle)
                else -> null
            }
        }
        .distinct()
        .groupBy { it.section }

internal data class SettingsSearchHit(val entry: SettingsSearchEntry, val score: Float)

internal fun searchSettings(query: String, catalog: List<SettingsSearchEntry>): List<SettingsSearchHit> {
    if (query.isBlank()) return emptyList()
    val scored = catalog
        .mapIndexed { index, entry -> Triple(index, entry, SettingsSearchEngine.score(query, entry.document)) }
        .filter { it.third > 0f }
    val floor = (scored.maxOfOrNull { it.third } ?: 0f) * RELATIVE_SCORE_FLOOR
    return scored
        .filter { it.third >= floor }
        .sortedWith(compareByDescending<Triple<Int, SettingsSearchEntry, Float>> { it.third }.thenBy { it.first })
        .map { SettingsSearchHit(it.second, it.third) }
}

private const val RELATIVE_SCORE_FLOOR = 0.3f

data class SettingsCatalogContext(
    val input: InputCapabilities,
    val desktop: Boolean,
    val phone: Boolean,
    val macOs: Boolean = false
)

internal suspend fun buildSettingsCatalog(context: SettingsCatalogContext): List<SettingsSearchEntry> =
    collectCatalog(context).map { item ->
        SettingsSearchEntry(
            section = item.section,
            tab = item.tab,
            tabTitle = item.tabTitle,
            groupTitle = item.groupTitle,
            text = item.text,
            keywords = item.keywords
        )
    }

private class CatalogItem(
    val section: SettingsSection,
    val tab: ModerationTab?,
    val tabTitle: String?,
    val groupTitle: String?,
    val text: CatalogText,
    val keywords: String
)

private suspend fun collectCatalog(context: SettingsCatalogContext): List<CatalogItem> {
    val builder = CatalogBuilder(context)
    with(builder) {
        section(SettingsSection.APPEARANCE) {
            group(text(Res.string.settings_theme)) {
                entry(text(Res.string.theme_accent_color), text(Res.string.theme_accent_color_hint), "тема цвет акцент палитра оформление темная светлая theme color accent palette dark light")
                entry(text(Res.string.settings_set_custom), keywords = "создать тему редактор свою тему theme creator custom")
            }
            group(text(Res.string.settings_hub_group_interface)) {
                entry(text(Res.string.settings_ui_scale), keywords = "масштаб размер интерфейса зум ui scale zoom")
                entry(text(Res.string.settings_language), text(Res.string.settings_language_desc), "язык локаль русский английский language locale english russian")
                if (!context.phone) {
                    entry(text(Res.string.settings_channel_navigation), keywords = "вкладки мини панель rail tabs навигация каналов")
                    entry(text(Res.string.settings_navigation_hidden), text(Res.string.settings_navigation_hidden_desc), "скрыть вкладки hide tabs navigation")
                }
            }
            group(text(Res.string.settings_typography)) {
                entry(text(Res.string.settings_font_family), keywords = "шрифт семейство font family typeface")
                entry(text(Res.string.settings_font_italic), keywords = "курсив italic")
                entry(text(Res.string.settings_font_underline), keywords = "подчеркивание underline")
                entry(text(Res.string.settings_font_strikethrough), keywords = "зачеркивание strikethrough")
            }
            if (context.desktop || context.phone) {
                group(text(Res.string.settings_window)) {
                    if (context.desktop) entry(text(Res.string.settings_always_on_top), text(Res.string.settings_always_on_top_desc), "поверх всех окон always on top pin")
                    if (context.desktop) entry(text(Res.string.settings_always_on_top_mode), text(Res.string.settings_always_on_top_mode_desc), "поверх браузера системных окон above browser system windows")
                    entry(text(Res.string.settings_hide_sidebar), text(Res.string.settings_hide_sidebar_desc), "боковая панель боковую панель скрывать sidebar")
                    if (context.desktop) entry(text(Res.string.settings_title_bar_mode), text(Res.string.settings_title_bar_mode_desc), "заголовок окна title bar titlebar")
                }
            }
        }
        section(SettingsSection.CHAT) {
            group(text(Res.string.settings_hub_group_message_look)) {
                entry(text(Res.string.settings_chat_message_scale), text(Res.string.settings_chat_message_scale_hint), "масштаб сообщений размер zoom scale")
                entry(text(Res.string.settings_font_size), keywords = "размер шрифта текст font size")
                entry(text(Res.string.settings_emote_size), keywords = "эмоуты смайлы emotes emote size")
                entry(text(Res.string.settings_message_spacing), keywords = "плотность отступы интервал spacing density")
            }
            group(text(Res.string.settings_hub_group_row_decor)) {
                entry(text(Res.string.settings_timestamps), text(Res.string.settings_show_timestamps_desc), "время метки времени timestamp time 12h 24h")
                entry(text(Res.string.settings_timestamp_format), keywords = "формат времени 12 24 часа format")
                entry(text(Res.string.chat_show_badges), text(Res.string.chat_show_badges_desc), "значки бейджи badges")
                entry(text(Res.string.settings_alternate_row_bg), text(Res.string.settings_alternate_row_bg_desc), "зебра полосы чередование stripes zebra")
                entry(text(Res.string.settings_readable_nick_colors), text(Res.string.settings_readable_nick_colors_desc), "цвет ника читаемые username color contrast")
            }
            group(text(Res.string.settings_messages)) {
                entry(text(Res.string.settings_show_deleted_messages), text(Res.string.settings_show_deleted_messages_desc), "удаленные сообщения deleted removed")
                entry(text(Res.string.settings_show_viewer_join_leave), text(Res.string.settings_show_viewer_join_leave_desc), "заходы выходы зрителей join leave")
                entry(text(Res.string.settings_smooth_chat), text(Res.string.settings_smooth_chat_desc), "плавный чат анимация smooth")
                entry(text(Res.string.settings_show_repeated_counter), text(Res.string.settings_show_repeated_counter_desc), "повторы счетчик спам repeat counter")
                entry(text(Res.string.settings_repeated_window), keywords = "окно повторов секунды repeat window")
            }
            group(text(Res.string.settings_hub_group_chat_panel)) {
                entry(text(Res.string.settings_show_chat_header), text(Res.string.settings_show_chat_header_desc), "шапка заголовок канала header")
                entry(text(Res.string.settings_chat_scrollbar_width), keywords = "полоса прокрутки скроллбар ширина scrollbar width")
                entry(text(Res.string.settings_chat_top_bar_height), keywords = "верхняя панель высота top bar height")
            }
            group(text(Res.string.settings_chat_input_group)) {
                entry(text(Res.string.settings_hide_placeholder), text(Res.string.settings_hide_placeholder_desc), "подсказка поле ввода placeholder")
                entry(text(Res.string.settings_hide_emoji_button), text(Res.string.settings_hide_emoji_button_desc), "кнопка эмоутов смайлики emoji button")
                entry(text(Res.string.settings_chat_input_glow), text(Res.string.settings_chat_input_glow_desc), "свечение подсветка поля glow")
            }
            if (context.input.pointer) {
                group(text(Res.string.settings_hub_group_mouse)) {
                    entry(text(Res.string.settings_pause_on_hover), text(Res.string.settings_pause_on_hover_desc), "пауза при наведении автопрокрутка hover pause scroll")
                    entry(text(Res.string.settings_close_on_mouse_leave), text(Res.string.settings_close_on_mouse_leave_desc), "закрывать панель эмоутов emote picker")
                }
            }
            group(text(Res.string.settings_history_group)) {
                entry(text(Res.string.settings_message_history_limit), keywords = "история лимит сообщений scrollback history limit")
            }
            group(text(Res.string.settings_mention_tabs)) {
                entry(text(Res.string.settings_mention_tabs), text(Res.string.settings_mention_tabs_desc), "упоминания вкладки mentions")
            }
            group(text(Res.string.settings_translation_lang)) {
                entry(text(Res.string.settings_translation_lang_desc), keywords = "перевод язык translate translation")
                entry(text(Res.string.settings_auto_translate_input), text(Res.string.settings_auto_translate_input_desc), "автоперевод перевод исходящих auto translate")
                entry(text(Res.string.settings_outgoing_translation_lang), text(Res.string.settings_outgoing_translation_lang_desc), "язык исходящих outgoing")
            }
        }
        section(SettingsSection.SCREEN_CHAT) {
            group(null) {
                entry(text(Res.string.screen_chat_settings_title), text(Res.string.screen_chat_settings_help), "экранный чат оверлей окно поверх прозрачный стрим игра overlay screen chat")
            }
            group(text(Res.string.screen_chat_group_window)) {
                entry(text(Res.string.screen_chat_layer_title), text(Res.string.screen_chat_layer_help), "поверх всех окон слой always on top layer")
                if (context.macOs) {
                    entry(text(Res.string.screen_chat_over_full_screen_title), text(Res.string.screen_chat_over_full_screen_help), "полноэкранный фуллскрин весь экран dock full screen fullscreen")
                    entry(text(Res.string.screen_chat_keep_dock_title), text(Res.string.screen_chat_keep_dock_help), "значок док иконка dock icon cmd tab")
                }
                entry(text(Res.string.screen_chat_locked_title), text(Res.string.screen_chat_locked_help), "закрепить окна заблокировать lock pin")
                entry(text(Res.string.screen_chat_hide_delay_title), keywords = "скрывать панель задержка hide delay")
            }
            group(text(Res.string.screen_chat_group_messages)) {
                entry(text(Res.string.screen_chat_reply_above_title), text(Res.string.screen_chat_reply_above_help), "ответ реплай reply строка сверху")
                entry(text(Res.string.screen_chat_moderation_title), text(Res.string.screen_chat_moderation_help), "логи модерации муты баны таймауты удаления mod log timeouts bans")
            }
            group(text(Res.string.screen_chat_group_look)) {
                entry(text(Res.string.screen_chat_font_size_title), keywords = "шрифт размер текста ники font size")
                entry(text(Res.string.screen_chat_scale_title), keywords = "масштаб экранного чата size scale zoom")
                entry(text(Res.string.screen_chat_text_title), keywords = "прозрачность сообщений текст opacity")
                entry(text(Res.string.screen_chat_background_title), keywords = "фон прозрачность background opacity")
                entry(text(Res.string.screen_chat_backdrop_title), text(Res.string.screen_chat_backdrop_help), "подложка плашка фон под сообщениями backdrop")
                entry(text(Res.string.screen_chat_backdrop_opacity_title), keywords = "плотность подложки backdrop opacity")
            }
        }
        section(SettingsSection.MEDIA) {
            group(text(Res.string.settings_hub_group_links_media)) {
                entry(text(Res.string.settings_open_links), keywords = "ссылки браузер инкогнито links browser incognito")
                entry(text(Res.string.settings_show_inline_images), text(Res.string.settings_show_inline_images_desc), "картинки изображения превью размытие inline images blur preview")
                entry(text(Res.string.settings_image_max_height), keywords = "высота картинок image height")
                entry(text(Res.string.settings_clip_preview_width), keywords = "клипы превью clip preview")
                entry(text(Res.string.settings_show_chat_gifs), text(Res.string.settings_show_chat_gifs_desc), "гифки gif анимации")
                entry(text(Res.string.settings_giphy_api_key), text(Res.string.settings_giphy_api_key_desc), "giphy ключ api key")
            }
            if (context.desktop) {
                group(text(Res.string.settings_hub_uploader_behaviour_group)) {
                    entry(text(Res.string.uploader_enable), text(Res.string.uploader_desc), "загрузчик картинки скриншоты хостинг uploader image upload")
                    entry(text(Res.string.uploader_ask_on_upload), keywords = "подтверждение confirm ask")
                }
                group(text(Res.string.settings_hub_uploader_service_group)) {
                    entry(text(Res.string.uploader_request_url), keywords = "адрес url сервер endpoint")
                    entry(text(Res.string.uploader_form_field), keywords = "поле формы form field multipart")
                    entry(text(Res.string.settings_hub_uploader_fields_title), text(Res.string.settings_hub_uploader_fields_help), "свои поля параметры ключ api key fields")
                    entry(text(Res.string.uploader_extra_headers), keywords = "заголовки headers authorization токен")
                    entry(text(Res.string.uploader_image_link), text(Res.string.settings_hub_uploader_link_help), "ссылка json путь link")
                    entry(text(Res.string.uploader_deletion_link), keywords = "удаление ссылка delete link")
                    entry(text(Res.string.uploader_check_status), keywords = "проверить доступность статус check status")
                }
            }
        }
        section(SettingsSection.NOTIFICATIONS) {
            if (context.phone) {
                group(null) {
                    entry(text(Res.string.settings_mention_alerts), text(Res.string.settings_mention_alerts_desc), "пуш уведомления телефон push")
                }
            }
            group(text(Res.string.settings_mention_sound)) {
                entry(text(Res.string.settings_enable_mention_sound), text(Res.string.settings_enable_mention_sound_desc), "звук упоминания пинг mention sound ping")
                entry(text(Res.string.settings_volume), keywords = "громкость volume")
            }
            group(text(Res.string.settings_custom_sound)) {
                entry(text(Res.string.settings_custom_sound), text(Res.string.settings_sound_supported), "свой звук wav ogg custom sound")
            }
        }
        section(SettingsSection.HIGHLIGHTS) {
            group(text(Res.string.settings_hub_highlight_built_in)) {
                entry(text(Res.string.highlight_rule_username), keywords = "ник упоминание mention username")
                entry(text(Res.string.highlight_rule_whispers), keywords = "шепот личные whispers")
                entry(text(Res.string.highlight_rule_subscriptions), keywords = "подписки сабы subs subscriptions")
                entry(text(Res.string.highlight_rule_first_message), keywords = "первое сообщение first message")
                entry(text(Res.string.highlight_rule_search_match), keywords = "поиск совпадения search")
                entry(text(Res.string.highlight_rule_channel_points), keywords = "баллы поинты награды channel points")
            }
            group(text(Res.string.settings_hub_highlight_custom)) {
                entry(text(Res.string.settings_add_rule), text(Res.string.settings_highlight_rules_hint), "правило слово регулярка regex keyword rule цвет color")
            }
        }
        section(SettingsSection.BACKGROUND) {
            group(text(Res.string.settings_chat_background_image)) {
                entry(text(Res.string.settings_chat_background_image), keywords = "фон обои картинка wallpaper background image")
                entry(text(Res.string.settings_overlay_opacity), keywords = "затемнение прозрачность opacity dim")
            }
        }
        if (SettingsSection.HOTKEYS.isAvailable(context.input)) {
            section(SettingsSection.HOTKEYS) {
                group(text(Res.string.hotkeys_combinations_title)) {
                    HotkeyAction.entries.forEach { action ->
                        entry(text(action.labelRes()), keywords = "горячая клавиша сочетание hotkey shortcut")
                    }
                }
                group(text(Res.string.settings_chat_controls)) {
                    entry(text(Res.string.settings_pause_auto_scroll), text(Res.string.settings_pause_auto_scroll_desc), "пауза клавиша мышь pause hotkey")
                    entry(text(Res.string.settings_pause_mode), keywords = "удерживать переключать hold toggle")
                    entry(text(Res.string.settings_block_scroll), text(Res.string.settings_block_scroll_desc), "прокрутка колесико alt scroll")
                }
            }
        }
        section(SettingsSection.COMMANDS) {
            group(text(Res.string.settings_commands_title)) {
                entry(text(Res.string.settings_commands_title), text(Res.string.settings_commands_desc), "команды сокращения алиасы замена триггер commands alias trigger expander")
                entry(text(Res.string.settings_commands_add), keywords = "новая команда add command")
            }
        }
        section(SettingsSection.ACTIONS) {
            group(text(Res.string.actions_automations)) {
                entry(text(Res.string.actions_automations), text(Res.string.actions_hint), "автоматизации таймер автоответ автоответчик automation timer auto reply")
                entry(text(Res.string.actions_kind_timer), text(Res.string.actions_automations_hint), "сообщения по таймеру timed message")
                entry(text(Res.string.actions_kind_reply), text(Res.string.actions_safety_note), "автоответ ключевое слово keyword reply")
                entry(text(Res.string.actions_kind_sound), keywords = "звук оповещение sound alert")
            }
            group(text(Res.string.actions_muted_phrases)) {
                entry(text(Res.string.actions_muted_phrases), text(Res.string.actions_muted_phrases_hint), "скрытые фразы мьют игнор muted phrases ignore filter")
            }
        }
        section(SettingsSection.MODERATION) {
            tab(ModerationTab.GENERAL) {
                group(text(Res.string.mod_default_timeout_duration)) {
                    entry(text(Res.string.mod_default_timeout_duration), keywords = "таймаут по умолчанию длительность default timeout")
                }
                group(text(Res.string.mod_saved_reasons_title)) {
                    entry(text(Res.string.mod_saved_timeout_reason_label), text(Res.string.mod_saved_reasons_desc), "причина таймаута reason")
                    entry(text(Res.string.mod_saved_ban_reason_label), keywords = "причина бана ban reason")
                }
                group(text(Res.string.mod_local_automod)) {
                    entry(text(Res.string.mod_local_automod), text(Res.string.mod_local_automod_desc), "автомод правила automod rules local")
                }
            }
            tab(ModerationTab.BUTTONS) {
                group(text(Res.string.mod_mod_action_buttons)) {
                    entry(text(Res.string.mod_mod_action_buttons), text(Res.string.mod_drag_reorder_hint), "кнопки модерации удалить таймаут бан порядок mod buttons delete timeout ban")
                    entry(text(Res.string.mod_button_colors), text(Res.string.mod_button_color_hint), "цвета кнопок colors")
                    entry(text(Res.string.mod_slider_buttons_in_chat), text(Res.string.mod_slider_buttons_in_chat_help), "кнопки в чате скрыть выключить спрятать мини-профиль buttons in chat hide")
                    if (context.input.pointer) entry(text(Res.string.mod_buttons_on_hover), text(Res.string.mod_buttons_on_hover_desc), "при наведении hover")
                    entry(text(Res.string.mod_add_timeout_button), keywords = "добавить таймаут кнопку custom timeout")
                }
            }
            if (context.desktop) {
                tab(ModerationTab.SLIDER) {
                    group(null) {
                        entry(text(Res.string.mod_slider_enable_title), text(Res.string.mod_slider_enable_help), "слайдер ручка включить выключить модерация slider handle enable")
                    }
                    group(text(Res.string.mod_slider_scale_title)) {
                        entry(text(Res.string.mod_slider_twitch_scale), text(Res.string.mod_slider_twitch_scale_help), "шкала твич секунды минуты часы дни twitch scale")
                        entry(text(Res.string.mod_slider_steps_title), text(Res.string.mod_slider_steps_help), "свои сроки мута таймауты custom steps durations")
                    }
                    group(text(Res.string.mod_slider_travel_title)) {
                        entry(text(Res.string.mod_slider_delete_at_start), text(Res.string.mod_slider_delete_at_start_help), "удаление delete")
                        entry(text(Res.string.mod_slider_ban_at_end), text(Res.string.mod_slider_ban_at_end_help), "бан ban")
                        entry(text(Res.string.mod_slider_travel_title), text(Res.string.mod_slider_travel_help), "ход длина процент travel percent")
                    }
                    group(text(Res.string.mod_slider_preview_title)) {
                        entry(text(Res.string.mod_slider_preview_title), text(Res.string.mod_slider_preview_hint), "попробовать пример preview demo")
                    }
                }
            }
            tab(ModerationTab.MACROS) {
                group(text(Res.string.mod_macros)) {
                    entry(text(Res.string.mod_macros), text(Res.string.mod_macros_desc), "макросы быстрая панель macros quick bar")
                    entry(text(Res.string.mod_create_macro), keywords = "новый макрос new macro")
                }
            }
        }
        section(SettingsSection.AI) {
            group(text(Res.string.ai_config_title)) {
                entry(text(Res.string.ai_enable), text(Res.string.ai_enable_desc), "ии ассистент нейросеть ollama lm studio gpt llm модель ai assistant")
            }
            group(text(Res.string.ai_auto_mod_title)) {
                entry(text(Res.string.ai_monitor_title), text(Res.string.ai_monitor_desc), "ии автомод сканер мониторинг ai automod")
                entry(text(Res.string.ai_sensitivity), keywords = "чувствительность sensitivity")
                entry(text(Res.string.ai_active_hours), keywords = "часы расписание hours schedule")
            }
        }
        section(SettingsSection.ACCOUNT) {
            group(text(Res.string.accounts_title)) {
                entry(text(Res.string.accounts_title), keywords = "аккаунты несколько аккаунтов переключить основной accounts switch primary multi")
                entry(text(Res.string.accounts_add), keywords = "добавить аккаунт войти login add account")
                entry(text(Res.string.accounts_proxy_title), text(Res.string.accounts_proxy_desc), "прокси socks http proxy")
            }
            group(text(Res.string.settings_account)) {
                entry(text(Res.string.settings_logout), text(Res.string.settings_logout_desc), "выйти выход logout sign out")
                entry(text(Res.string.settings_clear_cache), text(Res.string.settings_clear_cache_desc), "кеш данные очистить cache clear")
            }
            group(text(Res.string.ext_rights_title)) {
                entry(text(Res.string.ext_rights_title), keywords = "токен веб клиента first party gql integrity расширенные права token")
            }
            group(text(Res.string.blocked_users)) {
                entry(text(Res.string.blocked_users), text(Res.string.blocked_users_desc), "заблокированные блок лист игнор blocked ignore unblock")
                entry(text(Res.string.blocked_users_show_messages), keywords = "сообщения заблокированных blocked messages")
            }
            group(text(Res.string.streamer_mode_title)) {
                entry(text(Res.string.streamer_mode_hide_data), text(Res.string.streamer_mode_obs), "стример режим obs приватность скрывать streamer mode privacy")
                entry(text(Res.string.streamer_mask_tokens), keywords = "токены маска tokens")
                entry(text(Res.string.streamer_hide_mod), keywords = "мод действия mod actions")
                entry(text(Res.string.streamer_mask_channels), keywords = "каналы названия channels")
                entry(text(Res.string.streamer_hide_thumbs), keywords = "превью thumbnails")
                entry(text(Res.string.streamer_suppress_notif), keywords = "уведомления notifications")
            }
        }
        section(SettingsSection.ABOUT) {
            group(text(Res.string.settings_backup_title)) {
                entry(text(Res.string.settings_backup_title), text(Res.string.settings_backup_desc), "бекап экспорт импорт резервная копия backup export import restore")
            }
            group(text(Res.string.settings_app_info)) {
                entry(text(Res.string.settings_version), keywords = "версия о программе version about changelog")
                entry(text(Res.string.settings_telegram), keywords = "телеграм поддержка telegram support")
            }
        }
    }
    return builder.entries
}

private class CatalogBuilder(private val context: SettingsCatalogContext) {
    val entries = mutableListOf<CatalogItem>()
    private var section: SettingsSection = SettingsSection.APPEARANCE
    private var tab: ModerationTab? = null
    private var group: String? = null

    suspend fun text(resource: StringResource, vararg args: Any): String = getString(resource, *args)

    suspend fun section(target: SettingsSection, block: suspend CatalogBuilder.() -> Unit) {
        if (!target.isAvailable(context.input, context.desktop)) return
        section = target
        tab = null
        block()
    }

    suspend fun tab(target: ModerationTab, block: suspend CatalogBuilder.() -> Unit) {
        tab = target
        block()
        tab = null
    }

    suspend fun group(title: String?, block: suspend CatalogBuilder.() -> Unit) {
        group = title
        block()
        group = null
    }

    suspend fun entry(title: String, description: String? = null, keywords: String = "") {
        entries += CatalogItem(
            section = section,
            tab = tab,
            tabTitle = tab?.let { getString(it.labelRes()) },
            groupTitle = group,
            text = CatalogText(
                title = title,
                group = group?.takeIf { it != title },
                description = description?.takeIf { it.isNotBlank() },
                sectionLabel = getString(section.labelRes())
            ),
            keywords = keywords
        )
    }
}
