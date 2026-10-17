package io.rudione.chatone.presentation.screenchat

import androidx.compose.runtime.SideEffect
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import io.rudione.chatone.domain.model.DisplayMessage
import io.rudione.chatone.domain.model.ScreenChatSettings
import io.rudione.chatone.presentation.chat.ModerationMsgItem
import io.rudione.chatone.presentation.chat.PrivMsgItem
import io.rudione.chatone.presentation.chat.UserNoticeMsgItem
import io.rudione.chatone.presentation.chat.rendering.ChatFollowEffects
import io.rudione.chatone.presentation.chat.rendering.rememberChatFollowState
import io.rudione.chatone.presentation.settings.SettingsState
import io.rudione.chatone.util.chat.MessageToken

@Composable
internal fun ScreenChatMessages(
    messages: List<DisplayMessage>,
    chat: SettingsState,
    settings: ScreenChatSettings,
    live: Boolean,
    modifier: Modifier = Modifier
) {
    val shown = remember(messages, settings.showModeration) { screenChatItems(messages, settings.showModeration) }
    val listState = rememberLazyListState()
    val follow = rememberChatFollowState(listState)
    SideEffect(live) { if (live) follow.resume() }
    ChatFollowEffects(
        listState = listState,
        newestItemKey = shown.lastOrNull()?.id,
        paused = follow.isPausedByScroll,
        onPinnedToBottom = {}
    )
    val backdrop = Modifier.background(Color.Black.copy(alpha = settings.backdropAlpha), RowShape)
    val rowModifier = if (settings.backdrop) Modifier.fillMaxWidth().then(backdrop) else Modifier.fillMaxWidth()
    val fontSize = settings.fontSize.toFloat()
    val replyAbove = settings.replyAbove
    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(follow.nestedScrollConnection)
            .alpha(settings.textAlpha),
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.Bottom)
    ) {
        items(shown, key = { it.id }, contentType = { it::class }) { message ->
            Box(rowModifier) {
                when (message) {
                    is DisplayMessage.PrivMsg -> PrivMsgItem(
                        message = remember(message, replyAbove) { if (replyAbove) message else message.withInlineReply() },
                        timestampFormat = SettingsState.TimestampFormat.OFF,
                        showBadges = chat.showBadges,
                        emoteSize = chat.emoteSize,
                        chatFontSizeSp = fontSize,
                        highlightRules = chat.highlightRules,
                        modifier = Modifier.fillMaxWidth()
                    )
                    is DisplayMessage.UserNoticeMsg -> UserNoticeMsgItem(message)
                    is DisplayMessage.ModerationMsg -> ModerationMsgItem(message = message, chatFontSizeSp = fontSize)
                    else -> Unit
                }
            }
        }
    }
}

internal fun screenChatItems(messages: List<DisplayMessage>, showModeration: Boolean): List<DisplayMessage> = messages
    .asSequence()
    .filter { it is DisplayMessage.PrivMsg || it is DisplayMessage.UserNoticeMsg || (showModeration && it is DisplayMessage.ModerationMsg) }
    .toList()
    .takeLast(MAX_ITEMS)

internal fun DisplayMessage.PrivMsg.withInlineReply(): DisplayMessage.PrivMsg {
    val raw = rawMessage ?: return this
    val login = raw.replyParentUserLogin
    val name = raw.replyParentDisplayName
    if (raw.replyParentMsgBody == null || (login == null && name == null)) return this
    val shownName = when {
        login == null -> name
        name != null && name.equals(login, ignoreCase = true) -> name
        else -> login
    }
    return copy(
        tokens = listOf(MessageToken.Mention("@$shownName"), MessageToken.Text(" ")) + tokens,
        rawMessage = raw.copy(replyParentDisplayName = null, replyParentMsgBody = null)
    )
}

private val RowShape = RoundedCornerShape(8.dp)
private const val MAX_ITEMS = 200
