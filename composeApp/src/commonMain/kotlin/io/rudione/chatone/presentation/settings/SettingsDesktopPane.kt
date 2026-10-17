package io.rudione.chatone.presentation.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.rudione.chatone.icons.lucide.ArrowLeft
import io.rudione.chatone.icons.lucide.Lucide
import io.rudione.chatone.presentation.components.ChatoneScrollbar
import io.rudione.chatone.presentation.components.expressive.PillButton
import io.rudione.chatone.presentation.components.expressive.PillTone
import io.rudione.chatone.presentation.components.expressive.ScallopShape
import io.rudione.chatone.presentation.components.rows.HighlightedSettingsText
import io.rudione.chatone.presentation.settings.search.SettingsSubsection
import io.rudione.chatone.presentation.theme.i18n.LocalStrings

@Composable
internal fun SectionPane(
    section: SettingsSection,
    state: SettingsState,
    viewModel: SettingsViewModel,
    onOpenThemeCreator: (seedColor: Int?) -> Unit,
    resultCount: Int?,
    onBackToResults: () -> Unit,
    modifier: Modifier = Modifier
) {
    val s = LocalStrings.current
    key(section) {
        val scrollState = rememberScrollState()
        Box(modifier = modifier) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(end = 10.dp)
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(SectionChipShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(section.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(17.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        section.localizedLabel(s),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    if (resultCount != null) {
                        PillButton(
                            text = s.settingsHub.searchBackToResults + " · " + resultCount,
                            onClick = onBackToResults,
                            icon = Lucide.ArrowLeft,
                            tone = PillTone.Ghost,
                            height = 32.dp
                        )
                    }
                }
                SettingsSectionContent(
                    section = section,
                    state = state,
                    viewModel = viewModel,
                    onOpenThemeCreator = onOpenThemeCreator,
                    spacing = 6.dp
                )
            }
            ChatoneScrollbar(
                scrollState = scrollState,
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(10.dp)
            )
        }
    }
}

@Composable
internal fun SidebarNavItem(
    section: SettingsSection,
    isSelected: Boolean,
    count: Int,
    onClick: () -> Unit
) {
    val s = LocalStrings.current
    val scheme = MaterialTheme.colorScheme
    val bg by animateColorAsState(
        if (isSelected) scheme.primary.copy(alpha = 0.16f) else Color.Transparent,
        tween(160), label = "nav_bg"
    )
    val contentColor = if (isSelected) scheme.primary else scheme.onSurfaceVariant
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(34.dp)
            .clip(CircleShape)
            .drawBehind { drawRect(bg) }
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(section.icon, null, modifier = Modifier.size(16.dp), tint = contentColor)
        HighlightedSettingsText(
            section.localizedLabel(s),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isSelected) scheme.onSurface else scheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (count > 0) {
            Text(
                count.toString(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = scheme.primary,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(scheme.primary.copy(alpha = 0.14f))
                    .padding(horizontal = 7.dp, vertical = 1.dp)
            )
        }
    }
}

@Composable
internal fun SidebarSubsections(
    visible: Boolean,
    items: List<SettingsSubsection>,
    active: SettingsSubsection?,
    onOpen: (SettingsSubsection) -> Unit
) {
    AnimatedVisibility(
        visible = visible && items.size > 1,
        enter = expandVertically(tween(220)) + fadeIn(tween(220)),
        exit = shrinkVertically(tween(180)) + fadeOut(tween(140))
    ) {
        val guide = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = SubsectionIndent, top = 2.dp, bottom = 6.dp)
                .drawBehind {
                    val x = 1.dp.toPx()
                    drawLine(guide, Offset(x, 4.dp.toPx()), Offset(x, size.height - 4.dp.toPx()), strokeWidth = 1.dp.toPx())
                },
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            items.forEach { item ->
                SidebarSubsectionItem(title = item.title, selected = item == active, onClick = { onOpen(item) })
            }
        }
    }
}

@Composable
private fun SidebarSubsectionItem(title: String, selected: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val bg by animateColorAsState(
        if (selected) scheme.primary.copy(alpha = 0.10f) else Color.Transparent,
        tween(160), label = "subsection_bg"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp)
            .height(28.dp)
            .clip(CircleShape)
            .drawBehind { drawRect(bg) }
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
            color = if (selected) scheme.primary else scheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private val SubsectionIndent = 20.dp
private val SectionChipShape = ScallopShape(lobes = 8, depth = 0.09f)
