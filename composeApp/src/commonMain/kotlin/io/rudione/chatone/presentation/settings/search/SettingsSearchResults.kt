package io.rudione.chatone.presentation.settings.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.LaunchedEffect
import io.rudione.chatone.presentation.components.ChatoneLazyScrollbar
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.rudione.chatone.icons.lucide.ChevronRight
import io.rudione.chatone.icons.lucide.Lucide
import io.rudione.chatone.presentation.components.rows.HighlightedSettingsText
import io.rudione.chatone.presentation.settings.ModerationTab
import io.rudione.chatone.presentation.settings.SettingsSection
import io.rudione.chatone.presentation.theme.i18n.AppStrings
import io.rudione.chatone.presentation.theme.i18n.LocalStrings

internal class SettingsSearchGroup(val section: SettingsSection, val hits: List<SettingsSearchHit>)

internal fun groupSearchHits(hits: List<SettingsSearchHit>): List<SettingsSearchGroup> =
    hits.groupBy { it.entry.section }
        .map { (section, sectionHits) -> SettingsSearchGroup(section, sectionHits) }
        .sortedByDescending { group -> group.hits.maxOf { it.score } }

internal fun List<SettingsSearchGroup>.flatHits(): List<SettingsSearchHit> = flatMap { it.hits }

@Composable
internal fun SettingsSearchSectionHeader(section: SettingsSection, count: Int, modifier: Modifier = Modifier) {
    val s = LocalStrings.current
    Row(
        modifier = modifier.fillMaxWidth().padding(start = 6.dp, end = 6.dp, top = 10.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(section.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(15.dp))
        Text(
            section.localizedLabel(s),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
        )
        Text(
            count.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
internal fun SettingsSearchResultRow(
    hit: SettingsSearchHit,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val s = LocalStrings.current
    val entry = hit.entry
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val scheme = MaterialTheme.colorScheme
    val background = when {
        active -> scheme.primary.copy(alpha = 0.16f)
        hovered -> scheme.onSurface.copy(alpha = 0.06f)
        else -> Color.Transparent
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .clip(ResultShape)
            .background(background)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .pointerHoverIcon(PointerIcon.Hand)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            HighlightedSettingsText(
                entry.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = scheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val trail = breadcrumb(entry, s)
            if (trail.isNotEmpty()) {
                HighlightedSettingsText(
                    trail,
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.primary.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            entry.description?.let { description ->
                HighlightedSettingsText(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Icon(
            Lucide.ChevronRight,
            contentDescription = s.settingsHub.searchOpen,
            tint = if (active || hovered) scheme.primary else scheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
internal fun SettingsSearchEmpty(modifier: Modifier = Modifier) {
    val s = LocalStrings.current
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 24.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            s.settingsSearchNoResults,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            s.settingsHub.searchTryAnother,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun breadcrumb(entry: SettingsSearchEntry, s: AppStrings): String {
    val tab = entry.tab?.let { tab ->
        when (tab) {
            ModerationTab.GENERAL -> s.modSlider.tabGeneral
            ModerationTab.BUTTONS -> s.modSlider.tabButtons
            ModerationTab.SLIDER -> s.modSlider.tabSlider
            ModerationTab.MACROS -> s.modSlider.tabMacros
        }
    }
    return listOfNotNull(tab, entry.group).joinToString(" › ")
}

private val ResultShape = RoundedCornerShape(12.dp)

@Composable
internal fun SettingsSearchResultsPane(
    groups: List<SettingsSearchGroup>,
    activeIndex: Int,
    onOpen: (SettingsSearchHit) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val rows = remember(groups) { searchRows(groups) }
    LaunchedEffect(activeIndex, rows) {
        val target = rows.indexOfFirst { it is SearchRow.Result && it.flatIndex == activeIndex }
        if (target >= 0) listState.animateScrollToItem((target - 1).coerceAtLeast(0))
    }
    Box(modifier = modifier) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(end = 10.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            settingsSearchRows(rows, activeIndex, onOpen)
        }
        ChatoneLazyScrollbar(
            listState = listState,
            itemCount = listState.layoutInfo.totalItemsCount,
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(10.dp)
        )
    }
}

internal fun LazyListScope.settingsSearchResultItems(
    groups: List<SettingsSearchGroup>,
    activeIndex: Int,
    onOpen: (SettingsSearchHit) -> Unit
) {
    settingsSearchRows(searchRows(groups), activeIndex, onOpen)
}

private fun LazyListScope.settingsSearchRows(
    rows: List<SearchRow>,
    activeIndex: Int,
    onOpen: (SettingsSearchHit) -> Unit
) {
    items(rows, key = { it.key }, contentType = { it.contentType }) { row ->
        when (row) {
            is SearchRow.Header -> SettingsSearchSectionHeader(row.group.section, row.group.hits.size)
            is SearchRow.Result -> SettingsSearchResultRow(
                hit = row.hit,
                active = row.flatIndex == activeIndex,
                onClick = { onOpen(row.hit) }
            )
        }
    }
}

private sealed interface SearchRow {
    val key: String
    val contentType: Int

    class Header(val group: SettingsSearchGroup) : SearchRow {
        override val key: String = "header:${group.section.name}"
        override val contentType: Int = 0
    }

    class Result(val hit: SettingsSearchHit, val flatIndex: Int) : SearchRow {
        override val key: String = "result:${hit.entry.id}"
        override val contentType: Int = 1
    }
}

private fun searchRows(groups: List<SettingsSearchGroup>): List<SearchRow> {
    var flat = 0
    return buildList {
        groups.forEach { group ->
            add(SearchRow.Header(group))
            group.hits.forEach { hit -> add(SearchRow.Result(hit, flat++)) }
        }
    }
}
