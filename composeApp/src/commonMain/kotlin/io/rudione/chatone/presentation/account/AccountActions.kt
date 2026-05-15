package io.rudione.chatone.presentation.account

import io.rudione.chatone.data.repository.AuthRepository
import io.rudione.chatone.domain.model.AccountProxyConfig
import io.rudione.chatone.domain.model.TwitchAccount
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch


class AccountActions(
    private val authRepository: AuthRepository,
    private val accountManager: AccountManager,
    private val switchCoordinator: AccountSwitchCoordinator,
    private val scope: CoroutineScope
) {
    fun setPrimary(account: TwitchAccount, onComplete: (Boolean) -> Unit = {}) {
        switchCoordinator.switchTo(account, onComplete)
    }

    fun remove(account: TwitchAccount, onComplete: () -> Unit = {}) {
        scope.launch {
            try {
                accountManager.deleteAllPerAccountData(account.userId)
                authRepository.deleteAccount(account.userId)
                if (accountManager.activeAccountId.value == account.userId) {
                    authRepository.getFirstValidAccount()?.let { fallback ->
                        switchCoordinator.switchTo(fallback)
                    }
                }
                onComplete()
            } catch (_: Exception) {
                onComplete()
            }
        }
    }

    fun saveProxy(userId: String, proxy: AccountProxyConfig?) {
        accountManager.saveProxy(userId, proxy)
    }

    fun toggleOverride(userId: String, enabled: Boolean) {
        accountManager.setOverrideEnabled(userId, enabled)
    }

    fun saveOverrideJson(userId: String, json: String?) {
        accountManager.saveSettingsOverrideJson(userId, json)
    }
}
