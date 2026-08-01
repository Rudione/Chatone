package io.rudione.chatone.presentation.account

import io.github.aakira.napier.Napier
import io.rudione.chatone.data.repository.AuthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AccountMigration(
    private val authRepository: AuthRepository,
    private val accountManager: AccountManager,
    private val scope: CoroutineScope
) {
    companion object { private const val TAG = "AccountMigration" }

    fun run(onComplete: () -> Unit = {}) {
        scope.launch {
            try {
                val current = accountManager.activeAccountId.value
                if (current.isBlank()) {
                    val firstAcc = authRepository.getFirstValidAccount()
                    if (firstAcc != null) {
                        accountManager.setActiveAccount(firstAcc.userId)
                        Napier.d("Migration: set primary to ${firstAcc.login}", tag = TAG)
                    }
                }
                onComplete()
            } catch (e: Exception) {
                Napier.w("Migration error: ${e.message}", tag = TAG)
                onComplete()
            }
        }
    }
}
