package io.rudione.chatone.presentation.settings

import io.rudione.chatone.presentation.settings.search.SettingsCatalogContext
import kotlinx.coroutines.test.runTest
import java.util.Locale
import io.rudione.chatone.util.platform.InputCapabilities
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SettingsSearchTest {

    private fun inRussian(block: suspend () -> Unit) = runTest {
        val previous = Locale.getDefault()
        Locale.setDefault(Locale.forLanguageTag("ru"))
        try { block() } finally { Locale.setDefault(previous) }
    }

    private val desktop = SettingsCatalogContext(
        input = InputCapabilities(hardwareKeyboard = true, pointer = true),
        desktop = true,
        phone = false
    )

    @Test
    fun emptyQueryShowsEverySection() = inRussian {
        assertEquals(SettingsSection.entries.toList(), settingsSectionsMatching("  "))
    }

    @Test
    fun russianWordFormsFindTheSidebarSetting() = inRussian {
        assertTrue(SettingsSection.APPEARANCE in settingsSectionsMatching("боковую панель", desktop))
        assertTrue(SettingsSection.APPEARANCE in settingsSectionsMatching("Скрывать боковую", desktop))
    }

    @Test
    fun sectionTitlesMatchInBothLanguages() = inRussian {
        assertTrue(SettingsSection.HOTKEYS in settingsSectionsMatching("горячие"))
        assertTrue(SettingsSection.HOTKEYS in settingsSectionsMatching("hotkeys"))
    }

    @Test
    fun unknownQueryFindsNothing() = inRussian {
        assertTrue(settingsSectionsMatching("zzqx").isEmpty())
    }
}
