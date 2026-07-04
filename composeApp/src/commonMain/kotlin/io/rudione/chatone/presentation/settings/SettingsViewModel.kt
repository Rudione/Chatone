package io.rudione.chatone.presentation.settings

import androidx.lifecycle.viewModelScope
import com.russhwolf.settings.Settings
import io.github.aakira.napier.Napier
import io.rudione.chatone.base.BaseViewModel
import io.rudione.chatone.base.UIEffect
import io.rudione.chatone.base.UiEvent
import io.rudione.chatone.base.UiState
import io.rudione.chatone.data.remote.TwitchApiClient
import io.rudione.chatone.data.repository.AuthRepository
import io.rudione.chatone.domain.model.ChatCommand
import io.rudione.chatone.domain.model.ChatoneColorTokens
import io.rudione.chatone.domain.model.HighlightRule
import io.rudione.chatone.domain.model.ImageUploaderConfig
import io.rudione.chatone.domain.model.Macro
import io.rudione.chatone.domain.model.ModActionButton
import io.rudione.chatone.domain.usecase.GetFirstValidAccountUseCase
import io.rudione.chatone.presentation.theme.CustomThemeConfig
import io.rudione.chatone.presentation.theme.WallpaperDisplayConfig
import io.rudione.chatone.presentation.theme.WallpaperState
import io.rudione.chatone.util.AppDataCleaner
import io.rudione.chatone.util.AppRestarter
import io.rudione.chatone.util.WallpaperLoader
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

enum class PauseHotkeyMode { TOGGLE, HOLD }
enum class InlineImageMode { ON, OFF, BLUR }
enum class TitleBarMode { DARK, LIGHT, ADAPTIVE, SYSTEM }

data class SettingsState(
    val darkTheme: Boolean = true,
    val timestampFormat: TimestampFormat = TimestampFormat.H24,
    val showDeletedMessages: Boolean = true,
    val showViewerJoinLeave: Boolean = false,
    val imageUploader: ImageUploaderConfig = ImageUploaderConfig(),
    val scrollbackLimit: Int = 500,
    val emoteSize: EmoteSize = EmoteSize.MEDIUM,
    val showBadges: Boolean = true,
    val fontSize: FontSize = FontSize.MEDIUM,
    val defaultTimeoutDuration: Int = 600,
    val confirmModActions: Boolean = false,
    val channelNavigation: ChannelNavigation = ChannelNavigation.TAB_BAR,
    val highlightRules: List<HighlightRule> = listOf(
        HighlightRule.USERNAME_RULE.copy(pattern = ""),
        HighlightRule.WHISPER_RULE,
        HighlightRule.SUBSCRIPTION_RULE,
        HighlightRule.FIRST_MESSAGE_RULE,
        HighlightRule.SEARCH_MATCH_RULE,
        HighlightRule.CHANNEL_POINTS_RULE
    ),
    val mentionSoundEnabled: Boolean = true,
    val mentionSoundVolume: Float = 0.8f,
    val customMentionSoundPath: String = "",
    val alwaysOnTop: Boolean = false,
    val uiScale: Float = 1.0f,
    val pauseOnHover: Boolean = false,
    val pauseHotkey: String = "",
    val pauseHotkeyMode: PauseHotkeyMode = PauseHotkeyMode.TOGGLE,
    val showInlineImages: InlineImageMode = InlineImageMode.ON,
    val inlineImageMaxHeight: Int = 200,
    val chatScrollbarWidth: Int = 16,
    val automations: List<io.rudione.chatone.domain.model.ChatAutomation> = emptyList(),
    val autoClaimPoints: Boolean = false,
    val mutedPhrases: List<String> = emptyList(),
    val wallpaperPath: String = "",
    val wallpaperBlur: Float = 12f,
    val wallpaperDisplayConfig: WallpaperDisplayConfig = WallpaperDisplayConfig(),
    val activeCustomTheme: CustomThemeConfig? = null,
    val closeEmotePickerOnMouseLeave: Boolean = false,
    val customModButtons: List<ModActionButton> = emptyList(),
    val allModButtons: List<ModActionButton> = listOf(
        ModActionButton.DEFAULT_DELETE,
        ModActionButton.DEFAULT_TIMEOUT,
        ModActionButton.DEFAULT_BAN
    ),
    val modButtonsVersion: Int = 0,
    val macros: List<Macro> = emptyList(),
    val showChatHeader: Boolean = true,
    val smoothChatEnabled: Boolean = false,
    val alternateRowBackground: Boolean = false,
    val showDefaultDeleteButton: Boolean = true,
    val showDefaultTimeoutButton: Boolean = true,
    val showDefaultBanButton: Boolean = true,
    val disableScrollOnAlt: Boolean = true,
    val linkOpenMode: LinkOpenMode = LinkOpenMode.DEFAULT,
    val customThemesJson: String = "",
    val accentColorIndex: Int = 0,
    val customThemes: List<CustomThemeConfig> = emptyList(),
    val activeCustomThemeId: String? = null,
    val showThemeCreator: Boolean = false,
    val themeCreatorSeedColor: Int? = null,
    val language: String = "en",
    val fontFamilyName: String = "Default",
    val fontStyleItalic: Boolean = false,
    val fontUnderline: Boolean = false,
    val fontStrikethrough: Boolean = false,
    val customFontPaths: List<String> = emptyList(),
    val messageSpacing: MessageSpacing = MessageSpacing.LOW,
    val titleBarMode: TitleBarMode = TitleBarMode.DARK,
    val showBlockedMode: Int = 0,
    val blockedUsernames: List<String> = emptyList(),
    val isLoadingBlockedUsers: Boolean = false,
    val blockedLoadError: String? = null,
    val hideChatInputPlaceholder: Boolean = false,
    val hideEmojiButton: Boolean = false,
    val chatInputEventGlow: Boolean = true,
    val showRepeatedMessageCounter: Boolean = false,
    val repeatedMessageWindow: Int = 30,
    val savedTimeoutReason: String = "",
    val savedBanReason: String = "",
    val chatCommands: List<ChatCommand> = emptyList(),
    val colorTokens: ChatoneColorTokens = ChatoneColorTokens(),
    val translationTargetLang: String = "en",
    val mentionTabsEnabled: Boolean = false,
    val navigationHidden: Boolean = false,
    val hotkeys: Map<String, String> = io.rudione.chatone.util.defaultHotkeys(),
) : UiState {
    enum class TimestampFormat { H12, H24, OFF }
    enum class LinkOpenMode { DEFAULT, INCOGNITO }
    enum class EmoteSize { SMALL, MEDIUM, LARGE }
    enum class FontSize { SMALL, MEDIUM, LARGE }
    enum class ChannelNavigation { TAB_BAR, MINI_RAIL, BOTH }
    enum class MessageSpacing { NONE, LOW, MEDIUM, HIGH }

    val pinnedMacros: List<Macro>
        get() = Macro.pinnedFrom(macros)
}

sealed class SettingsEvent : UiEvent {
    data class OnDarkThemeChanged(val enabled: Boolean) : SettingsEvent()
    data class OnShowChatHeaderChanged(val show: Boolean) : SettingsEvent()
    data class OnOpenThemeCreator(val seedColor: Int? = null) : SettingsEvent()
    data object OnCloseThemeCreator : SettingsEvent()
    data class OnCustomThemeApplied(val theme: CustomThemeConfig?) : SettingsEvent()
    data class OnTimestampFormatChanged(val format: SettingsState.TimestampFormat) : SettingsEvent()
    data class OnShowDeletedChanged(val show: Boolean) : SettingsEvent()
    data class OnShowViewerJoinLeaveChanged(val show: Boolean) : SettingsEvent()
    data class OnImageUploaderChanged(val config: ImageUploaderConfig) : SettingsEvent()
    data class OnScrollbackLimitChanged(val limit: Int) : SettingsEvent()
    data class OnEmoteSizeChanged(val size: SettingsState.EmoteSize) : SettingsEvent()
    object OnLogoutClicked : SettingsEvent()
    object OnClearCacheClicked : SettingsEvent()
    data class OnShowBadgesChanged(val show: Boolean) : SettingsEvent()
    data class OnFontSizeChanged(val size: SettingsState.FontSize) : SettingsEvent()
    data class OnUiScaleChanged(val scale: Float) : SettingsEvent()
    data class OnLanguageChanged(val code: String) : SettingsEvent()

