package io.rudione.chatone.presentation.account

import io.rudione.chatone.data.repository.AuthRepository
import io.rudione.chatone.domain.model.TwitchAccount
import kotlinx.coroutines.flow.Flow

class AccountListLoader(private val authRepository: AuthRepository) {
    suspend fun list(): Flow<List<TwitchAccount>> = authRepository.getAccounts()
    suspend fun delete(userId: String) = authRepository.deleteAccount(userId)
    suspend fun firstValid(): TwitchAccount? = authRepository.getFirstValidAccount()
}
