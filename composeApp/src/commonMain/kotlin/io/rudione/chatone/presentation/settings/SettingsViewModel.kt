package io.rudione.chatone.presentation.settings

import io.rudione.chatone.presentation.theme.i18n.applyAppLocale
import androidx.lifecycle.viewModelScope
import com.russhwolf.settings.Settings
import io.github.aakira.napier.Napier
import io.rudione.chatone.base.BaseViewModel
import io.rudione.chatone.data.remote.TwitchApiClient
import io.rudione.chatone.data.repository.AuthRepository
import io.rudione.chatone.domain.model.ChatAutomation
import io.rudione.chatone.domain.model.ChatCommand
import io.rudione.chatone.domain.model.ChatoneColorTokens
import io.rudione.chatone.domain.model.HighlightRule
import io.rudione.chatone.domain.model.ImageUploaderConfig
import io.rudione.chatone.domain.model.Macro
import io.rudione.chatone.domain.model.ModActionButton
import io.rudione.chatone.domain.model.ModSliderSettings
import io.rudione.chatone.domain.model.ScreenChatSettings
import io.rudione.chatone.domain.usecase.GetFirstValidAccountUseCase
import io.rudione.chatone.presentation.theme.ACCENT_PALETTE_SCHEMA_VERSION
import io.rudione.chatone.presentation.theme.CustomThemeConfig
import io.rudione.chatone.presentation.theme.DEFAULT_ACCENT_INDEX
import io.rudione.chatone.presentation.theme.ExpressivePalettes
import io.rudione.chatone.presentation.theme.TWITCH_PALETTE_INDEX
import io.rudione.chatone.presentation.theme.WallpaperDisplayConfig
import io.rudione.chatone.util.Result
import io.rudione.chatone.util.system.AppDataCleaner
import io.rudione.chatone.util.system.AppRestarter
import io.rudione.chatone.util.system.isDesktopPlatform
import io.rudione.chatone.util.security.SecretVault
import io.rudione.chatone.util.security.getSecret
import io.rudione.chatone.util.security.putSecret
import io.rudione.chatone.util.settings.SettingsImportExport
import io.rudione.chatone.util.system.defaultHotkeys
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.coroutines.flow.merge

