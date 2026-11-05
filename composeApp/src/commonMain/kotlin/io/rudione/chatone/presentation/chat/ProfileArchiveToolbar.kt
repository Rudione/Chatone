package io.rudione.chatone.presentation.chat

import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.insights_archive_label
import chatone.composeapp.generated.resources.insights_archive_search
import chatone.composeapp.generated.resources.insights_months_short
import chatone.composeapp.generated.resources.profile_archive_back_to_session
import chatone.composeapp.generated.resources.profile_archive_filter
import chatone.composeapp.generated.resources.profile_archive_latest
import chatone.composeapp.generated.resources.profile_archive_loading
import chatone.composeapp.generated.resources.profile_archive_months
import chatone.composeapp.generated.resources.profile_archive_not_logged
import chatone.composeapp.generated.resources.profile_archive_retry
import chatone.composeapp.generated.resources.profile_archive_title
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.stringArrayResource
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.rudione.chatone.data.remote.LogMonth
import io.rudione.chatone.icons.lucide.CalendarDays
import io.rudione.chatone.icons.lucide.Check
import io.rudione.chatone.icons.lucide.History
import io.rudione.chatone.icons.lucide.Lucide
import io.rudione.chatone.icons.lucide.MessageSquare
import io.rudione.chatone.icons.lucide.RefreshCw
import io.rudione.chatone.icons.lucide.Search
import io.rudione.chatone.icons.lucide.X
import io.rudione.chatone.presentation.components.ChatoneDropdownMenu
import io.rudione.chatone.presentation.components.ChatoneIconButton

internal val PROFILE_CARD_INLINE_SEARCH_WIDTH = 470.dp

@Composable
internal fun ProfileArchiveControls(
    archive: UserLogArchiveState,
    inlineSearch: Boolean,
    searchExpanded: Boolean,
    onToggleSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!archive.isSupported) return
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        if (inlineSearch) {
            ArchiveSearchField(archive = archive, modifier = Modifier.width(150.dp))
        } else {
            val tint = if (searchExpanded || archive.isSearching) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            ChatoneIconButton(onClick = onToggleSearch, modifier = Modifier.size(24.dp)) {
                Icon(
                    Lucide.Search,
                    contentDescription = stringResource(Res.string.insights_archive_search),
                    modifier = Modifier.size(13.dp),
                    tint = tint
                )
            }
        }
        ArchiveMonthButton(archive)
    }
}

@Composable
internal fun ProfileArchiveSearchBar(archive: UserLogArchiveState, visible: Boolean) {
    AnimatedVisibility(
        visible = visible && archive.isSupported,
        enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
        exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut()
    ) {
        val focusRequester = remember { FocusRequester() }
        ArchiveSearchField(
            archive = archive,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 3.dp),
            focusRequester = focusRequester
        )
        LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }
    }
}

@Composable
private fun ArchiveSearchField(
    archive: UserLogArchiveState,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null
) {
    val accent = MaterialTheme.colorScheme.primary
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(8.dp)
    BasicTextField(
        value = archive.query,
        onValueChange = archive::updateQuery,
        singleLine = true,
        interactionSource = interactionSource,
        textStyle = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.onSurface),
        cursorBrush = SolidColor(accent),
        modifier = modifier
            .height(26.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.6f))
            .border(1.dp, if (focused) accent.copy(alpha = 0.55f) else muted.copy(alpha = 0.18f), shape)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier),
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier.padding(start = 7.dp, end = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(Lucide.Search, contentDescription = null, modifier = Modifier.size(11.dp), tint = muted.copy(alpha = 0.6f))
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (archive.query.isEmpty()) {
                        Text(
                            stringResource(Res.string.insights_archive_search),
                            style = MaterialTheme.typography.labelMedium,
                            color = muted.copy(alpha = 0.5f),
                            maxLines = 1
                        )
                    }
                    innerTextField()
                }
                if (archive.isSearching) {
                    if (archive.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(10.dp), strokeWidth = 1.5.dp)
                    } else {
                        Text(
                            archive.lines.size.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = accent.copy(alpha = 0.85f)
                        )
                    }
                    ChatoneIconButton(onClick = { archive.updateQuery("") }, modifier = Modifier.size(18.dp)) {
                        Icon(Lucide.X, contentDescription = null, modifier = Modifier.size(10.dp), tint = muted)
                    }
                }
            }
        }
    )
}

