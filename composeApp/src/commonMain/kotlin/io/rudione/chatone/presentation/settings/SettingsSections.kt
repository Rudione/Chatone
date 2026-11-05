package io.rudione.chatone.presentation.settings

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.StringResource
import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.ai_assistant_title
import chatone.composeapp.generated.resources.screen_chat_settings_title
import chatone.composeapp.generated.resources.section_actions
import chatone.composeapp.generated.resources.section_background
import chatone.composeapp.generated.resources.section_chat
import chatone.composeapp.generated.resources.section_commands
import chatone.composeapp.generated.resources.section_highlights
import chatone.composeapp.generated.resources.section_hotkeys
import chatone.composeapp.generated.resources.settings_about
import chatone.composeapp.generated.resources.settings_account
import chatone.composeapp.generated.resources.settings_appearance
import chatone.composeapp.generated.resources.settings_hub_section_media
import chatone.composeapp.generated.resources.settings_moderation
import chatone.composeapp.generated.resources.settings_notifications
import org.jetbrains.compose.resources.stringResource
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

    fun labelRes(): StringResource = when (this) {
        APPEARANCE -> Res.string.settings_appearance
        CHAT -> Res.string.section_chat
        SCREEN_CHAT -> Res.string.screen_chat_settings_title
        MEDIA -> Res.string.settings_hub_section_media
        NOTIFICATIONS -> Res.string.settings_notifications
        HIGHLIGHTS -> Res.string.section_highlights
        BACKGROUND -> Res.string.section_background
        HOTKEYS -> Res.string.section_hotkeys
        COMMANDS -> Res.string.section_commands
        ACTIONS -> Res.string.section_actions
        MODERATION -> Res.string.settings_moderation
        AI -> Res.string.ai_assistant_title
        ACCOUNT -> Res.string.settings_account
        ABOUT -> Res.string.settings_about
    }


    @Composable
    fun localizedLabel(): String = stringResource(labelRes())
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

internal suspend fun settingsSectionsMatching(
    query: String,
    context: SettingsCatalogContext = SettingsCatalogContext(
        input = InputCapabilities(hardwareKeyboard = true, pointer = true),
        desktop = isDesktopPlatform,
        phone = false
    )
): List<SettingsSection> {
    if (query.isBlank()) return SettingsSection.entries.toList()
    val hits = searchSettings(query, buildSettingsCatalog(context)).map { it.entry.section }.toSet()
    return SettingsSection.entries.filter { section ->
        section in hits || section.labelMatches(query, getString(section.labelRes()))
    }
}

internal fun SettingsSection.labelMatches(query: String, localized: String): Boolean =
    SettingsSearchEngine.score(
        query,
        SearchDocument(listOf(localized, label).distinct().map { SearchField(it, 1f) })
    ) > 0f
