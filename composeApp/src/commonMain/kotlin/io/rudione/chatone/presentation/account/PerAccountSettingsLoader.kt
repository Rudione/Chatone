package io.rudione.chatone.presentation.account

import io.rudione.chatone.presentation.settings.SettingsState
import io.rudione.chatone.presentation.settings.SettingsViewModel


class PerAccountSettingsLoader(private val accountManager: AccountManager) {


    fun loadForAccount(userId: String): SettingsState {
        val globalState = SettingsViewModel.loadInitialState()
        if (userId.isBlank()) return globalState
        if (!accountManager.isOverrideEnabled(userId)) return globalState

        return globalState
    }


    fun captureGlobalToAccount(userId: String, currentJson: String) {
        if (userId.isBlank()) return
        accountManager.saveSettingsOverrideJson(userId, currentJson)
        accountManager.setOverrideEnabled(userId, true)
    }


    fun isAccountUsingCustom(userId: String): Boolean =
        userId.isNotBlank() && accountManager.isOverrideEnabled(userId)
}
