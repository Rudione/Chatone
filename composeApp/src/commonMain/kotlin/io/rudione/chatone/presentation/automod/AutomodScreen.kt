package io.rudione.chatone.presentation.automod

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.*
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
import io.rudione.chatone.domain.model.*
import io.rudione.chatone.presentation.theme.ChatoneTheme
import io.rudione.chatone.presentation.theme.i18n.LocalStrings
import io.rudione.chatone.util.AutomodImportExport
import kotlinx.datetime.Clock
import org.koin.compose.koinInject

enum class AutomodFilter { ALL, GLOBAL, LOCAL }
private enum class AutomodTab { WORDS, CHAT_RULES }

private enum class TimeUnit(val label: String, val toSeconds: Long) {
    SECONDS("s", 1L),
    MINUTES("min", 60L),
    HOURS("h", 3600L)
}
private fun secondsToDisplay(totalSec: Int): Pair<Int, TimeUnit> = when {
    totalSec % 3600 == 0 && totalSec >= 3600 -> Pair(totalSec / 3600, TimeUnit.HOURS)
    totalSec % 60 == 0 && totalSec >= 60 -> Pair(totalSec / 60, TimeUnit.MINUTES)
    else -> Pair(totalSec, TimeUnit.SECONDS)
}
private fun msToDisplay(ms: Long): Triple<Long, TimeUnit, Long> {
    val totalSec = ms / 1000L
    return when {
        totalSec % 3600 == 0L && totalSec >= 3600 -> Triple(totalSec / 3600, TimeUnit.HOURS, ms)
        totalSec % 60 == 0L && totalSec >= 60 -> Triple(totalSec / 60, TimeUnit.MINUTES, ms)
        else -> Triple(totalSec, TimeUnit.SECONDS, ms)
    }
}

