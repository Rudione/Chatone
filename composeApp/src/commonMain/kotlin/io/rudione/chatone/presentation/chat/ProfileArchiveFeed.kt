package io.rudione.chatone.presentation.chat

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import io.rudione.chatone.icons.lucide.Ban
import io.rudione.chatone.icons.lucide.Lucide
import io.rudione.chatone.icons.lucide.Timer
import io.rudione.chatone.data.remote.ArchivedChatLine
import io.rudione.chatone.domain.model.ModActionButton
import io.rudione.chatone.presentation.theme.ChatoneTheme
import io.rudione.chatone.presentation.theme.i18n.LocalStrings
import io.rudione.chatone.presentation.theme.i18n.format
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

internal const val PAST_MESSAGE_ALPHA = 0.72f
private const val DELETED_MESSAGE_ALPHA = 0.28f

@Composable
internal fun PastMessageLine(
    displayName: String,
    nameColor: Color,
    text: String,
    timeText: String,
    isDeleted: Boolean = false,
    modifier: Modifier = Modifier
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val separatorColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    val deletedLabel = LocalStrings.current.profileMessageDeleted
    val line = buildAnnotatedString {
        withStyle(SpanStyle(color = nameColor, fontWeight = FontWeight.SemiBold)) { append(displayName) }
        withStyle(SpanStyle(color = separatorColor)) { append(": ") }
        withStyle(
            SpanStyle(
                color = onSurface.copy(alpha = if (isDeleted) DELETED_MESSAGE_ALPHA else PAST_MESSAGE_ALPHA),
                textDecoration = if (isDeleted) TextDecoration.LineThrough else null
            )
        ) {
            append(text.ifEmpty { if (isDeleted) deletedLabel else "" })
        }
    }
    Row(
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(text = line, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
        if (timeText.isNotEmpty()) {
            Spacer(Modifier.width(6.dp))
            Text(
                timeText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
internal fun ProfileArchiveFeed(
    archive: UserLogArchiveState,
    displayName: String,
    nameColor: Color,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        when {
            archive.lines.isEmpty() && archive.isLoading -> Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            }

            archive.lines.isEmpty() -> ArchiveEmptyState(archive)

            else -> ArchiveLineList(archive, displayName, nameColor)
        }
    }
}

@Composable
private fun ArchiveEmptyState(archive: UserLogArchiveState) {
    val s = LocalStrings.current
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            when {
                archive.availability == UserLogArchiveState.Availability.NotLogged -> s.profileArchiveNotLogged
                archive.loadFailed || archive.availability == UserLogArchiveState.Availability.Failed ->
                    s.profileArchiveFailed
                else -> s.profileArchiveEmpty
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
        )
        if (archive.loadFailed) {
            TextButton(onClick = { archive.open(archive.anchorMonth) }) {
                Text(s.profileArchiveRetry, style = MaterialTheme.typography.labelSmall)
            }
        } else if (archive.canLoadOlder) {
            TextButton(onClick = archive::loadOlder) {
                Text(s.profileArchiveLoadOlder, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

private sealed interface ArchiveRow {
    val key: String

    data class Day(override val key: String, val label: String) : ArchiveRow
    data class Line(override val key: String, val line: ArchivedChatLine) : ArchiveRow
}

@Composable
private fun ArchiveLineList(archive: UserLogArchiveState, displayName: String, nameColor: Color) {
    val s = LocalStrings.current
    val listState = rememberLazyListState()
    val rows = remember(archive.lines) { archiveRows(archive.lines) }
    var armed by remember(archive.anchorMonth, archive.query) { mutableStateOf(false) }

    LaunchedEffect(archive.anchorMonth, archive.query, rows.isNotEmpty()) {
        if (rows.isEmpty() || armed) return@LaunchedEffect
        listState.scrollToItem(rows.size)
        armed = true
    }
    LaunchedEffect(listState, armed) {
        if (!armed) return@LaunchedEffect
        snapshotFlow { listState.firstVisibleItemIndex }
            .collect { first -> if (first <= 1) archive.loadOlder() }
    }

    SelectionContainer {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            item(key = "archive-older") {
                DisableSelection {
                    when {
                        archive.isLoading -> Box(
                            Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(13.dp), strokeWidth = 2.dp)
                        }

                        archive.canLoadOlder -> TextButton(
                            onClick = archive::loadOlder,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(s.profileArchiveLoadOlder, style = MaterialTheme.typography.labelSmall)
                        }

                        else -> Spacer(Modifier.height(1.dp))
                    }
                }
            }
            items(rows, key = { it.key }) { row ->
                when (row) {
                    is ArchiveRow.Day -> DisableSelection { ArchiveDayHeader(row.label) }
                    is ArchiveRow.Line -> ArchiveLineRow(row.line, displayName, nameColor)
                }
            }
        }
    }
}

@Composable
private fun ArchiveDayHeader(label: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val lineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
        HorizontalDivider(modifier = Modifier.weight(1f), color = lineColor)
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
        HorizontalDivider(modifier = Modifier.weight(1f), color = lineColor)
    }
}

@Composable
private fun ArchiveLineRow(line: ArchivedChatLine, displayName: String, nameColor: Color) {
    val s = LocalStrings.current
    val time = remember(line.timestampMs) { formatMessageTime(line.timestampMs) }
    when (line.kind) {
        ArchivedChatLine.Kind.MESSAGE -> PastMessageLine(
            displayName = displayName,
            nameColor = nameColor,
            text = line.text,
            timeText = time,
            isDeleted = line.isDeleted
        )

        ArchivedChatLine.Kind.TIMEOUT, ArchivedChatLine.Kind.BAN -> {
            val isBan = line.kind == ArchivedChatLine.Kind.BAN
            val tint = if (isBan) ChatoneTheme.extraColors.modBan else ChatoneTheme.extraColors.modTimeout
            ArchiveEventRow(
                icon = if (isBan) Lucide.Ban else Lucide.Timer,
                tint = tint,
                text = if (isBan) s.profileArchiveBanned
                else s.format(s.profileArchiveTimedOut, ModActionButton.formatDuration(line.durationSeconds ?: 0)),
                time = time
            )
        }

        ArchivedChatLine.Kind.NOTICE -> Text(
            line.text,
            style = MaterialTheme.typography.labelSmall,
            fontStyle = FontStyle.Italic,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun ArchiveEventRow(icon: ImageVector, tint: Color, text: String, time: String) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp))
            .background(tint.copy(alpha = 0.08f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(12.dp), tint = tint)
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            color = tint,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        Text(
            time,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
    }
}

private fun archiveRows(lines: List<ArchivedChatLine>): List<ArchiveRow> {
    val zone = TimeZone.currentSystemDefault()
    val out = ArrayList<ArchiveRow>(lines.size + 16)
    var lastDay: String? = null
    lines.forEach { line ->
        val date = Instant.fromEpochMilliseconds(line.timestampMs).toLocalDateTime(zone).date
        val day = date.toString()
        if (day != lastDay) {
            lastDay = day
            out += ArchiveRow.Day("day_$day", formatDate(line.timestampMs))
        }
        out += ArchiveRow.Line("line_${line.id}", line)
    }
    return out
}
