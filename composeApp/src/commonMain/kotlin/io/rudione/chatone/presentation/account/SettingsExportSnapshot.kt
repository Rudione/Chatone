package io.rudione.chatone.presentation.account

object SettingsExportSnapshot {

    fun snapshot(globalJson: String, accountManager: AccountManager, userId: String) {
        if (userId.isBlank()) return
        accountManager.saveSettingsOverrideJson(userId, globalJson)
        accountManager.setOverrideEnabled(userId, true)
    }

    fun reset(accountManager: AccountManager, userId: String) {
        AccountSettingsOverlay.clearOverride(userId, accountManager)
    }

    fun mergeWithGlobal(globalJson: String, overrideJson: String): String {

        return overrideJson.ifBlank { globalJson }
    }
}
