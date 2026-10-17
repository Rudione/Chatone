package io.rudione.chatone.presentation.settings

import io.rudione.chatone.presentation.settings.search.SettingsCatalogContext
import io.rudione.chatone.presentation.theme.i18n.StringsRu
import io.rudione.chatone.util.platform.InputCapabilities
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SettingsSearchTest {

    private val desktop = SettingsCatalogContext(
        input = InputCapabilities(hardwareKeyboard = true, pointer = true),
        desktop = true,
        phone = false
    )

    @Test
    fun emptyQueryShowsEverySection() {
        assertEquals(SettingsSection.entries.toList(), settingsSectionsMatching("  ", StringsRu))
    }

    @Test
    fun russianWordFormsFindTheSidebarSetting() {
        assertTrue(SettingsSection.APPEARANCE in settingsSectionsMatching("боковую панель", StringsRu, desktop))
        assertTrue(SettingsSection.APPEARANCE in settingsSectionsMatching("Скрывать боковую", StringsRu, desktop))
    }

    @Test
    fun sectionTitlesMatchInBothLanguages() {
        assertTrue(SettingsSection.HOTKEYS in settingsSectionsMatching("горячие", StringsRu))
        assertTrue(SettingsSection.HOTKEYS in settingsSectionsMatching("hotkeys", StringsRu))
    }

    @Test
    fun unknownQueryFindsNothing() {
        assertTrue(settingsSectionsMatching("zzqx", StringsRu).isEmpty())
    }
}
