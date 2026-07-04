package io.rudione.chatone.presentation.main.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import io.rudione.chatone.domain.model.MentionEntry
import io.rudione.chatone.presentation.components.LiquidGlassSurface
import io.rudione.chatone.presentation.main.MainEvent
import io.rudione.chatone.presentation.main.MainState
import io.rudione.chatone.presentation.theme.i18n.LocalStrings
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime


enum class MentionFilter { ALL, UNREAD, TODAY, BY_USER, BY_CHANNEL }
enum class MentionSort { NEWEST, OLDEST, BY_USER, BY_CHANNEL }


@Composable
fun MentionsFeed(
    state: MainState,
    onEvent: (MainEvent) -> Unit,
    onChannelClick: (login: String, messageId: String) -> Unit,
    modifier: Modifier = Modifier,
    fillAvailableSpace: Boolean = false,
    onCloseClick: (() -> Unit)? = null
) {
    val listState = rememberLazyListState()
    var filter by remember { mutableStateOf(MentionFilter.ALL) }
    var sort by remember { mutableStateOf(MentionSort.NEWEST) }
    var showFilterMenu by remember { mutableStateOf(false) }
    var userSearchQuery by remember { mutableStateOf("") }
    var showUserSearchBar by remember { mutableStateOf(false) }

    LaunchedEffect(state.showMentionsFeed) {
        if (state.showMentionsFeed) {
            listState.scrollToItem(index = 0)
        }
    }

    val displayedMentions = remember(state.mentions, filter, sort, userSearchQuery) {
        val now = Clock.System.now().toEpochMilliseconds()
        val dayMs = 24L * 60L * 60L * 1000L
        val preFiltered = when (filter) {
            MentionFilter.ALL -> state.mentions
            MentionFilter.UNREAD -> state.mentions.filter { !it.isRead }
            MentionFilter.TODAY -> state.mentions.filter { now - it.timestamp < dayMs }
            MentionFilter.BY_USER -> state.mentions
            MentionFilter.BY_CHANNEL -> state.mentions
        }
        val filtered = if (userSearchQuery.isNotBlank()) {
            preFiltered.filter {
                it.fromUsername.contains(userSearchQuery, ignoreCase = true) ||
                        it.fromDisplayName.contains(userSearchQuery, ignoreCase = true)
            }
        } else preFiltered
        when {
            filter == MentionFilter.BY_USER ->
                filtered.sortedWith(compareBy({ it.fromUsername.lowercase() }, { -it.timestamp }))
            filter == MentionFilter.BY_CHANNEL ->
                filtered.sortedWith(compareBy({ it.channelLogin.lowercase() }, { -it.timestamp }))
            sort == MentionSort.NEWEST -> filtered.sortedByDescending { it.timestamp }
            sort == MentionSort.OLDEST -> filtered.sortedBy { it.timestamp }
            sort == MentionSort.BY_USER ->
                filtered.sortedWith(compareBy({ it.fromUsername.lowercase() }, { -it.timestamp }))
            sort == MentionSort.BY_CHANNEL ->
                filtered.sortedWith(compareBy({ it.channelLogin.lowercase() }, { -it.timestamp }))
            else -> filtered
        }
    }

    LiquidGlassSurface(
        modifier = if (fillAvailableSpace) modifier.fillMaxSize() else modifier.width(340.dp).heightIn(max = 520.dp),
        shape = RoundedCornerShape(20.dp),
        contentPadding = PaddingValues(0.dp),
        glassIntensity = 0.96f,
        backgroundAlphaHigh = 0.97f,
        backgroundAlphaLow = 0.92f,
        borderAlphaHigh = 0.22f,
        borderAlphaLow = 0.08f
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {


            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                                Color.Transparent
                            )
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Notifications, null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    LocalStrings.current.panelMentionsTitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Box {
                    IconButton(
                        onClick = { showFilterMenu = true },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Outlined.FilterList, null,
                            modifier = Modifier.size(16.dp),
                            tint = if (filter != MentionFilter.ALL || sort != MentionSort.NEWEST)
                                MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (showFilterMenu) {
                        Popup(
                            alignment = Alignment.TopEnd,
                            offset = IntOffset(0, 36),
                            onDismissRequest = { showFilterMenu = false },
                            properties = PopupProperties(focusable = true)
                        ) {
                            MentionFilterMenu(
                                currentFilter = filter,
                                currentSort = sort,
                                onFilterChange = { filter = it },
                                onSortChange = { sort = it },
                                onDismiss = { showFilterMenu = false }
                            )
                        }
                    }
                }
                if (state.unreadMentionsCount > 0) {
                    TextButton(
                        onClick = { onEvent(MainEvent.MarkAllMentionsRead) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text(
                            LocalStrings.current.panelMentionsMarkAllRead,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                IconButton(
                    onClick = { showUserSearchBar = !showUserSearchBar; if (!showUserSearchBar) userSearchQuery = "" },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Filled.Search, null,
                        modifier = Modifier.size(16.dp),
                        tint = if (userSearchQuery.isNotBlank()) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = { onCloseClick?.invoke() ?: onEvent(MainEvent.HideMentionsFeed) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Filled.Close, null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (showUserSearchBar) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = userSearchQuery,
                        onValueChange = { userSearchQuery = it },
                        modifier = Modifier.weight(1f).height(42.dp),
                        placeholder = {
                            Text(
                                LocalStrings.current.panelMentionsFilterByUserSearch,
                                style = MaterialTheme.typography.bodySmall
                            )
                        },
                        textStyle = MaterialTheme.typography.bodySmall,
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                            focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        leadingIcon = {
                            Icon(Icons.Filled.Search, null, modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        },
                        trailingIcon = if (userSearchQuery.isNotEmpty()) {
                            { IconButton(onClick = { userSearchQuery = "" }, modifier = Modifier.size(20.dp)) {
                                Icon(Icons.Filled.Close, null, modifier = Modifier.size(12.dp)) }
                            }
                        } else null
                    )
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))


            if (displayedMentions.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.Notifications, null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            LocalStrings.current.panelMentionsEmpty,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Text(
                            LocalStrings.current.panelMentionsHint,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = if (fillAvailableSpace) Modifier.fillMaxWidth().weight(1f)
                    else Modifier.fillMaxWidth().heightIn(max = 440.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(displayedMentions, key = { it.messageId }) { entry ->
                        MentionRow(
                            entry = entry,
                            onClick = {
                                onEvent(MainEvent.MarkChannelMentionsRead(entry.channelLogin))
                                onChannelClick(entry.channelLogin, entry.messageId)
                                onCloseClick?.invoke() ?: onEvent(MainEvent.HideMentionsFeed)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MentionRow(entry: MentionEntry, onClick: () -> Unit) {
    val isUnread = !entry.isRead

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(if (isUnread) MaterialTheme.colorScheme.primary.copy(alpha = 0.05f) else Color.Transparent)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Top
    ) {

        Box(
            modifier = Modifier.padding(top = 5.dp).size(7.dp).clip(CircleShape)
                .background(if (isUnread) MaterialTheme.colorScheme.primary else Color.Transparent)
        )
        Spacer(Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                val nameColor =
                    parseHexColorMentions(entry.fromColor) ?: MaterialTheme.colorScheme.primary
                Text(
                    entry.fromDisplayName,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = nameColor,
                    maxLines = 1
                )
                Text(
                    LocalStrings.current.panelInChannel.replace("{0}", entry.channelLogin),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    formatMentionTime(entry.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                    fontSize = 10.sp
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                entry.text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 14.dp),
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
    )
}

@Composable
private fun MentionFilterMenu(
    currentFilter: MentionFilter,
    currentSort: MentionSort,
    onFilterChange: (MentionFilter) -> Unit,
    onSortChange: (MentionSort) -> Unit,
    onDismiss: () -> Unit
) {
    val s = LocalStrings.current
    LiquidGlassSurface(
        modifier = Modifier.width(240.dp),
        shape = RoundedCornerShape(14.dp),
        contentPadding = PaddingValues(0.dp),
        glassIntensity = 0.96f,
        backgroundAlphaHigh = 0.97f,
        backgroundAlphaLow = 0.92f,
        borderAlphaHigh = 0.22f,
        borderAlphaLow = 0.08f
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
            SectionLabel(s.panelMentionsFilter)
            FilterMenuRow(s.panelMentionsFilterAll, currentFilter == MentionFilter.ALL) {
                onFilterChange(MentionFilter.ALL); onDismiss()
            }
            FilterMenuRow(s.panelMentionsFilterUnread, currentFilter == MentionFilter.UNREAD) {
                onFilterChange(MentionFilter.UNREAD); onDismiss()
            }
            FilterMenuRow(s.panelMentionsFilterToday, currentFilter == MentionFilter.TODAY) {
                onFilterChange(MentionFilter.TODAY); onDismiss()
            }
            FilterMenuRow(s.panelMentionsFilterByUser, currentFilter == MentionFilter.BY_USER) {
                onFilterChange(MentionFilter.BY_USER); onDismiss()
            }
            FilterMenuRow(s.panelMentionsFilterByChannel, currentFilter == MentionFilter.BY_CHANNEL) {
                onFilterChange(MentionFilter.BY_CHANNEL); onDismiss()
            }
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                color = Color.White.copy(alpha = 0.08f)
            )
            SectionLabel(s.panelMentionsSort)
            FilterMenuRow(s.panelMentionsSortNewest, currentSort == MentionSort.NEWEST) {
                onSortChange(MentionSort.NEWEST); onDismiss()
            }
            FilterMenuRow(s.panelMentionsSortOldest, currentSort == MentionSort.OLDEST) {
                onSortChange(MentionSort.OLDEST); onDismiss()
            }
            FilterMenuRow(s.panelMentionsSortByUser, currentSort == MentionSort.BY_USER) {
                onSortChange(MentionSort.BY_USER); onDismiss()
            }
            FilterMenuRow(s.panelMentionsSortByChannel, currentSort == MentionSort.BY_CHANNEL) {
                onSortChange(MentionSort.BY_CHANNEL); onDismiss()
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
        fontSize = 10.sp,
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
    )
}

@Composable
private fun FilterMenuRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        if (selected) {
            Icon(
                Icons.Filled.Check,
                null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

private fun parseHexColorMentions(hex: String?): Color? {
    if (hex == null || !hex.startsWith("#")) return null
    return try {
        val v = hex.substring(1).toLong(16)
        Color(
            red = ((v shr 16) and 0xFF) / 255f,
            green = ((v shr 8) and 0xFF) / 255f,
            blue = (v and 0xFF) / 255f
        )
    } catch (_: Exception) {
        null
    }
}

private fun formatMentionTime(ts: Long): String {
    val now = Clock.System.now().toEpochMilliseconds()
    val diffMs = now - ts
    val diffSeconds = diffMs / 1000
    val diffMinutes = diffSeconds / 60
    val diffHours = diffMinutes / 60
    val diffDays = diffHours / 24
    val diffWeeks = diffDays / 7

    return when {
        diffDays == 0L -> {
            val dt = Instant.fromEpochMilliseconds(ts).toLocalDateTime(TimeZone.currentSystemDefault())
            "${dt.hour.toString().padStart(2, '0')}:${dt.minute.toString().padStart(2, '0')}"
        }
        diffDays < 7L -> "${diffDays}d"
        else -> "${diffWeeks}w"
    }
}