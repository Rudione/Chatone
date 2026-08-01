package io.rudione.chatone.presentation.account

import io.rudione.chatone.domain.model.TwitchAccount
import io.rudione.chatone.data.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class AccountFlowGlue(
    private val authRepository: AuthRepository,
    private val accountManager: AccountManager
) {

    suspend fun activeAccountFlow(): Flow<TwitchAccount?> {
        return authRepository.getAccounts().map { accounts ->
            val activeId = accountManager.activeAccountId.value
            accounts.firstOrNull { it.userId == activeId }
                ?: accounts.firstOrNull()
        }.distinctUntilChanged()
    }

    suspend fun otherAccountsFlow(): Flow<List<TwitchAccount>> {
        return authRepository.getAccounts().map { accounts ->
            val activeId = accountManager.activeAccountId.value
            accounts.filter { it.userId != activeId }
        }
    }
}