@Composable
fun AutomodScreen(
    currentChannelLogin: String?,
    onClose: () -> Unit,
    onExport: (fileName: String, content: String) -> Unit,
    onImport: () -> Unit,
    modifier: Modifier = Modifier,
    repository: AutomodRepository = koinInject()
) {
    var activeTab by remember { mutableStateOf(AutomodTab.WORDS) }
    val wordRules by repository.rules.collectAsState()
    val chatRules by repository.chatRules.collectAsState()

    ChatoneTheme {
        BoxWithConstraints(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            val wide = maxWidth >= 640.dp
            var wordSelectedId by remember { mutableStateOf<String?>(null) }
            var chatSelectedId by remember { mutableStateOf<String?>(null) }

            LaunchedEffect(wordRules) { if (wordSelectedId == null) wordSelectedId = wordRules.firstOrNull()?.id }
            LaunchedEffect(chatRules) { if (chatSelectedId == null) chatSelectedId = chatRules.firstOrNull()?.id }

            val selectedWord = wordRules.firstOrNull { it.id == wordSelectedId }
            val selectedChat = chatRules.firstOrNull { it.id == chatSelectedId }

            if (wide) {
                Row(Modifier.fillMaxSize()) {
                    Surface(
                        modifier = Modifier.weight(0.40f).fillMaxHeight(),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        tonalElevation = 0.dp
                    ) {
                        Column(Modifier.fillMaxSize()) {
                            LeftHeader(
                                title = LocalStrings.current.automodLocalTitle,
                                subtitle = currentChannelLogin?.let { "#$it" },
                                activeTab = activeTab,
                                onTabChange = { activeTab = it },
                                onClose = onClose,
                                showImportExport = true,
                                wordRules = wordRules,
                                chatRules = chatRules,
                                onImport = onImport,
                                onExportJson = { onExport("automod-rules.json", AutomodImportExport.toJson(wordRules, chatRules)) },
                                onExportXlsx = { onExport("automod-rules.xlsx", AutomodImportExport.toXlsx(wordRules, chatRules)) },
                                onExportCsv = { onExport("automod-rules.csv", AutomodImportExport.toCsv(wordRules, chatRules)) },
                                onInstallDefaults = {
                                    AutomodImportExport.defaultWordStarterPack().forEach { repository.upsert(it) }
                                    AutomodImportExport.defaultChatStarterPack().forEach { repository.upsertChatRule(it) }
                                }
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            when (activeTab) {
                                AutomodTab.WORDS -> WordListPane(wordRules, repository, currentChannelLogin, wide = true,
                                    selectedId = wordSelectedId, onSelectId = { wordSelectedId = it })
                                AutomodTab.CHAT_RULES -> ChatRuleListPane(chatRules, repository, currentChannelLogin, wide = true,
                                    selectedId = chatSelectedId, onSelectId = { chatSelectedId = it })
                            }
                        }
                    }
                    Box(Modifier.width(1.dp).fillMaxHeight().background(MaterialTheme.colorScheme.outlineVariant))
                    Surface(
                        modifier = Modifier.weight(0.60f).fillMaxHeight(),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        when (activeTab) {
                            AutomodTab.WORDS -> WordDetailContent(
                                rule = selectedWord,
                                currentChannelLogin = currentChannelLogin,
                                onBack = null,
                                onChange = { repository.upsert(it) },
                                onDelete = { repository.delete(it.id); wordSelectedId = null }
                            )
                            AutomodTab.CHAT_RULES -> ChatRuleDetailContent(
                                rule = selectedChat,
                                currentChannelLogin = currentChannelLogin,
                                onBack = null,
                                onChange = { repository.upsertChatRule(it) },
                                onDelete = { repository.deleteChatRule(it.id); chatSelectedId = null }
                            )
                        }
                    }
                }
            } else {
                Column(Modifier.fillMaxSize()) {
                    LeftHeader(
                        title = LocalStrings.current.automodLocalTitle,
                        subtitle = currentChannelLogin?.let { "#$it" },
                        activeTab = activeTab, onTabChange = { activeTab = it }, onClose = onClose,
                        showImportExport = true, wordRules = wordRules, chatRules = chatRules,
                        onImport = onImport,
                        onExportJson = { onExport("automod-rules.json", AutomodImportExport.toJson(wordRules, chatRules)) },
                        onExportXlsx = { onExport("automod-rules.xlsx", AutomodImportExport.toXlsx(wordRules, chatRules)) },
                        onExportCsv = { onExport("automod-rules.csv", AutomodImportExport.toCsv(wordRules, chatRules)) },
                        onInstallDefaults = {
                            AutomodImportExport.defaultWordStarterPack().forEach { repository.upsert(it) }
                            AutomodImportExport.defaultChatStarterPack().forEach { repository.upsertChatRule(it) }
                        }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    when (activeTab) {
                        AutomodTab.WORDS -> WordListPane(wordRules, repository, currentChannelLogin, wide = false,
                            selectedId = wordSelectedId, onSelectId = { wordSelectedId = it })
                        AutomodTab.CHAT_RULES -> ChatRuleListPane(chatRules, repository, currentChannelLogin, wide = false,
                            selectedId = chatSelectedId, onSelectId = { chatSelectedId = it })
                    }
                }
            }
        }
    }
}


@Composable
private fun LeftHeader(
    title: String,
    subtitle: String?,
    activeTab: AutomodTab,
    onTabChange: (AutomodTab) -> Unit,
    onClose: () -> Unit,
    showImportExport: Boolean,
    wordRules: List<AutomodRule>,
    chatRules: List<ChatRule>,
    onImport: () -> Unit,
    onExportJson: () -> Unit,
    onExportXlsx: () -> Unit,
    onExportCsv: () -> Unit,
    onInstallDefaults: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    val s = LocalStrings.current

    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 14.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface)
                if (!subtitle.isNullOrBlank()) {
                    Text(subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (showImportExport) {
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Outlined.Download, contentDescription = "Options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    DropdownMenu(menuOpen, { menuOpen = false }) {
                        DropdownMenuItem({ Text(s.automodImport) }, { menuOpen = false; onImport() })
                        DropdownMenuItem({ Text(s.automodExportJson) }, { menuOpen = false; onExportJson() })
                        DropdownMenuItem({ Text(s.automodExportXlsx) }, { menuOpen = false; onExportXlsx() })
                        DropdownMenuItem({ Text(s.automodExportCsv) }, { menuOpen = false; onExportCsv() })
                        HorizontalDivider()
                        DropdownMenuItem({ Text(s.automodInstallStarter) }, { menuOpen = false; onInstallDefaults() })
                    }
                }
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.Close, contentDescription = "Close",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
            Md3Tab(
                label = s.automodTabWords,
                count = wordRules.size,
                selected = activeTab == AutomodTab.WORDS,
                onClick = { onTabChange(AutomodTab.WORDS) },
                modifier = Modifier.weight(1f)
            )
            Md3Tab(
                label = s.automodTabChatRules,
                count = chatRules.size,
                selected = activeTab == AutomodTab.CHAT_RULES,
                onClick = { onTabChange(AutomodTab.CHAT_RULES) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun Md3Tab(label: String, count: Int, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val color by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        tween(200)
    )
    Column(
        modifier = modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick
        ).padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = color,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
            if (count > 0) {
                Surface(
                    shape = CircleShape,
                    color = if (selected) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceContainerHighest,
                    tonalElevation = 0.dp
                ) {
                    Text(
                        count.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier.height(2.dp)
                .fillMaxWidth(if (selected) 0.5f else 0f)
                .clip(RoundedCornerShape(1.dp))
                .background(MaterialTheme.colorScheme.primary)
        )
    }
}

@Composable
private fun WordListPane(
    rules: List<AutomodRule>,
    repository: AutomodRepository,
    currentChannelLogin: String?,
    wide: Boolean,
    selectedId: String?,
    onSelectId: (String?) -> Unit
) {
    var filter by remember { mutableStateOf(AutomodFilter.ALL) }
    var query by remember { mutableStateOf("") }
    val s = LocalStrings.current
    val selected = rules.firstOrNull { it.id == selectedId }

    val filtered = remember(rules, filter, query) {
        rules.filter { r ->
            val mf = when (filter) {
                AutomodFilter.ALL -> true
                AutomodFilter.GLOBAL -> r.scope == AutomodScope.GLOBAL
                AutomodFilter.LOCAL -> r.scope == AutomodScope.LOCAL
            }
            val q = query.trim()
            mf && (q.isEmpty() || r.pattern.contains(q, true) || r.alternates.any { it.contains(q, true) }
                    || r.channelLogin?.contains(q, true) == true || r.note.contains(q, true))
        }
    }
    LaunchedEffect(filtered) {
        if (selectedId == null || filtered.none { it.id == selectedId })
            onSelectId(filtered.firstOrNull()?.id)
    }

    if (!wide && selected != null) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
            WordDetailContent(
                rule = selected,
                currentChannelLogin = currentChannelLogin,
                onBack = { onSelectId(null) },
                onChange = { repository.upsert(it) },
                onDelete = { repository.delete(it.id); onSelectId(null) }
            )
        }
        return
    }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            FilterChip(selected = filter == AutomodFilter.ALL, onClick = { filter = AutomodFilter.ALL },
                label = { Text(s.automodFilterAll) })
            FilterChip(selected = filter == AutomodFilter.GLOBAL, onClick = { filter = AutomodFilter.GLOBAL },
                label = { Text(s.automodFilterGlobal) })
            FilterChip(selected = filter == AutomodFilter.LOCAL, onClick = { filter = AutomodFilter.LOCAL },
                label = { Text(s.automodFilterLocal) })
            Spacer(Modifier.weight(1f))
            FilledIconButton(onClick = {
                val fresh = AutomodRule(id = AutomodImportExport.newId(),
                    scope = if (filter == AutomodFilter.LOCAL) AutomodScope.LOCAL else AutomodScope.GLOBAL,
                    channelLogin = if (filter == AutomodFilter.LOCAL) currentChannelLogin?.lowercase() else null,
                    pattern = "", action = AutomodAction.DELETE, enabled = true)
                repository.upsert(fresh)
                onSelectId(fresh.id)
            }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Filled.Add, "Add", modifier = Modifier.size(18.dp))
            }
        }
        Md3SearchBar(query, { query = it }, s.automodSearchPlaceholder)
        Spacer(Modifier.height(4.dp))
        if (filtered.isEmpty()) {
            EmptyHint(s.automodNoRules, s.automodNoRulesHint, Modifier.fillMaxSize())
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(filtered, key = { it.id }) { rule ->
                    WordRuleRow(
                        rule = rule,
                        selected = rule.id == selectedId && wide,
                        onClick = { onSelectId(rule.id) },
                        onToggle = { repository.setEnabled(rule.id, !rule.enabled) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatRuleListPane(
    rules: List<ChatRule>,
    repository: AutomodRepository,
    currentChannelLogin: String?,
    wide: Boolean,
    selectedId: String?,
    onSelectId: (String?) -> Unit
) {
    var filter by remember { mutableStateOf(AutomodFilter.ALL) }
    var query by remember { mutableStateOf("") }
    var channelFilter by remember { mutableStateOf<String?>(null) }
    val s = LocalStrings.current

    val knownChannels = remember(rules) {
        rules.mapNotNull { it.channelLogin?.takeIf { c -> c.isNotBlank() } }
            .map { it.lowercase() }
            .distinct()
            .sorted()
    }

    val filtered = remember(rules, filter, query, channelFilter) {
        rules.filter { r ->
            val mf = when (filter) {
                AutomodFilter.ALL -> true
                AutomodFilter.GLOBAL -> r.scope == AutomodScope.GLOBAL
                AutomodFilter.LOCAL -> r.scope == AutomodScope.LOCAL
            }
            val cf = channelFilter?.let {
                r.scope == AutomodScope.LOCAL && r.channelLogin.equals(it, ignoreCase = true)
            } ?: true
            val q = query.trim()
            mf && cf && (q.isEmpty()
                    || r.displayLabel.contains(q, true)
                    || r.eventMessage.contains(q, true)
                    || r.channelLogin?.contains(q, true) == true
                    || r.type.name.contains(q, true)
                    || r.action.name.contains(q, true))
        }
    }
    val selected = filtered.firstOrNull { it.id == selectedId }

    LaunchedEffect(filtered) {
        if (selectedId == null || filtered.none { it.id == selectedId })
            onSelectId(filtered.firstOrNull()?.id)
    }

    if (!wide && selected != null) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
            ChatRuleDetailContent(
                rule = selected, currentChannelLogin = currentChannelLogin,
                onBack = { onSelectId(null) },
                onChange = { repository.upsertChatRule(it) },
                onDelete = { repository.deleteChatRule(it.id); onSelectId(null) }
            )
        }
        return
    }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            FilterChip(selected = filter == AutomodFilter.ALL, onClick = { filter = AutomodFilter.ALL },
                label = { Text(s.automodFilterAll) })
            FilterChip(selected = filter == AutomodFilter.GLOBAL, onClick = { filter = AutomodFilter.GLOBAL },
                label = { Text(s.automodFilterGlobal) })
            FilterChip(selected = filter == AutomodFilter.LOCAL, onClick = { filter = AutomodFilter.LOCAL },
                label = { Text(s.automodFilterLocal) })
            Spacer(Modifier.weight(1f))
            FilledIconButton(onClick = {
                val fresh = ChatRule(
                    id = AutomodImportExport.newId(),
                    type = ChatRuleType.SPAM_RATE,
                    scope = if (filter == AutomodFilter.LOCAL) AutomodScope.LOCAL else AutomodScope.GLOBAL,
                    channelLogin = if (filter == AutomodFilter.LOCAL) currentChannelLogin?.lowercase() else null,
                    createdAt = Clock.System.now().toEpochMilliseconds()
                )
                repository.upsertChatRule(fresh)
                onSelectId(fresh.id)
            }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Filled.Add, "Add", modifier = Modifier.size(18.dp))
            }
        }
        Md3SearchBar(query, { query = it }, s.automodSearchPlaceholder)
        if (knownChannels.isNotEmpty()) {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = channelFilter == null,
                    onClick = { channelFilter = null },
                    label = { Text(s.chatRuleChannelFilterAll) }
                )
                knownChannels.forEach { ch ->
                    FilterChip(
                        selected = channelFilter == ch,
                        onClick = { channelFilter = if (channelFilter == ch) null else ch },
                        label = { Text("#$ch") }
                    )
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        if (filtered.isEmpty()) {
            EmptyHint(s.chatRuleNoRules, s.chatRuleNoRulesHint, Modifier.fillMaxSize())
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(filtered, key = { it.id }) { rule ->
                    ChatRuleRow(
                        rule = rule,
                        selected = rule.id == selectedId && wide,
                        onClick = { onSelectId(rule.id) },
                        onToggle = { repository.setChatRuleEnabled(rule.id, !rule.enabled) }
                    )
                }
            }
        }
    }
}

@Composable
private fun WordRuleRow(rule: AutomodRule, selected: Boolean, onClick: () -> Unit, onToggle: () -> Unit) {
    val bg by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.secondaryContainer
        else Color.Transparent, tween(150)
    )
    Row(
        modifier = Modifier.fillMaxWidth()
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ScopePill(rule.scope, rule.channelLogin)
                Text(rule.displayLabel.ifEmpty { "—" },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (rule.enabled) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(2.dp))
            Text(
                buildString {
                    append(rule.action.name.lowercase())
                    if (rule.action == AutomodAction.TIMEOUT) append(" · ${rule.timeoutMs/1000}s")
                    if (rule.alternates.isNotEmpty()) append(" · +${rule.alternates.size} alt")
                },
                style = MaterialTheme.typography.labelSmall,
                color = actionColor(rule.action)
            )
        }
        Switch(checked = rule.enabled, onCheckedChange = { onToggle() },
            modifier = Modifier.scale(0.78f))
    }
    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
}

