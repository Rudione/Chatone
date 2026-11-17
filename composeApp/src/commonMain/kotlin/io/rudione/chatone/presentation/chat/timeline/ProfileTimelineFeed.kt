package io.rudione.chatone.presentation.chat.timeline

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.profile_load_more_history
import chatone.composeapp.generated.resources.profile_loading_history
import chatone.composeapp.generated.resources.profile_no_messages_in_session
import io.rudione.chatone.data.remote.LogMonth
import io.rudione.chatone.data.repository.EmoteRepository
import io.rudione.chatone.domain.model.DisplayMessage
import io.rudione.chatone.icons.lucide.Lucide
import io.rudione.chatone.icons.lucide.MessageSquare
import io.rudione.chatone.presentation.chat.ArchiveLineRow
import io.rudione.chatone.presentation.chat.PrivMsgItem
import io.rudione.chatone.presentation.chat.ProfileHistorySelection
import io.rudione.chatone.presentation.chat.ProfileMessageCopy
import io.rudione.chatone.presentation.chat.chatDayLabel
import io.rudione.chatone.presentation.chat.localEpochDay
import io.rudione.chatone.presentation.chat.rendering.PreserveScrollAnchor
import io.rudione.chatone.presentation.settings.SettingsState
import io.rudione.chatone.presentation.settings.SettingsViewModel
import io.rudione.chatone.util.chat.plainText
import io.rudione.chatone.util.system.isDesktopPlatform
import kotlinx.coroutines.Dispatchers
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
    fontSizeSp: Float? = null,
    viewport: ProfileFeedViewport? = null
) {
    val emotes: EmoteRepository = koinInject()
    val settings = remember { SettingsViewModel.loadInitialState() }
    val zone = remember { TimeZone.currentSystemDefault() }
    val renderer = remember(emotes) { PastMessageRenderer(emotes) }
    val rows by produceState<List<TimelineRow>?>(null, source.entries, author, renderer) {
        value = withContext(Dispatchers.Default) { renderer.rows(source.entries, zone, author) }
    }
    val shown = rows ?: return Box(modifier, contentAlignment = Alignment.Center) {
        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
    }
    if (shown.isEmpty() && !source.isLoadingOlder) {
        TimelineEmpty(source, modifier)
        return
    }
    val newestFirst = remember(shown) { shown.asReversed() }
    val listState = rememberLazyListState()
    PreserveScrollAnchor(listState, newestFirst) { it.key }
    KeepNewestInView(listState, newestFirst) { it.key }
    LoadOlderNearTop(listState, source.canLoadOlder, source.isLoadingOlder, source.onLoadOlder)
    TrackFeedViewport(viewport, listState, newestFirst) { it.month(zone) }

    val fontSize = fontSizeSp ?: when (settings.fontSize) {
        SettingsState.FontSize.SMALL -> 12f
        SettingsState.FontSize.MEDIUM -> 15f
        SettingsState.FontSize.LARGE -> 18f
    }
    val today = remember { localEpochDay(Clock.System.now().toEpochMilliseconds(), zone) }
    ProfileHistorySelection(modifier = modifier) {
        LazyColumn(
            state = listState,
            reverseLayout = true,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp, Alignment.Bottom)
        ) {
            items(newestFirst, key = { it.key }, contentType = { it::class }) { row ->
                Box(
                    Modifier.animateItem(
                        fadeInSpec = tween(ROW_FADE_MS),
                        placementSpec = spring(
                            stiffness = Spring.StiffnessMediumLow,
                            visibilityThreshold = IntOffset.VisibilityThreshold
                        ),
                        fadeOutSpec = null
                    )
                ) {
                    when (row) {
                        is TimelineRow.Day -> DisableSelection { DayBreak(chatDayLabel(row.epochDay, today)) }
                        is TimelineRow.Message -> ProfileMessageCopy(
                            row.message.rawMessage?.message ?: row.message.tokens.joinToString("") { it.plainText() }
                        ) { copy ->
                            PrivMsgItem(
                                message = row.message,
                                isCompact = true,
                                timestampFormat = settings.timestampFormat,
                                showBadges = settings.showBadges,
                                emoteSize = settings.emoteSize,
                                chatFontSizeSp = fontSize,
                                highlightRules = settings.highlightRules,
                                onMessageLongPress = copy.takeUnless { isDesktopPlatform },
                                onCopyText = copy,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        is TimelineRow.Event -> ArchiveLineRow(row.line, author.displayName, nameColor)
                    }
                }
            }
            item(key = OLDER_KEY, contentType = OLDER_KEY) { DisableSelection { OlderControl(source) } }
        }
    }
}

private fun TimelineRow.month(zone: TimeZone): LogMonth? = when (this) {
    is TimelineRow.Day -> monthOfEpochDay(epochDay)
    is TimelineRow.Message -> monthOfEpochMs(message.timestamp, zone)
    is TimelineRow.Event -> monthOfEpochMs(line.timestampMs, zone)
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

private const val OLDER_KEY = "older"
private const val ROW_FADE_MS = 220
