package io.rudione.chatone.presentation.media

import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.media_chat_replay
import chatone.composeapp.generated.resources.media_replay_empty
import io.rudione.chatone.presentation.chat.PrivMsgItem
import io.rudione.chatone.presentation.chat.rendering.ChatFollowEffects
import io.rudione.chatone.presentation.chat.rendering.rememberChatFollowState
import io.rudione.chatone.presentation.settings.SettingsState
import io.rudione.chatone.presentation.settings.SettingsViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun MediaReplayChat(
    entries: List<ReplayEntry>,
    nowMs: () -> Long,
    primed: Boolean,
    hasClock: Boolean,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    val settingsViewModel: SettingsViewModel = koinViewModel()
    val settings by settingsViewModel.state.collectAsState()
    val now by rememberUpdatedState(nowMs)
    val revealed by remember(entries) { derivedStateOf { entries.revealedCount(now()) } }
    val visible = remember(entries, revealed) { entries.subList(0, revealed) }
    val list = rememberLazyListState()
    val follow = rememberChatFollowState(list)
    LaunchedEffect(primed) { if (!primed) follow.resume() }
    ChatFollowEffects(
        listState = list,
        newestItemKey = visible.lastOrNull()?.message?.id,
        paused = follow.isPausedByScroll,
        onPinnedToBottom = {}
    )
    Column(modifier) {
        Text(
            stringResource(Res.string.media_chat_replay),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )
        Box(Modifier.fillMaxSize()) {
            LazyColumn(
                state = list,
                contentPadding = contentPadding,
                modifier = Modifier.fillMaxSize().nestedScroll(follow.nestedScrollConnection)
            ) {
                items(visible, key = { it.message.id }) { entry ->
                    PrivMsgItem(
                        message = entry.message,
                        isCompact = true,
                        timestampFormat = if (hasClock) settings.timestampFormat else SettingsState.TimestampFormat.OFF,
                        showBadges = settings.showBadges,
                        emoteSize = settings.emoteSize,
                        chatFontSizeSp = settings.fontSize.replaySp(),
                        highlightRules = settings.highlightRules,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                            .animateItem(fadeInSpec = tween(REVEAL_FADE_MS), placementSpec = null, fadeOutSpec = null)
                    )
                }
            }
            when {
                !primed -> CircularProgressIndicator(
                    Modifier.align(Alignment.TopCenter).padding(top = 16.dp).size(20.dp),
                    strokeWidth = 2.dp
                )
                visible.isEmpty() -> Text(
                    stringResource(Res.string.media_replay_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }
}

internal fun List<ReplayEntry>.revealedCount(nowMs: Long): Int {
    var low = 0
    var high = size
    while (low < high) {
        val mid = (low + high) ushr 1
        if (this[mid].revealAtMs <= nowMs) low = mid + 1 else high = mid
    }
    return low
}

private fun SettingsState.FontSize.replaySp(): Float = when (this) {
    SettingsState.FontSize.SMALL -> 12f
    SettingsState.FontSize.MEDIUM -> 15f
    SettingsState.FontSize.LARGE -> 18f
}

private const val REVEAL_FADE_MS = 180
