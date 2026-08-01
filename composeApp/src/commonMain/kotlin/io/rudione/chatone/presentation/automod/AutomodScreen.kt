package io.rudione.chatone.presentation.automod

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import io.rudione.chatone.data.repository.AutomodRepository
import io.rudione.chatone.domain.model.*
import io.rudione.chatone.presentation.components.ChatoneTextField
import io.rudione.chatone.presentation.components.ExpressiveCheckChip
import io.rudione.chatone.presentation.theme.ChatoneTheme
import io.rudione.chatone.presentation.theme.i18n.LocalStrings
import io.rudione.chatone.util.automod.AutomodImportExport
import kotlin.time.Clock
import org.koin.compose.koinInject
import io.rudione.chatone.presentation.components.ChatoneIconButton
import io.rudione.chatone.presentation.components.ChatoneDropdownMenu

enum class AutomodFilter { ALL, GLOBAL, LOCAL }
private enum class AutomodTab { WORDS, CHAT_RULES }

private val WIDE_LAYOUT_MIN_WIDTH = 640.dp
private val LEFT_PANE_MIN_WIDTH = 260.dp
private val LEFT_PANE_DEFAULT_WIDTH = 340.dp
private val DETAIL_PANE_MIN_WIDTH = 320.dp
private val PANE_GUTTER = 10.dp
private val PANE_CORNER = 18.dp

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

    BoxWithConstraints(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        val wide = maxWidth >= WIDE_LAYOUT_MIN_WIDTH
        var wordSelectedId by remember { mutableStateOf<String?>(null) }
        var chatSelectedId by remember { mutableStateOf<String?>(null) }

        val density = LocalDensity.current
        val availableWidth = maxWidth - PANE_GUTTER * 2 - DividerHitWidth
        val maxLeftPaneWidth = (availableWidth - DETAIL_PANE_MIN_WIDTH).coerceAtLeast(LEFT_PANE_MIN_WIDTH)
        var leftPaneWidth by remember { mutableStateOf(LEFT_PANE_DEFAULT_WIDTH) }
        LaunchedEffect(maxLeftPaneWidth) {
            leftPaneWidth = leftPaneWidth.coerceIn(LEFT_PANE_MIN_WIDTH, maxLeftPaneWidth)
        }
        val clampedLeftPaneWidth = leftPaneWidth.coerceIn(LEFT_PANE_MIN_WIDTH, maxLeftPaneWidth)

        LaunchedEffect(wordRules) { if (wordSelectedId == null) wordSelectedId = wordRules.firstOrNull()?.id }
        LaunchedEffect(chatRules) { if (chatSelectedId == null) chatSelectedId = chatRules.firstOrNull()?.id }

        val selectedWord = wordRules.firstOrNull { it.id == wordSelectedId }
        val selectedChat = chatRules.firstOrNull { it.id == chatSelectedId }

        if (wide) {
            Row(Modifier.fillMaxSize().padding(PANE_GUTTER)) {
                Surface(
                    modifier = Modifier.width(clampedLeftPaneWidth).fillMaxHeight(),
                    shape = RoundedCornerShape(PANE_CORNER),
                    color = ChatoneTheme.extraColors.sidebarSurface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp
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
                AutomodPaneDivider(
                    onDelta = { deltaPx ->
                        val deltaDp = with(density) { deltaPx.toDp() }
                        leftPaneWidth = (leftPaneWidth + deltaDp)
                            .coerceIn(LEFT_PANE_MIN_WIDTH, maxLeftPaneWidth)
                    }
                )
                Surface(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    shape = RoundedCornerShape(PANE_CORNER),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp
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
            Surface(
                modifier = Modifier.fillMaxSize().padding(PANE_GUTTER),
                shape = RoundedCornerShape(PANE_CORNER),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                tonalElevation = 0.dp,
                shadowElevation = 0.dp
            ) {
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
                    ChatoneIconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Outlined.Download, contentDescription = "Options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    ChatoneDropdownMenu(menuOpen, { menuOpen = false }) {
                        DropdownMenuItem({ Text(s.automodImport) }, { menuOpen = false; onImport() })
                        DropdownMenuItem({ Text(s.automodExportJson) }, { menuOpen = false; onExportJson() })
                        DropdownMenuItem({ Text(s.automodExportXlsx) }, { menuOpen = false; onExportXlsx() })
                        DropdownMenuItem({ Text(s.automodExportCsv) }, { menuOpen = false; onExportCsv() })
                        HorizontalDivider()
                        DropdownMenuItem({ Text(s.automodInstallStarter) }, { menuOpen = false; onInstallDefaults() })
                    }
                }
            }
            ChatoneIconButton(onClick = onClose) {
                Icon(Icons.Filled.Close, contentDescription = "Close",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        AutomodTabRow(
            tabs = listOf(
                Triple(AutomodTab.WORDS, s.automodTabWords, wordRules.size),
                Triple(AutomodTab.CHAT_RULES, s.automodTabChatRules, chatRules.size)
            ),
            selected = activeTab,
            onSelect = onTabChange,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp)
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
            horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f).horizontalScroll(rememberScrollState())) {
                AutomodSegmented(
                    options = listOf(
                        AutomodFilter.ALL to s.automodFilterAll,
                        AutomodFilter.GLOBAL to s.automodFilterGlobal,
                        AutomodFilter.LOCAL to s.automodFilterLocal
                    ),
                    selected = filter,
                    onSelect = { filter = it }
                )
            }
            AutomodActionButton(
                icon = Icons.Filled.Add,
                contentDescription = "Add",
                onClick = {
                    val fresh = AutomodRule(id = AutomodImportExport.newId(),
                        scope = if (filter == AutomodFilter.LOCAL) AutomodScope.LOCAL else AutomodScope.GLOBAL,
                        channelLogin = if (filter == AutomodFilter.LOCAL) currentChannelLogin?.lowercase() else null,
                        pattern = "", action = AutomodAction.DELETE, enabled = true)
                    repository.upsert(fresh)
                    onSelectId(fresh.id)
                }
            )
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
            horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f).horizontalScroll(rememberScrollState())) {
                AutomodSegmented(
                    options = listOf(
                        AutomodFilter.ALL to s.automodFilterAll,
                        AutomodFilter.GLOBAL to s.automodFilterGlobal,
                        AutomodFilter.LOCAL to s.automodFilterLocal
                    ),
                    selected = filter,
                    onSelect = { filter = it }
                )
            }
            AutomodActionButton(
                icon = Icons.Filled.Add,
                contentDescription = "Add",
                onClick = {
                    val fresh = ChatRule(
                        id = AutomodImportExport.newId(),
                        type = ChatRuleType.SPAM_RATE,
                        scope = if (filter == AutomodFilter.LOCAL) AutomodScope.LOCAL else AutomodScope.GLOBAL,
                        channelLogin = if (filter == AutomodFilter.LOCAL) currentChannelLogin?.lowercase() else null,
                        createdAt = Clock.System.now().toEpochMilliseconds()
                    )
                    repository.upsertChatRule(fresh)
                    onSelectId(fresh.id)
                }
            )
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
        if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
        else Color.Transparent, tween(150)
    )
    val accent = MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier.fillMaxWidth()
            .background(bg)
            .drawWithContent {
                drawContent()
                if (selected) drawRect(color = accent, size = Size(3.dp.toPx(), size.height))
            }
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
        io.rudione.chatone.presentation.components.ChatoneSwitch(checked = rule.enabled, onCheckedChange = { onToggle() },
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
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)) {
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
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Md3Toggle(s.automodCaseSensitive, s.automodCaseSensitiveDesc, rule.caseSensitive, Modifier.weight(1f),
                    guide = s.automodCaseSensitiveGuide) { onChange(rule.copy(caseSensitive = it)) }
                Md3Toggle(s.automodWholeWordOnly, s.automodWholeWordDesc, rule.wholeWord, Modifier.weight(1f),
                    guide = s.automodWholeWordGuide) { onChange(rule.copy(wholeWord = it)) }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Md3Toggle(s.automodRegularExpression, s.automodRegexDesc, rule.isRegex, Modifier.weight(1f),
                    guide = s.automodRegexGuide) { onChange(rule.copy(isRegex = it)) }
                Md3Toggle(s.automodIgnoreLinks, s.automodIgnoreLinksDesc, rule.ignoreLinks, Modifier.weight(1f),
                    guide = s.automodIgnoreLinksGuide) { onChange(rule.copy(ignoreLinks = it)) }
            }
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
            ExemptionChips(
                rule.exemptMods, rule.exemptVips, rule.exemptSubs,
                s.automodExemptMods, s.automodExemptVips, s.automodExemptSubs,
                { onChange(rule.copy(exemptMods = it)) },
                { onChange(rule.copy(exemptVips = it)) },
                { onChange(rule.copy(exemptSubs = it)) }
            )
        }

        DetailSection(s.automodSectionNote) {
            Md3TextField(s.automodNoteLabel, rule.note, s.automodNotePlaceholder, multiline = true) { onChange(rule.copy(note = it)) }
        }

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(if (rule.enabled) s.automodRuleActive else s.automodRuleDisabled,
                style = MaterialTheme.typography.bodyMedium,
                color = if (rule.enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.weight(1f))
            io.rudione.chatone.presentation.components.ChatoneSwitch(rule.enabled, { onChange(rule.copy(enabled = it)) })
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
    val bg by animateColorAsState(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.10f) else Color.Transparent, tween(150))
    val accent = MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier.fillMaxWidth().background(bg)
            .drawWithContent {
                drawContent()
                if (selected) drawRect(color = accent, size = Size(3.dp.toPx(), size.height))
            }
            .clickable(onClick = onClick)
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
        io.rudione.chatone.presentation.components.ChatoneSwitch(checked = rule.enabled, onCheckedChange = { onToggle() }, modifier = Modifier.scale(0.78f))
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
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)) {
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
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Md3NumberField(s.chatRuleSpamMessages, rule.spamMaxMessages.toLong(), s.chatRuleSpamMessagesDesc, Modifier.weight(1f)) {
                            onChange(rule.copy(spamMaxMessages = it.toInt().coerceIn(1, 100)))
                        }
                        Md3NumberField(s.chatRuleSpamWindow, rule.spamWindowSeconds.toLong(), s.chatRuleSpamWindowDesc, Modifier.weight(1f)) {
                            onChange(rule.copy(spamWindowSeconds = it.toInt().coerceIn(1, 3600)))
                        }
                    }
                }
                ChatRuleType.ALL_CAPS -> {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Md3NumberField(s.chatRuleCapsPercent, rule.capsThresholdPercent.toLong(), s.chatRuleCapsPercentDesc, Modifier.weight(1f)) {
                            onChange(rule.copy(capsThresholdPercent = it.toInt().coerceIn(10, 100)))
                        }
                        Md3NumberField(s.chatRuleCapsMinLength, rule.capsMinLength.toLong(), s.chatRuleCapsMinLengthDesc, Modifier.weight(1f)) {
                            onChange(rule.copy(capsMinLength = it.toInt().coerceIn(1, 500)))
                        }
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
                            ChatoneTextField(
                                value = clipChansText,
                                onValueChange = { v ->
                                    clipChansText = v
                                    val chans = v.lines()
                                        .map { it.trim().lowercase().removePrefix("#") }
                                        .filter { it.isNotBlank() }
                                    onChange(rule.copy(linksClipsAllowedChannels = chans))
                                },
                                label = s.chatRuleLinksClipsAllowedChannels,
                                hint = s.chatRuleLinksClipsAllowedChannelsDesc,
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2,
                                maxLines = 6,
                                textStyle = MaterialTheme.typography.bodySmall,
                                singleLine = false
                            )
                        }
                    }
                    Md3Toggle(s.chatRuleLinksRequireHttps, s.chatRuleLinksRequireHttpsDesc, rule.linksRequireHttps) {
                        onChange(rule.copy(linksRequireHttps = it))
                    }
                    var sitesText by remember(rule.id) {
                        mutableStateOf(rule.linksAllowedSites.joinToString("\n"))
                    }
                    ChatoneTextField(
                        value = sitesText,
                        onValueChange = { v ->
                            sitesText = v
                            val sites = v.lines().map { it.trim().lowercase() }.filter { it.isNotBlank() }
                            onChange(rule.copy(linksAllowedSites = sites))
                        },
                        label = s.chatRuleLinksAllowedSites,
                        hint = s.chatRuleLinksAllowedSitesDesc,
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 6,
                        textStyle = MaterialTheme.typography.bodySmall,
                        singleLine = false
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
                    ChatoneTextField(
                        value = rule.eventMessage,
                        onValueChange = { onChange(rule.copy(eventMessage = it)) },
                        label = s.chatRuleEventMessage,
                        hint = s.chatRuleEventMessageDesc,
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 4,
                        textStyle = MaterialTheme.typography.bodySmall,
                        singleLine = false
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Md3NumberField(s.chatRuleEventRepeat, rule.eventRepeat.toLong(), s.chatRuleEventRepeatDesc, Modifier.weight(1f)) {
                            onChange(rule.copy(eventRepeat = it.toInt().coerceIn(1, 10)))
                        }
                        Md3NumberField(s.chatRuleEventDelay, rule.eventDelaySeconds.toLong(), s.chatRuleEventDelayDesc, Modifier.weight(1f)) {
                            onChange(rule.copy(eventDelaySeconds = it.toInt().coerceIn(0, 600)))
                        }
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
            ExemptionChips(
                rule.exemptMods, rule.exemptVips, rule.exemptSubs,
                s.chatRuleExemptMods, s.chatRuleExemptVips, s.chatRuleExemptSubs,
                { onChange(rule.copy(exemptMods = it)) },
                { onChange(rule.copy(exemptVips = it)) },
                { onChange(rule.copy(exemptSubs = it)) }
            )
        }

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(if (rule.enabled) s.chatRuleEnabled else s.chatRuleDisabled,
                style = MaterialTheme.typography.bodyMedium,
                color = if (rule.enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.weight(1f))
            io.rudione.chatone.presentation.components.ChatoneSwitch(rule.enabled, { onChange(rule.copy(enabled = it)) })
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
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.75f),
                            MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.60f)
                        )
                    )
                )
                .border(
                    1.dp,
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.20f),
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.06f)
                        )
                    ),
                    RoundedCornerShape(16.dp)
                )
        ) {
            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
        if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else Color.Transparent, tween(150))
    val fg by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, tween(150))
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
            ChatoneIconButton(onClick = { onQuery("") }, modifier = Modifier.size(20.dp)) {
                Icon(Icons.Filled.Close, null, modifier = Modifier.size(14.dp))
            }
        }
    }
}

