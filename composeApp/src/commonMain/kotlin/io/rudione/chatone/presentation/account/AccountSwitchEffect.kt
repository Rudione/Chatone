package io.rudione.chatone.presentation.account

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue


@Composable
fun AccountSwitchEffect(
    accountManager: AccountManager,
    onSwitched: (newUserId: String) -> Unit
) {
    val activeId by accountManager.activeAccountId.collectAsState()
    LaunchedEffect(activeId) {
        if (activeId.isNotBlank()) onSwitched(activeId)
    }
}
