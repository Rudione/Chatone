package io.rudione.chatone.presentation.account

import io.github.aakira.napier.Napier
import io.rudione.chatone.presentation.settings.SettingsViewModel
import io.rudione.chatone.util.settings.SettingsImportExport

class PerAccountSettingsLoader(private val accountManager: AccountManager) {

    companion object {
        private const val TAG = "PerAccountSettings"
    }

    private fun currentSnapshotJson(): String =
        SettingsImportExport.toJson(SettingsImportExport.snapshot(SettingsViewModel.settings))

    private fun apply(json: String): Boolean {
        val backup = SettingsImportExport.fromJson(json) ?: return false
        if (backup.values.isEmpty()) return false
        SettingsImportExport.applyReplace(SettingsViewModel.settings, backup)
        return true
    }

    fun captureFor(userId: String) {
        val json = currentSnapshotJson()
        if (userId.isNotBlank() && accountManager.isOverrideEnabled(userId)) {
            accountManager.saveSettingsOverrideJson(userId, json)
            Napier.d("Captured settings profile for $userId", tag = TAG)
        } else {
            accountManager.saveGlobalBaseJson(json)
            Napier.d("Captured global base settings", tag = TAG)
        }
    }

    fun applyFor(userId: String): Boolean {
        val applied = if (userId.isNotBlank() && accountManager.isOverrideEnabled(userId)) {
            val stored = accountManager.getSettingsOverrideJson(userId)
            if (stored.isNullOrBlank()) {
                accountManager.saveSettingsOverrideJson(userId, currentSnapshotJson())
                false
            } else {
                apply(stored)
            }
        } else {
            val base = accountManager.getGlobalBaseJson()
            if (base.isNullOrBlank()) false else apply(base)
        }
        if (applied) {
            Napier.d("Applied settings profile for ${userId.ifBlank { "<global>" }}", tag = TAG)
            SettingsViewModel.notifyExternalChange()
        }
        return applied
    }

    fun setOverrideEnabled(userId: String, enabled: Boolean) {
        if (userId.isBlank()) return
        val activeId = accountManager.activeAccountId.value
        if (enabled) {
            val activeUsesOverride =
                activeId.isNotBlank() && accountManager.isOverrideEnabled(activeId)
            if (!activeUsesOverride) {
                accountManager.saveGlobalBaseJson(currentSnapshotJson())
            }
            accountManager.saveSettingsOverrideJson(userId, currentSnapshotJson())
            accountManager.setOverrideEnabled(userId, true)
        } else {
            accountManager.setOverrideEnabled(userId, false)
            accountManager.saveSettingsOverrideJson(userId, null)
            if (userId == activeId) {
                val base = accountManager.getGlobalBaseJson()
                if (!base.isNullOrBlank() && apply(base)) {
                    SettingsViewModel.notifyExternalChange()
                }
            }
        }
    }

    fun isAccountUsingCustom(userId: String): Boolean =
        userId.isNotBlank() && accountManager.isOverrideEnabled(userId)
}
