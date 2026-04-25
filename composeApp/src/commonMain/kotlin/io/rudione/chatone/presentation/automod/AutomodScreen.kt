package io.rudione.chatone.presentation.automod

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.rudione.chatone.data.repository.AutomodRepository
import io.rudione.chatone.domain.model.AutomodAction
import io.rudione.chatone.domain.model.AutomodRule
import io.rudione.chatone.domain.model.AutomodScope
import io.rudione.chatone.presentation.theme.ChatoneTheme
import io.rudione.chatone.util.AutomodImportExport
import org.koin.compose.koinInject

enum class AutomodFilter { ALL, GLOBAL, LOCAL }

@Composable
fun AutomodScreen(
    currentChannelLogin: String?,
    onClose: () -> Unit,
    onExport: (fileName: String, content: String) -> Unit,
    onImport: () -> Unit,
    modifier: Modifier = Modifier,
    repository: AutomodRepository = koinInject()
) {
    val rules by repository.rules.collectAsState()

    var filter by remember { mutableStateOf(AutomodFilter.ALL) }
    var query by remember { mutableStateOf("") }
    var selectedId by remember { mutableStateOf<String?>(null) }

    val filtered = remember(rules, filter, query) {
        rules.filter { rule ->
            val matchesFilter = when (filter) {
                AutomodFilter.ALL -> true
                AutomodFilter.GLOBAL -> rule.scope == AutomodScope.GLOBAL
                AutomodFilter.LOCAL -> rule.scope == AutomodScope.LOCAL
            }
            val q = query.trim()
            val matchesQuery = q.isEmpty() ||
                rule.pattern.contains(q, ignoreCase = true) ||
                rule.alternates.any { it.contains(q, ignoreCase = true) } ||
                (rule.channelLogin?.contains(q, ignoreCase = true) == true) ||
                rule.note.contains(q, ignoreCase = true)
            matchesFilter && matchesQuery
        }
    }

   
    LaunchedEffect(filtered) {
        if (selectedId != null && filtered.none { it.id == selectedId }) {
            selectedId = filtered.firstOrNull()?.id
        } else if (selectedId == null) {
            selectedId = filtered.firstOrNull()?.id
        }
    }

    val selected = filtered.firstOrNull { it.id == selectedId }
        ?: rules.firstOrNull { it.id == selectedId }

    ChatoneTheme {
        BoxWithConstraints(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            val wide = maxWidth >= 640.dp

            Column(modifier = Modifier.fillMaxSize()) {
                AutomodTopBar(
                    title = "Local Automod",
                    subtitle = if (currentChannelLogin.isNullOrBlank()) null else "#${currentChannelLogin}",
                    onClose = onClose,
                    onImport = onImport,
                    onExportJson = {
                        onExport("automod-rules.json", AutomodImportExport.toJson(rules))
                    },
                    onExportMd = {
                        onExport("automod-rules.md", AutomodImportExport.toMarkdown(rules))
                    },
                    onInstallDefaults = {
                        AutomodImportExport.defaultStarterPack().forEach { repository.upsert(it) }
                    }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.10f))

                if (wide) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        Box(modifier = Modifier.weight(0.42f).fillMaxHeight()) {
                            LeftPane(
                                rules = filtered,
                                selectedId = selectedId,
                                filter = filter,
                                query = query,
                                totalCount = rules.size,
                                onFilterChanged = { filter = it },
                                onQueryChanged = { query = it },
                                onSelect = { selectedId = it.id },
                                onToggleEnabled = { r ->
                                    repository.setEnabled(r.id, !r.enabled)
                                },
                                onAddNew = {
                                    val fresh = emptyRule(currentChannelLogin, filter)
                                    repository.upsert(fresh)
                                    selectedId = fresh.id
                                }
                            )
                        }
                        HorizontalDivider(
                            modifier = Modifier.fillMaxHeight().width(1.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.10f)
                        )
                        Box(modifier = Modifier.weight(0.58f).fillMaxHeight()) {
                            RightPane(
                                rule = selected,
                                currentChannelLogin = currentChannelLogin,
                                onChange = { repository.upsert(it) },
                                onDelete = { r ->
                                    repository.delete(r.id)
                                    selectedId = null
                                }
                            )
                        }
                    }
                } else {
                    if (selected == null) {
                        LeftPane(
                            rules = filtered,
                            selectedId = selectedId,
                            filter = filter,
                            query = query,
                            totalCount = rules.size,
                            onFilterChanged = { filter = it },
                            onQueryChanged = { query = it },
                            onSelect = { selectedId = it.id },
                            onToggleEnabled = { r ->
                                repository.setEnabled(r.id, !r.enabled)
                            },
                            onAddNew = {
                                val fresh = emptyRule(currentChannelLogin, filter)
                                repository.upsert(fresh)
                                selectedId = fresh.id
                            }
                        )
                    } else {
                        RightPane(
                            rule = selected,
                            currentChannelLogin = currentChannelLogin,
                            onBack = { selectedId = null },
                            onChange = { repository.upsert(it) },
                            onDelete = { r ->
                                repository.delete(r.id)
                                selectedId = null
                            }
                        )
                    }
                }
            }
        }
    }
}

