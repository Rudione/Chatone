package io.rudione.chatone.presentation.settings

import androidx.compose.ui.graphics.vector.ImageVector
import io.rudione.chatone.icons.lucide.Bell
import io.rudione.chatone.icons.lucide.CircleUser
import io.rudione.chatone.icons.lucide.Highlighter
import io.rudione.chatone.icons.lucide.Image
import io.rudione.chatone.icons.lucide.Info
import io.rudione.chatone.icons.lucide.Keyboard
import io.rudione.chatone.icons.lucide.Lucide
import io.rudione.chatone.icons.lucide.MessagesSquare
import io.rudione.chatone.icons.lucide.Palette
import io.rudione.chatone.icons.lucide.PictureInPicture2
import io.rudione.chatone.icons.lucide.Shield
import io.rudione.chatone.icons.lucide.Sparkles
import io.rudione.chatone.icons.lucide.SquareSlash
import io.rudione.chatone.icons.lucide.Swords
import io.rudione.chatone.icons.lucide.Wallpaper
import io.rudione.chatone.presentation.settings.search.SettingsCatalogContext
import io.rudione.chatone.presentation.settings.search.SettingsSearchEngine
import io.rudione.chatone.presentation.settings.search.SearchDocument
import io.rudione.chatone.presentation.settings.search.SearchField
import io.rudione.chatone.presentation.settings.search.buildSettingsCatalog
import io.rudione.chatone.presentation.settings.search.searchSettings
import io.rudione.chatone.presentation.theme.i18n.AppLocale
import io.rudione.chatone.presentation.theme.i18n.AppStrings
import io.rudione.chatone.util.platform.InputCapabilities
import io.rudione.chatone.util.system.isDesktopPlatform

internal enum class SettingsSection(
    val label: String,
    val icon: ImageVector
) {
    APPEARANCE("Appearance", Lucide.Palette),
    CHAT("Chat", Lucide.MessagesSquare),
    SCREEN_CHAT("Screen chat", Lucide.PictureInPicture2),
    MEDIA("Images", Lucide.Image),
    NOTIFICATIONS("Notifications", Lucide.Bell),
    HIGHLIGHTS("Highlights", Lucide.Highlighter),
    BACKGROUND("Background", Lucide.Wallpaper),
    HOTKEYS("Hotkeys", Lucide.Keyboard),
    COMMANDS("Commands", Lucide.SquareSlash),
    ACTIONS("Actions", Lucide.Swords),
    MODERATION("Moderation", Lucide.Shield),
    AI("AI Assistant", Lucide.Sparkles),
    ACCOUNT("Account", Lucide.CircleUser),
    ABOUT("About", Lucide.Info);

    fun localizedLabel(s: AppStrings): String = when (this) {
        APPEARANCE -> s.settingsAppearance
        CHAT -> s.sectionChat
        SCREEN_CHAT -> s.screenChat.settingsTitle
        MEDIA -> s.settingsHub.sectionMedia
        NOTIFICATIONS -> s.settingsNotifications
        HIGHLIGHTS -> s.sectionHighlights
        BACKGROUND -> s.sectionBackground
        HOTKEYS -> s.sectionHotkeys
        COMMANDS -> s.sectionCommands
        ACTIONS -> s.sectionActions
        MODERATION -> s.settingsModeration
        AI -> s.aiAssistantTitle
        ACCOUNT -> s.settingsAccount
        ABOUT -> s.settingsAbout
    }
}

internal fun SettingsDestination.section(): SettingsSection = when (this) {
    is SettingsDestination.EditMacro -> SettingsSection.MODERATION
}

internal fun SettingsDestination.moderationTab(): ModerationTab? = when (this) {
    is SettingsDestination.EditMacro -> ModerationTab.MACROS
}

internal fun SettingsSection.isAvailable(input: InputCapabilities, desktop: Boolean = isDesktopPlatform): Boolean = when (this) {
    SettingsSection.HOTKEYS -> input.hardwareKeyboard
    SettingsSection.SCREEN_CHAT -> desktop
    else -> true
}

internal fun settingsSectionsMatching(
    query: String,
    strings: AppStrings,
    context: SettingsCatalogContext = SettingsCatalogContext(
        input = InputCapabilities(hardwareKeyboard = true, pointer = true),
        desktop = isDesktopPlatform,
        phone = false
    )
): List<SettingsSection> {
    if (query.isBlank()) return SettingsSection.entries.toList()
    val hits = searchSettings(query, buildSettingsCatalog(strings, context)).map { it.entry.section }.toSet()
    return SettingsSection.entries.filter { section -> section in hits || section.labelMatches(query, strings) }
}

internal fun SettingsSection.labelMatches(query: String, strings: AppStrings): Boolean {
    val labels = (listOf(strings) + AppLocale.all.map { AppStrings.forLocale(it.code) })
        .map(::localizedLabel) + label
    return SettingsSearchEngine.score(
        query,
        SearchDocument(labels.distinct().map { SearchField(it, 1f) })
    ) > 0f
}