@Composable
private fun WordDetailContent(
    rule: AutomodRule?,
    currentChannelLogin: String?,
    onBack: (() -> Unit)?,
    onChange: (AutomodRule) -> Unit,
    onDelete: (AutomodRule) -> Unit
) {
    val s = LocalStrings.current
    if (rule == null) { EmptyHint(s.automodSelectRuleHint, "", Modifier.fillMaxSize()); return }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (onBack != null) {
            TextButton(onClick = onBack) { Text("← ${s.automodBackToList}") }
        }

        DetailSection(s.automodSectionScope) {
            SegmentedRow {
                Seg(s.automodScopeGlobal, rule.scope == AutomodScope.GLOBAL) {
                    onChange(rule.copy(scope = AutomodScope.GLOBAL, channelLogin = null))
                }
                Seg(s.automodScopeLocalChannel, rule.scope == AutomodScope.LOCAL) {
                    onChange(rule.copy(scope = AutomodScope.LOCAL,
                        channelLogin = rule.channelLogin ?: currentChannelLogin?.lowercase()))
                }
            }
            if (rule.scope == AutomodScope.LOCAL) {
                Md3TextField(s.automodChannelLogin, rule.channelLogin.orEmpty(),
                    currentChannelLogin ?: s.automodChannelPlaceholder) {
                    onChange(rule.copy(channelLogin = it.lowercase().trim().takeIf { v -> v.isNotBlank() }))
                }
            }
        }

        DetailSection(s.automodSectionPattern) {
            Md3TextField(s.automodWordOrPhrase, rule.pattern, s.automodPatternPlaceholder) { onChange(rule.copy(pattern = it)) }
            AlternatesEditor(rule.alternates) { onChange(rule.copy(alternates = it)) }
        }

        DetailSection(s.automodSectionMatching) {
            Md3Toggle(s.automodCaseSensitive, s.automodCaseSensitiveDesc, rule.caseSensitive) { onChange(rule.copy(caseSensitive = it)) }
            Md3Toggle(s.automodWholeWordOnly, s.automodWholeWordDesc, rule.wholeWord) { onChange(rule.copy(wholeWord = it)) }
            Md3Toggle(s.automodRegularExpression, s.automodRegexDesc, rule.isRegex) { onChange(rule.copy(isRegex = it)) }
        }

        DetailSection(s.automodSectionAction) {
            SegmentedRow {
                Seg("Delete", rule.action == AutomodAction.DELETE) { onChange(rule.copy(action = AutomodAction.DELETE)) }
                Seg("Timeout", rule.action == AutomodAction.TIMEOUT) { onChange(rule.copy(action = AutomodAction.TIMEOUT)) }
                Seg("Ban", rule.action == AutomodAction.BAN) { onChange(rule.copy(action = AutomodAction.BAN)) }
            }
            if (rule.action == AutomodAction.TIMEOUT) {
                TimeInputField(
                    label = s.automodTimeoutMs,
                    totalSeconds = (rule.timeoutMs / 1000L).toInt()
                ) { onChange(rule.copy(timeoutMs = it * 1000L)) }
            }
        }

        DetailSection(s.automodSectionFrequency) {
            Md3NumberField(s.automodTriggerThreshold, rule.frequencyThreshold.toLong(), s.automodTriggerThresholdDesc) {
                onChange(rule.copy(frequencyThreshold = it.toInt().coerceIn(0, 999)))
            }
            if (rule.frequencyThreshold > 0) {
                TimeInputField(s.automodWindowMs, (rule.frequencyWindowMs / 1000L).toInt()) {
                    onChange(rule.copy(frequencyWindowMs = it * 1000L))
                }
            }
        }

        DetailSection(s.automodSectionExemptions) {
            Md3Toggle(s.automodExemptMods, null, rule.exemptMods) { onChange(rule.copy(exemptMods = it)) }
            Md3Toggle(s.automodExemptVips, null, rule.exemptVips) { onChange(rule.copy(exemptVips = it)) }
            Md3Toggle(s.automodExemptSubs, null, rule.exemptSubs) { onChange(rule.copy(exemptSubs = it)) }
        }

        DetailSection(s.automodSectionNote) {
            Md3TextField(s.automodNoteLabel, rule.note, s.automodNotePlaceholder, multiline = true) { onChange(rule.copy(note = it)) }
        }

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(if (rule.enabled) s.automodRuleActive else s.automodRuleDisabled,
                style = MaterialTheme.typography.bodyMedium,
                color = if (rule.enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.weight(1f))
            Switch(rule.enabled, { onChange(rule.copy(enabled = it)) })
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        OutlinedButton(onClick = { onDelete(rule) },
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.error))
        ) {
            Icon(Icons.Outlined.Delete, null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(s.automodDeleteRule)
        }
    }
}


