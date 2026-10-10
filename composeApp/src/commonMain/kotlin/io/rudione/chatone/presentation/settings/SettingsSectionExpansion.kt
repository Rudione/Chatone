package io.rudione.chatone.presentation.settings

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

@Stable
internal class SettingsSectionExpansion {
    private var expanded by mutableStateOf(emptySet<SettingsSection>())
    private var collapsedInSearch by mutableStateOf(emptySet<SettingsSection>())

    fun isOpen(section: SettingsSection, searching: Boolean): Boolean =
        if (searching) section !in collapsedInSearch else section in expanded

    fun toggle(section: SettingsSection, searching: Boolean) {
        val open = !isOpen(section, searching)
        expanded = if (open) expanded + section else expanded - section
        if (searching) {
            collapsedInSearch = if (open) collapsedInSearch - section else collapsedInSearch + section
        }
    }

    fun reveal(section: SettingsSection) {
        expanded = expanded + section
        collapsedInSearch = collapsedInSearch - section
    }

    fun onQueryChanged(previous: String, next: String) {
        if (previous.isBlank() && next.isNotBlank()) expanded = emptySet()
        if (previous.isBlank() != next.isBlank()) collapsedInSearch = emptySet()
    }
}
