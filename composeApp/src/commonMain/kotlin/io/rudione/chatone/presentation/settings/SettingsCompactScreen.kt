package io.rudione.chatone.presentation.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.draw.drawBehind
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.rudione.chatone.icons.lucide.ArrowLeft
import io.rudione.chatone.icons.lucide.ChevronDown
import io.rudione.chatone.icons.lucide.Lucide
import io.rudione.chatone.presentation.components.expressive.ExpressiveSearchField
import io.rudione.chatone.presentation.components.expressive.GlassIconButton
import io.rudione.chatone.presentation.components.expressive.ScallopShape
import io.rudione.chatone.presentation.components.expressive.ambientHaze
import io.rudione.chatone.presentation.components.expressive.touchHaze
import io.rudione.chatone.presentation.components.rows.LocalSettingsSearch
import io.rudione.chatone.presentation.settings.components.settingsCard
import io.rudione.chatone.presentation.theme.i18n.LocalStrings
import io.rudione.chatone.util.platform.rememberInputCapabilities
import io.rudione.chatone.util.system.isDesktopPlatform
import org.koin.compose.koinInject
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.rememberCoroutineScope
import io.rudione.chatone.presentation.screenchat.screenChatFloatsOverFullScreen
import io.rudione.chatone.presentation.settings.search.SettingsCatalogContext
import io.rudione.chatone.presentation.settings.search.SettingsSearchEmpty
import io.rudione.chatone.presentation.settings.search.SettingsSearchHit
import io.rudione.chatone.presentation.settings.search.SettingsSearchState
import io.rudione.chatone.presentation.settings.search.rememberSettingsCatalog
import io.rudione.chatone.presentation.settings.search.rememberSettingsSearchModel
import io.rudione.chatone.presentation.settings.search.reveal
import io.rudione.chatone.presentation.settings.search.settingsSearchKeys
import io.rudione.chatone.presentation.settings.search.settingsSearchResultItems
import io.rudione.chatone.util.platform.DeviceFormFactor
import io.rudione.chatone.util.platform.currentFormFactor
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun SettingsCompactScreen(
    state: SettingsState,
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier,
    onOpenThemeCreator: (seedColor: Int?) -> Unit = {}
) {
    val s = LocalStrings.current
    val search = remember { SettingsSearchState() }
    val page = remember { SettingsPageState() }
    val expansion = remember { SettingsSectionExpansion() }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val input = rememberInputCapabilities()
    val context = remember(input) {
        SettingsCatalogContext(
            input,
            desktop = isDesktopPlatform,
            phone = currentFormFactor() == DeviceFormFactor.PHONE,
            macOs = screenChatFloatsOverFullScreen
        )
    }
    val model = rememberSettingsSearchModel(search.query, s, rememberSettingsCatalog(s, context))
    val sections = remember(input) { SettingsSection.entries.filter { it.isAvailable(input) } }
    val settingsNavigator: SettingsNavigator = koinInject()
    val pendingDestination by settingsNavigator.pending.collectAsState()

    fun revealSection(section: SettingsSection, then: suspend () -> Unit) {
        search.clear()
        expansion.reveal(section)
        scope.launch {
            val index = sections.indexOf(section)
            if (index >= 0) listState.animateScrollToItem(index + FIRST_SECTION_ITEM)
            then()
        }
    }

    SideEffect(pendingDestination) {
        pendingDestination?.let { destination ->
            destination.moderationTab()?.let { page.moderationTab = it }
            revealSection(destination.section()) {}
        }
    }
    val openHit: (SettingsSearchHit) -> Unit = { hit ->
        revealSection(hit.entry.section) { page.reveal(hit.entry, settleMs = SECTION_EXPAND_MS) }
    }
    val insets = WindowInsets.safeDrawing.asPaddingValues()
    val scheme = MaterialTheme.colorScheme

    Box(
        modifier = modifier
            .fillMaxSize()
            .ambientHaze(base = scheme.background, strength = HAZE_STRENGTH, animated = !isDesktopPlatform)
            .touchHaze(tint = scheme.primary, strength = HAZE_STRENGTH)
    ) {
        CompositionLocalProvider(LocalSettingsSearch provides search.query, LocalSettingsPage provides page) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                    bottom = insets.calculateBottomPadding() + 32.dp
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item(key = "header", contentType = "header") {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        GlassIconButton(icon = Lucide.ArrowLeft, contentDescription = s.back, onClick = onNavigateBack)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            s.settingsTitle,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = scheme.onSurface
                        )
                    }
                }
                stickyHeader(key = "search") {
                    ExpressiveSearchField(
                        value = search.query,
                        onValueChange = search::change,
                        placeholder = s.settingsSearchPlaceholder,
                        clearLabel = s.clear,
                        modifier = Modifier
                            .padding(vertical = 6.dp)
                            .settingsSearchKeys(search, { model.flat.size }) {
                                model.flat.getOrNull(search.activeIndex)?.let(openHit)
                            }
                    )
                }
                if (search.isSearching) {
                    if (model.flat.isEmpty()) {
                        item(key = "empty", contentType = "empty") { SettingsSearchEmpty() }
                    } else {
                        item(key = "found", contentType = "found") {
                            Text(
                                s.settingsHub.searchFound.replace("{0}", model.flat.size.toString()),
                                style = MaterialTheme.typography.labelMedium,
                                color = scheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp)
                            )
                        }
                        settingsSearchResultItems(model.groups, search.activeIndex, openHit)
                    }
                } else {
                    items(sections, key = { it.name }, contentType = { "section" }) { section ->
                        val open = expansion.isOpen(section)
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            SectionHeader(
                                section = section,
                                label = section.localizedLabel(s),
                                open = open,
                                onToggle = { expansion.toggle(section) }
                            )
                            AnimatedVisibility(
                                visible = open,
                                enter = expandVertically(tween(220)) + fadeIn(tween(180)),
                                exit = shrinkVertically(tween(200)) + fadeOut(tween(140))
                            ) {
                                SettingsSectionContent(
                                    section = section,
                                    state = state,
                                    viewModel = viewModel,
                                    onOpenThemeCreator = onOpenThemeCreator,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    section: SettingsSection,
    label: String,
    open: Boolean,
    onToggle: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val chip by animateColorAsState(
        scheme.primary.copy(alpha = if (open) 0.30f else 0.14f),
        tween(220),
        label = "sectionChip"
    )
    val turn by animateFloatAsState(
        if (open) 0f else -90f,
        spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow),
        label = "sectionChevron"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .settingsCard()
            .clickable(onClick = onToggle)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(SectionChipShape).drawBehind { drawRect(chip) },
            contentAlignment = Alignment.Center
        ) {
            Icon(section.icon, contentDescription = null, tint = scheme.primary, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Text(
            label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (open) FontWeight.SemiBold else FontWeight.Medium,
            color = scheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Icon(
            Lucide.ChevronDown,
            contentDescription = null,
            tint = scheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp).graphicsLayer { rotationZ = turn }
        )
    }
}

private val SectionChipShape = ScallopShape(lobes = 8, depth = 0.09f)
private const val HAZE_STRENGTH = 0.6f
private const val FIRST_SECTION_ITEM = 2
private const val SECTION_EXPAND_MS = 260L
