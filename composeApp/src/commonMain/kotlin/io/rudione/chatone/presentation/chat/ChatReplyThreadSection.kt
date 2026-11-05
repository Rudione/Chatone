package io.rudione.chatone.presentation.chat

import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import io.rudione.chatone.domain.model.DisplayMessage
import io.rudione.chatone.presentation.chat.threads.ReplyThreadActions
import io.rudione.chatone.presentation.chat.threads.ReplyThreadOpener
import io.rudione.chatone.presentation.chat.threads.ReplyThreadSheet
import io.rudione.chatone.presentation.chat.threads.ThreadMessageStyle
import io.rudione.chatone.presentation.chat.threads.replyThreadOf
import io.rudione.chatone.presentation.settings.SettingsState
import kotlin.time.Clock

internal class ReplyThreadHost {
    var openRootId by mutableStateOf<String?>(null)
        private set

    val opener = ReplyThreadOpener { rootId -> openRootId = rootId }

    fun close() {
        openRootId = null
    }
}

@Composable
internal fun rememberReplyThreadHost(): ReplyThreadHost = remember { ReplyThreadHost() }

internal fun ProfilePopupStack.openMention(mention: String, messages: List<DisplayMessage>, channel: String) {
    val login = mention.removePrefix("@")
    val known = messages.lastOrNull { it is DisplayMessage.PrivMsg && it.username.equals(login, ignoreCase = true) }
    if (known is DisplayMessage.PrivMsg) open(known)
    else openLogin(login = login, channel = channel, nowMs = Clock.System.now().toEpochMilliseconds())
}

@Composable
internal fun ChatReplyThreadSection(
    host: ReplyThreadHost,
    state: ChatState,
    settings: SettingsState,
    profiles: ProfilePopupStack,
    onSend: (DisplayMessage.PrivMsg, String) -> Unit
) {
    val rootId = host.openRootId ?: return
    val thread = remember(rootId, state.messages) { replyThreadOf(state.messages, rootId) }
    val latestMessages by rememberUpdatedState(state.messages)
    if (thread.root == null && thread.origin == null && thread.replies.isEmpty()) {
        LaunchedEffect(rootId) { host.close() }
        return
    }
    val style = remember(settings.timestampFormat, settings.showBadges, settings.emoteSize, settings.fontSize, settings.highlightRules) {
        ThreadMessageStyle(
            timestampFormat = settings.timestampFormat,
            showBadges = settings.showBadges,
            emoteSize = settings.emoteSize,
            fontSizeSp = when (settings.fontSize) {
                SettingsState.FontSize.SMALL -> 12f
                SettingsState.FontSize.MEDIUM -> 15f
                SettingsState.FontSize.LARGE -> 18f
            },
            highlightRules = settings.highlightRules
        )
    }
    val actions = remember(host, profiles, state.channelLogin, onSend) {
        ReplyThreadActions(
            onOpenProfile = profiles::open,
            onMentionClick = { mention -> profiles.openMention(mention, latestMessages, state.channelLogin) },
            onSend = onSend,
            onDismiss = host::close
        )
    }
    ReplyThreadSheet(
        thread = thread,
        channelLogin = state.channelLogin,
        style = style,
        canReply = state.currentUserId.isNotEmpty(),
        actions = actions
    )
}
