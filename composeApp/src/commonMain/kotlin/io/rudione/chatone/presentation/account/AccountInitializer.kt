package io.rudione.chatone.presentation.account

import io.github.aakira.napier.Napier
import io.rudione.chatone.data.repository.AuthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AccountInitializer(
    private val authRepository: AuthRepository,
    private val accountManager: AccountManager,
    private val scope: CoroutineScope
) {
    companion object { private const val TAG = "AccountInitializer" }

    fun initialize(onReady: (defaultUserId: String) -> Unit = {}) {
        scope.launch {
            try {

                if (accountManager.activeAccountId.value.isBlank()) {
                    val accounts = authRepository.getAccounts().first()
                    val first = accounts.firstOrNull()
                    if (first != null) {
                        accountManager.setActiveAccount(first.userId)
                        Napier.d("Initial active account: ${first.login}", tag = TAG)
                        onReady(first.userId)
                    } else {
                        onReady("")
                    }
                } else {
                    onReady(accountManager.activeAccountId.value)
                }
            } catch (e: Exception) {
                Napier.e("AccountInitializer error: ${e.message}", e, tag = TAG)
                onReady("")
            }
        }
    }
}