@Composable
private fun ChatRuleRow(rule: ChatRule, selected: Boolean, onClick: () -> Unit, onToggle: () -> Unit) {
    val typeColor = chatRuleTypeColor(rule.type)
    val bg by animateColorAsState(if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent, tween(150))
    Row(
        modifier = Modifier.fillMaxWidth().background(bg).clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(typeColor.copy(alpha = if (rule.enabled) 1f else 0.3f)))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ScopePill(rule.scope, rule.channelLogin)
                Text(rule.displayLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (rule.enabled) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(2.dp))
            Text(
                when (rule.action) {
                    ChatRuleAction.DELETE -> "delete"
                    ChatRuleAction.TIMEOUT -> "timeout ${rule.timeoutSeconds}s"
                    ChatRuleAction.BAN -> "ban"
                    ChatRuleAction.SEND_MESSAGE -> "send message"
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = rule.enabled, onCheckedChange = { onToggle() }, modifier = Modifier.scale(0.78f))
    }
    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
}

@Composable
private fun ChatRuleDetailContent(
    rule: ChatRule?,
    currentChannelLogin: String?,
    onBack: (() -> Unit)?,
    onChange: (ChatRule) -> Unit,
    onDelete: (ChatRule) -> Unit
) {
    val s = LocalStrings.current
    if (rule == null) { EmptyHint(s.chatRuleSelectHint, "", Modifier.fillMaxSize()); return }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (onBack != null) TextButton(onClick = onBack) { Text("← ${s.automodBackToList}") }

        DetailSection(s.chatRuleSectionScope) {
            SegmentedRow {
                Seg(s.automodScopeGlobal, rule.scope == AutomodScope.GLOBAL) {
                    onChange(rule.copy(scope = AutomodScope.GLOBAL, channelLogin = null))
                }
                Seg(s.automodScopeLocalChannel, rule.scope == AutomodScope.LOCAL) {
                    onChange(rule.copy(scope = AutomodScope.LOCAL,
                        channelLogin = rule.channelLogin ?: currentChannelLogin?.lowercase()))
                }
            }
            if (rule.scope == AutomodScope.LOCAL) {
                Md3TextField(s.automodChannelLogin, rule.channelLogin.orEmpty(),
                    currentChannelLogin ?: s.automodChannelPlaceholder) {
                    onChange(rule.copy(channelLogin = it.lowercase().trim().takeIf { v -> v.isNotBlank() }))
                }
            }
        }

        DetailSection(s.chatRuleType) {
            val types = listOf(
                ChatRuleType.SPAM_RATE to s.chatRuleTypeSpam,
                ChatRuleType.ALL_CAPS to s.chatRuleTypeCaps,
                ChatRuleType.LINKS to s.chatRuleTypeLinks,
                ChatRuleType.EMOTE_SPAM to s.chatRuleTypeEmotes,
                ChatRuleType.NEW_ACCOUNT to s.chatRuleTypeNewUser,
                ChatRuleType.DUPLICATE_MESSAGE to s.chatRuleTypeDuplicate,
                ChatRuleType.CONSECUTIVE_NUMBERS to s.chatRuleTypeConsecutiveNumbers,
                ChatRuleType.STREAM_ONLINE to s.chatRuleTypeStreamOnline,
                ChatRuleType.STREAM_OFFLINE to s.chatRuleTypeStreamOffline,
                ChatRuleType.FIRST_MESSAGE_GREETING to s.chatRuleTypeFirstMessageGreeting,
                ChatRuleType.RAID_WELCOME to s.chatRuleTypeRaidWelcome
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                types.chunked(2).forEach { chunk ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        chunk.forEach { (type, label) ->
                            TypeChip(label, rule.type == type, Modifier.weight(1f)) {
                                val newAction = if (type.isEventTrigger) ChatRuleAction.SEND_MESSAGE
                                else if (rule.action == ChatRuleAction.SEND_MESSAGE) ChatRuleAction.DELETE
                                else rule.action
                                onChange(rule.copy(type = type, action = newAction))
                            }
                        }
                        if (chunk.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }

        DetailSection(s.chatRuleSectionConfig) {
            when (rule.type) {
                ChatRuleType.SPAM_RATE -> {
                    Md3NumberField(s.chatRuleSpamMessages, rule.spamMaxMessages.toLong(), s.chatRuleSpamMessagesDesc) {
                        onChange(rule.copy(spamMaxMessages = it.toInt().coerceIn(1, 100)))
                    }
                    Md3NumberField(s.chatRuleSpamWindow, rule.spamWindowSeconds.toLong(), s.chatRuleSpamWindowDesc) {
                        onChange(rule.copy(spamWindowSeconds = it.toInt().coerceIn(1, 3600)))
                    }
                }
                ChatRuleType.ALL_CAPS -> {
                    Md3NumberField(s.chatRuleCapsPercent, rule.capsThresholdPercent.toLong(), s.chatRuleCapsPercentDesc) {
                        onChange(rule.copy(capsThresholdPercent = it.toInt().coerceIn(10, 100)))
                    }
                    Md3NumberField(s.chatRuleCapsMinLength, rule.capsMinLength.toLong(), s.chatRuleCapsMinLengthDesc) {
                        onChange(rule.copy(capsMinLength = it.toInt().coerceIn(1, 500)))
                    }
                }
                ChatRuleType.LINKS -> {
                    Md3Toggle(s.chatRuleLinksAllowClips, s.chatRuleLinksAllowClipsDesc, rule.linksAllowClips) {
                        onChange(rule.copy(linksAllowClips = it))
                    }
                    if (rule.linksAllowClips) {
                        Md3Toggle(
                            s.chatRuleLinksClipsSameChannelOnly,
                            s.chatRuleLinksClipsSameChannelOnlyDesc,
                            rule.linksClipsSameChannelOnly
                        ) { onChange(rule.copy(linksClipsSameChannelOnly = it)) }
                        if (rule.linksClipsSameChannelOnly) {
                            var clipChansText by remember(rule.id) {
                                mutableStateOf(rule.linksClipsAllowedChannels.joinToString("\n"))
                            }
                            OutlinedTextField(
                                value = clipChansText,
                                onValueChange = { v ->
                                    clipChansText = v
                                    val chans = v.lines()
                                        .map { it.trim().lowercase().removePrefix("#") }
                                        .filter { it.isNotBlank() }
                                    onChange(rule.copy(linksClipsAllowedChannels = chans))
                                },
                                label = { Text(s.chatRuleLinksClipsAllowedChannels) },
                                supportingText = { Text(s.chatRuleLinksClipsAllowedChannelsDesc) },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2,
                                maxLines = 6,
                                textStyle = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    Md3Toggle(s.chatRuleLinksRequireHttps, s.chatRuleLinksRequireHttpsDesc, rule.linksRequireHttps) {
                        onChange(rule.copy(linksRequireHttps = it))
                    }
                    var sitesText by remember(rule.id) {
                        mutableStateOf(rule.linksAllowedSites.joinToString("\n"))
                    }
                    OutlinedTextField(
                        value = sitesText,
                        onValueChange = { v ->
                            sitesText = v
                            val sites = v.lines().map { it.trim().lowercase() }.filter { it.isNotBlank() }
                            onChange(rule.copy(linksAllowedSites = sites))
                        },
                        label = { Text(s.chatRuleLinksAllowedSites) },
                        supportingText = { Text(s.chatRuleLinksAllowedSitesDesc) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 6,
                        textStyle = MaterialTheme.typography.bodySmall
                    )
                }
                ChatRuleType.EMOTE_SPAM -> Md3NumberField(s.chatRuleEmotesMax, rule.emoteMaxCount.toLong(), s.chatRuleEmotesMaxDesc) {
                    onChange(rule.copy(emoteMaxCount = it.toInt().coerceIn(1, 200)))
                }
                ChatRuleType.NEW_ACCOUNT -> Md3NumberField(s.chatRuleNewUserAccountAge, rule.newAccountAgeDays.toLong(), s.chatRuleNewUserAccountAgeDesc) {
                    onChange(rule.copy(newAccountAgeDays = it.toInt().coerceIn(1, 3650)))
                }
                ChatRuleType.DUPLICATE_MESSAGE -> Md3NumberField(s.chatRuleDuplicateMin, rule.duplicateMinLength.toLong(), s.chatRuleDuplicateMinDesc) {
                    onChange(rule.copy(duplicateMinLength = it.toInt().coerceIn(1, 500)))
                }
                ChatRuleType.CONSECUTIVE_NUMBERS -> Md3NumberField(s.chatRuleConsecutiveNumbers, rule.consecutiveNumbersThreshold.toLong(), s.chatRuleConsecutiveNumbersDesc) {
                    onChange(rule.copy(consecutiveNumbersThreshold = it.toInt().coerceIn(2, 100)))
                }
                ChatRuleType.STREAM_ONLINE,
                ChatRuleType.STREAM_OFFLINE,
                ChatRuleType.FIRST_MESSAGE_GREETING,
                ChatRuleType.RAID_WELCOME -> {
                    OutlinedTextField(
                        value = rule.eventMessage,
                        onValueChange = { onChange(rule.copy(eventMessage = it)) },
                        label = { Text(s.chatRuleEventMessage) },
                        supportingText = { Text(s.chatRuleEventMessageDesc) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 4,
                        textStyle = MaterialTheme.typography.bodySmall
                    )
                    Md3NumberField(s.chatRuleEventRepeat, rule.eventRepeat.toLong(), s.chatRuleEventRepeatDesc) {
                        onChange(rule.copy(eventRepeat = it.toInt().coerceIn(1, 10)))
                    }
                    Md3NumberField(s.chatRuleEventDelay, rule.eventDelaySeconds.toLong(), s.chatRuleEventDelayDesc) {
                        onChange(rule.copy(eventDelaySeconds = it.toInt().coerceIn(0, 600)))
                    }
                }
            }
        }

        if (!rule.type.isEventTrigger) {
            DetailSection(s.chatRuleSectionAction) {
                SegmentedRow {
                    Seg(s.chatRuleActionDelete, rule.action == ChatRuleAction.DELETE) { onChange(rule.copy(action = ChatRuleAction.DELETE)) }
                    Seg(s.chatRuleActionTimeout, rule.action == ChatRuleAction.TIMEOUT) { onChange(rule.copy(action = ChatRuleAction.TIMEOUT)) }
                    Seg(s.chatRuleActionBan, rule.action == ChatRuleAction.BAN) { onChange(rule.copy(action = ChatRuleAction.BAN)) }
                }
                if (rule.action == ChatRuleAction.TIMEOUT) {
                    TimeInputField(s.chatRuleTimeoutDuration, rule.timeoutSeconds) {
                        onChange(rule.copy(timeoutSeconds = it.coerceIn(1, 1_209_600)))
                    }
                }
            }
        }

        DetailSection(s.chatRuleSectionExemptions) {
            Md3Toggle(s.chatRuleExemptMods, null, rule.exemptMods) { onChange(rule.copy(exemptMods = it)) }
            Md3Toggle(s.chatRuleExemptVips, null, rule.exemptVips) { onChange(rule.copy(exemptVips = it)) }
            Md3Toggle(s.chatRuleExemptSubs, null, rule.exemptSubs) { onChange(rule.copy(exemptSubs = it)) }
        }

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(if (rule.enabled) s.chatRuleEnabled else s.chatRuleDisabled,
                style = MaterialTheme.typography.bodyMedium,
                color = if (rule.enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.weight(1f))
            Switch(rule.enabled, { onChange(rule.copy(enabled = it)) })
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        OutlinedButton(onClick = { onDelete(rule) },
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.error))
        ) {
            Icon(Icons.Outlined.Delete, null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(s.chatRuleDelete)
        }
    }
}

@Composable
private fun DetailSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary)
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            tonalElevation = 0.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun SegmentedRow(content: @Composable RowScope.() -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.small)
            .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.small),
        content = content
    )
}

@Composable
private fun RowScope.Seg(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent, tween(150))
    val fg by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface, tween(150))
    Box(
        Modifier.weight(1f).background(bg).clickable(onClick = onClick).padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (selected) Icon(Icons.Filled.Check, null, tint = fg, modifier = Modifier.size(14.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = fg, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
        }
    }
}

@Composable
private fun TypeChip(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        modifier = modifier,
        leadingIcon = if (selected) { { Icon(Icons.Filled.Check, null, modifier = Modifier.size(14.dp)) } } else null
    )
}

@Composable
private fun Md3SearchBar(query: String, onQuery: (String) -> Unit, placeholder: String) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Box(Modifier.weight(1f)) {
            if (query.isEmpty()) Text(placeholder, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
            BasicTextField(query, onQuery, singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth())
        }
        if (query.isNotEmpty()) {
            IconButton(onClick = { onQuery("") }, modifier = Modifier.size(20.dp)) {
                Icon(Icons.Filled.Close, null, modifier = Modifier.size(14.dp))
            }
        }
    }
}

