package io.rudione.chatone.presentation.chat.timeline

import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.DisableSelection
import io.rudione.chatone.presentation.chat.rendering.ChatSelectionContainer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.profile_load_more_history
import chatone.composeapp.generated.resources.profile_loading_history
import chatone.composeapp.generated.resources.profile_no_messages_in_session
import io.rudione.chatone.data.repository.EmoteRepository
import io.rudione.chatone.domain.model.ChatMessage
import io.rudione.chatone.domain.model.DisplayMessage
import io.rudione.chatone.util.chat.MessageToken
import io.rudione.chatone.icons.lucide.Lucide
import io.rudione.chatone.icons.lucide.MessageSquare
import io.rudione.chatone.presentation.chat.ArchiveLineRow
import io.rudione.chatone.presentation.chat.PrivMsgItem
import io.rudione.chatone.presentation.chat.chatDayLabel
import io.rudione.chatone.presentation.chat.localEpochDay
import io.rudione.chatone.presentation.settings.SettingsState
import io.rudione.chatone.presentation.settings.SettingsViewModel
import io.rudione.chatone.util.chat.MessageTokenizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import kotlin.time.Clock

@Immutable
class TimelineSource(
    val entries: List<TimelineEntry>,
    val isLoadingOlder: Boolean,
    val canLoadOlder: Boolean,
    val onLoadOlder: () -> Unit
)

@Composable
internal fun ProfileTimelineFeed(
    source: TimelineSource,
    author: DisplayMessage.PrivMsg,
    nameColor: Color,
    modifier: Modifier = Modifier,
    fontSizeSp: Float? = null
) {
    val emotes: EmoteRepository = koinInject()
    val settings = remember { SettingsViewModel.loadInitialState() }
    val zone = remember { TimeZone.currentSystemDefault() }
    val rows by produceState<List<TimelineRow>?>(null, source.entries, author) {
        value = withContext(Dispatchers.Default) {
            ProfileTimeline.rows(source.entries, zone) { past -> pastMessage(emotes, author, past) }
        }
    }
    val shown = rows ?: return Box(modifier, contentAlignment = Alignment.Center) {
        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
    }
    if (shown.isEmpty() && !source.isLoadingOlder) {
        TimelineEmpty(source, modifier)
        return
    }
    val listState = rememberLazyListState()
    var armed by remember { mutableStateOf(false) }
    var stickToBottom by remember { mutableStateOf(true) }
    val latest by rememberUpdatedState(source)
    val total = shown.size + 1

    LaunchedEffect(total) {
        if (!armed) {
            listState.scrollToItem(total - 1)
            armed = true
        } else if (stickToBottom) {
            listState.animateScrollToItem(total - 1)
        }
    }
    LaunchedEffect(listState) {
        snapshotFlow {
            val info = listState.layoutInfo
            (info.visibleItemsInfo.lastOrNull()?.index ?: 0) >= info.totalItemsCount - 2
        }.collect { stickToBottom = it }
    }
    LaunchedEffect(listState, armed) {
        if (!armed) return@LaunchedEffect
        snapshotFlow { listState.firstVisibleItemIndex <= 1 && latest.canLoadOlder && !latest.isLoadingOlder }
            .distinctUntilChanged()
            .collect { atTop -> if (atTop) latest.onLoadOlder() }
    }

    val fontSize = fontSizeSp ?: when (settings.fontSize) {
        SettingsState.FontSize.SMALL -> 12f
        SettingsState.FontSize.MEDIUM -> 15f
        SettingsState.FontSize.LARGE -> 18f
    }
    val today = remember { localEpochDay(Clock.System.now().toEpochMilliseconds(), zone) }
    ChatSelectionContainer(modifier = modifier) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            item(key = "older", contentType = "older") { DisableSelection { OlderControl(source) } }
            items(shown, key = { it.key }, contentType = { it::class }) { row ->
                when (row) {
                    is TimelineRow.Day -> DisableSelection { DayBreak(chatDayLabel(row.epochDay, today)) }
                    is TimelineRow.Message -> PrivMsgItem(
                        message = row.message,
                        isCompact = true,
                        timestampFormat = settings.timestampFormat,
                        showBadges = settings.showBadges,
                        emoteSize = settings.emoteSize,
                        chatFontSizeSp = fontSize,
                        highlightRules = settings.highlightRules,
                        modifier = Modifier.fillMaxWidth()
                    )
                    is TimelineRow.Event -> ArchiveLineRow(row.line, author.displayName, nameColor)
                }
            }
        }
    }
}

@Composable
private fun OlderControl(source: TimelineSource) {
    when {
        source.isLoadingOlder -> Row(
            Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(Modifier.size(13.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(6.dp))
            Text(
                stringResource(Res.string.profile_loading_history),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        source.canLoadOlder -> TextButton(onClick = source.onLoadOlder, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(Res.string.profile_load_more_history), style = MaterialTheme.typography.labelSmall)
        }
        else -> Spacer(Modifier.height(1.dp))
    }
}

@Composable
private fun DayBreak(label: String) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val line = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
        HorizontalDivider(Modifier.weight(1f), color = line)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
        HorizontalDivider(Modifier.weight(1f), color = line)
    }
}

@Composable
private fun TimelineEmpty(source: TimelineSource, modifier: Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(
                Lucide.MessageSquare,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
            )
            Text(
                stringResource(Res.string.profile_no_messages_in_session),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
            OlderControl(source)
        }
    }
}

private fun pastMessage(emotes: EmoteRepository, author: DisplayMessage.PrivMsg, past: TimelineEntry.Past): DisplayMessage.PrivMsg {
    val raw = ChatMessage(
        id = past.id,
        channelId = author.rawMessage?.channelId.orEmpty(),
        channelName = author.channel,
        userId = author.userId,
        username = author.username,
        displayName = author.displayName,
        message = past.text,
        timestamp = past.timestamp,
        color = author.color
    )
    val tokens = runCatching {
        MessageTokenizer.tokenize(raw, emotes.getResolvedEmotes(author.channel), personalEmotes = emotes.getCachedPersonalEmotes(author.userId))
    }.getOrElse { listOf(MessageToken.Text(past.text)) }
    return author.copy(
        id = past.id,
        timestamp = past.timestamp,
        tokens = tokens,
        isDeleted = past.deleted,
        isMention = false,
        isAction = false,
        isFirstMessage = false,
        isHighlighted = false,
        customRewardId = null,
        rewardName = null,
        rawMessage = raw,
        highlightColor = null
    )
}

fun profileAuthor(
    session: List<DisplayMessage.PrivMsg>,
    userId: String,
    username: String,
    displayName: String,
    color: String?,
    channel: String
): DisplayMessage.PrivMsg = session.lastOrNull()?.copy(rawMessage = session.last().rawMessage?.copy(replyParentMsgBody = null, replyParentDisplayName = null))
    ?: DisplayMessage.PrivMsg(
        id = "author_$userId",
        timestamp = 0L,
        channel = channel,
        userId = userId,
        username = username,
        displayName = displayName,
        tokens = emptyList(),
        color = color,
        badges = emptyList(),
        isModerator = false,
        isSubscriber = false,
        isVip = false,
        isBroadcaster = false,
        isMention = false,
        isAction = false
    )
