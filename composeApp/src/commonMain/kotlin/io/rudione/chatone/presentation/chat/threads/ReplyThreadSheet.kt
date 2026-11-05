package io.rudione.chatone.presentation.chat.threads

import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import io.rudione.chatone.util.system.isDesktopPlatform
import io.rudione.chatone.presentation.window.windowDragArea
import io.rudione.chatone.presentation.chat.UserProfileContainer
import io.rudione.chatone.presentation.chat.ProfileCardHeaderActions
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.Key
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.thread_origin
import chatone.composeapp.generated.resources.thread_replies
import chatone.composeapp.generated.resources.thread_reply_hint
import chatone.composeapp.generated.resources.thread_send
import chatone.composeapp.generated.resources.thread_sign_in
import chatone.composeapp.generated.resources.thread_subtitle
import chatone.composeapp.generated.resources.thread_title
import io.rudione.chatone.domain.model.DisplayMessage
import io.rudione.chatone.domain.model.HighlightRule
import io.rudione.chatone.icons.lucide.Lucide
import io.rudione.chatone.icons.lucide.MessagesSquare
import io.rudione.chatone.icons.lucide.SendHorizontal
import io.rudione.chatone.presentation.chat.PrivMsgItem
import io.rudione.chatone.presentation.components.ChatoneTextField
import io.rudione.chatone.presentation.components.FitText
import io.rudione.chatone.presentation.components.expressive.ExpressiveSheet
import io.rudione.chatone.presentation.screenchat.withInlineReply
import io.rudione.chatone.presentation.settings.SettingsState
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@Immutable
class ThreadMessageStyle(
    val timestampFormat: SettingsState.TimestampFormat,
    val showBadges: Boolean,
    val emoteSize: SettingsState.EmoteSize,
    val fontSizeSp: Float,
    val highlightRules: List<HighlightRule>
)

@Immutable
class ReplyThreadActions(
    val onOpenProfile: (DisplayMessage.PrivMsg) -> Unit,
    val onMentionClick: (String) -> Unit,
    val onSend: (DisplayMessage.PrivMsg, String) -> Unit,
    val onDismiss: () -> Unit
)

@Composable
internal fun ReplyThreadSheet(
    thread: ReplyThread,
    channelLogin: String,
    style: ThreadMessageStyle,
    canReply: Boolean,
    actions: ReplyThreadActions
) {
    if (isDesktopPlatform) {
        ReplyThreadWindow(thread, channelLogin, style, canReply, actions)
        return
    }
    ExpressiveSheet(onDismissRequest = actions.onDismiss) {
        ThreadHeader(thread, channelLogin, Modifier.padding(start = 20.dp, end = 20.dp, bottom = 8.dp))
        ThreadBody(thread, style, actions, Modifier.weight(1f, fill = false).heightIn(max = 460.dp))
        ThreadComposer(thread, canReply, actions)
    }
}

@Composable
private fun ReplyThreadWindow(
    thread: ReplyThread,
    channelLogin: String,
    style: ThreadMessageStyle,
    canReply: Boolean,
    actions: ReplyThreadActions
) {
    var pinned by remember { mutableStateOf(false) }
    var width by remember { mutableStateOf(ThreadWindowWidth) }
    var height by remember { mutableStateOf(ThreadWindowHeight) }
    UserProfileContainer(
        isPinned = pinned,
        width = width,
        height = height,
        onResize = { w, h -> width = w; height = h },
        onDismiss = actions.onDismiss
    ) {
        Column(
            Modifier.fillMaxSize().onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && event.key == Key.Escape) {
                    actions.onDismiss()
                    true
                } else false
            }
        ) {
            Row(
                Modifier.fillMaxWidth().windowDragArea().padding(start = 12.dp, end = 6.dp, top = 8.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ThreadHeader(thread, channelLogin, Modifier.weight(1f))
                ProfileCardHeaderActions(
                    isPinned = pinned,
                    onTogglePin = { pinned = !pinned },
                    onDetach = null,
                    onDismiss = actions.onDismiss
                )
            }
            ThreadBody(thread, style, actions, Modifier.weight(1f))
            ThreadComposer(thread, canReply, actions)
        }
    }
}

