package io.rudione.chatone.presentation.chat.multichat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import io.rudione.chatone.presentation.chat.ChatViewModel
import org.koin.compose.koinInject

@Composable
fun PanelChatSlot(
    panelId: String,
    channelLogin: String,
    isCompact: Boolean,
    accessToken: String,
    currentUserId: String,
    currentUserLogin: String,
    currentDisplayName: String,
    content: @Composable (
        viewModel: ChatViewModel,
        channelLogin: String,
        isCompact: Boolean,
        modifier: Modifier
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: ChatViewModel = koinInject()

    DisposableEffect(panelId, channelLogin) {
        onDispose { }
    }

    content(viewModel, channelLogin, isCompact, modifier)
}
