package io.rudione.chatone.presentation.settings

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SettingsSectionExpansionTest {

    @Test
    fun sectionsStartClosed() {
        assertFalse(SettingsSectionExpansion().isOpen(SettingsSection.CHAT))
    }

    @Test
    fun toggleOpensAndClosesASection() {
        val expansion = SettingsSectionExpansion()
        expansion.toggle(SettingsSection.APPEARANCE)
        assertTrue(expansion.isOpen(SettingsSection.APPEARANCE))
        expansion.toggle(SettingsSection.APPEARANCE)
        assertFalse(expansion.isOpen(SettingsSection.APPEARANCE))
    }

    @Test
    fun revealKeepsOtherSectionsAsTheyWere() {
        val expansion = SettingsSectionExpansion()
        expansion.toggle(SettingsSection.CHAT)
        expansion.reveal(SettingsSection.ACCOUNT)
        expansion.reveal(SettingsSection.ACCOUNT)
        assertTrue(expansion.isOpen(SettingsSection.ACCOUNT))
        assertTrue(expansion.isOpen(SettingsSection.CHAT))
    }
}