private fun emptyRule(currentChannel: String?, filter: AutomodFilter): AutomodRule {
    val scope = when (filter) {
        AutomodFilter.LOCAL -> AutomodScope.LOCAL
        else -> AutomodScope.GLOBAL
    }
    return AutomodRule(
        id = AutomodImportExport.newId(),
        scope = scope,
        channelLogin = if (scope == AutomodScope.LOCAL) currentChannel?.lowercase() else null,
        pattern = "",
        action = AutomodAction.DELETE,
        enabled = true
    )
}

@Composable
private fun AutomodTopBar(
    title: String,
    subtitle: String?,
    onClose: () -> Unit,
    onImport: () -> Unit,
    onExportJson: () -> Unit,
    onExportMd: () -> Unit,
    onInstallDefaults: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
        PillButton("Import") { onImport() }
        Spacer(Modifier.width(6.dp))
        Box {
            PillButton("Export ▾") { menuOpen = true }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text("Export JSON") },
                    onClick = { menuOpen = false; onExportJson() }
                )
                DropdownMenuItem(
                    text = { Text("Export Markdown (docx-friendly)") },
                    onClick = { menuOpen = false; onExportMd() }
                )
                DropdownMenuItem(
                    text = { Text("Install starter pack") },
                    onClick = { menuOpen = false; onInstallDefaults() }
                )
            }
        }
        Spacer(Modifier.width(6.dp))
        IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
            Icon(
                Icons.Filled.Close,
                contentDescription = "Close",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LeftPane(
    rules: List<AutomodRule>,
    selectedId: String?,
    filter: AutomodFilter,
    query: String,
    totalCount: Int,
    onFilterChanged: (AutomodFilter) -> Unit,
    onQueryChanged: (String) -> Unit,
    onSelect: (AutomodRule) -> Unit,
    onToggleEnabled: (AutomodRule) -> Unit,
    onAddNew: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterPill("All", filter == AutomodFilter.ALL) { onFilterChanged(AutomodFilter.ALL) }
            FilterPill("Global", filter == AutomodFilter.GLOBAL) { onFilterChanged(AutomodFilter.GLOBAL) }
            FilterPill("Local", filter == AutomodFilter.LOCAL) { onFilterChanged(AutomodFilter.LOCAL) }
            Spacer(Modifier.weight(1f))
            Text(
                "${rules.size}/$totalCount",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
            Spacer(Modifier.width(4.dp))
            IconButton(
                onClick = onAddNew,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))
            ) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = "Add rule",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(6.dp))
            Box(modifier = Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text(
                        "Search patterns, channel, note…",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChanged,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChanged("") }, modifier = Modifier.size(22.dp)) {
                    Icon(Icons.Filled.Close, null, modifier = Modifier.size(12.dp))
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        if (rules.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Outlined.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f),
                    modifier = Modifier.size(36.dp)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "No rules yet",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Tap + to create your first rule",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(rules, key = { it.id }) { rule ->
                    RuleListItem(
                        rule = rule,
                        selected = rule.id == selectedId,
                        onClick = { onSelect(rule) },
                        onToggle = { onToggleEnabled(rule) }
                    )
                }
            }
        }
    }
}

