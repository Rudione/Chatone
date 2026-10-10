package io.rudione.chatone.presentation.settings

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SettingsSectionExpansionTest {

    @Test
    fun matchingSectionsOpenWhileSearching() {
        val expansion = SettingsSectionExpansion()
        expansion.onQueryChanged("", "шрифт")
        assertTrue(expansion.isOpen(SettingsSection.CHAT, searching = true))
    }

    @Test
    fun aSectionCanBeClosedWhileSearching() {
        val expansion = SettingsSectionExpansion()
        expansion.toggle(SettingsSection.APPEARANCE, searching = false)
        expansion.onQueryChanged("", "боковую")
        expansion.toggle(SettingsSection.APPEARANCE, searching = true)
        assertFalse(expansion.isOpen(SettingsSection.APPEARANCE, searching = true))
        expansion.toggle(SettingsSection.APPEARANCE, searching = true)
        assertTrue(expansion.isOpen(SettingsSection.APPEARANCE, searching = true))
    }

    @Test
    fun startingASearchClosesSectionsOpenedBefore() {
        val expansion = SettingsSectionExpansion()
        expansion.toggle(SettingsSection.APPEARANCE, searching = false)
        expansion.onQueryChanged("", "звук")
        expansion.onQueryChanged("звук", "")
        assertFalse(expansion.isOpen(SettingsSection.APPEARANCE, searching = false))
    }

    @Test
    fun refiningTheQueryKeepsSectionsTheUserClosed() {
        val expansion = SettingsSectionExpansion()
        expansion.onQueryChanged("", "з")
        expansion.toggle(SettingsSection.NOTIFICATIONS, searching = true)
        expansion.onQueryChanged("з", "зв")
        assertFalse(expansion.isOpen(SettingsSection.NOTIFICATIONS, searching = true))
    }

    @Test
    fun sectionReopenedDuringSearchStaysOpenAfterIt() {
        val expansion = SettingsSectionExpansion()
        expansion.onQueryChanged("", "чат")
        expansion.toggle(SettingsSection.CHAT, searching = true)
        expansion.toggle(SettingsSection.CHAT, searching = true)
        expansion.onQueryChanged("чат", "")
        assertTrue(expansion.isOpen(SettingsSection.CHAT, searching = false))
    }

    @Test
    fun revealOpensTheSectionEverywhere() {
        val expansion = SettingsSectionExpansion()
        expansion.onQueryChanged("", "x")
        expansion.toggle(SettingsSection.ACCOUNT, searching = true)
        expansion.reveal(SettingsSection.ACCOUNT)
        assertTrue(expansion.isOpen(SettingsSection.ACCOUNT, searching = true))
        assertTrue(expansion.isOpen(SettingsSection.ACCOUNT, searching = false))
    }
}
