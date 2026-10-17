package io.rudione.chatone.presentation.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import io.rudione.chatone.presentation.screenchat.ScreenChatController
import io.rudione.chatone.util.system.ChannelPanelRequestBus
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
internal fun ChannelMenuRequestEffects(viewModel: MainViewModel, activeChannelLogin: String?) {
    val active by rememberUpdatedState(activeChannelLogin)
    LaunchedEffect(viewModel) {
        launch {
            ChannelPanelRequestBus.showChatters.collect { login ->
                if (!login.equals(active, ignoreCase = true)) viewModel.sendEvent(MainEvent.SelectChannel(login))
                viewModel.sendEvent(MainEvent.ShowChattersPanel)
            }
        }
        launch {
            ChannelPanelRequestBus.mentionsMuted.collect { (login, muted) ->
                viewModel.sendEvent(
                    if (muted) MainEvent.MuteMentionsChannel(login) else MainEvent.UnmuteMentionsChannel(login)
                )
            }
        }
    }
}

@Composable
internal fun ScreenChatChannelSync(openChannelLogins: List<String>) {
    val controller: ScreenChatController = koinInject()
    val seen = remember { mutableSetOf<String>() }
    LaunchedEffect(openChannelLogins) {
        val current = openChannelLogins.map { it.lowercase() }.toSet()
        val closed = seen - current
        if (closed.isNotEmpty()) controller.closeAll(closed)
        seen.clear()
        seen.addAll(current)
    }
}
