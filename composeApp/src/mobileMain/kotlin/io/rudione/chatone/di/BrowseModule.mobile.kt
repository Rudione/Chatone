package io.rudione.chatone.di

import io.rudione.chatone.data.remote.TwitchDirectoryClient
import io.rudione.chatone.data.repository.AccountManager
import io.rudione.chatone.data.repository.AuthRepository
import io.rudione.chatone.domain.browse.FollowedAccount
import io.rudione.chatone.domain.browse.FollowedAccountProvider
import io.rudione.chatone.domain.browse.StreamDirectory
import io.rudione.chatone.presentation.browse.BrowseViewModel
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

private class DefaultFollowedAccountProvider(
    private val accountManager: AccountManager,
    private val authRepository: AuthRepository
) : FollowedAccountProvider {
    override val activeAccountId: StateFlow<String> = accountManager.activeAccountId

    override suspend fun account(activeId: String): FollowedAccount? {
        val account = activeId.takeIf { it.isNotBlank() }?.let { authRepository.getAccountById(it) }
            ?: authRepository.getFirstValidAccount()
        return account?.let { FollowedAccount(it.userId, it.accessToken) }
    }
}

actual val platformBrowseModule: Module = module {
    single<StreamDirectory> { TwitchDirectoryClient(httpClient = get()) }
    single<FollowedAccountProvider> { DefaultFollowedAccountProvider(get(), get()) }
    viewModelOf(::BrowseViewModel)
}
