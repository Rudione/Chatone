package io.rudione.chatone.presentation.settings

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
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import io.rudione.chatone.util.system.isDesktopPlatform
import org.koin.compose.koinInject

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun SettingsCompactScreen(
    state: SettingsState,
    onNavigateBack: () -> Unit,
    onThemeChanged: (Boolean) -> Unit,
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier,
    onOpenThemeCreator: (seedColor: Int?) -> Unit = {}
) {
    val s = LocalStrings.current
    var query by rememberSaveable { mutableStateOf("") }
    var expanded by remember { mutableStateOf(setOf<SettingsSection>()) }
    val settingsNavigator: SettingsNavigator = koinInject()
    val pendingDestination by settingsNavigator.pending.collectAsState()
    LaunchedEffect(pendingDestination) {
        pendingDestination?.let { destination ->
            query = ""
            expanded = expanded + destination.section()
        }
    }
    val matching = remember(query, s) { settingsSectionsMatching(query, s) }
    val searching = query.isNotBlank()
    val insets = WindowInsets.safeDrawing.asPaddingValues()
    val scheme = MaterialTheme.colorScheme

    Box(
        modifier = modifier
            .fillMaxSize()
            .ambientHaze(base = scheme.background, strength = HAZE_STRENGTH, animated = !isDesktopPlatform)
            .touchHaze(tint = scheme.primary, strength = HAZE_STRENGTH)
    ) {
        CompositionLocalProvider(LocalSettingsSearch provides query) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = insets.calculateTopPadding() + 8.dp,
                    bottom = insets.calculateBottomPadding() + 32.dp
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item(key = "header") {
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
                        value = query,
                        onValueChange = { query = it },
                        placeholder = s.settingsSearchPlaceholder,
                        clearLabel = s.clear,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                }
                if (matching.isEmpty()) {
                    item(key = "empty") {
                        Text(
                            s.settingsSearchNoResults,
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
                items(matching, key = { it.name }) { section ->
                    val open = searching || section in expanded
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SectionHeader(
                            section = section,
                            label = section.localizedLabel(s),
                            open = open,
                            onToggle = {
                                expanded = if (section in expanded) expanded - section else expanded + section
                            }
                        )
                        AnimatedVisibility(
                            visible = open,
                            enter = expandVertically(tween(220)) + fadeIn(tween(180)),
                            exit = shrinkVertically(tween(200)) + fadeOut(tween(140))
                        ) {
                            SectionContentColumn(
                                section = section,
                                state = state,
                                onThemeChanged = onThemeChanged,
                                viewModel = viewModel,
                                modifier = Modifier.padding(bottom = 6.dp),
                                onOpenThemeCreator = onOpenThemeCreator
                            )
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
            modifier = Modifier.size(40.dp).clip(SectionChipShape).background(chip),
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