@Composable
private fun ArchiveMonthButton(archive: UserLogArchiveState) {
    var expanded by remember { mutableStateOf(false) }
    val active = archive.isActive
    val accent = MaterialTheme.colorScheme.primary
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val shownMonth = archive.anchorMonth ?: archive.months.firstOrNull()
    val label = if (active && !archive.isSearching && shownMonth != null) shortMonthLabel(shownMonth)
    else stringResource(Res.string.insights_archive_label)
    Box {
        Row(
            modifier = Modifier
                .height(22.dp)
                .clip(CircleShape)
                .background(if (active) accent.copy(alpha = 0.14f) else muted.copy(alpha = 0.08f))
                .clickable {
                    expanded = true
                    archive.ensureMonths()
                }
                .padding(start = 7.dp, end = if (active) 2.dp else 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (active && archive.isLoading && !archive.isSearching) {
                CircularProgressIndicator(modifier = Modifier.size(10.dp), strokeWidth = 1.5.dp)
            } else {
                Icon(
                    Lucide.CalendarDays,
                    contentDescription = stringResource(Res.string.profile_archive_filter),
                    modifier = Modifier.size(11.dp),
                    tint = if (active) accent else muted.copy(alpha = 0.75f)
                )
            }
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                color = if (active) accent else muted,
                maxLines = 1
            )
            if (active) {
                ChatoneIconButton(onClick = archive::close, modifier = Modifier.size(18.dp)) {
                    Icon(
                        Lucide.X,
                        contentDescription = stringResource(Res.string.profile_archive_back_to_session),
                        modifier = Modifier.size(10.dp),
                        tint = accent.copy(alpha = 0.8f)
                    )
                }
            }
        }
        ChatoneDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.widthIn(min = 180.dp).heightIn(max = 320.dp)
        ) {
            Text(
                stringResource(Res.string.profile_archive_title),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = muted.copy(alpha = 0.7f),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
            if (active) {
                ArchiveMenuRow(stringResource(Res.string.profile_archive_back_to_session), Lucide.MessageSquare) {
                    archive.close()
                    expanded = false
                }
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 3.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                )
            }
            ArchiveMonthsMenuContent(archive = archive, onPicked = { expanded = false })
        }
    }
}

@Composable
private fun ArchiveMonthsMenuContent(archive: UserLogArchiveState, onPicked: () -> Unit) {
    when (archive.availability) {
        UserLogArchiveState.Availability.Unknown,
        UserLogArchiveState.Availability.Loading -> Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp)
            Text(stringResource(Res.string.profile_archive_loading), style = MaterialTheme.typography.bodySmall)
        }

        UserLogArchiveState.Availability.NotLogged -> Text(
            stringResource(Res.string.profile_archive_not_logged),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )

        UserLogArchiveState.Availability.Failed ->
            ArchiveMenuRow(stringResource(Res.string.profile_archive_retry), Lucide.RefreshCw) { archive.ensureMonths() }

        UserLogArchiveState.Availability.Available -> {
            ArchiveMenuRow(
                text = stringResource(Res.string.profile_archive_latest),
                icon = Lucide.History,
                selected = archive.isActive && archive.anchorMonth == null && !archive.isSearching
            ) {
                archive.open(null)
                onPicked()
            }
            archive.months.forEach { month ->
                ArchiveMenuRow(
                    text = monthLabel(month),
                    icon = null,
                    selected = archive.isActive && archive.anchorMonth == month && !archive.isSearching
                ) {
                    archive.open(month)
                    onPicked()
                }
            }
        }
    }
}

@Composable
private fun ArchiveMenuRow(
    text: String,
    icon: ImageVector?,
    selected: Boolean = false,
    onClick: () -> Unit
) {
    val tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(modifier = Modifier.size(14.dp), contentAlignment = Alignment.Center) {
            val shown = if (selected) Lucide.Check else icon
            if (shown != null) {
                Icon(
                    shown,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = if (selected) tint else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text(text, style = MaterialTheme.typography.bodySmall, color = tint, maxLines = 1)
    }
}

@Composable
internal fun monthLabel(month: LogMonth): String =
    "${stringArrayResource(Res.array.profile_archive_months).getOrElse(month.month - 1) { month.month.toString() }} ${month.year}"

@Composable
internal fun shortMonthLabel(month: LogMonth): String =
    "${stringArrayResource(Res.array.insights_months_short).getOrElse(month.month - 1) { month.month.toString() }}'${(month.year % 100).toString().padStart(2, '0')}"
