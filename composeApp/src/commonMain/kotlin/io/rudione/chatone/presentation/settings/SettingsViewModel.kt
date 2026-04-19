package io.rudione.chatone.presentation.settings

import com.russhwolf.settings.Settings
import io.rudione.chatone.base.BaseViewModel
import io.rudione.chatone.base.UIEffect
import io.rudione.chatone.base.UiEvent
import io.rudione.chatone.base.UiState
import io.rudione.chatone.domain.model.HighlightRule
import io.rudione.chatone.domain.model.Macro
import io.rudione.chatone.domain.model.ModActionButton
import io.rudione.chatone.presentation.theme.WallpaperState
import io.rudione.chatone.util.WallpaperLoader
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.datetime.Clock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

enum class PauseHotkeyMode { TOGGLE, HOLD }
enum class InlineImageMode { ON, OFF, BLUR }

data class SettingsState(
    val darkTheme: Boolean = true,
    val timestampFormat: TimestampFormat = TimestampFormat.H24,
    val showDeletedMessages: Boolean = true,
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
        HighlightRule.FIRST_MESSAGE_RULE
    ),
    val mentionSoundEnabled: Boolean = true,
    val mentionSoundVolume: Float = 0.8f,
    val customMentionSoundPath: String = "",
    val alwaysOnTop: Boolean = true,
    val uiScale: Float = 1.0f,
    val pauseOnHover: Boolean = false,
    val pauseHotkey: String = "",
    val pauseHotkeyMode: PauseHotkeyMode = PauseHotkeyMode.TOGGLE,
    val showInlineImages: InlineImageMode = InlineImageMode.ON,
    val inlineImageMaxHeight: Int = 200,
    val wallpaperPath: String = "",
    val wallpaperBlur: Float = 12f,
    val closeEmotePickerOnMouseLeave: Boolean = false,
    val customModButtons: List<ModActionButton> = emptyList(),
    val allModButtons: List<ModActionButton> = listOf(
        ModActionButton.DEFAULT_DELETE,
        ModActionButton.DEFAULT_TIMEOUT,
        ModActionButton.DEFAULT_BAN
    ),
    val macros: List<Macro> = emptyList(),
    val showChatHeader: Boolean = true,
    val smoothChatEnabled: Boolean = false,
    val showDefaultDeleteButton: Boolean = true,
    val showDefaultTimeoutButton: Boolean = true,
    val showDefaultBanButton: Boolean = true
) : UiState {
    enum class TimestampFormat { H12, H24, OFF }
    enum class EmoteSize { SMALL, MEDIUM, LARGE }
    enum class FontSize { SMALL, MEDIUM, LARGE }
    enum class ChannelNavigation { TAB_BAR, MINI_RAIL, BOTH }

    val pinnedMacros: List<Macro>
        get() = macros.filter { it.pinnedIndex in 0..4 }.sortedBy { it.pinnedIndex }.take(5)
}

sealed class SettingsEvent : UiEvent {
    data class OnDarkThemeChanged(val enabled: Boolean) : SettingsEvent()
    data class OnShowChatHeaderChanged(val show: Boolean) : SettingsEvent()
    data class OnTimestampFormatChanged(val format: SettingsState.TimestampFormat) : SettingsEvent()
    data class OnShowDeletedChanged(val show: Boolean) : SettingsEvent()
    data class OnScrollbackLimitChanged(val limit: Int) : SettingsEvent()
    data class OnEmoteSizeChanged(val size: SettingsState.EmoteSize) : SettingsEvent()
    data class OnShowBadgesChanged(val show: Boolean) : SettingsEvent()
    data class OnFontSizeChanged(val size: SettingsState.FontSize) : SettingsEvent()
    data class OnUiScaleChanged(val scale: Float) : SettingsEvent()
    data class OnDefaultTimeoutChanged(val duration: Int) : SettingsEvent()
    data class OnConfirmModActionsChanged(val confirm: Boolean) : SettingsEvent()
    data class OnChannelNavigationChanged(val navigation: SettingsState.ChannelNavigation) :
        SettingsEvent()

    data class OnHighlightRuleToggled(val ruleId: String, val enabled: Boolean) : SettingsEvent()
    data class OnHighlightRuleSoundToggled(val ruleId: String, val playSound: Boolean) :
        SettingsEvent()

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
    data class OnWallpaperPathChanged(val path: String) : SettingsEvent()
    data class OnWallpaperBlurChanged(val blur: Float) : SettingsEvent()
    data class OnCloseEmotePickerOnMouseLeaveChanged(val enabled: Boolean) : SettingsEvent()


