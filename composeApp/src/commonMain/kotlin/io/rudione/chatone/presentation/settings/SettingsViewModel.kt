package io.rudione.chatone.presentation.settings

import com.russhwolf.settings.Settings
import io.rudione.chatone.base.BaseViewModel
import io.rudione.chatone.base.UIEffect
import io.rudione.chatone.base.UiEvent
import io.rudione.chatone.base.UiState
import io.rudione.chatone.domain.model.HighlightRule
import io.rudione.chatone.presentation.theme.WallpaperState
import io.rudione.chatone.util.WallpaperLoader
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.datetime.Clock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

data class SettingsState(
    val darkTheme: Boolean = true,
    val timestampFormat: TimestampFormat = TimestampFormat.H24,
    val showDeletedMessages: Boolean = true,
    val scrollbackLimit: Int = 500,
    val emoteSize: EmoteSize = EmoteSize.MEDIUM,
    val showBadges: Boolean = true,
    val fontSize: FontSize = FontSize.MEDIUM,
    val defaultTimeoutDuration: Int = 600,
    val confirmModActions: Boolean = true,
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
    val pauseOnHover: Boolean = false,
    val pauseHotkey: String = "",
    val wallpaperPath: String = "",
    val wallpaperBlur: Float = 12f
) : UiState {
    enum class TimestampFormat { H12, H24, OFF }
    enum class EmoteSize { SMALL, MEDIUM, LARGE }
    enum class FontSize { SMALL, MEDIUM, LARGE }
    enum class ChannelNavigation { TAB_BAR, MINI_RAIL, BOTH }
}

sealed class SettingsEvent : UiEvent {
    data class OnDarkThemeChanged(val enabled: Boolean) : SettingsEvent()
    data class OnTimestampFormatChanged(val format: SettingsState.TimestampFormat) : SettingsEvent()
    data class OnShowDeletedChanged(val show: Boolean) : SettingsEvent()
    data class OnScrollbackLimitChanged(val limit: Int) : SettingsEvent()
    data class OnEmoteSizeChanged(val size: SettingsState.EmoteSize) : SettingsEvent()
    data class OnShowBadgesChanged(val show: Boolean) : SettingsEvent()
    data class OnFontSizeChanged(val size: SettingsState.FontSize) : SettingsEvent()
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
    data class OnWallpaperPathChanged(val path: String) : SettingsEvent()
    data class OnWallpaperBlurChanged(val blur: Float) : SettingsEvent()
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
        private const val KEY_PAUSE_ON_HOVER = "pause_on_hover"
        private const val KEY_PAUSE_HOTKEY = "pause_hotkey"
        private const val KEY_WALLPAPER_PATH = "wallpaper_path"
        private const val KEY_WALLPAPER_BLUR = "wallpaper_blur"
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
                confirmModActions = settings.getBoolean(KEY_CONFIRM_MOD, true),
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
                pauseOnHover = settings.getBoolean(KEY_PAUSE_ON_HOVER, false),
                pauseHotkey = settings.getStringOrNull(KEY_PAUSE_HOTKEY) ?: "",
                wallpaperPath = settings.getStringOrNull(KEY_WALLPAPER_PATH) ?: "",
                wallpaperBlur = settings.getFloat(KEY_WALLPAPER_BLUR, 12f)
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

            is SettingsEvent.OnHighlightRuleToggled -> {
                update { s ->
                    val n =
                        s.highlightRules.map { if (it.id == event.ruleId) it.copy(enabled = event.enabled) else it }; saveHighlightRules(
                    n
                ); s.copy(highlightRules = n)
                }
            }

            is SettingsEvent.OnHighlightRuleSoundToggled -> {
                update { s ->
                    val n =
                        s.highlightRules.map { if (it.id == event.ruleId) it.copy(playSound = event.playSound) else it }; saveHighlightRules(
                    n
                ); s.copy(highlightRules = n)
                }
            }

            is SettingsEvent.OnHighlightRuleColorChanged -> {
                update { s ->
                    val n =
                        s.highlightRules.map { if (it.id == event.ruleId) it.copy(color = event.color) else it }; saveHighlightRules(
                    n
                ); s.copy(highlightRules = n)
                }
            }

            is SettingsEvent.OnAddHighlightRule -> {
                update { s ->
                    val rule = HighlightRule(
                        id = "custom_${Clock.System.now().toEpochMilliseconds()}",
                        pattern = event.pattern,
                        playSound = true,
                        showInMentions = true
                    );
                    val n =
                        s.highlightRules + rule; saveHighlightRules(n); s.copy(highlightRules = n)
                }
            }

            is SettingsEvent.OnRemoveHighlightRule -> {
                update { s ->
                    val n =
                        s.highlightRules.filter { it.id != event.ruleId }; saveHighlightRules(n); s.copy(
                    highlightRules = n
                )
                }
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

            is SettingsEvent.OnWallpaperPathChanged -> {
                settings.putString(KEY_WALLPAPER_PATH, event.path)

                val wallpaper = loadWallpaper(event.path, state.value.wallpaperBlur)

                update {
                    it.copy(wallpaperPath = event.path)
                }

                wallpaper?.let {
                    sendEffect(SettingsEffect.WallpaperChanged(it))
                }
            }

            is SettingsEvent.OnWallpaperBlurChanged -> {
                settings.putFloat(KEY_WALLPAPER_BLUR, event.blur)

                val wallpaper = loadWallpaper(state.value.wallpaperPath, event.blur)

                update {
                    it.copy(wallpaperBlur = event.blur)
                }

                wallpaper?.let {
                    sendEffect(SettingsEffect.WallpaperChanged(it))
                }
            }
        }
    }

    private fun loadWallpaper(path: String, blur: Float): WallpaperState? {
        return wallpaperLoader.load(path, blur)
    }

    private fun saveHighlightRules(rules: List<HighlightRule>) {
        settings.putString(KEY_HIGHLIGHT_RULES, json.encodeToString(rules))
    }
}