package io.rudione.chatone.presentation.chat.multichat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import io.rudione.chatone.presentation.chat.ChatViewModel
import org.koin.core.context.GlobalContext


@Composable
fun rememberPanelChatViewModel(panelId: String): ChatViewModel {

    val vm = remember(panelId) {
        GlobalContext.get().get<ChatViewModel>()
    }
    DisposableEffect(panelId) {
        onDispose {

        }
    }
    return vm
}