    data class OnFontFamilyChanged(val name: String) : SettingsEvent()
    data class OnFontItalicChanged(val italic: Boolean) : SettingsEvent()
    data class OnFontUnderlineChanged(val underline: Boolean) : SettingsEvent()
    data class OnFontStrikethroughChanged(val strikethrough: Boolean) : SettingsEvent()
    data class OnAddCustomFontPath(val path: String) : SettingsEvent()
    data class OnRemoveCustomFontPath(val path: String) : SettingsEvent()
    data class OnMessageSpacingChanged(val spacing: SettingsState.MessageSpacing) : SettingsEvent()
    data class OnTitleBarModeChanged(val mode: TitleBarMode) : SettingsEvent()
    data class OnShowBlockedModeChanged(val mode: Int) : SettingsEvent()
    object OnLoadBlockedUsers : SettingsEvent()
    data class OnUnblockUserFromSettings(val username: String, val userId: String) : SettingsEvent()
    data class BlockedUsersLoaded(val usernames: List<String>) : SettingsEvent()
    data class BlockedUsersLoadFailed(val error: String) : SettingsEvent()
    data class OnDefaultTimeoutChanged(val duration: Int) : SettingsEvent()
    data class OnConfirmModActionsChanged(val confirm: Boolean) : SettingsEvent()
    data class OnChannelNavigationChanged(val navigation: SettingsState.ChannelNavigation) :
        SettingsEvent()

    data class OnHighlightRuleToggled(val ruleId: String, val enabled: Boolean) : SettingsEvent()
    data class OnHighlightRuleSoundToggled(val ruleId: String, val playSound: Boolean) :
        SettingsEvent()
    data class OnHighlightRuleSubstringToggled(val ruleId: String, val matchSubstring: Boolean) :
        SettingsEvent()
    data class OnHideChatPlaceholderChanged(val hide: Boolean) : SettingsEvent()
    data class OnHideEmojiButtonChanged(val hide: Boolean) : SettingsEvent()
    data class OnChatInputGlowChanged(val enabled: Boolean) : SettingsEvent()
    data class OnShowRepeatedCounterChanged(val show: Boolean) : SettingsEvent()
    data class OnRepeatedWindowChanged(val seconds: Int) : SettingsEvent()
    data class OnSavedTimeoutReasonChanged(val text: String) : SettingsEvent()
    data class OnSavedBanReasonChanged(val text: String) : SettingsEvent()

    data class OnHighlightRuleColorChanged(val ruleId: String, val color: Long) : SettingsEvent()
    data class OnAddHighlightRule(val pattern: String) : SettingsEvent()
    data class OnRemoveHighlightRule(val ruleId: String) : SettingsEvent()
    data class OnMentionSoundChanged(val enabled: Boolean) : SettingsEvent()
    data class OnMentionSoundVolumeChanged(val volume: Float) : SettingsEvent()
    data class OnCustomMentionSoundPathChanged(val path: String) : SettingsEvent()
    data class OnAlwaysOnTopChanged(val enabled: Boolean) : SettingsEvent()
    data class OnPauseOnHoverChanged(val enabled: Boolean) : SettingsEvent()
    data class OnPauseHotkeyChanged(val hotkey: String) : SettingsEvent()
    data class OnPauseHotkeyModeChanged(val mode: PauseHotkeyMode) : SettingsEvent()
    data class OnShowInlineImagesChanged(val mode: InlineImageMode) : SettingsEvent()
    data class OnInlineImageMaxHeightChanged(val height: Int) : SettingsEvent()
    data class OnChatScrollbarWidthChanged(val width: Int) : SettingsEvent()
    data class OnAddAutomation(val automation: io.rudione.chatone.domain.model.ChatAutomation) : SettingsEvent()
    data class OnRemoveAutomation(val id: String) : SettingsEvent()
    data class OnToggleAutomation(val id: String, val enabled: Boolean) : SettingsEvent()
    data class OnAutoClaimPointsChanged(val enabled: Boolean) : SettingsEvent()
    data class OnAddMutedPhrase(val phrase: String) : SettingsEvent()
    data class OnRemoveMutedPhrase(val phrase: String) : SettingsEvent()
    data class OnWallpaperPathChanged(val path: String) : SettingsEvent()
    data class OnWallpaperBlurChanged(val blur: Float) : SettingsEvent()
    data class OnWallpaperDisplayConfigChanged(val config: WallpaperDisplayConfig) : SettingsEvent()
    data class OnCloseEmotePickerOnMouseLeaveChanged(val enabled: Boolean) : SettingsEvent()
    data class OnActiveCustomThemeIdChanged(val themeId: String?) : SettingsEvent()
    data class OnCustomThemesJsonChanged(val json: String) : SettingsEvent()
    data class OnAddModButton(val durationSeconds: Int, val label: String) : SettingsEvent()
    data class OnRemoveModButton(val id: String) : SettingsEvent()
    data class OnUpdateModButton(val button: ModActionButton) : SettingsEvent()
    data class OnReorderAllModButtons(val newOrder: List<ModActionButton>) : SettingsEvent()
    data class OnSetModButtonEnabled(val id: String, val enabled: Boolean) : SettingsEvent()
    data class OnShowDefaultDeleteChanged(val show: Boolean) : SettingsEvent()
    data class OnShowDefaultTimeoutChanged(val show: Boolean) : SettingsEvent()
    data class OnShowDefaultBanChanged(val show: Boolean) : SettingsEvent()
    data class OnReorderModButtons(val from: Int, val to: Int) : SettingsEvent()


    data class OnAddMacro(val name: String, val icon: String) : SettingsEvent()
    data class OnRemoveMacro(val id: String) : SettingsEvent()
    data class OnUpdateMacro(val macro: Macro) : SettingsEvent()
    data class OnPinMacro(val macroId: String, val slotIndex: Int) : SettingsEvent()

    data class OnSmoothChatEnabledChanged(val enabled: Boolean) : SettingsEvent()
    data class OnAlternateRowBackgroundChanged(val enabled: Boolean) : SettingsEvent()
    data class OnDisableScrollOnAltChanged(val enabled: Boolean) : SettingsEvent()
    data class OnLinkOpenModeChanged(val mode: SettingsState.LinkOpenMode) : SettingsEvent()
    data class OnAccentColorChanged(val index: Int) : SettingsEvent()
    data class OnSaveCustomTheme(val theme: CustomThemeConfig) : SettingsEvent()
    data class OnDeleteCustomTheme(val themeId: String) : SettingsEvent()
    data class OnApplyCustomTheme(val themeId: String?) : SettingsEvent()
    data class OnImportSettingsText(val text: String) : SettingsEvent()

    data class OnAddChatCommand(val command: ChatCommand) : SettingsEvent()
    data class OnUpdateChatCommand(val command: ChatCommand) : SettingsEvent()
    data class OnRemoveChatCommand(val id: String) : SettingsEvent()

    data class OnColorTokensChanged(val tokens: ChatoneColorTokens) : SettingsEvent()
    data class OnTranslationLangChanged(val code: String) : SettingsEvent()
    data class OnMentionTabsChanged(val enabled: Boolean) : SettingsEvent()
    data class OnNavigationHiddenChanged(val hidden: Boolean) : SettingsEvent()
    data class OnHotkeyChanged(val actionId: String, val combo: String) : SettingsEvent()
}

sealed class SettingsEffect : UIEffect {
    data class WallpaperChanged(val wallpaper: WallpaperState) : SettingsEffect()
    object NavigateToAuth : SettingsEffect()
}

