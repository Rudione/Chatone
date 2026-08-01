package io.rudione.chatone.presentation.account

object AccountSettingsOverlay {

    fun getEffectiveOverrideJson(
        userId: String,
        accountManager: AccountManager
    ): String? {
        if (userId.isBlank()) return null
        if (!accountManager.isOverrideEnabled(userId)) return null
        return accountManager.getSettingsOverrideJson(userId)
    }

    fun saveOverrideJson(
        userId: String,
        accountManager: AccountManager,
        json: String
    ) {
        if (userId.isBlank()) return
        accountManager.saveSettingsOverrideJson(userId, json)
    }

    fun clearOverride(userId: String, accountManager: AccountManager) {
        accountManager.saveSettingsOverrideJson(userId, null)
        accountManager.setOverrideEnabled(userId, false)
    }
}
