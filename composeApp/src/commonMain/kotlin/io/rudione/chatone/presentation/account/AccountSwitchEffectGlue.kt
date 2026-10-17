package io.rudione.chatone.presentation.account

import androidx.compose.runtime.getValue
import androidx.compose.runtime.SideEffect
import io.rudione.chatone.data.repository.AccountManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import io.rudione.chatone.data.repository.EmoteRepository

@Composable
fun AccountSwitchInvalidateEmotes(
    accountManager: AccountManager,
    emoteRepository: EmoteRepository
) {
    val activeId by accountManager.activeAccountId.collectAsState()
    SideEffect(activeId) {
        if (activeId.isNotBlank()) {

            emoteRepository.invalidatePersonalEmotes()
        }
    }
}
