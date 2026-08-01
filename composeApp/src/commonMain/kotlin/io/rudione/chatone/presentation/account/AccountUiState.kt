package io.rudione.chatone.presentation.account

import io.rudione.chatone.domain.model.AccountProxyConfig
import io.rudione.chatone.domain.model.TwitchAccount

data class AccountUiState(
    val accounts: List<TwitchAccount> = emptyList(),
    val activeUserId: String = "",
    val isSwitching: Boolean = false,
    val pendingProxyValidation: AccountProxyConfig? = null,
    val lastError: String? = null
) {
    val activeAccount: TwitchAccount? get() = accounts.firstOrNull { it.userId == activeUserId }
    val hasMultipleAccounts: Boolean get() = accounts.size > 1
}
