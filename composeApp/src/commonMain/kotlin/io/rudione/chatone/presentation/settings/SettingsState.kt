package io.rudione.chatone.presentation.settings

import io.rudione.chatone.base.UiState
import io.rudione.chatone.domain.model.ChatAutomation
import io.rudione.chatone.domain.model.ChatCommand
import io.rudione.chatone.domain.model.ChatoneColorTokens
import io.rudione.chatone.domain.model.HighlightRule
import io.rudione.chatone.domain.model.ImageUploaderConfig
import io.rudione.chatone.domain.model.Macro
import io.rudione.chatone.domain.model.ModActionButton
import io.rudione.chatone.domain.model.ModSliderSettings
import io.rudione.chatone.domain.model.ScreenChatSettings
import io.rudione.chatone.presentation.theme.CustomThemeConfig
import io.rudione.chatone.presentation.settings.theme_settings.ThemeCreatorSection
import io.rudione.chatone.presentation.theme.WallpaperDisplayConfig
import io.rudione.chatone.util.system.defaultHotkeys

enum class PauseHotkeyMode { TOGGLE, HOLD }

enum class InlineImageMode { ON, OFF, BLUR }

enum class TitleBarMode { DARK, LIGHT, ADAPTIVE, SYSTEM }

enum class AlwaysOnTopMode { EXCEPT_SYSTEM, ALL_WINDOWS, BROWSER }

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
    val alwaysOnTop: Boolean = false,
    val alwaysOnTopMode: AlwaysOnTopMode = AlwaysOnTopMode.EXCEPT_SYSTEM,
    val hideSidebar: Boolean = false,
    val uiScale: Float = 1.0f,
    val pauseOnHover: Boolean = false,
    val pauseHotkey: String = "",
    val pauseHotkeyMode: PauseHotkeyMode = PauseHotkeyMode.TOGGLE,
    val showInlineImages: InlineImageMode = InlineImageMode.ON,
    val inlineImageMaxHeight: Int = 200,
    val showChatGifs: Boolean = true,
    val giphyApiKey: String = "",
    val clipPreviewWidth: Int = 140,
    val chatScrollbarWidth: Int = 12,
    val chatTopBarHeight: Int = 36,
    val automations: List<ChatAutomation> = emptyList(),
    val autoClaimPoints: Boolean = false,
    val mutedPhrases: List<String> = emptyList(),
    val wallpaperPath: String = "",
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
    val readableNickColors: Boolean = true,
    val showDefaultDeleteButton: Boolean = true,
    val showDefaultTimeoutButton: Boolean = true,
    val showDefaultBanButton: Boolean = true,
    val modButtonsOnHover: Boolean = false,
    val modButtonsInChat: Boolean = true,
    val modSlider: ModSliderSettings = ModSliderSettings(),
    val screenChat: ScreenChatSettings = ScreenChatSettings(),
    val disableScrollOnAlt: Boolean = true,
    val linkOpenMode: LinkOpenMode = LinkOpenMode.DEFAULT,
    val customThemesJson: String = "",
    val accentColorIndex: Int = 0,
    val customThemes: List<CustomThemeConfig> = emptyList(),
    val activeCustomThemeId: String? = null,
    val showThemeCreator: Boolean = false,
    val themeCreatorSeedColor: Int? = null,
    val themeCreatorSection: ThemeCreatorSection = ThemeCreatorSection.THEMES,
    val language: String = "en",
    val fontFamilyName: String = "Inter",
    val fontStyleItalic: Boolean = false,
    val fontUnderline: Boolean = false,
    val fontStrikethrough: Boolean = false,
    val customFontPaths: List<String> = emptyList(),
    val messageSpacing: MessageSpacing = MessageSpacing.LOW,
    val titleBarMode: TitleBarMode = TitleBarMode.ADAPTIVE,
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
    val autoTranslateInput: Boolean = false,
    val outgoingTranslationLang: String = "en",
    val mentionTabsEnabled: Boolean = false,
    val navigationHidden: Boolean = false,
    val hotkeys: Map<String, String> = defaultHotkeys(),
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