@Composable
private fun RuleListItem(
    rule: AutomodRule,
    selected: Boolean,
    onClick: () -> Unit,
    onToggle: () -> Unit
) {
    val bg = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent
    val scopeColor = if (rule.scope == AutomodScope.GLOBAL)
        MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
    val actionColor = when (rule.action) {
        AutomodAction.DELETE -> MaterialTheme.colorScheme.onSurfaceVariant
        AutomodAction.TIMEOUT -> Color(0xFFFFB74D)
        AutomodAction.BAN -> Color(0xFFE57373)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ScopeBadge(rule, scopeColor)
                Spacer(Modifier.width(6.dp))
                Text(
                    rule.displayLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (rule.enabled) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (rule.enabled) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(actionColor.copy(alpha = 0.9f))
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    when (rule.action) {
                        AutomodAction.DELETE -> "delete"
                        AutomodAction.TIMEOUT -> "timeout ${rule.timeoutMs / 1000}s"
                        AutomodAction.BAN -> "ban"
                    } + if (rule.alternates.isNotEmpty()) " · ${rule.alternates.size} alt" else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
        Switch(
            checked = rule.enabled,
            onCheckedChange = { onToggle() },
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
            ),
            modifier = Modifier.scale(0.8f)
        )
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.05f))
}

@Composable
private fun ScopeBadge(rule: AutomodRule, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.18f))
            .padding(horizontal = 5.dp, vertical = 2.dp)
    ) {
        Text(
            rule.scopeLabel,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 9.sp,
            color = color,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

@Composable
private fun RightPane(
    rule: AutomodRule?,
    currentChannelLogin: String?,
    onBack: (() -> Unit)? = null,
    onChange: (AutomodRule) -> Unit,
    onDelete: (AutomodRule) -> Unit
) {
    if (rule == null) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "Select a rule on the left",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "…or create a new one",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
            )
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (onBack != null) {
            TextButtonSmall("← Back to list") { onBack() }
        }

        SectionLabel("Scope")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterPill("Global", rule.scope == AutomodScope.GLOBAL) {
                onChange(rule.copy(scope = AutomodScope.GLOBAL, channelLogin = null))
            }
            FilterPill(
                "Local channel",
                rule.scope == AutomodScope.LOCAL
            ) {
                onChange(
                    rule.copy(
                        scope = AutomodScope.LOCAL,
                        channelLogin = (rule.channelLogin ?: currentChannelLogin)?.lowercase()
                    )
                )
            }
        }
        if (rule.scope == AutomodScope.LOCAL) {
            TextRow(
                label = "Channel login",
                value = rule.channelLogin.orEmpty(),
                placeholder = currentChannelLogin ?: "e.g. shroud",
                onChange = { onChange(rule.copy(channelLogin = it.lowercase().trim().takeIf { s -> s.isNotBlank() })) }
            )
        }

        SectionLabel("Pattern")
        TextRow(
            label = "Word or phrase",
            value = rule.pattern,
            placeholder = "e.g. spoiler",
            onChange = { onChange(rule.copy(pattern = it)) }
        )
        AlternatesEditor(
            alternates = rule.alternates,
            onChange = { onChange(rule.copy(alternates = it)) }
        )

        SectionLabel("Matching")
        ToggleRow(
            "Case sensitive",
            "Match only when the exact case is used",
            rule.caseSensitive
        ) { onChange(rule.copy(caseSensitive = it)) }
        ToggleRow(
            "Whole word only",
            "Ignore partial matches inside longer words",
            rule.wholeWord
        ) { onChange(rule.copy(wholeWord = it)) }
        ToggleRow(
            "Regular expression",
            "Advanced: treat the pattern as a regex",
            rule.isRegex
        ) { onChange(rule.copy(isRegex = it)) }

        SectionLabel("Action")
        ActionSelector(rule.action) { onChange(rule.copy(action = it)) }
        if (rule.action == AutomodAction.TIMEOUT) {
            NumberRow(
                label = "Timeout (ms)",
                value = rule.timeoutMs,
                helper = "= ${rule.timeoutMs / 1000} seconds",
                onChange = { onChange(rule.copy(timeoutMs = it.coerceAtLeast(1_000L))) }
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(
                    "1s" to 1_000L, "10s" to 10_000L, "1m" to 60_000L,
                    "10m" to 600_000L, "1h" to 3_600_000L, "24h" to 86_400_000L
                ).forEach { (label, ms) ->
                    FilterPill(label, rule.timeoutMs == ms) {
                        onChange(rule.copy(timeoutMs = ms))
                    }
                }
            }
        }

        SectionLabel("Frequency (spam)")
        NumberRow(
            label = "Trigger threshold",
            value = rule.frequencyThreshold.toLong(),
            helper = "0 = fire on first match. Otherwise require this many hits per window.",
            onChange = { onChange(rule.copy(frequencyThreshold = it.toInt().coerceIn(0, 999))) }
        )
        if (rule.frequencyThreshold > 0) {
            NumberRow(
                label = "Window (ms)",
                value = rule.frequencyWindowMs,
                helper = "Sliding window in milliseconds.",
                onChange = { onChange(rule.copy(frequencyWindowMs = it.coerceAtLeast(1_000L))) }
            )
        }

        SectionLabel("Exemptions")
        ToggleRow("Exempt mods", null, rule.exemptMods) { onChange(rule.copy(exemptMods = it)) }
        ToggleRow("Exempt VIPs", null, rule.exemptVips) { onChange(rule.copy(exemptVips = it)) }
        ToggleRow("Exempt subs", null, rule.exemptSubs) { onChange(rule.copy(exemptSubs = it)) }

        SectionLabel("Note")
        TextRow(
            label = "Why this rule exists (optional)",
            value = rule.note,
            placeholder = "e.g. spoilers for tonight's stream",
            onChange = { onChange(rule.copy(note = it)) },
            multiline = true
        )

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                if (rule.enabled) "Rule is active" else "Rule is disabled",
                style = MaterialTheme.typography.labelMedium,
                color = if (rule.enabled) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
            Spacer(Modifier.weight(1f))
            Switch(
                checked = rule.enabled,
                onCheckedChange = { onChange(rule.copy(enabled = it)) }
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.End
        ) {
            DangerButton("Delete rule") { onDelete(rule) }
        }
    }
}

