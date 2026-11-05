package io.rudione.chatone.presentation.settings.search

import androidx.compose.runtime.produceState
import io.rudione.chatone.presentation.theme.i18n.LocalAppLocale
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import io.rudione.chatone.presentation.settings.SettingsPageState
import io.rudione.chatone.presentation.settings.SettingsSection
import io.rudione.chatone.presentation.settings.labelMatches
import kotlinx.coroutines.delay

@Stable
internal class SettingsSearchState {
    var query by mutableStateOf("")
        private set

    var activeIndex by mutableIntStateOf(0)

    var showResults by mutableStateOf(true)

    val isSearching: Boolean get() = query.isNotBlank()

    fun change(next: String) {
        query = next
        activeIndex = 0
        showResults = true
    }

    fun clear() = change("")
}

internal class SettingsSearchModel(
    val hits: List<SettingsSearchHit>,
    val groups: List<SettingsSearchGroup>,
    val labelMatches: Set<SettingsSection>
) {
    val flat: List<SettingsSearchHit> = groups.flatHits()
    val countBySection = hits.groupingBy { it.entry.section }.eachCount()

    fun shows(section: SettingsSection): Boolean = section in countBySection || section in labelMatches
}

@Composable
internal fun rememberSettingsCatalog(context: SettingsCatalogContext): List<SettingsSearchEntry> {
    val language = LocalAppLocale.current
    val catalog by produceState(emptyList<SettingsSearchEntry>(), context, language) {
        value = buildSettingsCatalog(context)
    }
    return catalog
}

@Composable
internal fun rememberSettingsSearchModel(query: String, catalog: List<SettingsSearchEntry>): SettingsSearchModel {
    val labels = SettingsSection.entries.associateWith { it.localizedLabel() }
    return remember(query, catalog, labels) {
        val hits = searchSettings(query, catalog)
        val matches = if (query.isBlank()) emptySet() else labels.filter { (section, label) -> section.labelMatches(query, label) }.keys
        SettingsSearchModel(hits, groupSearchHits(hits), matches)
    }
}

internal fun Modifier.settingsSearchKeys(
    search: SettingsSearchState,
    resultCount: () -> Int,
    onOpenActive: () -> Unit
): Modifier = onPreviewKeyEvent { event ->
    if (event.type != KeyEventType.KeyDown || !search.isSearching) return@onPreviewKeyEvent false
    val count = resultCount()
    when (event.key) {
        Key.DirectionDown -> {
            if (count > 0) search.activeIndex = (search.activeIndex + 1).coerceAtMost(count - 1)
            search.showResults = true
            true
        }
        Key.DirectionUp -> {
            search.activeIndex = (search.activeIndex - 1).coerceAtLeast(0)
            search.showResults = true
            true
        }
        Key.Enter, Key.NumPadEnter -> {
            if (count > 0) onOpenActive()
            count > 0
        }
        Key.Escape -> {
            search.clear()
            true
        }
        else -> false
    }
}

internal suspend fun SettingsPageState.reveal(entry: SettingsSearchEntry, settleMs: Long) {
    entry.tab?.let { moderationTab = it }
    if (settleMs > 0) delay(settleMs)
    focus(entry.anchor)
    delay(FALLBACK_AFTER_MS)
    val fallback = entry.fallbackAnchor
    if (focusTarget == entry.anchor && fallback != null) focus(fallback)
    delay(FALLBACK_AFTER_MS)
    focusTarget?.let(::consume)
}

internal suspend fun SettingsPageState.reveal(subsection: SettingsSubsection) {
    subsection.tab?.let { moderationTab = it }
    val anchor = subsection.anchor ?: return
    focus(anchor, highlight = false)
    delay(FALLBACK_AFTER_MS * 2)
    consume(anchor)
}

private const val FALLBACK_AFTER_MS = 450L
