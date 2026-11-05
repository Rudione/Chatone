package io.rudione.chatone.presentation.settings.search

import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.mod_slider_tab_macros
import chatone.composeapp.generated.resources.screen_chat_over_full_screen_title
import chatone.composeapp.generated.resources.settings_always_on_top
import chatone.composeapp.generated.resources.settings_emote_size
import chatone.composeapp.generated.resources.settings_font_family
import chatone.composeapp.generated.resources.settings_giphy_api_key
import chatone.composeapp.generated.resources.settings_hide_sidebar
import chatone.composeapp.generated.resources.settings_hub_group_message_look
import chatone.composeapp.generated.resources.settings_message_history_limit
import chatone.composeapp.generated.resources.settings_show_chat_gifs
import chatone.composeapp.generated.resources.settings_ui_scale
import chatone.composeapp.generated.resources.uploader_enable
import io.rudione.chatone.presentation.settings.ModerationTab
import io.rudione.chatone.presentation.settings.SettingsSection
import io.rudione.chatone.presentation.settings.labelMatches
import io.rudione.chatone.util.platform.InputCapabilities
import kotlinx.coroutines.test.runTest
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SettingsCatalogSearchTest {

    private val desktop = SettingsCatalogContext(InputCapabilities(hardwareKeyboard = true, pointer = true), desktop = true, phone = false)
    private val phone = SettingsCatalogContext(InputCapabilities(hardwareKeyboard = false, pointer = false), desktop = false, phone = true)
    private lateinit var ru: List<SettingsSearchEntry>
    private lateinit var en: List<SettingsSearchEntry>

    private suspend fun <T> inLocale(tag: String, block: suspend () -> T): T {
        val previous = Locale.getDefault()
        Locale.setDefault(Locale.forLanguageTag(tag))
        return try { block() } finally { Locale.setDefault(previous) }
    }

    private suspend fun <T> inRussian(block: suspend () -> T): T = inLocale("ru", block)

    private suspend fun ruText(resource: StringResource): String = inRussian { getString(resource) }

    private suspend fun enText(resource: StringResource): String = inLocale("en", { getString(resource) })

    private suspend fun sectionLabel(section: SettingsSection): String = ruText(section.labelRes())

    private suspend fun catalogs() {
        if (!::ru.isInitialized) ru = inRussian { buildSettingsCatalog(desktop) }
        if (!::en.isInitialized) en = inLocale("en") { buildSettingsCatalog(desktop) }
    }

    private suspend fun top(query: String, catalog: List<SettingsSearchEntry>? = null): SettingsSearchEntry {
        catalogs()
        return searchSettings(query, catalog ?: ru).first().entry
    }

    @Test
    fun exactTitleRanksFirst() = runTest {
        catalogs()
        assertEquals(ruText(Res.string.settings_ui_scale), top("масштаб интерфейса").title)
    }

    @Test
    fun wordFormsStillMatch() = runTest {
        catalogs()
        assertEquals(ruText(Res.string.settings_hide_sidebar), top("боковую панель").title)
        assertTrue(searchSettings("уведомлений", ru).any { it.entry.section == SettingsSection.NOTIFICATIONS })
    }

    @Test
    fun typosAreForgiven() = runTest {
        catalogs()
        assertEquals(ruText(Res.string.settings_ui_scale), top("масштап интерфейса").title)
        assertEquals(ruText(Res.string.uploader_enable), top("загрущик").title)
    }

    @Test
    fun wrongKeyboardLayoutIsUnderstood() = runTest {
        catalogs()
        assertEquals(SettingsSection.APPEARANCE, top("ntvf").section)
        assertTrue(searchSettings("[jnrtb", ru).isNotEmpty() || searchSettings("ujhzxbt", ru).isNotEmpty())
    }

    @Test
    fun wordsCanComeInAnyOrder() = runTest {
        catalogs()
        assertEquals(ruText(Res.string.settings_message_history_limit), top("сообщений лимит").title)
    }

    @Test
    fun synonymsReachTheRightSetting() = runTest {
        catalogs()
        assertEquals(SettingsSection.ACCOUNT, top("режим стримера").section)
        assertEquals(ruText(Res.string.settings_giphy_api_key), top("giphy").title)
    }

    @Test
    fun moderationEntriesPointAtTheirTab() = runTest {
        catalogs()
        val slider = searchSettings("ход слайдера", ru).first().entry
        assertEquals(SettingsSection.MODERATION, slider.section)
        assertEquals(ModerationTab.SLIDER, slider.tab)
        assertEquals(ModerationTab.MACROS, top("макросы").tab)
    }

    @Test
    fun englishCatalogWorksToo() = runTest {
        catalogs()
        assertEquals(enText(Res.string.settings_ui_scale), top("ui scale", en).title)
        assertEquals(SettingsSection.MEDIA, top("uploader", en).section)
    }

    @Test
    fun phoneCatalogHasNoDesktopOnlySettings() = runTest {
        catalogs()
        val catalog = inRussian { buildSettingsCatalog(phone) }
        assertTrue(catalog.none { it.section == SettingsSection.SCREEN_CHAT || it.section == SettingsSection.HOTKEYS })
        assertTrue(catalog.none { it.title == ruText(Res.string.uploader_enable) })
        assertTrue(catalog.any { it.section == SettingsSection.MEDIA && it.title == ruText(Res.string.settings_show_chat_gifs) })
        assertTrue(catalog.none { it.tab == ModerationTab.SLIDER })
        assertTrue(catalog.none { it.title == ruText(Res.string.settings_always_on_top) })
    }

    @Test
    fun screenChatAndImagesHaveTheirOwnSections() = runTest {
        catalogs()
        assertEquals(SettingsSection.SCREEN_CHAT, top("подложка под сообщениями").section)
        assertEquals(SettingsSection.MEDIA, top("гифки").section)
        assertTrue(ru.none { it.title == ruText(Res.string.screen_chat_over_full_screen_title) })
        val mac = inRussian { buildSettingsCatalog(desktop.copy(macOs = true)) }
        assertEquals(SettingsSection.SCREEN_CHAT, mac.single { it.title == ruText(Res.string.screen_chat_over_full_screen_title) }.section)
    }

    @Test
    fun everyEntryHasAUniqueId() = runTest {
        catalogs()
        val ids = ru.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun gibberishFindsNothing() = runTest {
        catalogs()
        assertTrue(searchSettings("zzqxw", ru).isEmpty())
        assertTrue(searchSettings("   ", ru).isEmpty())
    }

    @Test
    fun russianQueryFindsSettingsInEnglishInterface() = runTest {
        catalogs()
        assertTrue(searchSettings("фон", en).take(5).any { it.entry.section == SettingsSection.BACKGROUND })
        assertEquals(enText(Res.string.settings_ui_scale), top("масштаб интерфейса", en).title)
        assertEquals(enText(Res.string.settings_hide_sidebar), top("боковую панель", en).title)
    }

    @Test
    fun englishQueryFindsSettingsInRussianInterface() = runTest {
        catalogs()
        assertEquals(ruText(Res.string.settings_font_family), top("font family").title)
        assertTrue(SettingsSection.NOTIFICATIONS.labelMatches("notifications", sectionLabel(SettingsSection.NOTIFICATIONS)))
    }

    @Test
    fun synonymsWidenTheQuery() = runTest {
        catalogs()
        assertEquals(SettingsSection.BACKGROUND, top("обои", en).section)
        assertTrue(searchSettings("смайлики", ru).any { it.entry.title == ruText(Res.string.settings_emote_size) })
        assertTrue(SettingsSection.NOTIFICATIONS.labelMatches("оповещения", sectionLabel(SettingsSection.NOTIFICATIONS)))
    }

    @Test
    fun transliteratedQueryFindsTheFontSetting() = runTest {
        catalogs()
        assertTrue(searchSettings("shrift", ru).any { it.entry.title == ruText(Res.string.settings_font_family) })
    }

    @Test
    fun subsectionsFollowCatalogGroups() = runTest {
        catalogs()
        val chat = settingsSubsections(ru).getValue(SettingsSection.CHAT)
        assertEquals(ruText(Res.string.settings_hub_group_message_look), chat.first().title)
        assertEquals(chat.size, chat.map { it.title }.toSet().size)
        val moderation = settingsSubsections(ru).getValue(SettingsSection.MODERATION)
        assertEquals(ModerationTab.entries, moderation.map { it.tab })
        assertEquals(ruText(Res.string.mod_slider_tab_macros), moderation.last().title)
        assertTrue(moderation.all { it.anchor == null })
    }

    @Test
    fun weakTailMatchesAreDropped() = runTest {
        catalogs()
        val hits = searchSettings("ntvf", ru)
        val best = hits.first().score
        assertTrue(hits.all { it.score >= best * 0.3f })
    }
}
