package io.rudione.chatone.presentation.account

import io.rudione.chatone.util.SettingsImportExport


class AccountSettingsExporter(
    private val accountManager: AccountManager
) {


    fun captureCurrentToAccount(
        userId: String,
        backupJson: String
    ): Boolean {
        if (userId.isBlank() || backupJson.isBlank()) return false
        accountManager.saveSettingsOverrideJson(userId, backupJson)
        accountManager.setOverrideEnabled(userId, true)
        return true
    }


    fun load(userId: String): String? {
        if (userId.isBlank()) return null
        if (!accountManager.isOverrideEnabled(userId)) return null
        return accountManager.getSettingsOverrideJson(userId)
    }


    fun reset(userId: String) {
        accountManager.saveSettingsOverrideJson(userId, null)
        accountManager.setOverrideEnabled(userId, false)
    }
}