@Composable
private fun Md3TextField(label: String, value: String, placeholder: String, multiline: Boolean = false, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        placeholder = { Text(placeholder, style = MaterialTheme.typography.bodySmall) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = !multiline,
        maxLines = if (multiline) 4 else 1,
        textStyle = MaterialTheme.typography.bodyMedium,
        shape = MaterialTheme.shapes.small
    )
}

@Composable
private fun Md3NumberField(label: String, value: Long, helper: String? = null, onChange: (Long) -> Unit) {
    var text by remember(value) { mutableStateOf(value.toString()) }
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        OutlinedTextField(
            value = text,
            onValueChange = { t ->
                val f = t.filter { it.isDigit() }
                text = f
                f.toLongOrNull()?.let { onChange(it) }
            },
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            textStyle = MaterialTheme.typography.bodyMedium,
            shape = MaterialTheme.shapes.small
        )
        if (!helper.isNullOrBlank()) {
            Text(helper, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun TimeInputField(label: String, totalSeconds: Int, onChange: (Int) -> Unit) {
    val (initValue, initUnit) = secondsToDisplay(totalSeconds)
    var displayValue by remember(totalSeconds) { mutableStateOf(initValue.toString()) }
    var unit by remember(totalSeconds) { mutableStateOf(initUnit) }
    var unitMenuOpen by remember { mutableStateOf(false) }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
        OutlinedTextField(
            value = displayValue,
            onValueChange = { t ->
                val f = t.filter { it.isDigit() }
                displayValue = f
                f.toIntOrNull()?.let { v -> onChange((v * unit.toSeconds).toInt().coerceAtLeast(1)) }
            },
            label = { Text(label) },
            modifier = Modifier.weight(1f),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            textStyle = MaterialTheme.typography.bodyMedium,
            shape = MaterialTheme.shapes.small
        )
        Box {
            OutlinedButton(
                onClick = { unitMenuOpen = true },
                modifier = Modifier.padding(top = 8.dp),
                shape = MaterialTheme.shapes.small
            ) { Text(unit.label, style = MaterialTheme.typography.labelLarge) }
            DropdownMenu(unitMenuOpen, { unitMenuOpen = false }) {
                TimeUnit.entries.forEach { u ->
                    DropdownMenuItem(
                        text = { Text(u.label) },
                        onClick = {
                            unitMenuOpen = false
                            val current = displayValue.toIntOrNull() ?: 1
                            unit = u
                            onChange((current * u.toSeconds).toInt().coerceAtLeast(1))
                        },
                        leadingIcon = if (u == unit) { { Icon(Icons.Filled.Check, null, modifier = Modifier.size(14.dp)) } } else null
                    )
                }
            }
        }
    }
}

@Composable
private fun Md3Toggle(label: String, desc: String?, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            if (!desc.isNullOrBlank())
                Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(value, onChange)
    }
}

@Composable
private fun ScopePill(scope: AutomodScope, channelLogin: String?) {
    val (bg, fg, label) = when (scope) {
        AutomodScope.GLOBAL -> Triple(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer, "G")
        AutomodScope.LOCAL -> Triple(
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer,
            channelLogin?.take(1)?.uppercase() ?: "L")
    }
    Surface(shape = CircleShape, color = bg, tonalElevation = 0.dp, modifier = Modifier.size(20.dp)) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = fg, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun EmptyHint(title: String, hint: String, modifier: Modifier) {
    Column(modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (hint.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
        }
    }
}

@Composable
private fun AlternatesEditor(alternates: List<String>, onChange: (List<String>) -> Unit) {
    var draft by remember { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Alternates / transliterations", style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        alternates.forEachIndexed { idx, alt ->
            Row(Modifier.fillMaxWidth()
                .clip(MaterialTheme.shapes.extraSmall)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text(alt, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                IconButton({ onChange(alternates.toMutableList().also { it.removeAt(idx) }) },
                    modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Outlined.Delete, "Remove", tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(14.dp))
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = draft, onValueChange = { draft = it },
                placeholder = { Text("Add variant…") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall,
                shape = MaterialTheme.shapes.extraSmall
            )
            FilledTonalButton(onClick = {
                val v = draft.trim()
                if (v.isNotEmpty() && v !in alternates) { onChange(alternates + v); draft = "" }
            }) { Text("Add") }
        }
    }
}

@Composable
private fun chatRuleTypeColor(type: ChatRuleType): Color = when (type) {
    ChatRuleType.SPAM_RATE -> Color(0xFF4FC3F7)
    ChatRuleType.ALL_CAPS -> Color(0xFFFFB74D)
    ChatRuleType.LINKS -> Color(0xFF81C784)
    ChatRuleType.EMOTE_SPAM -> Color(0xFFBA68C8)
    ChatRuleType.NEW_ACCOUNT -> Color(0xFFFF8A65)
    ChatRuleType.DUPLICATE_MESSAGE -> Color(0xFF90A4AE)
    ChatRuleType.CONSECUTIVE_NUMBERS -> Color(0xFFF48FB1)
    ChatRuleType.STREAM_ONLINE -> Color(0xFF66BB6A)
    ChatRuleType.STREAM_OFFLINE -> Color(0xFFE57373)
    ChatRuleType.FIRST_MESSAGE_GREETING -> Color(0xFFFFD54F)
    ChatRuleType.RAID_WELCOME -> Color(0xFF7E57C2)
}

@Composable
private fun actionColor(action: AutomodAction): Color = when (action) {
    AutomodAction.DELETE -> MaterialTheme.colorScheme.onSurfaceVariant
    AutomodAction.TIMEOUT -> Color(0xFFFFB74D)
    AutomodAction.BAN -> MaterialTheme.colorScheme.error
}