@Composable
private fun AlternatesEditor(
    alternates: List<String>,
    onChange: (List<String>) -> Unit
) {
    var draft by remember { mutableStateOf("") }
    SectionLabelSmall("Alternates / transliterations")
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        alternates.forEachIndexed { idx, alt ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    alt,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = {
                        onChange(alternates.toMutableList().also { it.removeAt(idx) })
                    },
                    modifier = Modifier.size(22.dp)
                ) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = "Remove",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                    RoundedCornerShape(10.dp)
                )
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                if (draft.isEmpty()) {
                    Text(
                        "Add variant (enter to add)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }
                BasicTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(Modifier.width(6.dp))
            PillButton("Add") {
                val v = draft.trim()
                if (v.isNotEmpty() && v !in alternates) {
                    onChange(alternates + v)
                    draft = ""
                }
            }
        }
    }
}

@Composable
private fun ActionSelector(action: AutomodAction, onChange: (AutomodAction) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        FilterPill("Delete", action == AutomodAction.DELETE) { onChange(AutomodAction.DELETE) }
        FilterPill("Timeout", action == AutomodAction.TIMEOUT) { onChange(AutomodAction.TIMEOUT) }
        FilterPill("Ban", action == AutomodAction.BAN) { onChange(AutomodAction.BAN) }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
private fun SectionLabelSmall(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
    )
}

@Composable
private fun TextRow(
    label: String,
    value: String,
    placeholder: String = "",
    multiline: Boolean = false,
    onChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SectionLabelSmall(label)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                    RoundedCornerShape(10.dp)
                )
                .padding(horizontal = 10.dp, vertical = if (multiline) 10.dp else 10.dp)
        ) {
            if (value.isEmpty()) {
                Text(
                    placeholder,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onChange,
                singleLine = !multiline,
                maxLines = if (multiline) 4 else 1,
                textStyle = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun NumberRow(
    label: String,
    value: Long,
    helper: String? = null,
    onChange: (Long) -> Unit
) {
    var text by remember(value) { mutableStateOf(value.toString()) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SectionLabelSmall(label)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                    RoundedCornerShape(10.dp)
                )
                .padding(horizontal = 10.dp, vertical = 10.dp)
        ) {
            BasicTextField(
                value = text,
                onValueChange = { t ->
                    val filtered = t.filter { it.isDigit() }
                    text = filtered
                    filtered.toLongOrNull()?.let { onChange(it) }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                textStyle = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (!helper.isNullOrBlank()) {
            Text(
                helper,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun ToggleRow(
    label: String,
    helper: String?,
    value: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (!helper.isNullOrBlank()) {
                Text(
                    helper,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
        Switch(
            checked = value,
            onCheckedChange = onChange
        )
    }
}

@Composable
private fun FilterPill(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun PillButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun TextButtonSmall(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun DangerButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Outlined.Delete,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

