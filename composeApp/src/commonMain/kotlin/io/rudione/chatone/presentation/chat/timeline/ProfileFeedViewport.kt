package io.rudione.chatone.presentation.chat.timeline

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import io.rudione.chatone.data.remote.LogMonth
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

@Stable
internal class ProfileFeedViewport {
    var visibleMonth: LogMonth? by mutableStateOf(null)
        private set
    var isAwayFromNewest: Boolean by mutableStateOf(false)
        private set

    internal fun update(month: LogMonth?, away: Boolean) {
        visibleMonth = month
        isAwayFromNewest = away
    }

    internal fun reset() = update(null, false)
}

@Composable
internal fun rememberProfileFeedViewport(key: Any?): ProfileFeedViewport = remember(key) { ProfileFeedViewport() }

@Composable
internal fun <T> TrackFeedViewport(
    viewport: ProfileFeedViewport?,
    listState: LazyListState,
    newestFirst: List<T>,
    monthOf: (T) -> LogMonth?
) {
    if (viewport == null) return
    val rows by rememberUpdatedState(newestFirst)
    val month by rememberUpdatedState(monthOf)
    val density = LocalDensity.current
    val leaveAt = with(density) { AWAY_FROM_NEWEST_ENTER.toPx() }
    val returnAt = with(density) { AWAY_FROM_NEWEST_EXIT.toPx() }
    LaunchedEffect(viewport, listState, leaveAt, returnAt) {
        snapshotFlow {
            val current = rows
            val top = listState.layoutInfo.visibleItemsInfo.lastOrNull { it.index < current.size }?.index
            val distance = if (listState.firstVisibleItemIndex > 0) Float.MAX_VALUE
            else listState.firstVisibleItemScrollOffset.toFloat()
            val away = distance > if (viewport.isAwayFromNewest) returnAt else leaveAt
            FeedPosition(top?.let { monthNear(current, it, month) }, away)
        }.distinctUntilChanged().collect { viewport.update(it.month, it.away) }
    }
    DisposableEffect(viewport) { onDispose { viewport.reset() } }
}

internal fun <T> monthNear(rows: List<T>, index: Int, monthOf: (T) -> LogMonth?): LogMonth? {
    for (candidate in index downTo 0) monthOf(rows[candidate])?.let { return it }
    for (candidate in index + 1 until rows.size) monthOf(rows[candidate])?.let { return it }
    return null
}

internal fun monthOfEpochMs(epochMs: Long, zone: TimeZone): LogMonth? {
    if (epochMs <= 0L) return null
    val date = Instant.fromEpochMilliseconds(epochMs).toLocalDateTime(zone).date
    return LogMonth(date.year, date.month.ordinal + 1)
}

internal fun monthOfEpochDay(epochDay: Long): LogMonth {
    val date = LocalDate.fromEpochDays(epochDay)
    return LogMonth(date.year, date.month.ordinal + 1)
}

private data class FeedPosition(val month: LogMonth?, val away: Boolean)

private val AWAY_FROM_NEWEST_ENTER = 40.dp
private val AWAY_FROM_NEWEST_EXIT = 6.dp
