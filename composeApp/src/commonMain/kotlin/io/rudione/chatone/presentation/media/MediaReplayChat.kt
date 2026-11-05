package io.rudione.chatone.presentation.media

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.media_chat_replay
import chatone.composeapp.generated.resources.media_replay_empty
import io.rudione.chatone.presentation.chat.PrivMsgItem
import io.rudione.chatone.presentation.settings.SettingsState
import io.rudione.chatone.presentation.settings.SettingsViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun MediaReplayChat(
    entries: List<ReplayEntry>,
    nowSeconds: Int,
    hasClock: Boolean,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    val settingsViewModel: SettingsViewModel = koinViewModel()
    val settings by settingsViewModel.state.collectAsState()
    val visible = remember(entries, nowSeconds) {
        entries.filter { it.offsetSeconds <= nowSeconds }.takeLast(VISIBLE_REPLAY)
    }
    val list = rememberLazyListState()
    LaunchedEffect(visible.lastOrNull()?.message?.id) {
        if (visible.isNotEmpty()) list.animateScrollToItem(visible.lastIndex)
    }
    Column(modifier) {
        Text(
            stringResource(Res.string.media_chat_replay),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )
        if (visible.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(contentPadding)) {
                Text(
                    stringResource(Res.string.media_replay_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
            return@Column
        }
        LazyColumn(
            state = list,
            contentPadding = contentPadding,
            modifier = Modifier.fillMaxSize()
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
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).animateItem()
                )
            }
        }
    }
}

private fun SettingsState.FontSize.replaySp(): Float = when (this) {
    SettingsState.FontSize.SMALL -> 12f
    SettingsState.FontSize.MEDIUM -> 15f
    SettingsState.FontSize.LARGE -> 18f
}

private const val VISIBLE_REPLAY = 150
