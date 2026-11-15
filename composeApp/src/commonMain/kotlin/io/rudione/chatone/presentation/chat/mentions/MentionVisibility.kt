package io.rudione.chatone.presentation.chat.mentions

import androidx.compose.foundation.lazy.LazyListLayoutInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged

const val MENTION_SEEN_DWELL_MS = 350L
private const val HALF_SHOWN = 0.5f
private const val FULLY_SHOWN = 1f

@Immutable
internal data class RowSpan(val key: Any, val top: Int, val bottom: Int)

internal fun shownKeys(
    rows: List<RowSpan>,
    viewportTop: Int,
    viewportBottom: Int,
    wanted: Set<String>,
    minShare: Float = HALF_SHOWN
): Set<String> {
    if (wanted.isEmpty() || viewportBottom <= viewportTop) return emptySet()
    val shown = HashSet<String>()
    for (row in rows) {
        val key = row.key as? String ?: continue
        if (key !in wanted) continue
        val height = row.bottom - row.top
        if (height <= 0) continue
        val overlap = minOf(row.bottom, viewportBottom) - maxOf(row.top, viewportTop)
        if (overlap >= height * minShare) shown += key
    }
    return shown
}

internal fun LazyListLayoutInfo.shownKeys(wanted: Set<String>, minShare: Float = HALF_SHOWN): Set<String> = shownKeys(
    rows = visibleItemsInfo.map { RowSpan(it.key, it.offset, it.offset + it.size) },
    viewportTop = viewportStartOffset,
    viewportBottom = viewportEndOffset - afterContentPadding,
    wanted = wanted,
    minShare = minShare
)

internal fun LazyListLayoutInfo.isFullyShown(key: String): Boolean =
    shownKeys(setOf(key), minShare = FULLY_SHOWN).isNotEmpty()

internal fun LazyListLayoutInfo.centerIndex(): Int {
    val items = visibleItemsInfo
    if (items.isEmpty()) return totalItemsCount - 1
    return (items.first().index + items.last().index) / 2
}

@Composable
fun MentionTrackingEffects(
    listState: LazyListState,
    mentions: MentionJumpState,
    attention: ChatAttention,
    followingLive: Boolean
) {
    val live by rememberUpdatedState(followingLive)
    LaunchedEffect(mentions, attention) {
        snapshotFlow { attention.isAttentive }.collect { mentions.updateAttention(it) }
    }
    LaunchedEffect(listState, mentions) {
        snapshotFlow { listState.layoutInfo.shownKeys(mentions.targetIds) }
            .distinctUntilChanged()
            .collect { mentions.updateOnScreen(it) }
    }
    LaunchedEffect(mentions, attention) {
        snapshotFlow { if (attention.isAttentive) mentions.pendingOnScreen else emptySet() }
            .distinctUntilChanged()
            .collectLatest { shown ->
                if (shown.isEmpty()) return@collectLatest
                delay(MENTION_SEEN_DWELL_MS)
                mentions.markSeen(shown, live)
            }
    }
}