@Composable
private fun ThreadBody(thread: ReplyThread, style: ThreadMessageStyle, actions: ReplyThreadActions, modifier: Modifier) {
    val listState = rememberLazyListState()
    LaunchedEffect(thread.replies.size) { listState.animateScrollToItem(thread.replies.size) }
    CompositionLocalProvider(LocalReplyThreadOpener provides null) {
        LazyColumn(
            state = listState,
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            item(key = "thread_root") {
                val root = thread.root
                if (root != null) {
                    RootFrame { ThreadMessage(root.withoutReplyContext(), style, actions) }
                } else {
                    thread.origin?.let { OriginCard(it, actions) }
                }
            }
            items(thread.replies, key = { it.id }) { reply ->
                val shown = if (reply.rawMessage?.replyParentMsgId == thread.rootId) reply.withoutReplyContext() else reply.withInlineReply()
                ThreadMessage(shown, style, actions, Modifier.padding(start = 14.dp))
            }
        }
    }
}

@Composable
private fun ThreadHeader(thread: ReplyThread, channelLogin: String, modifier: Modifier) {
    Row(
        modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Lucide.MessagesSquare, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        }
        Column(Modifier.weight(1f)) {
            FitText(
                stringResource(Res.string.thread_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            FitText(
                stringResource(Res.string.thread_subtitle, thread.starterName, channelLogin) + " · " +
                    pluralStringResource(Res.plurals.thread_replies, thread.replies.size, thread.replies.size),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private val ThreadWindowWidth = 380.dp
private val ThreadWindowHeight = 480.dp

@Composable
private fun RootFrame(content: @Composable () -> Unit) {
    val accent = MaterialTheme.colorScheme.primary
    Box(
        Modifier
            .padding(horizontal = 8.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(accent.copy(alpha = 0.08f))
            .drawBehind { drawRect(accent, size = Size(3.dp.toPx(), size.height)) }
            .padding(start = 6.dp, top = 2.dp, bottom = 2.dp)
    ) { content() }
}

@Composable
private fun OriginCard(origin: ThreadOrigin, actions: ReplyThreadActions) {
    RootFrame {
        Column(Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
            Text(
                stringResource(Res.string.thread_origin),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row {
                Text(
                    origin.displayName + ": ",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable(role = Role.Button) { origin.login?.let { actions.onMentionClick("@$it") } }
                )
                Text(origin.body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
private fun ThreadMessage(
    message: DisplayMessage.PrivMsg,
    style: ThreadMessageStyle,
    actions: ReplyThreadActions,
    modifier: Modifier = Modifier
) {
    PrivMsgItem(
        message = message,
        isCompact = true,
        timestampFormat = style.timestampFormat,
        showBadges = style.showBadges,
        emoteSize = style.emoteSize,
        chatFontSizeSp = style.fontSizeSp,
        highlightRules = style.highlightRules,
        onUsernameClick = { actions.onOpenProfile(message) },
        onMentionClick = actions.onMentionClick,
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
private fun ThreadComposer(thread: ReplyThread, canReply: Boolean, actions: ReplyThreadActions) {
    var draft by rememberSaveable(thread.rootId) { mutableStateOf("") }
    val target = thread.replyTarget
    val send = {
        if (target != null && draft.isNotBlank()) {
            actions.onSend(target, draft)
            draft = ""
        }
    }
    Row(
        Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.ime.exclude(WindowInsets.navigationBars))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ChatoneTextField(
            value = draft,
            onValueChange = { draft = it },
            modifier = Modifier.weight(1f),
            placeholder = stringResource(if (canReply) Res.string.thread_reply_hint else Res.string.thread_sign_in),
            enabled = canReply && target != null,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { send() })
        )
        Box(Modifier.width(8.dp))
        val active = canReply && target != null && draft.isNotBlank()
        Box(
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest)
                .clickable(enabled = active, role = Role.Button) { send() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Lucide.SendHorizontal,
                contentDescription = stringResource(Res.string.thread_send),
                tint = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

private fun DisplayMessage.PrivMsg.withoutReplyContext(): DisplayMessage.PrivMsg {
    val raw = rawMessage ?: return this
    if (raw.replyParentMsgBody == null) return this
    return copy(rawMessage = raw.copy(replyParentDisplayName = null, replyParentMsgBody = null))
}
