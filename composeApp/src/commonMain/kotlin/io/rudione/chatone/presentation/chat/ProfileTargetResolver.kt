package io.rudione.chatone.presentation.chat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import io.rudione.chatone.data.remote.ChatterFame
import io.rudione.chatone.data.remote.TwitchChatterFameClient
import io.rudione.chatone.domain.model.DisplayMessage
import org.koin.compose.koinInject

@Composable
internal fun rememberResolvedProfileMessage(message: DisplayMessage.PrivMsg): DisplayMessage.PrivMsg {
    val client: TwitchChatterFameClient = koinInject()
    var resolved by remember(message) { mutableStateOf(message) }
    LaunchedEffect(message) {
        if (message.userId.isNotEmpty() || message.username.isBlank()) return@LaunchedEffect
        val login = message.username.lowercase()
        val user = client.lookupLogins(listOf(login))?.get(login) ?: return@LaunchedEffect
        resolved = message.withUser(user)
    }
    return resolved
}

internal fun DisplayMessage.PrivMsg.withUser(user: ChatterFame): DisplayMessage.PrivMsg = copy(
    userId = user.userId,
    username = user.login.ifBlank { username },
    displayName = user.displayName.ifBlank { displayName },
    color = color ?: user.chatColor
)
