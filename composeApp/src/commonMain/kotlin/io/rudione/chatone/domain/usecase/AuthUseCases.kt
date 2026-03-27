package io.rudione.chatone.domain.usecase

import io.rudione.chatone.data.repository.AuthRepository
import io.rudione.chatone.domain.model.TwitchAccount
import io.rudione.chatone.util.Result
import kotlinx.coroutines.flow.Flow

class AuthenticateWithTokenUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(accessToken: String): Result<TwitchAccount> {
        return authRepository.authenticateWithToken(accessToken)
    }
}

class GetAccountsUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(): Flow<List<TwitchAccount>> {
        return authRepository.getAccounts()
    }
}

class DeleteAccountUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(userId: String) {
        authRepository.deleteAccount(userId)
    }
}

class ValidateTokenUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(account: TwitchAccount): Result<Boolean> {
        return authRepository.validateToken(account)
    }
}

class GetFirstValidAccountUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(): TwitchAccount? {
        return authRepository.getFirstValidAccount()
    }
}