@Composable
private fun Md3TextField(label: String, value: String, placeholder: String, multiline: Boolean = false, onChange: (String) -> Unit) {
    ChatoneTextField(
        value = value,
        onValueChange = onChange,
        label = label,
        placeholder = placeholder,
        modifier = Modifier.fillMaxWidth(),
        singleLine = !multiline,
        textStyle = MaterialTheme.typography.bodyMedium
    )
}

@Composable
private fun Md3NumberField(label: String, value: Long, helper: String? = null, modifier: Modifier = Modifier, onChange: (Long) -> Unit) {
    var text by remember(value) { mutableStateOf(value.toString()) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        ChatoneTextField(
            value = text,
            onValueChange = { t ->
                val f = t.filter { it.isDigit() }
                text = f
                f.toLongOrNull()?.let { onChange(it) }
            },
            label = label,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            textStyle = MaterialTheme.typography.bodyMedium
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
        ChatoneTextField(
            value = displayValue,
            onValueChange = { t ->
                val f = t.filter { it.isDigit() }
                displayValue = f
                f.toIntOrNull()?.let { v -> onChange((v * unit.toSeconds).toInt().coerceAtLeast(1)) }
            },
            label = label,
            modifier = Modifier.weight(1f),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            textStyle = MaterialTheme.typography.bodyMedium
        )
        Box {
            OutlinedButton(
                onClick = { unitMenuOpen = true },
                modifier = Modifier.padding(top = 8.dp),
                shape = MaterialTheme.shapes.small
            ) { Text(unit.label, style = MaterialTheme.typography.labelLarge) }
            ChatoneDropdownMenu(unitMenuOpen, { unitMenuOpen = false }) {
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExemptionChips(
    mods: Boolean, vips: Boolean, subs: Boolean,
    modsLabel: String, vipsLabel: String, subsLabel: String,
    onMods: (Boolean) -> Unit, onVips: (Boolean) -> Unit, onSubs: (Boolean) -> Unit
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ExpressiveCheckChip(modsLabel, mods, onMods)
        ExpressiveCheckChip(vipsLabel, vips, onVips)
        ExpressiveCheckChip(subsLabel, subs, onSubs)
    }
}

@Composable
private fun Md3Toggle(
    label: String,
    desc: String?,
    value: Boolean,
    modifier: Modifier = Modifier,
    guide: String? = null,
    onChange: (Boolean) -> Unit
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                if (!guide.isNullOrBlank()) {
                    io.rudione.chatone.presentation.chat.components.LiquidGlassRichTooltipBox(
                        tooltipContent = {
                            Column(Modifier.widthIn(max = 220.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(label, style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Text(guide, style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    ) {
                        Icon(Icons.Outlined.Info, contentDescription = label,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp))
                    }
                }
            }
            if (!desc.isNullOrBlank())
                Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        io.rudione.chatone.presentation.components.ChatoneSwitch(value, onChange)
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
        Text(io.rudione.chatone.presentation.theme.i18n.LocalStrings.current.automodAlternates, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        alternates.forEachIndexed { idx, alt ->
            Row(Modifier.fillMaxWidth()
                .clip(MaterialTheme.shapes.extraSmall)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text(alt, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                ChatoneIconButton({ onChange(alternates.toMutableList().also { it.removeAt(idx) }) },
                    modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Outlined.Delete, "Remove", tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(14.dp))
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ChatoneTextField(
                value = draft,
                onValueChange = { draft = it },
                placeholder = io.rudione.chatone.presentation.theme.i18n.LocalStrings.current.automodAddVariant,
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall
            )
            FilledTonalButton(onClick = {
                val v = draft.trim()
                if (v.isNotEmpty() && v !in alternates) { onChange(alternates + v); draft = "" }
            }) { Text("Add") }
        }
    }
}

@Composable
private fun chatRuleTypeColor(type: ChatRuleType): Color =
    Color(ChatoneTheme.colorTokens.automodColorFor(type))

@Composable
private fun actionColor(action: AutomodAction): Color = when (action) {
    AutomodAction.DELETE -> MaterialTheme.colorScheme.onSurfaceVariant
    AutomodAction.TIMEOUT -> Color(ChatoneTheme.colorTokens.modTimeout)
    AutomodAction.BAN -> MaterialTheme.colorScheme.error
}
