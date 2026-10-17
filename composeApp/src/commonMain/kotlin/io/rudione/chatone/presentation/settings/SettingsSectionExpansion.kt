package io.rudione.chatone.presentation.settings

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

@Stable
internal class SettingsSectionExpansion {
    private var expanded by mutableStateOf(emptySet<SettingsSection>())

    fun isOpen(section: SettingsSection): Boolean = section in expanded

    fun toggle(section: SettingsSection) {
        expanded = if (section in expanded) expanded - section else expanded + section
    }

    fun reveal(section: SettingsSection) {
        expanded = expanded + section
    }
}
