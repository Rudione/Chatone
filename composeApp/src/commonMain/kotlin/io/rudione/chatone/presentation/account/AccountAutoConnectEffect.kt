package io.rudione.chatone.presentation.account

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import io.rudione.chatone.data.repository.AuthRepository
import io.rudione.chatone.data.repository.MultiAccountConnectionRegistry

@Composable
fun AccountAutoConnectEffect(
    authRepository: AuthRepository,
    registry: MultiAccountConnectionRegistry,
    accountManager: AccountManager
) {
    val activeId by accountManager.activeAccountId.collectAsState()
    LaunchedEffect(activeId) {
        if (activeId.isBlank()) return@LaunchedEffect

        val accounts = try {
            authRepository.getAccounts()
        } catch (_: Exception) { return@LaunchedEffect }
        accounts.collect { list ->
            val active = list.firstOrNull { it.userId == activeId } ?: return@collect
            if (!registry.isConnected(active.userId)) {
                registry.connect(active)
            }
        }
    }
}