class SettingsViewModel(
    private val wallpaperLoader: WallpaperLoader,
    private val authRepository: AuthRepository,
    private val getFirstValidAccountUseCase: GetFirstValidAccountUseCase,
    private val twitchApiClient: TwitchApiClient? = null
) : BaseViewModel<SettingsState, SettingsEvent, SettingsEffect>(loadInitialState()) {
    companion object {
        val settings: Settings = Settings()
        private const val KEY_DARK_THEME = "dark_theme"
        private const val KEY_TIMESTAMP_FORMAT = "timestamp_format"
        private const val KEY_SHOW_DELETED = "show_deleted"
        private const val KEY_SHOW_VIEWER_JOIN_LEAVE = "show_viewer_join_leave"
        private const val KEY_IMAGE_UPLOADER = "image_uploader_config"
        private const val KEY_SCROLLBACK_LIMIT = "scrollback_limit"
        private const val KEY_EMOTE_SIZE = "emote_size"
        private const val KEY_SHOW_BADGES = "show_badges"
        private const val KEY_FONT_SIZE = "font_size"
        private const val KEY_FONT_FAMILY = "font_family_name"
        private const val KEY_FONT_ITALIC = "font_italic"
        private const val KEY_FONT_UNDERLINE = "font_underline"
        private const val KEY_FONT_STRIKETHROUGH = "font_strikethrough"
        private const val KEY_CUSTOM_FONT_PATHS = "custom_font_paths"
        private const val KEY_MESSAGE_SPACING = "message_spacing"
        private const val KEY_DEFAULT_TIMEOUT = "default_timeout"
        private const val KEY_CONFIRM_MOD = "confirm_mod_actions"
        private const val KEY_CHANNEL_NAV = "channel_navigation"
        private const val KEY_HIGHLIGHT_RULES = "highlight_rules"
        private const val KEY_MENTION_SOUND = "mention_sound"
        private const val KEY_MENTION_VOLUME = "mention_volume"
        private const val KEY_CUSTOM_SOUND_PATH = "custom_sound_path"
        private const val KEY_ALWAYS_ON_TOP = "always_on_top"
        private const val KEY_UI_SCALE = "ui_scale"
        private const val KEY_LANGUAGE = "language"
        private const val KEY_PAUSE_ON_HOVER = "pause_on_hover"
        private const val KEY_PAUSE_HOTKEY = "pause_hotkey"
        private const val KEY_PAUSE_HOTKEY_MODE = "pause_hotkey_mode"
        private const val KEY_SHOW_INLINE_IMAGES = "show_inline_images"
        private const val KEY_INLINE_IMAGE_MAX_HEIGHT = "inline_image_max_height"
        private const val KEY_CHAT_SCROLLBAR_WIDTH = "chat_scrollbar_width"
        private const val KEY_AUTOMATIONS = "chat_automations"
        private const val KEY_AUTO_CLAIM_POINTS = "auto_claim_points"
        private const val KEY_MUTED_PHRASES = "muted_phrases"

        private const val KEY_CUSTOM_THEMES = "custom_themes_json"
        private const val KEY_ACTIVE_THEME_ID = "active_custom_theme_id"
        private const val KEY_WALLPAPER_PATH = "wallpaper_path"
        private const val KEY_WALLPAPER_BLUR = "wallpaper_blur"
        private const val KEY_EMOTE_PICKER_MOUSE_LEAVE = "emote_picker_mouse_leave"
        private const val KEY_CUSTOM_MOD_BUTTONS = "custom_mod_buttons"
        private const val KEY_ALL_MOD_BUTTONS = "all_mod_buttons_v2"
        private const val KEY_MACROS = "macros"
        private const val KEY_CHAT_COMMANDS = "chat_commands"
        private const val KEY_SHOW_CHAT_HEADER = "show_chat_header"
        private const val KEY_SMOOTH_CHAT = "smooth_chat_enabled"
        private const val KEY_ALTERNATE_ROW_BG = "alternate_row_bg"
        private const val KEY_SHOW_DEFAULT_DELETE = "show_default_delete"
        private const val KEY_SHOW_DEFAULT_TIMEOUT = "show_default_timeout"
        private const val KEY_SHOW_DEFAULT_BAN = "show_default_ban"
        private const val KEY_DISABLE_SCROLL_ON_ALT = "disable_scroll_on_alt"
        private const val KEY_LINK_OPEN_MODE = "link_open_mode"
        private const val KEY_ACCENT_COLOR_INDEX = "accent_color_index"
        private const val KEY_TITLE_BAR_MODE = "title_bar_mode"
        private const val KEY_SHOW_BLOCKED_MODE = "show_blocked_mode"
        private const val KEY_HIDE_CHAT_PLACEHOLDER = "hide_chat_input_placeholder"
        private const val KEY_HIDE_EMOJI_BUTTON = "hide_emoji_button"
        private const val KEY_CHAT_INPUT_GLOW = "chat_input_event_glow"
        private const val KEY_SHOW_REPEAT_COUNTER = "show_repeated_message_counter"
        private const val KEY_REPEAT_WINDOW = "repeated_message_window"
        private const val KEY_SAVED_TIMEOUT_REASON = "saved_timeout_reason"
        private const val KEY_SAVED_BAN_REASON = "saved_ban_reason"
        private const val KEY_COLOR_TOKENS = "color_tokens"
        private const val KEY_TRANSLATION_LANG = "translation_target_lang"
        private const val KEY_MENTION_TABS = "mention_tabs_enabled"
        private const val KEY_NAVIGATION_HIDDEN = "navigation_hidden"
        private const val KEY_HOTKEYS = "hotkeys_map"
        private val json = Json { ignoreUnknownKeys = true }
        private val _effects = MutableSharedFlow<SettingsEffect>()
        val effects = _effects.asSharedFlow()

        private val _changeBroadcast = MutableSharedFlow<Long>(extraBufferCapacity = 16)
        val changeBroadcast = _changeBroadcast.asSharedFlow()
        private fun emitChange() {
            _changeBroadcast.tryEmit(Clock.System.now().toEpochMilliseconds())
        }

        /**
         * Authoritative, conflated mod-button list shared across every SettingsViewModel
         * instance (chat windows + the detached settings window). Unlike the timestamp
         * broadcast above, this carries the actual list, so it can never be dropped or lose
         * the disk write/read race — a drag-reorder reaches the chat immediately instead of
         * only after the next unrelated change (the old "toggle a button to apply" workaround).
         */
        private val _modButtonsLive = MutableStateFlow<List<ModActionButton>?>(null)

        /**
         * Public, process-wide live view of the mod-button list. The chat UI collects this
         * DIRECTLY (not through any per-instance SettingsState), so a reorder/add/remove/toggle
         * done in the detached settings window shows up in every chat window immediately,
         * regardless of which SettingsViewModel instance koinViewModel() handed out.
         * Null until the first mod-button write this session — callers fall back to their state.
         */
        val modButtonsLive: StateFlow<List<ModActionButton>?> = _modButtonsLive.asStateFlow()

        private val _macrosLive = MutableStateFlow<List<Macro>?>(null)
        val macrosLive: StateFlow<List<Macro>?> = _macrosLive.asStateFlow()

        /**
         * Notify every SettingsViewModel instance that the settings store was rewritten
         * outside the normal event flow (per-account profile swap, import). A negative
         * stamp tells collectors to do a FULL reload including macros/commands/rules.
         */
        fun notifyExternalChange() {
            _changeBroadcast.tryEmit(-Clock.System.now().toEpochMilliseconds())
        }

        fun loadInitialState(): SettingsState {
            val rules = try {
                val j = settings.getStringOrNull(KEY_HIGHLIGHT_RULES)
                if (j != null) json.decodeFromString<List<HighlightRule>>(j) else null
            } catch (_: Exception) {
                null
            }
            val modButtons = try {
                val j = settings.getStringOrNull(KEY_CUSTOM_MOD_BUTTONS)
                if (j != null) json.decodeFromString<List<ModActionButton>>(j) else emptyList()
            } catch (_: Exception) {
                emptyList()
            }
            val allModBtns = try {
                val j = settings.getStringOrNull(KEY_ALL_MOD_BUTTONS)
                if (j != null) json.decodeFromString<List<ModActionButton>>(j)
                else null
            } catch (_: Exception) {
                null
            }
            val macros = try {
                val j = settings.getStringOrNull(KEY_MACROS)
                if (j != null) json.decodeFromString<List<Macro>>(j) else emptyList()
            } catch (_: Exception) {
                emptyList()
            }

            val automations = try {
                val j = settings.getStringOrNull(KEY_AUTOMATIONS)
                if (j != null) json.decodeFromString<List<io.rudione.chatone.domain.model.ChatAutomation>>(j)
                else emptyList()
            } catch (_: Exception) {
                emptyList()
            }
            val mutedPhrases = try {
                val j = settings.getStringOrNull(KEY_MUTED_PHRASES)
                if (j != null) json.decodeFromString<List<String>>(j) else emptyList()
            } catch (_: Exception) {
                emptyList()
            }

            val chatCommands = try {
                val j = settings.getStringOrNull(KEY_CHAT_COMMANDS)
                if (j != null) json.decodeFromString<List<ChatCommand>>(j) else emptyList()
            } catch (_: Exception) {
                emptyList()
            }

            val customThemes = try {
                val j = settings.getStringOrNull(KEY_CUSTOM_THEMES)
                if (j != null) json.decodeFromString<List<CustomThemeConfig>>(j) else emptyList()
            } catch (_: Exception) {
                emptyList()
            }
            val colorTokens = try {
                val j = settings.getStringOrNull(KEY_COLOR_TOKENS)
                if (j != null) json.decodeFromString<ChatoneColorTokens>(j) else ChatoneColorTokens()
            } catch (_: Exception) {
                ChatoneColorTokens()
            }
            val activeThemeId =
                settings.getStringOrNull(KEY_ACTIVE_THEME_ID)?.takeIf { it.isNotBlank() }
            val activeCustomTheme = activeThemeId?.let { id -> customThemes.find { it.id == id } }
            val wallpaperDisplayConfig = WallpaperDisplayConfig.fromJson(
                settings.getStringOrNull(WallpaperDisplayConfig.SETTINGS_KEY)
            )

            return SettingsState(
                darkTheme = settings.getBoolean(KEY_DARK_THEME, true),
                timestampFormat = SettingsState.TimestampFormat.entries.getOrNull(
                    settings.getInt(
                        KEY_TIMESTAMP_FORMAT,
                        1
                    )
                ) ?: SettingsState.TimestampFormat.H24,
                showDeletedMessages = settings.getBoolean(KEY_SHOW_DELETED, true),
                showViewerJoinLeave = settings.getBoolean(KEY_SHOW_VIEWER_JOIN_LEAVE, false),
                imageUploader = try {
                    settings.getStringOrNull(KEY_IMAGE_UPLOADER)
                        ?.let { json.decodeFromString<ImageUploaderConfig>(it) }
                        ?: ImageUploaderConfig()
                } catch (_: Exception) {
                    ImageUploaderConfig()
                },
                scrollbackLimit = settings.getInt(KEY_SCROLLBACK_LIMIT, 500),
                emoteSize = SettingsState.EmoteSize.entries.getOrNull(
                    settings.getInt(
                        KEY_EMOTE_SIZE,
                        1
                    )
                ) ?: SettingsState.EmoteSize.MEDIUM,
                showBadges = settings.getBoolean(KEY_SHOW_BADGES, true),
                fontSize = SettingsState.FontSize.entries.getOrNull(
                    settings.getInt(
                        KEY_FONT_SIZE,
                        1
                    )
                ) ?: SettingsState.FontSize.MEDIUM,
                defaultTimeoutDuration = settings.getInt(KEY_DEFAULT_TIMEOUT, 600),
                confirmModActions = settings.getBoolean(KEY_CONFIRM_MOD, false),
                channelNavigation = SettingsState.ChannelNavigation.entries.getOrNull(
                    settings.getInt(
                        KEY_CHANNEL_NAV,
                        0
                    )
                ) ?: SettingsState.ChannelNavigation.TAB_BAR,
                highlightRules = run {
                    val defaults = SettingsState().highlightRules
                    val loaded = (rules ?: defaults).filter { it.id != "mention_accent" }
                    val loadedIds = loaded.map { it.id }.toSet()
                    loaded + defaults.filter { it.id !in loadedIds }
                },
                mentionSoundEnabled = settings.getBoolean(KEY_MENTION_SOUND, true),
                mentionSoundVolume = settings.getFloat(KEY_MENTION_VOLUME, 0.8f),
                customMentionSoundPath = settings.getStringOrNull(KEY_CUSTOM_SOUND_PATH) ?: "",
                alwaysOnTop = settings.getBoolean(KEY_ALWAYS_ON_TOP, false),
                uiScale = settings.getFloat(KEY_UI_SCALE, 1.0f),
                language = settings.getString(KEY_LANGUAGE, "en"),
                pauseOnHover = settings.getBoolean(KEY_PAUSE_ON_HOVER, false),
                pauseHotkey = settings.getStringOrNull(KEY_PAUSE_HOTKEY) ?: "",
                pauseHotkeyMode = PauseHotkeyMode.entries.getOrNull(
                    settings.getInt(KEY_PAUSE_HOTKEY_MODE, 0)
                ) ?: PauseHotkeyMode.TOGGLE,
                showInlineImages = InlineImageMode.entries.getOrNull(
                    settings.getInt(KEY_SHOW_INLINE_IMAGES, 0)
                ) ?: InlineImageMode.ON,
                inlineImageMaxHeight = settings.getInt(KEY_INLINE_IMAGE_MAX_HEIGHT, 200),
                chatScrollbarWidth = settings.getInt(KEY_CHAT_SCROLLBAR_WIDTH, 16),
                wallpaperPath = settings.getStringOrNull(KEY_WALLPAPER_PATH) ?: "",
                wallpaperBlur = settings.getFloat(KEY_WALLPAPER_BLUR, 12f),
                wallpaperDisplayConfig = wallpaperDisplayConfig,
                closeEmotePickerOnMouseLeave = settings.getBoolean(
                    KEY_EMOTE_PICKER_MOUSE_LEAVE,
                    false
                ),
                customModButtons = modButtons,
                macros = macros,
                automations = automations,
                mutedPhrases = mutedPhrases,
                autoClaimPoints = settings.getBoolean(KEY_AUTO_CLAIM_POINTS, false),
                showChatHeader = settings.getBoolean(KEY_SHOW_CHAT_HEADER, true),
                smoothChatEnabled = settings.getBoolean(KEY_SMOOTH_CHAT, false),
                alternateRowBackground = settings.getBoolean(KEY_ALTERNATE_ROW_BG, false),
                showDefaultDeleteButton = settings.getBoolean(KEY_SHOW_DEFAULT_DELETE, true),
                showDefaultTimeoutButton = settings.getBoolean(KEY_SHOW_DEFAULT_TIMEOUT, true),
                showDefaultBanButton = settings.getBoolean(KEY_SHOW_DEFAULT_BAN, true),
                disableScrollOnAlt = settings.getBoolean(KEY_DISABLE_SCROLL_ON_ALT, true),
                linkOpenMode = try {
                    SettingsState.LinkOpenMode.valueOf(
                        settings.getString(
                            KEY_LINK_OPEN_MODE,
                            SettingsState.LinkOpenMode.DEFAULT.name
                        )
                    )
                } catch (_: Exception) {
                    SettingsState.LinkOpenMode.DEFAULT
                },
                accentColorIndex = settings.getInt(KEY_ACCENT_COLOR_INDEX, 0),
                allModButtons = allModBtns ?: run {
                    val defaults = ModActionButton.defaultOrderedList()
                    val combined = (defaults + modButtons).distinctBy { it.id }
                    combined.mapIndexed { i, b -> b.copy(sortOrder = i) }
                },
                customThemes = customThemes,
                activeCustomThemeId = activeThemeId,
                activeCustomTheme = activeCustomTheme,
                fontFamilyName = settings.getString(KEY_FONT_FAMILY, "Default"),
                fontStyleItalic = settings.getBoolean(KEY_FONT_ITALIC, false),
                fontUnderline = settings.getBoolean(KEY_FONT_UNDERLINE, false),
                fontStrikethrough = settings.getBoolean(KEY_FONT_STRIKETHROUGH, false),
                customFontPaths = try {
                    val j = settings.getStringOrNull(KEY_CUSTOM_FONT_PATHS)
                    if (j != null) json.decodeFromString<List<String>>(j) else emptyList()
                } catch (_: Exception) {
                    emptyList()
                },
                messageSpacing = SettingsState.MessageSpacing.entries.getOrNull(
                    settings.getInt(KEY_MESSAGE_SPACING, SettingsState.MessageSpacing.LOW.ordinal)
                ) ?: SettingsState.MessageSpacing.LOW,
                titleBarMode = try {
                    TitleBarMode.valueOf(settings.getString(KEY_TITLE_BAR_MODE, TitleBarMode.DARK.name))
                } catch (_: Exception) { TitleBarMode.DARK },
                showBlockedMode = settings.getInt(KEY_SHOW_BLOCKED_MODE, 0),
                hideChatInputPlaceholder = settings.getBoolean(KEY_HIDE_CHAT_PLACEHOLDER, false),
                hideEmojiButton = settings.getBoolean(KEY_HIDE_EMOJI_BUTTON, false),
                chatInputEventGlow = settings.getBoolean(KEY_CHAT_INPUT_GLOW, true),
                showRepeatedMessageCounter = settings.getBoolean(KEY_SHOW_REPEAT_COUNTER, false),
                repeatedMessageWindow = settings.getInt(KEY_REPEAT_WINDOW, 30),
                savedTimeoutReason = settings.getString(KEY_SAVED_TIMEOUT_REASON, ""),
                savedBanReason = settings.getString(KEY_SAVED_BAN_REASON, ""),
                chatCommands = chatCommands,
                colorTokens = colorTokens,
                translationTargetLang = settings.getString(KEY_TRANSLATION_LANG, "en"),
                mentionTabsEnabled = settings.getBoolean(KEY_MENTION_TABS, false),
                navigationHidden = settings.getBoolean(KEY_NAVIGATION_HIDDEN, false),
                hotkeys = try {
                    val j = settings.getStringOrNull(KEY_HOTKEYS)
                    val stored = if (j != null) json.decodeFromString<Map<String, String>>(j) else emptyMap()
                    io.rudione.chatone.util.defaultHotkeys() + stored
                } catch (_: Exception) {
                    io.rudione.chatone.util.defaultHotkeys()
                },
            )
        }
    }

    init {
        subscribeToEvents()
        viewModelScope.launch {
            _modButtonsLive.collect { live ->
                if (live == null) return@collect
                super.update { current ->
                    if (live == current.allModButtons) current
                    else current.copy(
                        allModButtons = live,
                        customModButtons = live.filter { !it.isDefault },
                        modButtonsVersion = current.modButtonsVersion + 1
                    )
                }
            }
        }
        viewModelScope.launch {
            kotlinx.coroutines.flow.merge(
                changeBroadcast.filter { it >= 0 }.debounce(50),
                changeBroadcast.filter { it < 0 }
            ).collect { stamp ->
                val fullReload = stamp < 0
                super.update { current ->
                    val fresh = loadInitialState()
                    val modButtonsChanged = fresh.allModButtons != current.allModButtons ||
                            fresh.showDefaultDeleteButton != current.showDefaultDeleteButton ||
                            fresh.showDefaultTimeoutButton != current.showDefaultTimeoutButton ||
                            fresh.showDefaultBanButton != current.showDefaultBanButton
                    if (modButtonsChanged) {
                        Napier.d(
                            tag = "ModReorder",
                            message = "[changeBroadcast@${this@SettingsViewModel.hashCode()}] " +
                                    "disk=${fresh.allModButtons.map { "${it.id}(${it.sortOrder})" }} " +
                                    "was=${current.allModButtons.map { "${it.id}(${it.sortOrder})" }} fullReload=$fullReload"
                        )
                    }
                    fresh.copy(
                        showThemeCreator = current.showThemeCreator,
                        themeCreatorSeedColor = current.themeCreatorSeedColor,
                        isLoadingBlockedUsers = current.isLoadingBlockedUsers,
                        blockedUsernames = current.blockedUsernames,
                        blockedLoadError = current.blockedLoadError,
                        macros = if (fullReload) fresh.macros else current.macros,
                        chatCommands = if (fullReload) fresh.chatCommands else current.chatCommands,
                        highlightRules = if (fullReload) fresh.highlightRules else current.highlightRules,
                        modButtonsVersion = if (modButtonsChanged || fullReload) current.modButtonsVersion + 1
                        else current.modButtonsVersion
                    )
                }
            }
        }
    }

    override fun update(updater: (SettingsState) -> SettingsState) {
        super.update(updater)
        emitChange()
    }

    override suspend fun onEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.OnDarkThemeChanged -> {
                settings.putBoolean(
                    KEY_DARK_THEME,
                    event.enabled
                ); update { it.copy(darkTheme = event.enabled) }
            }

            is SettingsEvent.OnActiveCustomThemeIdChanged -> {
                update { it.copy(activeCustomThemeId = event.themeId) }
            }

            is SettingsEvent.OnCustomThemesJsonChanged -> {
                settings.putString("custom_themes_json", event.json)
                update { it.copy(customThemesJson = event.json) }
            }

            is SettingsEvent.OnShowChatHeaderChanged -> {
                settings.putBoolean(KEY_SHOW_CHAT_HEADER, event.show)
                update { it.copy(showChatHeader = event.show) }
            }

            is SettingsEvent.OnTimestampFormatChanged -> {
                settings.putInt(KEY_TIMESTAMP_FORMAT, event.format.ordinal); update {
                    it.copy(
                        timestampFormat = event.format
                    )
                }
            }

            is SettingsEvent.OnClearCacheClicked -> {
                viewModelScope.launch {
                    try {
                        AppDataCleaner.clearAll()
                    } catch (e: Exception) {
                        Napier.e("Clear cache error: ${e.message}", e, tag = "SettingsViewModel")
                    }
                    AppRestarter.restart(delayMs = 300L)
                }
            }

            is SettingsEvent.OnLogoutClicked -> {
                viewModelScope.launch {
                    try {
                        val account = getFirstValidAccountUseCase()
                        if (account != null) {
                            authRepository.revokeToken(account)
                            authRepository.deleteAccount(account.userId)
                            Napier.d("User logged out: ${account.login}", tag = "SettingsViewModel")
                        }
                    } catch (e: Exception) {
                        Napier.e("Logout error: ${e.message}", e, tag = "SettingsViewModel")
                        try {
                            val account = getFirstValidAccountUseCase()
                            account?.let { authRepository.deleteAccount(it.userId) }
                        } catch (_: Exception) {
                        }
                    }
                    AppRestarter.restart(delayMs = 300L)
                }
            }

            is SettingsEvent.OnOpenThemeCreator -> {
                update { it.copy(showThemeCreator = true, themeCreatorSeedColor = event.seedColor) }
            }

            is SettingsEvent.OnCloseThemeCreator -> {
                update { it.copy(showThemeCreator = false) }
            }

            is SettingsEvent.OnApplyCustomTheme -> {
                saveCustomThemes(state.value.customThemes, event.themeId)
                update { it.copy(activeCustomThemeId = event.themeId) }
            }

            is SettingsEvent.OnImportSettingsText -> {
                val backup = io.rudione.chatone.util.SettingsImportExport.fromJson(event.text)
                if (backup != null) {
                    io.rudione.chatone.util.SettingsImportExport.applyReplace(settings, backup)
                    update { loadInitialState() }
                }
            }

            is SettingsEvent.OnCustomThemeApplied -> {
                settings.putString(KEY_ACTIVE_THEME_ID, event.theme?.id ?: "")

                update {
                    it.copy(
                        activeCustomTheme = event.theme,
                        activeCustomThemeId = event.theme?.id,
                        showThemeCreator = false
                    )
                }
            }

            is SettingsEvent.OnDeleteCustomTheme -> {
                val newThemes = state.value.customThemes.filter { it.id != event.themeId }
                val newActiveId = if (state.value.activeCustomThemeId == event.themeId) null
                else state.value.activeCustomThemeId
                saveCustomThemes(newThemes, newActiveId)
                update {
                    it.copy(
                        customThemes = newThemes,
                        activeCustomThemeId = newActiveId,
                        activeCustomTheme = newThemes.find { t -> t.id == newActiveId }
                    )
                }
            }

            is SettingsEvent.OnShowDeletedChanged -> {
                settings.putBoolean(KEY_SHOW_DELETED, event.show); update {
                    it.copy(
                        showDeletedMessages = event.show
                    )
                }
            }

            is SettingsEvent.OnImageUploaderChanged -> {
                settings.putString(KEY_IMAGE_UPLOADER, json.encodeToString(event.config))
                update { it.copy(imageUploader = event.config) }
            }

            is SettingsEvent.OnShowViewerJoinLeaveChanged -> {
                settings.putBoolean(KEY_SHOW_VIEWER_JOIN_LEAVE, event.show); update {
                    it.copy(
                        showViewerJoinLeave = event.show
                    )
                }
            }

            is SettingsEvent.OnScrollbackLimitChanged -> {
                settings.putInt(
                    KEY_SCROLLBACK_LIMIT,
                    event.limit
                ); update { it.copy(scrollbackLimit = event.limit) }
            }

            is SettingsEvent.OnEmoteSizeChanged -> {
                settings.putInt(
                    KEY_EMOTE_SIZE,
                    event.size.ordinal
                ); update { it.copy(emoteSize = event.size) }
            }

            is SettingsEvent.OnSaveCustomTheme -> {
                val newThemes = state.value.customThemes.toMutableList()
                val existingIndex = newThemes.indexOfFirst { it.id == event.theme.id }
                if (existingIndex >= 0) {
                    newThemes[existingIndex] = event.theme
                } else {
                    newThemes.add(event.theme)
                }
                saveCustomThemes(newThemes, event.theme.id)
                update {
                    it.copy(
                        customThemes = newThemes,
                        activeCustomThemeId = event.theme.id,
                        activeCustomTheme = event.theme,
                        showThemeCreator = false
                    )
                }
            }

            is SettingsEvent.OnShowBadgesChanged -> {
                settings.putBoolean(
                    KEY_SHOW_BADGES,
                    event.show
                ); update { it.copy(showBadges = event.show) }
            }

            is SettingsEvent.OnUiScaleChanged -> {
                settings.putFloat(KEY_UI_SCALE, event.scale.coerceIn(0.7f, 2.0f))
                update { it.copy(uiScale = event.scale.coerceIn(0.7f, 2.0f)) }
            }

            is SettingsEvent.OnLanguageChanged -> {
                settings.putString(KEY_LANGUAGE, event.code)
                update { it.copy(language = event.code) }
            }

            is SettingsEvent.OnFontFamilyChanged -> {
                settings.putString(KEY_FONT_FAMILY, event.name)
                update { it.copy(fontFamilyName = event.name) }
            }

            is SettingsEvent.OnFontItalicChanged -> {
                settings.putBoolean(KEY_FONT_ITALIC, event.italic)
                update { it.copy(fontStyleItalic = event.italic) }
            }

            is SettingsEvent.OnFontUnderlineChanged -> {
                settings.putBoolean(KEY_FONT_UNDERLINE, event.underline)
                update { it.copy(fontUnderline = event.underline) }
            }

            is SettingsEvent.OnFontStrikethroughChanged -> {
                settings.putBoolean(KEY_FONT_STRIKETHROUGH, event.strikethrough)
                update { it.copy(fontStrikethrough = event.strikethrough) }
            }

            is SettingsEvent.OnAddCustomFontPath -> {
                val updated = (state.value.customFontPaths + event.path).distinct()
                settings.putString(KEY_CUSTOM_FONT_PATHS, json.encodeToString(updated))
                update { it.copy(customFontPaths = updated) }
            }

            is SettingsEvent.OnRemoveCustomFontPath -> {
                val updated = state.value.customFontPaths.filter { it != event.path }
                settings.putString(KEY_CUSTOM_FONT_PATHS, json.encodeToString(updated))
                update { it.copy(customFontPaths = updated) }
            }

            is SettingsEvent.OnMessageSpacingChanged -> {
                settings.putInt(KEY_MESSAGE_SPACING, event.spacing.ordinal)
                update { it.copy(messageSpacing = event.spacing) }
            }

            is SettingsEvent.OnTitleBarModeChanged -> {
                settings.putString(KEY_TITLE_BAR_MODE, event.mode.name)
                update { it.copy(titleBarMode = event.mode) }
            }
            is SettingsEvent.OnShowBlockedModeChanged -> {
                settings.putInt(KEY_SHOW_BLOCKED_MODE, event.mode)
                update { it.copy(showBlockedMode = event.mode) }
            }
            is SettingsEvent.OnLoadBlockedUsers -> {
                update { it.copy(isLoadingBlockedUsers = true, blockedLoadError = null) }
                viewModelScope.launch {
                    try {
                        val api = twitchApiClient
                        if (api == null) {
                            update { it.copy(isLoadingBlockedUsers = false, blockedLoadError = "API not available") }
                            return@launch
                        }
                        val accounts = authRepository.getAccounts().first()
                        if (accounts.isEmpty()) {
                            update { it.copy(isLoadingBlockedUsers = false, blockedLoadError = "Not logged in") }
                            return@launch
                        }
                        val aggregated = linkedSetOf<String>()
                        var anySuccess = false
                        var lastError: String? = null
                        for (account in accounts) {
                            val validatedUserId = runCatching {
                                val v = api.validateToken(account.accessToken)
                                if (v is io.rudione.chatone.util.Result.Success) v.data.userId else account.userId
                            }.getOrDefault(account.userId)
                            val result = api.getBlockedUsers(account.accessToken, validatedUserId, first = 100)
                            when (result) {
                                is io.rudione.chatone.util.Result.Success -> {
                                    anySuccess = true
                                    result.data.data.forEach { aggregated.add(it.userLogin) }
                                }
                                is io.rudione.chatone.util.Result.Error -> {
                                    lastError = result.exception.message ?: "Failed to load"
                                }
                                else -> {}
                            }
                        }
                        update {
                            it.copy(
                                blockedUsernames = aggregated.toList().sorted(),
                                isLoadingBlockedUsers = false,
                                blockedLoadError = if (anySuccess) null else lastError
                            )
                        }
                    } catch (e: Exception) {
                        update { it.copy(isLoadingBlockedUsers = false, blockedLoadError = e.message ?: "Failed to load") }
                    }
                }
            }
            is SettingsEvent.BlockedUsersLoaded -> {
                update { it.copy(blockedUsernames = event.usernames, isLoadingBlockedUsers = false) }
            }
            is SettingsEvent.BlockedUsersLoadFailed -> {
                update { it.copy(isLoadingBlockedUsers = false, blockedLoadError = event.error) }
            }
            is SettingsEvent.OnUnblockUserFromSettings -> {
                update { st ->
                    st.copy(blockedUsernames = st.blockedUsernames.filter {
                        !it.equals(event.username, ignoreCase = true)
                    })
                }
                viewModelScope.launch {
                    try {
                        val api = twitchApiClient ?: return@launch
                        val accounts = authRepository.getAccounts().first()
                        if (accounts.isEmpty()) return@launch

                        var targetId: String? = event.userId.takeIf { it.isNotBlank() }
                        val resolver = accounts.first()
                        if (targetId.isNullOrEmpty()) {
                            val r = api.getUsers(resolver.accessToken, logins = listOf(event.username))
                            if (r is io.rudione.chatone.util.Result.Success) {
                                targetId = r.data.data.firstOrNull()?.id
                            }
                        }
                        val id = targetId
                        if (id.isNullOrEmpty()) {
                            update { it.copy(blockedLoadError = "Could not resolve user id for ${event.username}") }
                            return@launch
                        }
                        accounts.forEach { acc ->
                            runCatching { api.unblockUser(acc.accessToken, id) }
                        }
                    } catch (e: Exception) {
                        update { it.copy(blockedLoadError = e.message ?: "Failed to unblock") }
                    }
                }
            }

            is SettingsEvent.OnFontSizeChanged -> {
                settings.putInt(
                    KEY_FONT_SIZE,
                    event.size.ordinal
                ); update { it.copy(fontSize = event.size) }
            }

            is SettingsEvent.OnDefaultTimeoutChanged -> {
                settings.putInt(KEY_DEFAULT_TIMEOUT, event.duration); update {
                    it.copy(
                        defaultTimeoutDuration = event.duration
                    )
                }
            }

            is SettingsEvent.OnConfirmModActionsChanged -> {
                settings.putBoolean(KEY_CONFIRM_MOD, event.confirm); update {
                    it.copy(
                        confirmModActions = event.confirm
                    )
                }
            }

            is SettingsEvent.OnChannelNavigationChanged -> {
                settings.putInt(KEY_CHANNEL_NAV, event.navigation.ordinal); update {
                    it.copy(
                        channelNavigation = event.navigation
                    )
                }
            }

            is SettingsEvent.OnHighlightRuleToggled -> update { s ->
                val n =
                    s.highlightRules.map { if (it.id == event.ruleId) it.copy(enabled = event.enabled) else it }; saveHighlightRules(
                n
            ); s.copy(highlightRules = n)
            }

            is SettingsEvent.OnHighlightRuleSoundToggled -> update { s ->
                val n =
                    s.highlightRules.map { if (it.id == event.ruleId) it.copy(playSound = event.playSound) else it }; saveHighlightRules(
                n
            ); s.copy(highlightRules = n)
            }

            is SettingsEvent.OnHighlightRuleSubstringToggled -> update { s ->
                val n = s.highlightRules.map {
                    if (it.id == event.ruleId) it.copy(matchSubstring = event.matchSubstring) else it
                }
                saveHighlightRules(n)
                s.copy(highlightRules = n)
            }

            is SettingsEvent.OnHideChatPlaceholderChanged -> {
                settings.putBoolean(KEY_HIDE_CHAT_PLACEHOLDER, event.hide)
                update { it.copy(hideChatInputPlaceholder = event.hide) }
            }
            is SettingsEvent.OnHideEmojiButtonChanged -> {
                settings.putBoolean(KEY_HIDE_EMOJI_BUTTON, event.hide)
                update { it.copy(hideEmojiButton = event.hide) }
            }
            is SettingsEvent.OnChatInputGlowChanged -> {
                settings.putBoolean(KEY_CHAT_INPUT_GLOW, event.enabled)
                update { it.copy(chatInputEventGlow = event.enabled) }
            }
            is SettingsEvent.OnShowRepeatedCounterChanged -> {
                settings.putBoolean(KEY_SHOW_REPEAT_COUNTER, event.show)
                update { it.copy(showRepeatedMessageCounter = event.show) }
            }
            is SettingsEvent.OnRepeatedWindowChanged -> {
                val v = event.seconds.coerceIn(5, 300)
                settings.putInt(KEY_REPEAT_WINDOW, v)
                update { it.copy(repeatedMessageWindow = v) }
            }
            is SettingsEvent.OnSavedTimeoutReasonChanged -> {
                settings.putString(KEY_SAVED_TIMEOUT_REASON, event.text)
                update { it.copy(savedTimeoutReason = event.text) }
            }
            is SettingsEvent.OnSavedBanReasonChanged -> {
                settings.putString(KEY_SAVED_BAN_REASON, event.text)
                update { it.copy(savedBanReason = event.text) }
            }

            is SettingsEvent.OnHighlightRuleColorChanged -> update { s ->
                val n =
                    s.highlightRules.map { if (it.id == event.ruleId) it.copy(color = event.color) else it }; saveHighlightRules(
                n
            ); s.copy(highlightRules = n)
            }

            is SettingsEvent.OnAddHighlightRule -> update { s ->
                val rule = HighlightRule(
                    id = "custom_${Clock.System.now().toEpochMilliseconds()}",
                    pattern = event.pattern,
                    playSound = true,
                    showInMentions = true
                );
                val n = s.highlightRules + rule; saveHighlightRules(n); s.copy(highlightRules = n)
            }

            is SettingsEvent.OnRemoveHighlightRule -> update { s ->
                val n =
                    s.highlightRules.filter { it.id != event.ruleId }; saveHighlightRules(n); s.copy(
                highlightRules = n
            )
            }

            is SettingsEvent.OnMentionSoundChanged -> {
                settings.putBoolean(KEY_MENTION_SOUND, event.enabled); update {
                    it.copy(
                        mentionSoundEnabled = event.enabled
                    )
                }
            }

            is SettingsEvent.OnMentionSoundVolumeChanged -> {
                settings.putFloat(KEY_MENTION_VOLUME, event.volume); update {
                    it.copy(
                        mentionSoundVolume = event.volume
                    )
                }
            }

            is SettingsEvent.OnCustomMentionSoundPathChanged -> {
                settings.putString(KEY_CUSTOM_SOUND_PATH, event.path); update {
                    it.copy(
                        customMentionSoundPath = event.path
                    )
                }
            }

            is SettingsEvent.OnAlwaysOnTopChanged -> {
                settings.putBoolean(
                    KEY_ALWAYS_ON_TOP,
                    event.enabled
                ); update { it.copy(alwaysOnTop = event.enabled) }
            }

            is SettingsEvent.OnPauseOnHoverChanged -> {
                settings.putBoolean(KEY_PAUSE_ON_HOVER, event.enabled); update {
                    it.copy(
                        pauseOnHover = event.enabled
                    )
                }
            }

            is SettingsEvent.OnPauseHotkeyChanged -> {
                settings.putString(
                    KEY_PAUSE_HOTKEY,
                    event.hotkey
                ); update { it.copy(pauseHotkey = event.hotkey) }
            }

            is SettingsEvent.OnPauseHotkeyModeChanged -> {
                settings.putInt(KEY_PAUSE_HOTKEY_MODE, event.mode.ordinal)
                update { it.copy(pauseHotkeyMode = event.mode) }
            }

            is SettingsEvent.OnShowInlineImagesChanged -> {
                settings.putInt(KEY_SHOW_INLINE_IMAGES, event.mode.ordinal)
                update { it.copy(showInlineImages = event.mode) }
            }

            is SettingsEvent.OnInlineImageMaxHeightChanged -> {
                settings.putInt(KEY_INLINE_IMAGE_MAX_HEIGHT, event.height.coerceIn(50, 500))
                update { it.copy(inlineImageMaxHeight = event.height.coerceIn(50, 500)) }
            }

            is SettingsEvent.OnChatScrollbarWidthChanged -> {
                val w = event.width.coerceIn(6, 32)
                settings.putInt(KEY_CHAT_SCROLLBAR_WIDTH, w)
                update { it.copy(chatScrollbarWidth = w) }
            }

            is SettingsEvent.OnAddAutomation -> update { st ->
                val n = st.automations + event.automation
                settings.putString(KEY_AUTOMATIONS, json.encodeToString(n))
                st.copy(automations = n)
            }

            is SettingsEvent.OnRemoveAutomation -> update { st ->
                val n = st.automations.filter { it.id != event.id }
                settings.putString(KEY_AUTOMATIONS, json.encodeToString(n))
                st.copy(automations = n)
            }

            is SettingsEvent.OnToggleAutomation -> update { st ->
                val n = st.automations.map { if (it.id == event.id) it.copy(enabled = event.enabled) else it }
                settings.putString(KEY_AUTOMATIONS, json.encodeToString(n))
                st.copy(automations = n)
            }

            is SettingsEvent.OnAutoClaimPointsChanged -> {
                settings.putBoolean(KEY_AUTO_CLAIM_POINTS, event.enabled)
                update { it.copy(autoClaimPoints = event.enabled) }
            }

            is SettingsEvent.OnAddMutedPhrase -> update { st ->
                val phrase = event.phrase.trim()
                if (phrase.isEmpty() || st.mutedPhrases.any { it.equals(phrase, ignoreCase = true) }) st
                else {
                    val n = st.mutedPhrases + phrase
                    settings.putString(KEY_MUTED_PHRASES, json.encodeToString(n))
                    st.copy(mutedPhrases = n)
                }
            }

            is SettingsEvent.OnRemoveMutedPhrase -> update { st ->
                val n = st.mutedPhrases.filter { it != event.phrase }
                settings.putString(KEY_MUTED_PHRASES, json.encodeToString(n))
                st.copy(mutedPhrases = n)
            }

            is SettingsEvent.OnWallpaperPathChanged -> {
                settings.putString(KEY_WALLPAPER_PATH, event.path);
                val w = loadWallpaper(event.path, state.value.wallpaperBlur); update {
                    it.copy(
                        wallpaperPath = event.path
                    )
                }; w?.let { sendEffect(SettingsEffect.WallpaperChanged(it)) }
            }

            is SettingsEvent.OnWallpaperBlurChanged -> {
                settings.putFloat(KEY_WALLPAPER_BLUR, event.blur);
                val w = loadWallpaper(state.value.wallpaperPath, event.blur); update {
                    it.copy(
                        wallpaperBlur = event.blur
                    )
                }; w?.let { sendEffect(SettingsEffect.WallpaperChanged(it)) }
            }

            is SettingsEvent.OnWallpaperDisplayConfigChanged -> {
                settings.putString(
                    WallpaperDisplayConfig.SETTINGS_KEY,
                    WallpaperDisplayConfig.toJson(event.config)
                )
                update { it.copy(wallpaperDisplayConfig = event.config) }
            }

            is SettingsEvent.OnCloseEmotePickerOnMouseLeaveChanged -> {
                settings.putBoolean(KEY_EMOTE_PICKER_MOUSE_LEAVE, event.enabled); update {
                    it.copy(
                        closeEmotePickerOnMouseLeave = event.enabled
                    )
                }
            }

            is SettingsEvent.OnAddModButton -> update { s ->
                if (s.customModButtons.size >= 8) return@update s
                val btn = ModActionButton(
                    id = "btn_${Clock.System.now().toEpochMilliseconds()}",
                    durationSeconds = event.durationSeconds,
                    label = event.label
                )
                val n = s.customModButtons + btn
                saveModButtons(n)
                val newAll = (s.allModButtons + btn).mapIndexed { i, b -> b.copy(sortOrder = i) }
                saveAllModButtons(newAll)
                s.copy(customModButtons = n, allModButtons = newAll)
            }

            is SettingsEvent.OnRemoveModButton -> update { s ->
                val n = s.customModButtons.filter { it.id != event.id }
                saveModButtons(n)
                val newAll = s.allModButtons.filter { it.id != event.id }
                    .mapIndexed { i, b -> b.copy(sortOrder = i) }
                saveAllModButtons(newAll)
                s.copy(customModButtons = n, allModButtons = newAll)
            }

            is SettingsEvent.OnUpdateModButton -> update { s ->
                val n =
                    s.customModButtons.map { if (it.id == event.button.id) event.button else it }; saveModButtons(
                n
            ); s.copy(customModButtons = n)
            }

            is SettingsEvent.OnReorderModButtons -> update { s ->
                val list = s.customModButtons.toMutableList()
                if (event.from in list.indices && event.to in list.indices) {
                    val item = list.removeAt(event.from); list.add(event.to, item)
                }
                saveModButtons(list); s.copy(customModButtons = list)
            }

            is SettingsEvent.OnReorderAllModButtons -> update { s ->
                val reordered = event.newOrder.mapIndexed { i, b -> b.copy(sortOrder = i) }
                saveAllModButtons(reordered)
                val custom = reordered.filter { !it.isDefault }
                saveModButtons(custom)
                val newVer = s.modButtonsVersion + 1
                println("ModReorder[OnReorderAllModButtons] order=${reordered.map { it.id + "(" + it.sortOrder + ")" }} ver=$newVer")
                s.copy(allModButtons = reordered, customModButtons = custom, modButtonsVersion = newVer)
            }

            is SettingsEvent.OnSetModButtonEnabled -> update { s ->
                val newAll = s.allModButtons.map {
                    if (it.id == event.id) it.copy(enabled = event.enabled) else it
                }
                saveAllModButtons(newAll)
                val custom = newAll.filter { !it.isDefault }
                saveModButtons(custom)
                s.copy(
                    allModButtons = newAll,
                    customModButtons = custom,
                    modButtonsVersion = s.modButtonsVersion + 1
                )
            }

            is SettingsEvent.OnShowDefaultDeleteChanged -> {
                settings.putBoolean(KEY_SHOW_DEFAULT_DELETE, event.show)
                update { it.copy(showDefaultDeleteButton = event.show, modButtonsVersion = it.modButtonsVersion + 1) }
            }

            is SettingsEvent.OnShowDefaultTimeoutChanged -> {
                settings.putBoolean(KEY_SHOW_DEFAULT_TIMEOUT, event.show)
                update { it.copy(showDefaultTimeoutButton = event.show, modButtonsVersion = it.modButtonsVersion + 1) }
            }

            is SettingsEvent.OnShowDefaultBanChanged -> {
                settings.putBoolean(KEY_SHOW_DEFAULT_BAN, event.show)
                update { it.copy(showDefaultBanButton = event.show, modButtonsVersion = it.modButtonsVersion + 1) }
            }

            is SettingsEvent.OnAddMacro -> update { s ->
                val macro = Macro(
                    id = "macro_${Clock.System.now().toEpochMilliseconds()}",
                    name = event.name,
                    icon = event.icon
                )
                val n = s.macros + macro; saveMacros(n); s.copy(macros = n)
            }

            is SettingsEvent.OnRemoveMacro -> update { s ->
                val n = s.macros.filter { it.id != event.id }; saveMacros(n); s.copy(macros = n)
            }

            is SettingsEvent.OnUpdateMacro -> update { s ->
                val n =
                    s.macros.map { if (it.id == event.macro.id) event.macro else it }; saveMacros(n); s.copy(
                macros = n
            )
            }

            is SettingsEvent.OnSmoothChatEnabledChanged -> {
                settings.putBoolean(KEY_SMOOTH_CHAT, event.enabled)
                update { it.copy(smoothChatEnabled = event.enabled) }
            }

            is SettingsEvent.OnAlternateRowBackgroundChanged -> {
                settings.putBoolean(KEY_ALTERNATE_ROW_BG, event.enabled)
                update { it.copy(alternateRowBackground = event.enabled) }
            }

            is SettingsEvent.OnDisableScrollOnAltChanged -> {
                settings.putBoolean(KEY_DISABLE_SCROLL_ON_ALT, event.enabled)
                update { it.copy(disableScrollOnAlt = event.enabled) }
            }

            is SettingsEvent.OnLinkOpenModeChanged -> {
                settings.putString(KEY_LINK_OPEN_MODE, event.mode.name)
                update { it.copy(linkOpenMode = event.mode) }
            }

            is SettingsEvent.OnAccentColorChanged -> {
                settings.putInt(KEY_ACCENT_COLOR_INDEX, event.index)
                update { it.copy(accentColorIndex = event.index) }
            }

            is SettingsEvent.OnPinMacro -> update { s ->
                val n = s.macros.map { m ->
                    when {
                        m.id == event.macroId -> m.copy(pinnedIndex = event.slotIndex); m.pinnedIndex == event.slotIndex && event.slotIndex != -1 -> m.copy(
                        pinnedIndex = -1
                    ); else -> m
                    }
                }
                saveMacros(n); s.copy(macros = n)
            }

            is SettingsEvent.OnAddChatCommand -> update { s ->
                val n = s.chatCommands + event.command
                saveChatCommands(n); s.copy(chatCommands = n)
            }

            is SettingsEvent.OnUpdateChatCommand -> update { s ->
                val n = s.chatCommands.map { if (it.id == event.command.id) event.command else it }
                saveChatCommands(n); s.copy(chatCommands = n)
            }

            is SettingsEvent.OnRemoveChatCommand -> update { s ->
                val n = s.chatCommands.filter { it.id != event.id }
                saveChatCommands(n); s.copy(chatCommands = n)
            }

            is SettingsEvent.OnColorTokensChanged -> {
                settings.putString(KEY_COLOR_TOKENS, json.encodeToString(event.tokens))
                update { it.copy(colorTokens = event.tokens) }
            }

            is SettingsEvent.OnTranslationLangChanged -> {
                settings.putString(KEY_TRANSLATION_LANG, event.code)
                update { it.copy(translationTargetLang = event.code) }
            }

            is SettingsEvent.OnMentionTabsChanged -> {
                settings.putBoolean(KEY_MENTION_TABS, event.enabled)
                update { it.copy(mentionTabsEnabled = event.enabled) }
            }

            is SettingsEvent.OnNavigationHiddenChanged -> {
                settings.putBoolean(KEY_NAVIGATION_HIDDEN, event.hidden)
                update { it.copy(navigationHidden = event.hidden) }
            }

            is SettingsEvent.OnHotkeyChanged -> {
                val updated = state.value.hotkeys.toMutableMap().apply { put(event.actionId, event.combo) }
                settings.putString(KEY_HOTKEYS, json.encodeToString(updated))
                update { it.copy(hotkeys = updated) }
            }
        }
    }

    private fun saveChatCommands(commands: List<ChatCommand>) =
        settings.putString(KEY_CHAT_COMMANDS, json.encodeToString(commands))

    private fun saveCustomThemes(themes: List<CustomThemeConfig>, activeId: String?) {
        settings.putString(KEY_CUSTOM_THEMES, json.encodeToString(themes))
        settings.putString(KEY_ACTIVE_THEME_ID, activeId ?: "")
    }

    private fun loadWallpaper(path: String, blur: Float): WallpaperState? =
        wallpaperLoader.load(path, blur)

    private fun saveHighlightRules(rules: List<HighlightRule>) =
        settings.putString(KEY_HIGHLIGHT_RULES, json.encodeToString(rules))

    private fun saveAllModButtons(buttons: List<ModActionButton>) {
        settings.putString(KEY_ALL_MOD_BUTTONS, json.encodeToString(buttons))
        _modButtonsLive.value = buttons
        println("ModReorder[publish->modButtonsLive] ${buttons.map { "${it.id}(${it.sortOrder})" }}")
        notifyExternalChange()
        val readBack = try {
            settings.getStringOrNull(KEY_ALL_MOD_BUTTONS)
                ?.let { json.decodeFromString<List<ModActionButton>>(it) }
        } catch (_: Exception) { null }
        val ok = readBack?.map { it.id to it.sortOrder } == buttons.map { it.id to it.sortOrder }
        Napier.d(
            tag = "ModReorder",
            message = "[saveAllModButtons] wrote=${buttons.map { "${it.id}(${it.sortOrder})" }} readBackOk=$ok"
        )
    }

    private fun saveModButtons(buttons: List<ModActionButton>) =
        settings.putString(KEY_CUSTOM_MOD_BUTTONS, json.encodeToString(buttons))

    private fun saveMacros(macros: List<Macro>) {
        settings.putString(KEY_MACROS, json.encodeToString(macros))
        _macrosLive.value = macros
        notifyExternalChange()
    }
}