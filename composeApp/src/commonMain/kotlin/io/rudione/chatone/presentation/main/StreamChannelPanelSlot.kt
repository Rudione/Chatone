package io.rudione.chatone.presentation.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import io.rudione.chatone.data.repository.AiAssistantController
import io.rudione.chatone.domain.channel.ChannelSnapshot
import io.rudione.chatone.presentation.chat.ChatViewModel
import io.rudione.chatone.presentation.stream.channel.StreamChannelActions
import io.rudione.chatone.presentation.stream.channel.StreamChannelEvent
import io.rudione.chatone.presentation.stream.channel.StreamChannelExtras
import io.rudione.chatone.presentation.stream.channel.StreamChannelPanel
import io.rudione.chatone.presentation.stream.channel.StreamChannelViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun StreamChannelPanelSlot(
    streamLogin: String,
    state: MainState,
    onEvent: (MainEvent) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StreamChannelViewModel = koinViewModel(),
    assistant: AiAssistantController = koinInject()
) {
    val login = streamLogin.lowercase()
    val chatViewModel: ChatViewModel = koinViewModel()
    val chatHint by remember(chatViewModel, login) {
        chatViewModel.state
            .map { chat ->
                chat.takeIf { it.channelLogin.equals(login, ignoreCase = true) }?.let {
                    ChannelSnapshot(
                        login = login,
                        channelId = it.channelId,
                        displayName = it.channelDisplayName,
                        title = it.liveStream?.title.orEmpty(),
                        category = it.liveStream?.gameName.orEmpty()
                    )
                }
            }
            .distinctUntilChanged()
    }.collectAsState(null)
    val pinned = state.openChannels.firstOrNull { it.login == login }
    val tab = pinned ?: state.recentChannels.firstOrNull { it.login == login }
    val hint = remember(chatHint, tab, login) {
        val fromTab = ChannelSnapshot(login = login, displayName = tab?.displayName.orEmpty(), avatarUrl = tab?.profileImageUrl.orEmpty())
        chatHint?.let(fromTab::mergedWith) ?: fromTab
    }
    LaunchedEffect(viewModel, login, hint) { viewModel.sendEvent(StreamChannelEvent.Bind(login, hint)) }

    val channelState by viewModel.state.collectAsState()
    val assistantEnabled by assistant.enabled.collectAsState()
    val extras = StreamChannelExtras(
        liveAlerts = login in state.liveNotifyChannels,
        canPin = pinned == null && tab != null,
        assistantAvailable = assistantEnabled,
        chatElsewhere = !state.activeChannelLogin.equals(login, ignoreCase = true)
    )

    val uriHandler = LocalUriHandler.current
    val sendMain by rememberUpdatedState(onEvent)
    val channel by rememberUpdatedState(channelState.channel)
    val actions = remember(viewModel, assistant, uriHandler, login) {
        StreamChannelActions(
            onToggleFollow = { viewModel.sendEvent(StreamChannelEvent.ToggleFollow) },
            onToggleLiveAlerts = { sendMain(MainEvent.ToggleLiveNotify(login)) },
            onPin = { sendMain(MainEvent.KeepRecentChannel(login)) },
            onOpenAssistant = assistant::open,
            onReturnToStreamChat = {
                sendMain(MainEvent.OpenRecentChannel(login, channel.displayName, channel.avatarUrl))
            },
            onOpenOnTwitch = { uriHandler.openUri("https://www.twitch.tv/$login") }
        )
    }
    StreamChannelPanel(state = channelState, extras = extras, actions = actions, modifier = modifier)
}