    data class OnAddModButton(val durationSeconds: Int, val label: String) : SettingsEvent()
    data class OnRemoveModButton(val id: String) : SettingsEvent()
    data class OnUpdateModButton(val button: ModActionButton) : SettingsEvent()
    data class OnReorderAllModButtons(val newOrder: List<ModActionButton>) : SettingsEvent()
    data class OnShowDefaultDeleteChanged(val show: Boolean) : SettingsEvent()
    data class OnShowDefaultTimeoutChanged(val show: Boolean) : SettingsEvent()
    data class OnShowDefaultBanChanged(val show: Boolean) : SettingsEvent()
    data class OnReorderModButtons(val from: Int, val to: Int) : SettingsEvent()


    data class OnAddMacro(val name: String, val icon: String) : SettingsEvent()
    data class OnRemoveMacro(val id: String) : SettingsEvent()
    data class OnUpdateMacro(val macro: Macro) : SettingsEvent()
    data class OnPinMacro(val macroId: String, val slotIndex: Int) : SettingsEvent()

    data class OnSmoothChatEnabledChanged(val enabled: Boolean) : SettingsEvent()
}

sealed class SettingsEffect : UIEffect {
    data class WallpaperChanged(val wallpaper: WallpaperState) : SettingsEffect()
}

class SettingsViewModel(
    private val wallpaperLoader: WallpaperLoader
) : BaseViewModel<SettingsState, SettingsEvent, SettingsEffect>(loadInitialState()) {
    companion object {
        private val settings = Settings()
        private const val KEY_DARK_THEME = "dark_theme"
        private const val KEY_TIMESTAMP_FORMAT = "timestamp_format"
        private const val KEY_SHOW_DELETED = "show_deleted"
        private const val KEY_SCROLLBACK_LIMIT = "scrollback_limit"
        private const val KEY_EMOTE_SIZE = "emote_size"
        private const val KEY_SHOW_BADGES = "show_badges"
        private const val KEY_FONT_SIZE = "font_size"
        private const val KEY_DEFAULT_TIMEOUT = "default_timeout"
        private const val KEY_CONFIRM_MOD = "confirm_mod_actions"
        private const val KEY_CHANNEL_NAV = "channel_navigation"
        private const val KEY_HIGHLIGHT_RULES = "highlight_rules"
        private const val KEY_MENTION_SOUND = "mention_sound"
        private const val KEY_MENTION_VOLUME = "mention_volume"
        private const val KEY_CUSTOM_SOUND_PATH = "custom_sound_path"
        private const val KEY_ALWAYS_ON_TOP = "always_on_top"
        private const val KEY_UI_SCALE = "ui_scale"
        private const val KEY_PAUSE_ON_HOVER = "pause_on_hover"
        private const val KEY_PAUSE_HOTKEY = "pause_hotkey"
        private const val KEY_PAUSE_HOTKEY_MODE = "pause_hotkey_mode"
        private const val KEY_SHOW_INLINE_IMAGES = "show_inline_images"
        private const val KEY_INLINE_IMAGE_MAX_HEIGHT = "inline_image_max_height"
        private const val KEY_WALLPAPER_PATH = "wallpaper_path"
        private const val KEY_WALLPAPER_BLUR = "wallpaper_blur"
        private const val KEY_EMOTE_PICKER_MOUSE_LEAVE = "emote_picker_mouse_leave"
        private const val KEY_CUSTOM_MOD_BUTTONS = "custom_mod_buttons"
        private const val KEY_ALL_MOD_BUTTONS = "all_mod_buttons_v2"
        private const val KEY_MACROS = "macros"
        private const val KEY_SHOW_CHAT_HEADER = "show_chat_header"
        private const val KEY_SMOOTH_CHAT = "smooth_chat_enabled"
        private const val KEY_SHOW_DEFAULT_DELETE = "show_default_delete"
        private const val KEY_SHOW_DEFAULT_TIMEOUT = "show_default_timeout"
        private const val KEY_SHOW_DEFAULT_BAN = "show_default_ban"
        private val json = Json { ignoreUnknownKeys = true }
        private val _effects = MutableSharedFlow<SettingsEffect>()
        val effects = _effects.asSharedFlow()

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
            } catch (_: Exception) { null }
            val macros = try {
                val j = settings.getStringOrNull(KEY_MACROS)
                if (j != null) json.decodeFromString<List<Macro>>(j) else emptyList()
            } catch (_: Exception) {
                emptyList()
            }

            return SettingsState(
                darkTheme = settings.getBoolean(KEY_DARK_THEME, true),
                timestampFormat = SettingsState.TimestampFormat.entries.getOrNull(
                    settings.getInt(
                        KEY_TIMESTAMP_FORMAT,
                        1
                    )
                ) ?: SettingsState.TimestampFormat.H24,
                showDeletedMessages = settings.getBoolean(KEY_SHOW_DELETED, true),
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
                highlightRules = rules ?: SettingsState().highlightRules,
                mentionSoundEnabled = settings.getBoolean(KEY_MENTION_SOUND, true),
                mentionSoundVolume = settings.getFloat(KEY_MENTION_VOLUME, 0.8f),
                customMentionSoundPath = settings.getStringOrNull(KEY_CUSTOM_SOUND_PATH) ?: "",
                alwaysOnTop = settings.getBoolean(KEY_ALWAYS_ON_TOP, true),
                uiScale = settings.getFloat(KEY_UI_SCALE, 1.0f),
                pauseOnHover = settings.getBoolean(KEY_PAUSE_ON_HOVER, false),
                pauseHotkey = settings.getStringOrNull(KEY_PAUSE_HOTKEY) ?: "",
                pauseHotkeyMode = PauseHotkeyMode.entries.getOrNull(
                    settings.getInt(KEY_PAUSE_HOTKEY_MODE, 0)
                ) ?: PauseHotkeyMode.TOGGLE,
                showInlineImages = InlineImageMode.entries.getOrNull(
                    settings.getInt(KEY_SHOW_INLINE_IMAGES, 0)
                ) ?: InlineImageMode.ON,
                inlineImageMaxHeight = settings.getInt(KEY_INLINE_IMAGE_MAX_HEIGHT, 200),
                wallpaperPath = settings.getStringOrNull(KEY_WALLPAPER_PATH) ?: "",
                wallpaperBlur = settings.getFloat(KEY_WALLPAPER_BLUR, 12f),
                closeEmotePickerOnMouseLeave = settings.getBoolean(
                    KEY_EMOTE_PICKER_MOUSE_LEAVE,
                    false
                ),
                customModButtons = modButtons,
                macros = macros,
                showChatHeader = settings.getBoolean(KEY_SHOW_CHAT_HEADER, true),
                smoothChatEnabled = settings.getBoolean(KEY_SMOOTH_CHAT, false),
                showDefaultDeleteButton = settings.getBoolean(KEY_SHOW_DEFAULT_DELETE, true),
                showDefaultTimeoutButton = settings.getBoolean(KEY_SHOW_DEFAULT_TIMEOUT, true),
                showDefaultBanButton = settings.getBoolean(KEY_SHOW_DEFAULT_BAN, true),
                allModButtons = allModBtns ?: run {
                    val defaults = ModActionButton.defaultOrderedList()
                    val combined = (defaults + modButtons).distinctBy { it.id }
                    combined.mapIndexed { i, b -> b.copy(sortOrder = i) }
                }
            )
        }
    }

    init {
        subscribeToEvents()
    }

    override suspend fun onEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.OnDarkThemeChanged -> {
                settings.putBoolean(
                    KEY_DARK_THEME,
                    event.enabled
                ); update { it.copy(darkTheme = event.enabled) }
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

            is SettingsEvent.OnShowDeletedChanged -> {
                settings.putBoolean(KEY_SHOW_DELETED, event.show); update {
                    it.copy(
                        showDeletedMessages = event.show
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
                val newAll = s.allModButtons.filter { it.id != event.id }.mapIndexed { i, b -> b.copy(sortOrder = i) }
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
                s.copy(allModButtons = reordered, customModButtons = custom)
            }

            is SettingsEvent.OnShowDefaultDeleteChanged -> {
                settings.putBoolean(KEY_SHOW_DEFAULT_DELETE, event.show)
                update { it.copy(showDefaultDeleteButton = event.show) }
            }
            is SettingsEvent.OnShowDefaultTimeoutChanged -> {
                settings.putBoolean(KEY_SHOW_DEFAULT_TIMEOUT, event.show)
                update { it.copy(showDefaultTimeoutButton = event.show) }
            }
            is SettingsEvent.OnShowDefaultBanChanged -> {
                settings.putBoolean(KEY_SHOW_DEFAULT_BAN, event.show)
                update { it.copy(showDefaultBanButton = event.show) }
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
        }
    }

    private fun loadWallpaper(path: String, blur: Float): WallpaperState? =
        wallpaperLoader.load(path, blur)

    private fun saveHighlightRules(rules: List<HighlightRule>) =
        settings.putString(KEY_HIGHLIGHT_RULES, json.encodeToString(rules))

    private fun saveAllModButtons(buttons: List<ModActionButton>) =
        settings.putString(KEY_ALL_MOD_BUTTONS, json.encodeToString(buttons))

    private fun saveModButtons(buttons: List<ModActionButton>) =
        settings.putString(KEY_CUSTOM_MOD_BUTTONS, json.encodeToString(buttons))

    private fun saveMacros(macros: List<Macro>) =
        settings.putString(KEY_MACROS, json.encodeToString(macros))
}