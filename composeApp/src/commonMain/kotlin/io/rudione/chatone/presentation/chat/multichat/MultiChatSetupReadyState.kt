package io.rudione.chatone.presentation.chat.multichat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import io.rudione.chatone.presentation.account.AccountManager


@Composable
fun rememberMultiChatReadyState(
    accountManager: AccountManager,
    panelManager: ChatPanelManager
): MultiChatReadyState {
    val activeId by accountManager.activeAccountId.collectAsState()
    val panels by panelManager.panels.collectAsState()
    return MultiChatReadyState(
        hasActiveAccount = activeId.isNotBlank(),
        panelCount = panels.size,
        ready = activeId.isNotBlank()
    )
}

data class MultiChatReadyState(
    val hasActiveAccount: Boolean,
    val panelCount: Int,
    val ready: Boolean
)
