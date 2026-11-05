package io.rudione.chatone.data.repository

import kotlinx.coroutines.flow.StateFlow

data class ViewerSession(
    val userId: String,
    val login: String,
    val displayName: String,
    val avatarUrl: String,
    val helixToken: String,
    val gqlToken: String,
    val scopes: List<String>
) {
    fun hasScope(scope: String): Boolean = scope in scopes
}

class ViewerSessionProvider(
    private val accountManager: AccountManager,
    private val authRepository: AuthRepository,
    private val moderationAuth: ModerationAuthStore
) {
    val activeAccountId: StateFlow<String> get() = accountManager.activeAccountId

    suspend fun current(): ViewerSession? {
        val activeId = accountManager.activeAccountId.value
        val account = activeId.takeIf { it.isNotBlank() }?.let { authRepository.getAccountById(it) }
            ?: authRepository.getFirstValidAccount()
            ?: return null
        if (account.accessToken.isBlank()) return null
        return ViewerSession(
            userId = account.userId,
            login = account.login.lowercase(),
            displayName = account.displayName.ifBlank { account.login },
            avatarUrl = account.profileImageUrl,
            helixToken = account.accessToken,
            gqlToken = moderationAuth.resolveToken(account.accessToken)
                .ifBlank { account.accessToken },
            scopes = account.scopes
        )
    }
}
