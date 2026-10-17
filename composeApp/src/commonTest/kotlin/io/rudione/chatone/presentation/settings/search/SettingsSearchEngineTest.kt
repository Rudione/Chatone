package io.rudione.chatone.presentation.settings.search

import io.rudione.chatone.presentation.settings.ModerationTab
import io.rudione.chatone.presentation.settings.SettingsSection
import io.rudione.chatone.presentation.settings.labelMatches
import io.rudione.chatone.presentation.theme.i18n.StringsRu
import io.rudione.chatone.util.i18n.StringsEn
import io.rudione.chatone.util.platform.InputCapabilities
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SettingsSearchEngineTest {

    private val desktop = SettingsCatalogContext(InputCapabilities(hardwareKeyboard = true, pointer = true), desktop = true, phone = false)
    private val phone = SettingsCatalogContext(InputCapabilities(hardwareKeyboard = false, pointer = false), desktop = false, phone = true)
    private val ru = buildSettingsCatalog(StringsRu, desktop)
    private val en = buildSettingsCatalog(StringsEn, desktop)

    private fun top(query: String, catalog: List<SettingsSearchEntry> = ru): SettingsSearchEntry =
        searchSettings(query, catalog).first().entry

    @Test
    fun exactTitleRanksFirst() {
        assertEquals(StringsRu.settingsUiScale, top("масштаб интерфейса").title)
    }

    @Test
    fun wordFormsStillMatch() {
        assertEquals(StringsRu.settingsHideSidebar, top("боковую панель").title)
        assertTrue(searchSettings("уведомлений", ru).any { it.entry.section == SettingsSection.NOTIFICATIONS })
    }

    @Test
    fun typosAreForgiven() {
        assertEquals(StringsRu.settingsUiScale, top("масштап интерфейса").title)
        assertEquals(StringsRu.uploaderEnable, top("загрущик").title)
    }

    @Test
    fun wrongKeyboardLayoutIsUnderstood() {
        assertEquals(SettingsSection.APPEARANCE, top("ntvf").section)
        assertTrue(searchSettings("[jnrtb", ru).isNotEmpty() || searchSettings("ujhzxbt", ru).isNotEmpty())
    }

    @Test
    fun wordsCanComeInAnyOrder() {
        assertEquals(StringsRu.settingsMessageHistoryLimit, top("сообщений лимит").title)
    }

    @Test
    fun synonymsReachTheRightSetting() {
        assertEquals(SettingsSection.ACCOUNT, top("режим стримера").section)
        assertEquals(StringsRu.settingsGiphyApiKey, top("giphy").title)
    }

    @Test
    fun moderationEntriesPointAtTheirTab() {
        val slider = searchSettings("ход слайдера", ru).first().entry
        assertEquals(SettingsSection.MODERATION, slider.section)
        assertEquals(ModerationTab.SLIDER, slider.tab)
        assertEquals(ModerationTab.MACROS, top("макросы").tab)
    }

    @Test
    fun englishCatalogWorksToo() {
        assertEquals(StringsEn.settingsUiScale, top("ui scale", en).title)
        assertEquals(SettingsSection.MEDIA, top("uploader", en).section)
    }

    @Test
    fun phoneCatalogHasNoDesktopOnlySettings() {
        val catalog = buildSettingsCatalog(StringsRu, phone)
        assertTrue(catalog.none { it.section == SettingsSection.SCREEN_CHAT || it.section == SettingsSection.HOTKEYS })
        assertTrue(catalog.none { it.title == StringsRu.uploaderEnable })
        assertTrue(catalog.any { it.section == SettingsSection.MEDIA && it.title == StringsRu.settingsShowChatGifs })
        assertTrue(catalog.none { it.tab == ModerationTab.SLIDER })
        assertTrue(catalog.none { it.title == StringsRu.settingsAlwaysOnTop })
    }

    @Test
    fun screenChatAndImagesHaveTheirOwnSections() {
        assertEquals(SettingsSection.SCREEN_CHAT, top("подложка под сообщениями").section)
        assertEquals(SettingsSection.MEDIA, top("гифки").section)
        assertTrue(ru.none { it.title == StringsRu.screenChat.overFullScreenTitle })
        val mac = buildSettingsCatalog(StringsRu, desktop.copy(macOs = true))
        assertEquals(SettingsSection.SCREEN_CHAT, mac.single { it.title == StringsRu.screenChat.overFullScreenTitle }.section)
    }

    @Test
    fun everyEntryHasAUniqueId() {
        val ids = ru.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun gibberishFindsNothing() {
        assertTrue(searchSettings("zzqxw", ru).isEmpty())
        assertTrue(searchSettings("   ", ru).isEmpty())
    }

    @Test
    fun normalizationFoldsCaseYoAndPunctuation() {
        assertEquals("зачеркивание и текст", SearchText.normalize("  Зачёркивание, и — ТЕКСТ! "))
    }

    @Test
    fun stemmerCutsCommonEndings() {
        assertEquals(SearchText.stem("боковая"), SearchText.stem("боковую"))
        assertEquals("notification", SearchText.stem("notifications"))
    }

    @Test
    fun distanceCountsTranspositionsAsOneEdit() {
        assertEquals(1, SearchText.distance("тмеа", "тема", 2))
        assertEquals(3, SearchText.distance("abc", "xyzabc", 2))
    }

    @Test
    fun highlightCoversWholeWordsForStemsAndLayouts() {
        assertEquals(listOf(8..17), SearchText.highlightRanges("Масштаб интерфейса", "интерфейс"))
        assertEquals(listOf(0..3), SearchText.highlightRanges("Тема", "ntvf"))
        assertEquals(2, SearchText.highlightRanges("Лимит истории сообщений", "сообщений лимит").size)
        assertTrue(SearchText.highlightRanges("Тема", "x").isEmpty())
    }

    @Test
    fun russianQueryFindsSettingsInEnglishInterface() {
        assertTrue(searchSettings("фон", en).take(5).any { it.entry.section == SettingsSection.BACKGROUND })
        assertEquals(StringsEn.settingsUiScale, top("масштаб интерфейса", en).title)
        assertEquals(StringsEn.settingsHideSidebar, top("боковую панель", en).title)
    }

    @Test
    fun englishQueryFindsSettingsInRussianInterface() {
        assertEquals(StringsRu.settingsFontFamily, top("font family").title)
        assertTrue(SettingsSection.NOTIFICATIONS.labelMatches("notifications", StringsRu))
    }

    @Test
    fun synonymsWidenTheQuery() {
        assertEquals(SettingsSection.BACKGROUND, top("обои", en).section)
        assertTrue(searchSettings("смайлики", ru).any { it.entry.title == StringsRu.settingsEmoteSize })
        assertTrue(SettingsSection.NOTIFICATIONS.labelMatches("оповещения", StringsRu))
    }

    @Test
    fun transliterationIsUnderstood() {
        assertEquals("fon", SearchText.transliterate("фон"))
        assertEquals("чат", SearchText.transliterate("chat"))
        assertTrue(searchSettings("shrift", ru).any { it.entry.title == StringsRu.settingsFontFamily })
    }

    @Test
    fun subsectionsFollowCatalogGroups() {
        val chat = settingsSubsections(ru, StringsRu).getValue(SettingsSection.CHAT)
        assertEquals(StringsRu.settingsHub.groupMessageLook, chat.first().title)
        assertEquals(chat.size, chat.map { it.title }.toSet().size)
        val moderation = settingsSubsections(ru, StringsRu).getValue(SettingsSection.MODERATION)
        assertEquals(ModerationTab.entries, moderation.map { it.tab })
        assertEquals(StringsRu.modSlider.tabMacros, moderation.last().title)
        assertTrue(moderation.all { it.anchor == null })
    }

    @Test
    fun weakTailMatchesAreDropped() {
        val hits = searchSettings("ntvf", ru)
        val best = hits.first().score
        assertTrue(hits.all { it.score >= best * 0.3f })
    }
}