class SettingsViewModel(
    private val authRepository: AuthRepository,
    private val getFirstValidAccountUseCase: GetFirstValidAccountUseCase,
    private val twitchApiClient: TwitchApiClient? = null
) : BaseViewModel<SettingsState, SettingsEvent, SettingsEffect>(loadInitialState()) {
    companion object {
        val settings: Settings = Settings()

        private const val TWITCH_PALETTE_SCHEMA_BEFORE = 4
        private val json = Json { ignoreUnknownKeys = true }
        private val _effects = MutableSharedFlow<SettingsEffect>()
        val effects = _effects.asSharedFlow()

        private val _changeBroadcast = MutableSharedFlow<Long>(extraBufferCapacity = 16)
        val changeBroadcast = _changeBroadcast.asSharedFlow()
        private fun emitChange() {
            _changeBroadcast.tryEmit(Clock.System.now().toEpochMilliseconds())
        }

        private val _modButtonsLive = MutableStateFlow<List<ModActionButton>?>(null)

        val modButtonsLive: StateFlow<List<ModActionButton>?> = _modButtonsLive.asStateFlow()

        private val _macrosLive = MutableStateFlow<List<Macro>?>(null)
        val macrosLive: StateFlow<List<Macro>?> = _macrosLive.asStateFlow()

        fun currentLanguage(): String =
            settings.getString(SettingsKeys.LANGUAGE, "en").also(::applyAppLocale)

        fun isTrustedUploaderHost(host: String): Boolean =
            settings.getStringOrNull(SettingsKeys.IMAGE_UPLOADER_TRUSTED_HOST)
                .equals(host, ignoreCase = true)

        fun trustUploaderHost(host: String) {
            settings.putString(SettingsKeys.IMAGE_UPLOADER_TRUSTED_HOST, host.lowercase())
        }

        fun notifyExternalChange() {
            if (_modButtonsLive.value != null) {
                _modButtonsLive.value = runCatching {
                    settings.getStringOrNull(SettingsKeys.ALL_MOD_BUTTONS)
                        ?.let { json.decodeFromString<List<ModActionButton>>(it) }
                }.getOrNull()
            }
            if (_macrosLive.value != null) {
                _macrosLive.value = runCatching {
                    settings.getStringOrNull(SettingsKeys.MACROS)
                        ?.let { json.decodeFromString<List<Macro>>(it) }
                }.getOrNull()
            }
            _changeBroadcast.tryEmit(-Clock.System.now().toEpochMilliseconds())
        }

        private fun migrateMobileNavigationDefault() {
            if (isDesktopPlatform || settings.getBoolean(
                    SettingsKeys.MOBILE_NAVIGATION_DEFAULT,
                    false
                )
            ) return
            settings.putBoolean(SettingsKeys.NAVIGATION_HIDDEN, true)
            settings.putBoolean(SettingsKeys.MOBILE_NAVIGATION_DEFAULT, true)
        }

        private fun migrateAccentPalette() {
            val stored = settings.getInt(SettingsKeys.ACCENT_PALETTE_SCHEMA, 0)
            if (stored >= ACCENT_PALETTE_SCHEMA_VERSION) return
            if (stored == TWITCH_PALETTE_SCHEMA_BEFORE) {
                val index = settings.getInt(SettingsKeys.ACCENT_COLOR_INDEX, DEFAULT_ACCENT_INDEX)
                if (index >= TWITCH_PALETTE_INDEX) settings.putInt(
                    SettingsKeys.ACCENT_COLOR_INDEX,
                    index + 1
                )
            } else {
                settings.putInt(SettingsKeys.ACCENT_COLOR_INDEX, DEFAULT_ACCENT_INDEX)
                settings.putString(SettingsKeys.ACTIVE_THEME_ID, "")
            }
            settings.putInt(SettingsKeys.ACCENT_PALETTE_SCHEMA, ACCENT_PALETTE_SCHEMA_VERSION)
        }

        fun loadInitialState(): SettingsState {
            migrateAccentPalette()
            migrateMobileNavigationDefault()
            val rules = try {
                val j = settings.getStringOrNull(SettingsKeys.HIGHLIGHT_RULES)
                if (j != null) json.decodeFromString<List<HighlightRule>>(j) else null
            } catch (_: Exception) {
                null
            }
            val modButtons = try {
                val j = settings.getStringOrNull(SettingsKeys.CUSTOM_MOD_BUTTONS)
                if (j != null) json.decodeFromString<List<ModActionButton>>(j) else emptyList()
            } catch (_: Exception) {
                emptyList()
            }
            val allModBtns = try {
                val j = settings.getStringOrNull(SettingsKeys.ALL_MOD_BUTTONS)
                if (j != null) json.decodeFromString<List<ModActionButton>>(j)
                else null
            } catch (_: Exception) {
                null
            }
            val macros = try {
                val j = settings.getStringOrNull(SettingsKeys.MACROS)
                if (j != null) json.decodeFromString<List<Macro>>(j) else emptyList()
            } catch (_: Exception) {
                emptyList()
            }

            val automations = try {
                val j = settings.getStringOrNull(SettingsKeys.AUTOMATIONS)
                if (j != null) json.decodeFromString<List<ChatAutomation>>(j)
                else emptyList()
            } catch (_: Exception) {
                emptyList()
            }
            val mutedPhrases = try {
                val j = settings.getStringOrNull(SettingsKeys.MUTED_PHRASES)
                if (j != null) json.decodeFromString<List<String>>(j) else emptyList()
            } catch (_: Exception) {
                emptyList()
            }

            val chatCommands = try {
                val j = settings.getStringOrNull(SettingsKeys.CHAT_COMMANDS)
                if (j != null) json.decodeFromString<List<ChatCommand>>(j) else emptyList()
            } catch (_: Exception) {
                emptyList()
            }

            val customThemes = try {
                val j = settings.getStringOrNull(SettingsKeys.CUSTOM_THEMES)
                if (j != null) json.decodeFromString<List<CustomThemeConfig>>(j) else emptyList()
            } catch (_: Exception) {
                emptyList()
            }
            val colorTokens = try {
                val j = settings.getStringOrNull(SettingsKeys.COLOR_TOKENS)
                if (j != null) json.decodeFromString<ChatoneColorTokens>(j) else ChatoneColorTokens()
            } catch (_: Exception) {
                ChatoneColorTokens()
            }
            val activeThemeId =
                settings.getStringOrNull(SettingsKeys.ACTIVE_THEME_ID)?.takeIf { it.isNotBlank() }
            val activeCustomTheme = activeThemeId?.let { id -> customThemes.find { it.id == id } }
            val wallpaperDisplayConfig = WallpaperDisplayConfig.fromJson(
                settings.getStringOrNull(WallpaperDisplayConfig.SETTINGS_KEY)
            )

            return SettingsState(
                darkTheme = settings.getBoolean(SettingsKeys.DARK_THEME, true),
                timestampFormat = SettingsState.TimestampFormat.entries.getOrNull(
                    settings.getInt(
                        SettingsKeys.TIMESTAMP_FORMAT,
                        1
                    )
                ) ?: SettingsState.TimestampFormat.H24,
                showDeletedMessages = settings.getBoolean(SettingsKeys.SHOW_DELETED, true),
                showViewerJoinLeave = settings.getBoolean(SettingsKeys.SHOW_VIEWER_JOIN_LEAVE, false),
                imageUploader = try {
                    settings.getStringOrNull(SettingsKeys.IMAGE_UPLOADER)
                        ?.let { json.decodeFromString<ImageUploaderConfig>(it) }
                        ?.opened(SecretVault::open)
                        ?.withStandardService()
                        ?: ImageUploaderConfig()
                } catch (_: Exception) {
                    ImageUploaderConfig()
                },
                scrollbackLimit = settings.getInt(SettingsKeys.SCROLLBACK_LIMIT, 500),
                emoteSize = SettingsState.EmoteSize.entries.getOrNull(
                    settings.getInt(
                        SettingsKeys.EMOTE_SIZE,
                        1
                    )
                ) ?: SettingsState.EmoteSize.MEDIUM,
                showBadges = settings.getBoolean(SettingsKeys.SHOW_BADGES, true),
                fontSize = SettingsState.FontSize.entries.getOrNull(
                    settings.getInt(
                        SettingsKeys.FONT_SIZE,
                        1
                    )
                ) ?: SettingsState.FontSize.MEDIUM,
                defaultTimeoutDuration = settings.getInt(SettingsKeys.DEFAULT_TIMEOUT, 600),
                confirmModActions = settings.getBoolean(SettingsKeys.CONFIRM_MOD, false),
                channelNavigation = SettingsState.ChannelNavigation.entries.getOrNull(
                    settings.getInt(
                        SettingsKeys.CHANNEL_NAV,
                        0
                    )
                ) ?: SettingsState.ChannelNavigation.TAB_BAR,
                highlightRules = run {
                    val defaults = SettingsState().highlightRules
                    val loaded = (rules ?: defaults).filter { it.id != "mention_accent" }
                    val loadedIds = loaded.map { it.id }.toSet()
                    HighlightRule.swapLegacyDefaultColors(
                        loaded + defaults.filter { it.id !in loadedIds }
                    )
                },
                mentionSoundEnabled = settings.getBoolean(SettingsKeys.MENTION_SOUND, true),
                mentionSoundVolume = settings.getFloat(SettingsKeys.MENTION_VOLUME, 0.8f),
                alwaysOnTop = settings.getBoolean(SettingsKeys.ALWAYS_ON_TOP, false),
                alwaysOnTopMode = AlwaysOnTopMode.entries.firstOrNull {
                    it.name == settings.getString(
                        SettingsKeys.ALWAYS_ON_TOP_MODE,
                        AlwaysOnTopMode.EXCEPT_SYSTEM.name
                    )
                } ?: AlwaysOnTopMode.EXCEPT_SYSTEM,
                hideSidebar = settings.getBoolean(SettingsKeys.HIDE_SIDEBAR, false),
                uiScale = settings.getFloat(SettingsKeys.UI_SCALE, 1.0f),
                language = settings.getString(SettingsKeys.LANGUAGE, "en"),
                pauseOnHover = settings.getBoolean(SettingsKeys.PAUSE_ON_HOVER, false),
                pauseHotkey = settings.getStringOrNull(SettingsKeys.PAUSE_HOTKEY) ?: "",
                pauseHotkeyMode = PauseHotkeyMode.entries.getOrNull(
                    settings.getInt(SettingsKeys.PAUSE_HOTKEY_MODE, 0)
                ) ?: PauseHotkeyMode.TOGGLE,
                showInlineImages = InlineImageMode.entries.getOrNull(
                    settings.getInt(SettingsKeys.SHOW_INLINE_IMAGES, 0)
                ) ?: InlineImageMode.ON,
                inlineImageMaxHeight = settings.getInt(SettingsKeys.INLINE_IMAGE_MAX_HEIGHT, 200),
                showChatGifs = settings.getBoolean(SettingsKeys.SHOW_CHAT_GIFS, true),
                giphyApiKey = settings.getSecret(SettingsKeys.GIPHY_API_KEY),
                clipPreviewWidth = settings.getInt(SettingsKeys.CLIP_PREVIEW_WIDTH, 140),
                chatScrollbarWidth = settings.getInt(SettingsKeys.CHAT_SCROLLBAR_WIDTH, 12),
                chatTopBarHeight = settings.getInt(SettingsKeys.CHAT_TOP_BAR_HEIGHT, 36).coerceIn(28, 64),
                wallpaperPath = settings.getStringOrNull(SettingsKeys.WALLPAPER_PATH) ?: "",
                wallpaperDisplayConfig = wallpaperDisplayConfig,
                closeEmotePickerOnMouseLeave = settings.getBoolean(
                    SettingsKeys.EMOTE_PICKER_MOUSE_LEAVE,
                    false
                ),
                customModButtons = modButtons,
                macros = macros,
                automations = automations,
                mutedPhrases = mutedPhrases,
                autoClaimPoints = settings.getBoolean(SettingsKeys.AUTO_CLAIM_POINTS, false),
                showChatHeader = settings.getBoolean(SettingsKeys.SHOW_CHAT_HEADER, true),
                smoothChatEnabled = settings.getBoolean(SettingsKeys.SMOOTH_CHAT, false),
                alternateRowBackground = settings.getBoolean(SettingsKeys.ALTERNATE_ROW_BG, false),
                readableNickColors = settings.getBoolean(SettingsKeys.READABLE_NICK_COLORS, true),
                showDefaultDeleteButton = settings.getBoolean(SettingsKeys.SHOW_DEFAULT_DELETE, true),
                showDefaultTimeoutButton = settings.getBoolean(SettingsKeys.SHOW_DEFAULT_TIMEOUT, true),
                showDefaultBanButton = settings.getBoolean(SettingsKeys.SHOW_DEFAULT_BAN, true),
                modButtonsOnHover = settings.getBoolean(SettingsKeys.MOD_BUTTONS_ON_HOVER, false),
                modButtonsInChat = settings.getBoolean(SettingsKeys.MOD_BUTTONS_IN_CHAT, true),
                modSlider = runCatching {
                    settings.getStringOrNull(SettingsKeys.MOD_SLIDER)
                        ?.let { json.decodeFromString<ModSliderSettings>(it).sanitized() }
                }.getOrNull() ?: ModSliderSettings(),
                screenChat = runCatching {
                    settings.getStringOrNull(SettingsKeys.SCREEN_CHAT)
                        ?.let { json.decodeFromString<ScreenChatSettings>(it).sanitized() }
                }.getOrNull() ?: ScreenChatSettings(),
                disableScrollOnAlt = settings.getBoolean(SettingsKeys.DISABLE_SCROLL_ON_ALT, true),
                linkOpenMode = try {
                    SettingsState.LinkOpenMode.valueOf(
                        settings.getString(
                            SettingsKeys.LINK_OPEN_MODE,
                            SettingsState.LinkOpenMode.DEFAULT.name
                        )
                    )
                } catch (_: Exception) {
                    SettingsState.LinkOpenMode.DEFAULT
                },
                accentColorIndex = settings.getInt(SettingsKeys.ACCENT_COLOR_INDEX, DEFAULT_ACCENT_INDEX)
                    .coerceIn(0, ExpressivePalettes.lastIndex),
                allModButtons = allModBtns ?: run {
                    val defaults = ModActionButton.defaultOrderedList()
                    val combined = (defaults + modButtons).distinctBy { it.id }
                    combined.mapIndexed { i, b -> b.copy(sortOrder = i) }
                },
                customThemes = customThemes,
                activeCustomThemeId = activeThemeId,
                activeCustomTheme = activeCustomTheme,
                fontFamilyName = settings.getString(SettingsKeys.FONT_FAMILY, "Inter"),
                fontStyleItalic = settings.getBoolean(SettingsKeys.FONT_ITALIC, false),
                fontUnderline = settings.getBoolean(SettingsKeys.FONT_UNDERLINE, false),
                fontStrikethrough = settings.getBoolean(SettingsKeys.FONT_STRIKETHROUGH, false),
                customFontPaths = try {
                    val j = settings.getStringOrNull(SettingsKeys.CUSTOM_FONT_PATHS)
                    if (j != null) json.decodeFromString<List<String>>(j) else emptyList()
                } catch (_: Exception) {
                    emptyList()
                },
                messageSpacing = SettingsState.MessageSpacing.entries.getOrNull(
                    settings.getInt(SettingsKeys.MESSAGE_SPACING, SettingsState.MessageSpacing.LOW.ordinal)
                ) ?: SettingsState.MessageSpacing.LOW,
                titleBarMode = try {
                    TitleBarMode.valueOf(
                        settings.getString(
                            SettingsKeys.TITLE_BAR_MODE,
                            TitleBarMode.ADAPTIVE.name
                        )
                    )
                } catch (_: Exception) {
                    TitleBarMode.ADAPTIVE
                },
                showBlockedMode = settings.getInt(SettingsKeys.SHOW_BLOCKED_MODE, 0),
                hideChatInputPlaceholder = settings.getBoolean(SettingsKeys.HIDE_CHAT_PLACEHOLDER, false),
                hideEmojiButton = settings.getBoolean(SettingsKeys.HIDE_EMOJI_BUTTON, false),
                chatInputEventGlow = settings.getBoolean(SettingsKeys.CHAT_INPUT_GLOW, true),
                showRepeatedMessageCounter = settings.getBoolean(SettingsKeys.SHOW_REPEAT_COUNTER, false),
                repeatedMessageWindow = settings.getInt(SettingsKeys.REPEAT_WINDOW, 30),
                savedTimeoutReason = settings.getString(SettingsKeys.SAVED_TIMEOUT_REASON, ""),
                savedBanReason = settings.getString(SettingsKeys.SAVED_BAN_REASON, ""),
                chatCommands = chatCommands,
                colorTokens = colorTokens,
                translationTargetLang = settings.getString(SettingsKeys.TRANSLATION_LANG, "en"),
                autoTranslateInput = settings.getBoolean(SettingsKeys.AUTO_TRANSLATE_INPUT, false),
                outgoingTranslationLang = settings.getStringOrNull(SettingsKeys.OUTGOING_TRANSLATION_LANG)
                    ?: settings.getString(SettingsKeys.TRANSLATION_LANG, "en"),
                mentionTabsEnabled = settings.getBoolean(SettingsKeys.MENTION_TABS, false),
                navigationHidden = settings.getBoolean(SettingsKeys.NAVIGATION_HIDDEN, false),
                hotkeys = try {
                    val j = settings.getStringOrNull(SettingsKeys.HOTKEYS)
                    val stored =
                        if (j != null) json.decodeFromString<Map<String, String>>(j) else emptyMap()
                    defaultHotkeys() + stored
                } catch (_: Exception) {
                    defaultHotkeys()
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
            merge(
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
                        themeCreatorSection = current.themeCreatorSection,
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
                    SettingsKeys.DARK_THEME,
                    event.enabled
                ); update { it.copy(darkTheme = event.enabled) }
            }

            is SettingsEvent.OnActiveCustomThemeIdChanged -> {
                settings.putString(SettingsKeys.ACTIVE_THEME_ID, event.themeId.orEmpty())
                update { s ->
                    s.copy(
                        activeCustomThemeId = event.themeId,
                        activeCustomTheme = event.themeId?.let { id -> s.customThemes.find { it.id == id } }
                    )
                }
            }

            is SettingsEvent.OnCustomThemesJsonChanged -> {
                settings.putString("custom_themes_json", event.json)
                update { it.copy(customThemesJson = event.json) }
            }

            is SettingsEvent.OnShowChatHeaderChanged -> {
                settings.putBoolean(SettingsKeys.SHOW_CHAT_HEADER, event.show)
                update { it.copy(showChatHeader = event.show) }
            }

            is SettingsEvent.OnTimestampFormatChanged -> {
                settings.putInt(SettingsKeys.TIMESTAMP_FORMAT, event.format.ordinal); update {
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
                update {
                    it.copy(
                        showThemeCreator = true,
                        themeCreatorSeedColor = event.seedColor,
                        themeCreatorSection = event.section
                    )
                }
            }

            is SettingsEvent.OnCloseThemeCreator -> {
                update { it.copy(showThemeCreator = false) }
            }

            is SettingsEvent.OnApplyCustomTheme -> {
                saveCustomThemes(state.value.customThemes, event.themeId)
                update { it.copy(activeCustomThemeId = event.themeId) }
            }

            is SettingsEvent.OnImportSettingsText -> {
                val backup = SettingsImportExport.fromJson(event.text)
                if (backup != null) {
                    SettingsImportExport.applyUntrusted(settings, backup)
                    update { loadInitialState() }
                }
            }

            is SettingsEvent.OnCustomThemeApplied -> {
                settings.putString(SettingsKeys.ACTIVE_THEME_ID, event.theme?.id ?: "")

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
                settings.putBoolean(SettingsKeys.SHOW_DELETED, event.show); update {
                    it.copy(
                        showDeletedMessages = event.show
                    )
                }
            }

            is SettingsEvent.OnImageUploaderChanged -> {
                settings.putString(
                    SettingsKeys.IMAGE_UPLOADER,
                    json.encodeToString(
                        event.config.sealed(SecretVault::seal)
                    )
                )
                update { it.copy(imageUploader = event.config) }
            }

            is SettingsEvent.OnShowViewerJoinLeaveChanged -> {
                settings.putBoolean(SettingsKeys.SHOW_VIEWER_JOIN_LEAVE, event.show); update {
                    it.copy(
                        showViewerJoinLeave = event.show
                    )
                }
            }

            is SettingsEvent.OnScrollbackLimitChanged -> {
                settings.putInt(
                    SettingsKeys.SCROLLBACK_LIMIT,
                    event.limit
                ); update { it.copy(scrollbackLimit = event.limit) }
            }

            is SettingsEvent.OnEmoteSizeChanged -> {
                settings.putInt(
                    SettingsKeys.EMOTE_SIZE,
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
                    SettingsKeys.SHOW_BADGES,
                    event.show
                ); update { it.copy(showBadges = event.show) }
            }

            is SettingsEvent.OnUiScaleChanged -> {
                settings.putFloat(SettingsKeys.UI_SCALE, event.scale.coerceIn(0.7f, 2.0f))
                update { it.copy(uiScale = event.scale.coerceIn(0.7f, 2.0f)) }
            }

            is SettingsEvent.OnLanguageChanged -> {
                settings.putString(SettingsKeys.LANGUAGE, event.code)
                applyAppLocale(event.code)
                update { it.copy(language = event.code) }
            }

            is SettingsEvent.OnFontFamilyChanged -> {
                settings.putString(SettingsKeys.FONT_FAMILY, event.name)
                update { it.copy(fontFamilyName = event.name) }
            }

            is SettingsEvent.OnFontItalicChanged -> {
                settings.putBoolean(SettingsKeys.FONT_ITALIC, event.italic)
                update { it.copy(fontStyleItalic = event.italic) }
            }

            is SettingsEvent.OnFontUnderlineChanged -> {
                settings.putBoolean(SettingsKeys.FONT_UNDERLINE, event.underline)
                update { it.copy(fontUnderline = event.underline) }
            }

            is SettingsEvent.OnFontStrikethroughChanged -> {
                settings.putBoolean(SettingsKeys.FONT_STRIKETHROUGH, event.strikethrough)
                update { it.copy(fontStrikethrough = event.strikethrough) }
            }

            is SettingsEvent.OnAddCustomFontPath -> {
                val updated = (state.value.customFontPaths + event.path).distinct()
                settings.putString(SettingsKeys.CUSTOM_FONT_PATHS, json.encodeToString(updated))
                update { it.copy(customFontPaths = updated) }
            }

            is SettingsEvent.OnRemoveCustomFontPath -> {
                val updated = state.value.customFontPaths.filter { it != event.path }
                settings.putString(SettingsKeys.CUSTOM_FONT_PATHS, json.encodeToString(updated))
                update { it.copy(customFontPaths = updated) }
            }

            is SettingsEvent.OnMessageSpacingChanged -> {
                settings.putInt(SettingsKeys.MESSAGE_SPACING, event.spacing.ordinal)
                update { it.copy(messageSpacing = event.spacing) }
            }

            is SettingsEvent.OnTitleBarModeChanged -> {
                settings.putString(SettingsKeys.TITLE_BAR_MODE, event.mode.name)
                update { it.copy(titleBarMode = event.mode) }
            }

            is SettingsEvent.OnShowBlockedModeChanged -> {
                settings.putInt(SettingsKeys.SHOW_BLOCKED_MODE, event.mode)
                update { it.copy(showBlockedMode = event.mode) }
            }

            is SettingsEvent.OnLoadBlockedUsers -> {
                update { it.copy(isLoadingBlockedUsers = true, blockedLoadError = null) }
                viewModelScope.launch {
                    try {
                        val api = twitchApiClient
                        if (api == null) {
                            update {
                                it.copy(
                                    isLoadingBlockedUsers = false,
                                    blockedLoadError = "API not available"
                                )
                            }
                            return@launch
                        }
                        val accounts = authRepository.getAccounts().first()
                        if (accounts.isEmpty()) {
                            update {
                                it.copy(
                                    isLoadingBlockedUsers = false,
                                    blockedLoadError = "Not logged in"
                                )
                            }
                            return@launch
                        }
                        val aggregated = linkedSetOf<String>()
                        var anySuccess = false
                        var lastError: String? = null
                        for (account in accounts) {
                            val validatedUserId = runCatching {
                                val v = api.validateToken(account.accessToken)
                                if (v is Result.Success) v.data.userId else account.userId
                            }.getOrDefault(account.userId)
                            val result = api.getBlockedUsers(
                                account.accessToken,
                                validatedUserId,
                                first = 100
                            )
                            when (result) {
                                is Result.Success -> {
                                    anySuccess = true
                                    result.data.data.forEach { aggregated.add(it.userLogin) }
                                }

                                is Result.Error -> {
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
                        update {
                            it.copy(
                                isLoadingBlockedUsers = false,
                                blockedLoadError = e.message ?: "Failed to load"
                            )
                        }
                    }
                }
            }

            is SettingsEvent.BlockedUsersLoaded -> {
                update {
                    it.copy(
                        blockedUsernames = event.usernames,
                        isLoadingBlockedUsers = false
                    )
                }
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
                            val r =
                                api.getUsers(resolver.accessToken, logins = listOf(event.username))
                            if (r is Result.Success) {
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
                    SettingsKeys.FONT_SIZE,
                    event.size.ordinal
                ); update { it.copy(fontSize = event.size) }
            }

            is SettingsEvent.OnDefaultTimeoutChanged -> {
                settings.putInt(SettingsKeys.DEFAULT_TIMEOUT, event.duration); update {
                    it.copy(
                        defaultTimeoutDuration = event.duration
                    )
                }
            }

            is SettingsEvent.OnConfirmModActionsChanged -> {
                settings.putBoolean(SettingsKeys.CONFIRM_MOD, event.confirm); update {
                    it.copy(
                        confirmModActions = event.confirm
                    )
                }
            }

            is SettingsEvent.OnChannelNavigationChanged -> {
                settings.putInt(SettingsKeys.CHANNEL_NAV, event.navigation.ordinal); update {
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
                settings.putBoolean(SettingsKeys.HIDE_CHAT_PLACEHOLDER, event.hide)
                update { it.copy(hideChatInputPlaceholder = event.hide) }
            }

            is SettingsEvent.OnHideEmojiButtonChanged -> {
                settings.putBoolean(SettingsKeys.HIDE_EMOJI_BUTTON, event.hide)
                update { it.copy(hideEmojiButton = event.hide) }
            }

            is SettingsEvent.OnChatInputGlowChanged -> {
                settings.putBoolean(SettingsKeys.CHAT_INPUT_GLOW, event.enabled)
                update { it.copy(chatInputEventGlow = event.enabled) }
            }

            is SettingsEvent.OnShowRepeatedCounterChanged -> {
                settings.putBoolean(SettingsKeys.SHOW_REPEAT_COUNTER, event.show)
                update { it.copy(showRepeatedMessageCounter = event.show) }
            }

            is SettingsEvent.OnRepeatedWindowChanged -> {
                val v = event.seconds.coerceIn(5, 300)
                settings.putInt(SettingsKeys.REPEAT_WINDOW, v)
                update { it.copy(repeatedMessageWindow = v) }
            }

            is SettingsEvent.OnSavedTimeoutReasonChanged -> {
                settings.putString(SettingsKeys.SAVED_TIMEOUT_REASON, event.text)
                update { it.copy(savedTimeoutReason = event.text) }
            }

            is SettingsEvent.OnSavedBanReasonChanged -> {
                settings.putString(SettingsKeys.SAVED_BAN_REASON, event.text)
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
                settings.putBoolean(SettingsKeys.MENTION_SOUND, event.enabled); update {
                    it.copy(
                        mentionSoundEnabled = event.enabled
                    )
                }
            }

            is SettingsEvent.OnMentionSoundVolumeChanged -> {
                settings.putFloat(SettingsKeys.MENTION_VOLUME, event.volume); update {
                    it.copy(
                        mentionSoundVolume = event.volume
                    )
                }
            }

            is SettingsEvent.OnAlwaysOnTopChanged -> {
                settings.putBoolean(
                    SettingsKeys.ALWAYS_ON_TOP,
                    event.enabled
                ); update { it.copy(alwaysOnTop = event.enabled) }
            }

            is SettingsEvent.OnAlwaysOnTopModeChanged -> {
                settings.putString(SettingsKeys.ALWAYS_ON_TOP_MODE, event.mode.name)
                update { it.copy(alwaysOnTopMode = event.mode) }
            }

            is SettingsEvent.OnHideSidebarChanged -> {
                settings.putBoolean(SettingsKeys.HIDE_SIDEBAR, event.enabled)
                update { it.copy(hideSidebar = event.enabled) }
            }

            is SettingsEvent.OnPauseOnHoverChanged -> {
                settings.putBoolean(SettingsKeys.PAUSE_ON_HOVER, event.enabled); update {
                    it.copy(
                        pauseOnHover = event.enabled
                    )
                }
            }

            is SettingsEvent.OnPauseHotkeyChanged -> {
                settings.putString(
                    SettingsKeys.PAUSE_HOTKEY,
                    event.hotkey
                ); update { it.copy(pauseHotkey = event.hotkey) }
            }

            is SettingsEvent.OnPauseHotkeyModeChanged -> {
                settings.putInt(SettingsKeys.PAUSE_HOTKEY_MODE, event.mode.ordinal)
                update { it.copy(pauseHotkeyMode = event.mode) }
            }

            is SettingsEvent.OnShowInlineImagesChanged -> {
                settings.putInt(SettingsKeys.SHOW_INLINE_IMAGES, event.mode.ordinal)
                update { it.copy(showInlineImages = event.mode) }
            }

            is SettingsEvent.OnInlineImageMaxHeightChanged -> {
                settings.putInt(SettingsKeys.INLINE_IMAGE_MAX_HEIGHT, event.height.coerceIn(50, 500))
                update { it.copy(inlineImageMaxHeight = event.height.coerceIn(50, 500)) }
            }

            is SettingsEvent.OnShowChatGifsChanged -> {
                settings.putBoolean(SettingsKeys.SHOW_CHAT_GIFS, event.enabled)
                update { it.copy(showChatGifs = event.enabled) }
            }

            is SettingsEvent.OnGiphyApiKeyChanged -> {
                val key = event.key.trim()
                settings.putSecret(SettingsKeys.GIPHY_API_KEY, key)
                update { it.copy(giphyApiKey = key) }
            }

            is SettingsEvent.OnClipPreviewWidthChanged -> {
                val w = event.width.coerceIn(90, 320)
                settings.putInt(SettingsKeys.CLIP_PREVIEW_WIDTH, w)
                update { it.copy(clipPreviewWidth = w) }
            }

            is SettingsEvent.OnChatScrollbarWidthChanged -> {
                val w = event.width.coerceIn(6, 32)
                settings.putInt(SettingsKeys.CHAT_SCROLLBAR_WIDTH, w)
                update { it.copy(chatScrollbarWidth = w) }
            }

            is SettingsEvent.OnChatTopBarHeightChanged -> {
                val h = event.height.coerceIn(28, 64)
                settings.putInt(SettingsKeys.CHAT_TOP_BAR_HEIGHT, h)
                update { it.copy(chatTopBarHeight = h) }
            }

            is SettingsEvent.OnAddAutomation -> update { st ->
                val n = st.automations + event.automation
                settings.putString(SettingsKeys.AUTOMATIONS, json.encodeToString(n))
                st.copy(automations = n)
            }

            is SettingsEvent.OnRemoveAutomation -> update { st ->
                val n = st.automations.filter { it.id != event.id }
                settings.putString(SettingsKeys.AUTOMATIONS, json.encodeToString(n))
                st.copy(automations = n)
            }

            is SettingsEvent.OnToggleAutomation -> update { st ->
                val n =
                    st.automations.map { if (it.id == event.id) it.copy(enabled = event.enabled) else it }
                settings.putString(SettingsKeys.AUTOMATIONS, json.encodeToString(n))
                st.copy(automations = n)
            }

            is SettingsEvent.OnUpdateAutomation -> update { st ->
                val n =
                    st.automations.map { if (it.id == event.automation.id) event.automation else it }
                settings.putString(SettingsKeys.AUTOMATIONS, json.encodeToString(n))
                st.copy(automations = n)
            }

            is SettingsEvent.OnAutoClaimPointsChanged -> {
                settings.putBoolean(SettingsKeys.AUTO_CLAIM_POINTS, event.enabled)
                update { it.copy(autoClaimPoints = event.enabled) }
            }

            is SettingsEvent.OnAddMutedPhrase -> update { st ->
                val phrase = event.phrase.trim()
                if (phrase.isEmpty() || st.mutedPhrases.any {
                        it.equals(
                            phrase,
                            ignoreCase = true
                        )
                    }) st
                else {
                    val n = st.mutedPhrases + phrase
                    settings.putString(SettingsKeys.MUTED_PHRASES, json.encodeToString(n))
                    st.copy(mutedPhrases = n)
                }
            }

            is SettingsEvent.OnRemoveMutedPhrase -> update { st ->
                val n = st.mutedPhrases.filter { it != event.phrase }
                settings.putString(SettingsKeys.MUTED_PHRASES, json.encodeToString(n))
                st.copy(mutedPhrases = n)
            }

            is SettingsEvent.OnWallpaperPathChanged -> {
                settings.putString(SettingsKeys.WALLPAPER_PATH, event.path)
                update { it.copy(wallpaperPath = event.path) }
            }

            is SettingsEvent.OnWallpaperDisplayConfigChanged -> {
                settings.putString(
                    WallpaperDisplayConfig.SETTINGS_KEY,
                    WallpaperDisplayConfig.toJson(event.config)
                )
                update { it.copy(wallpaperDisplayConfig = event.config) }
            }

            is SettingsEvent.OnCloseEmotePickerOnMouseLeaveChanged -> {
                settings.putBoolean(SettingsKeys.EMOTE_PICKER_MOUSE_LEAVE, event.enabled); update {
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
                s.copy(
                    allModButtons = reordered,
                    customModButtons = custom,
                    modButtonsVersion = newVer
                )
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
                settings.putBoolean(SettingsKeys.SHOW_DEFAULT_DELETE, event.show)
                update {
                    it.copy(
                        showDefaultDeleteButton = event.show,
                        modButtonsVersion = it.modButtonsVersion + 1
                    )
                }
            }

            is SettingsEvent.OnShowDefaultTimeoutChanged -> {
                settings.putBoolean(SettingsKeys.SHOW_DEFAULT_TIMEOUT, event.show)
                update {
                    it.copy(
                        showDefaultTimeoutButton = event.show,
                        modButtonsVersion = it.modButtonsVersion + 1
                    )
                }
            }

            is SettingsEvent.OnShowDefaultBanChanged -> {
                settings.putBoolean(SettingsKeys.SHOW_DEFAULT_BAN, event.show)
                update {
                    it.copy(
                        showDefaultBanButton = event.show,
                        modButtonsVersion = it.modButtonsVersion + 1
                    )
                }
            }

            is SettingsEvent.OnModButtonsOnHoverChanged -> {
                settings.putBoolean(SettingsKeys.MOD_BUTTONS_ON_HOVER, event.enabled)
                update { it.copy(modButtonsOnHover = event.enabled) }
            }

            is SettingsEvent.OnModButtonsInChatChanged -> {
                settings.putBoolean(SettingsKeys.MOD_BUTTONS_IN_CHAT, event.enabled)
                update { it.copy(modButtonsInChat = event.enabled) }
            }

            is SettingsEvent.OnModSliderChanged -> {
                val sanitized = event.settings.sanitized()
                settings.putString(SettingsKeys.MOD_SLIDER, json.encodeToString(sanitized))
                update { it.copy(modSlider = sanitized) }
            }

            is SettingsEvent.OnScreenChatChanged -> {
                val sanitized = event.settings.sanitized()
                settings.putString(SettingsKeys.SCREEN_CHAT, json.encodeToString(sanitized))
                update { it.copy(screenChat = sanitized) }
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
                settings.putBoolean(SettingsKeys.SMOOTH_CHAT, event.enabled)
                update { it.copy(smoothChatEnabled = event.enabled) }
            }

            is SettingsEvent.OnAlternateRowBackgroundChanged -> {
                settings.putBoolean(SettingsKeys.ALTERNATE_ROW_BG, event.enabled)
                update { it.copy(alternateRowBackground = event.enabled) }
            }

            is SettingsEvent.OnReadableNickColorsChanged -> {
                settings.putBoolean(SettingsKeys.READABLE_NICK_COLORS, event.enabled)
                update { it.copy(readableNickColors = event.enabled) }
            }

            is SettingsEvent.OnDisableScrollOnAltChanged -> {
                settings.putBoolean(SettingsKeys.DISABLE_SCROLL_ON_ALT, event.enabled)
                update { it.copy(disableScrollOnAlt = event.enabled) }
            }

            is SettingsEvent.OnLinkOpenModeChanged -> {
                settings.putString(SettingsKeys.LINK_OPEN_MODE, event.mode.name)
                update { it.copy(linkOpenMode = event.mode) }
            }

            is SettingsEvent.OnAccentColorChanged -> {
                settings.putInt(SettingsKeys.ACCENT_COLOR_INDEX, event.index)
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
                settings.putString(SettingsKeys.COLOR_TOKENS, json.encodeToString(event.tokens))
                update { it.copy(colorTokens = event.tokens) }
            }

            is SettingsEvent.OnTranslationLangChanged -> {
                settings.putString(SettingsKeys.TRANSLATION_LANG, event.code)
                update { it.copy(translationTargetLang = event.code) }
            }

            is SettingsEvent.OnAutoTranslateInputChanged -> {
                settings.putBoolean(SettingsKeys.AUTO_TRANSLATE_INPUT, event.enabled)
                update { it.copy(autoTranslateInput = event.enabled) }
            }

            is SettingsEvent.OnOutgoingTranslationLangChanged -> {
                settings.putString(SettingsKeys.OUTGOING_TRANSLATION_LANG, event.code)
                update { it.copy(outgoingTranslationLang = event.code) }
            }

            is SettingsEvent.OnMentionTabsChanged -> {
                settings.putBoolean(SettingsKeys.MENTION_TABS, event.enabled)
                update { it.copy(mentionTabsEnabled = event.enabled) }
            }

            is SettingsEvent.OnNavigationHiddenChanged -> {
                settings.putBoolean(SettingsKeys.NAVIGATION_HIDDEN, event.hidden)
                update { it.copy(navigationHidden = event.hidden) }
            }

            is SettingsEvent.OnHotkeyChanged -> {
                val updated =
                    state.value.hotkeys.toMutableMap().apply { put(event.actionId, event.combo) }
                settings.putString(SettingsKeys.HOTKEYS, json.encodeToString(updated))
                update { it.copy(hotkeys = updated) }
            }
        }
    }

    private fun saveChatCommands(commands: List<ChatCommand>) =
        settings.putString(SettingsKeys.CHAT_COMMANDS, json.encodeToString(commands))

    private fun saveCustomThemes(themes: List<CustomThemeConfig>, activeId: String?) {
        settings.putString(SettingsKeys.CUSTOM_THEMES, json.encodeToString(themes))
        settings.putString(SettingsKeys.ACTIVE_THEME_ID, activeId ?: "")
    }

    private fun saveHighlightRules(rules: List<HighlightRule>) =
        settings.putString(SettingsKeys.HIGHLIGHT_RULES, json.encodeToString(rules))

    private fun saveAllModButtons(buttons: List<ModActionButton>) {
        settings.putString(SettingsKeys.ALL_MOD_BUTTONS, json.encodeToString(buttons))
        _modButtonsLive.value = buttons
        notifyExternalChange()
    }

    private fun saveModButtons(buttons: List<ModActionButton>) =
        settings.putString(SettingsKeys.CUSTOM_MOD_BUTTONS, json.encodeToString(buttons))

    private fun saveMacros(macros: List<Macro>) {
        settings.putString(SettingsKeys.MACROS, json.encodeToString(macros))
        _macrosLive.value = macros
        notifyExternalChange()
    }
}
