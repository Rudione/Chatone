package io.rudione.chatone.presentation.account

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import io.rudione.chatone.domain.model.TwitchAccount
import kotlinx.coroutines.flow.Flow


@Composable
fun rememberAccountListState(loader: AccountListLoader): List<TwitchAccount> {
    var list by remember { mutableStateOf<List<TwitchAccount>>(emptyList()) }
    LaunchedEffect(loader) {
        loader.list().collect { list = it }
    }
    return list
}


@Composable
fun rememberAccountUiState(
    loader: AccountListLoader,
    accountManager: AccountManager
): AccountUiState {
    val accounts = rememberAccountListState(loader)
    val activeId by accountManager.activeAccountId.collectAsState()
    return AccountUiState(
        accounts = accounts,
        activeUserId = activeId
    )
}
