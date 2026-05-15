package io.rudione.chatone.presentation.account

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import io.rudione.chatone.data.repository.EmoteRepository


@Composable
fun AccountSwitchInvalidateEmotes(
    accountManager: AccountManager,
    emoteRepository: EmoteRepository
) {
    val activeId by accountManager.activeAccountId.collectAsState()
    LaunchedEffect(activeId) {
        if (activeId.isNotBlank()) {

            emoteRepository.invalidatePersonalEmotes()
        }
    }
}
