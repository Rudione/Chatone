package io.rudione.chatone.presentation.settings

import io.rudione.chatone.base.UiEvent
import io.rudione.chatone.domain.model.ChatAutomation
import io.rudione.chatone.domain.model.ChatCommand
import io.rudione.chatone.domain.model.ChatoneColorTokens
import io.rudione.chatone.domain.model.ImageUploaderConfig
import io.rudione.chatone.domain.model.Macro
import io.rudione.chatone.domain.model.ModActionButton
import io.rudione.chatone.domain.model.ModSliderSettings
import io.rudione.chatone.domain.model.ScreenChatSettings
import io.rudione.chatone.presentation.theme.CustomThemeConfig
import io.rudione.chatone.presentation.settings.theme_settings.ThemeCreatorSection
import io.rudione.chatone.presentation.theme.WallpaperDisplayConfig

sealed class SettingsEvent : UiEvent {
    data class OnDarkThemeChanged(val enabled: Boolean) : SettingsEvent()
    data class OnShowChatHeaderChanged(val show: Boolean) : SettingsEvent()
    data class OnOpenThemeCreator(
        val seedColor: Int? = null,
        val section: ThemeCreatorSection = ThemeCreatorSection.THEMES
    ) : SettingsEvent()

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
    data class OnAlwaysOnTopChanged(val enabled: Boolean) : SettingsEvent()
    data class OnAlwaysOnTopModeChanged(val mode: AlwaysOnTopMode) : SettingsEvent()
    data class OnHideSidebarChanged(val enabled: Boolean) : SettingsEvent()
    data class OnPauseOnHoverChanged(val enabled: Boolean) : SettingsEvent()
    data class OnPauseHotkeyChanged(val hotkey: String) : SettingsEvent()
    data class OnPauseHotkeyModeChanged(val mode: PauseHotkeyMode) : SettingsEvent()
    data class OnShowInlineImagesChanged(val mode: InlineImageMode) : SettingsEvent()
    data class OnInlineImageMaxHeightChanged(val height: Int) : SettingsEvent()
    data class OnShowChatGifsChanged(val enabled: Boolean) : SettingsEvent()
    data class OnGiphyApiKeyChanged(val key: String) : SettingsEvent()
    data class OnClipPreviewWidthChanged(val width: Int) : SettingsEvent()
    data class OnChatScrollbarWidthChanged(val width: Int) : SettingsEvent()
    data class OnChatTopBarHeightChanged(val height: Int) : SettingsEvent()
    data class OnAddAutomation(val automation: ChatAutomation) : SettingsEvent()
    data class OnRemoveAutomation(val id: String) : SettingsEvent()
    data class OnToggleAutomation(val id: String, val enabled: Boolean) : SettingsEvent()
    data class OnUpdateAutomation(val automation: ChatAutomation) : SettingsEvent()
    data class OnAutoClaimPointsChanged(val enabled: Boolean) : SettingsEvent()
    data class OnAddMutedPhrase(val phrase: String) : SettingsEvent()
    data class OnRemoveMutedPhrase(val phrase: String) : SettingsEvent()
    data class OnWallpaperPathChanged(val path: String) : SettingsEvent()
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
    data class OnModButtonsOnHoverChanged(val enabled: Boolean) : SettingsEvent()
    data class OnModButtonsInChatChanged(val enabled: Boolean) : SettingsEvent()
    data class OnModSliderChanged(val settings: ModSliderSettings) : SettingsEvent()
    data class OnScreenChatChanged(val settings: ScreenChatSettings) : SettingsEvent()
    data class OnReorderModButtons(val from: Int, val to: Int) : SettingsEvent()

    data class OnAddMacro(val name: String, val icon: String) : SettingsEvent()
    data class OnRemoveMacro(val id: String) : SettingsEvent()
    data class OnUpdateMacro(val macro: Macro) : SettingsEvent()
    data class OnPinMacro(val macroId: String, val slotIndex: Int) : SettingsEvent()

    data class OnSmoothChatEnabledChanged(val enabled: Boolean) : SettingsEvent()
    data class OnAlternateRowBackgroundChanged(val enabled: Boolean) : SettingsEvent()
    data class OnReadableNickColorsChanged(val enabled: Boolean) : SettingsEvent()
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
    data class OnAutoTranslateInputChanged(val enabled: Boolean) : SettingsEvent()
    data class OnOutgoingTranslationLangChanged(val code: String) : SettingsEvent()
    data class OnMentionTabsChanged(val enabled: Boolean) : SettingsEvent()
    data class OnNavigationHiddenChanged(val hidden: Boolean) : SettingsEvent()
    data class OnHotkeyChanged(val actionId: String, val combo: String) : SettingsEvent()
}
