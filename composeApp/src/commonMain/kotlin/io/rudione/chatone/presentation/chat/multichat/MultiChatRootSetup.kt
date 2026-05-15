package io.rudione.chatone.presentation.chat.multichat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import io.rudione.chatone.presentation.account.AccountInitializer
import io.rudione.chatone.presentation.account.AccountManager
import io.rudione.chatone.presentation.account.AccountMigration
import io.rudione.chatone.presentation.account.AccountStateRefresher
import org.koin.compose.koinInject


@Composable
fun MultiChatRootSetup(
    onActiveAccountChanged: (userId: String) -> Unit = {}
) {
    val panelManager: ChatPanelManager = koinInject()
    val persistence: PanelPersistence = koinInject()
    val lifecycleSync: PanelLifecycleSync = koinInject()
    val accountInit: AccountInitializer = koinInject()
    val accountMigration: AccountMigration = koinInject()
    val accountManager: AccountManager = koinInject()
    val refresher: AccountStateRefresher = koinInject()

    LaunchedEffect(Unit) {
        accountMigration.run {
            accountInit.initialize { activeUserId ->
                onActiveAccountChanged(activeUserId)
            }
        }
        refresher.startPeriodicRefresh()
        lifecycleSync.attach(panelManager)
        persistence.load(panelManager)
    }

    val activeId by accountManager.activeAccountId.collectAsState()
    LaunchedEffect(activeId) {
        if (activeId.isNotBlank()) onActiveAccountChanged(activeId)
    }
}
