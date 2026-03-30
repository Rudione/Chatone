package io.rudione.chatone.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.rudione.chatone.domain.model.HighlightRule
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onThemeChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Appearance section
            item {
                SectionHeader("Appearance")
            }
            item {
                SwitchPreference(
                    title = "Dark Theme",
                    subtitle = "Use dark color scheme",
                    checked = state.darkTheme,
                    onCheckedChange = {
                        viewModel.sendEvent(SettingsEvent.OnDarkThemeChanged(it))
                        onThemeChanged(it)
                    }
                )
            }
            item {
                ListPreference(
                    title = "Font Size",
                    value = state.fontSize.name.lowercase().replaceFirstChar { it.uppercase() },
                    options = SettingsState.FontSize.entries.map { it.name.lowercase().replaceFirstChar { c -> c.uppercase() } },
                    onSelected = { index ->
                        viewModel.sendEvent(SettingsEvent.OnFontSizeChanged(SettingsState.FontSize.entries[index]))
                    }
                )
            }
            item {
                ListPreference(
                    title = "Emote Size",
                    value = state.emoteSize.name.lowercase().replaceFirstChar { it.uppercase() },
                    options = SettingsState.EmoteSize.entries.map { it.name.lowercase().replaceFirstChar { c -> c.uppercase() } },
                    onSelected = { index ->
                        viewModel.sendEvent(SettingsEvent.OnEmoteSizeChanged(SettingsState.EmoteSize.entries[index]))
                    }
                )
            }

            item {
                ListPreference(
                    title = "Channel Navigation",
                    value = when (state.channelNavigation) {
                        SettingsState.ChannelNavigation.TAB_BAR -> "Tab Bar"
                        SettingsState.ChannelNavigation.MINI_RAIL -> "Mini Rail"
                        SettingsState.ChannelNavigation.BOTH -> "Both"
                    },
                    options = listOf("Tab Bar", "Mini Rail", "Both"),
                    onSelected = { index ->
                        viewModel.sendEvent(
                            SettingsEvent.OnChannelNavigationChanged(
                                SettingsState.ChannelNavigation.entries[index]
                            )
                        )
                    }
                )
            }

            // Window section (desktop only)
            item {
                SectionHeader("Window")
            }
            item {
                SwitchPreference(
                    title = "Always on Top",
                    subtitle = "Keep window above other windows",
                    checked = state.alwaysOnTop,
                    onCheckedChange = {
                        viewModel.sendEvent(SettingsEvent.OnAlwaysOnTopChanged(it))
                    }
                )
            }

            // Chat section
            item {
                SectionHeader("Chat")
            }
            item {
                SwitchPreference(
                    title = "Show Timestamps",
                    subtitle = "Display message time in chat",
                    checked = state.timestampFormat != SettingsState.TimestampFormat.OFF,
                    onCheckedChange = { enabled ->
                        viewModel.sendEvent(
                            SettingsEvent.OnTimestampFormatChanged(
                                if (enabled) SettingsState.TimestampFormat.H24
                                else SettingsState.TimestampFormat.OFF
                            )
                        )
                    }
                )
            }
            if (state.timestampFormat != SettingsState.TimestampFormat.OFF) {
                item {
                    ListPreference(
                        title = "Timestamp Format",
                        value = when (state.timestampFormat) {
                            SettingsState.TimestampFormat.H12 -> "12-hour"
                            SettingsState.TimestampFormat.H24 -> "24-hour"
                            SettingsState.TimestampFormat.OFF -> "Off"
                        },
                        options = listOf("12-hour", "24-hour"),
                        onSelected = { index ->
                            viewModel.sendEvent(
                                SettingsEvent.OnTimestampFormatChanged(
                                    if (index == 0) SettingsState.TimestampFormat.H12
                                    else SettingsState.TimestampFormat.H24
                                )
                            )
                        }
                    )
                }
            }
            item {
                SwitchPreference(
                    title = "Show Badges",
                    subtitle = "Display user badges in chat",
                    checked = state.showBadges,
                    onCheckedChange = {
                        viewModel.sendEvent(SettingsEvent.OnShowBadgesChanged(it))
                    }
                )
            }
            item {
                SwitchPreference(
                    title = "Show Deleted Messages",
                    subtitle = "Show deleted messages as grayed out",
                    checked = state.showDeletedMessages,
                    onCheckedChange = {
                        viewModel.sendEvent(SettingsEvent.OnShowDeletedChanged(it))
                    }
                )
            }
            item {
                SliderPreference(
                    title = "Message History Limit",
                    value = state.scrollbackLimit,
                    valueRange = 100f..2000f,
                    steps = 18,
                    valueLabel = "${state.scrollbackLimit} messages",
                    onValueChange = {
                        viewModel.sendEvent(SettingsEvent.OnScrollbackLimitChanged(it.toInt()))
                    }
                )
            }

            // Notifications section
            item {
                SectionHeader("Notifications & Highlights")
            }
            item {
                SwitchPreference(
                    title = "Mention Sound",
                    subtitle = "Play sound when you are mentioned",
                    checked = state.mentionSoundEnabled,
                    onCheckedChange = {
                        viewModel.sendEvent(SettingsEvent.OnMentionSoundChanged(it))
                    }
                )
            }
            if (state.mentionSoundEnabled) {
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                        Text(
                            text = "Volume: ${(state.mentionSoundVolume * 100).toInt()}%",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Slider(
                            value = state.mentionSoundVolume,
                            onValueChange = {
                                viewModel.sendEvent(SettingsEvent.OnMentionSoundVolumeChanged(it))
                            },
                            valueRange = 0f..1f,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                        Text(
                            text = "Custom Sound (MP3/WAV path)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(4.dp))
                        OutlinedTextField(
                            value = state.customMentionSoundPath,
                            onValueChange = {
                                viewModel.sendEvent(SettingsEvent.OnCustomMentionSoundPathChanged(it))
                            },
                            placeholder = {
                                Text(
                                    "Leave empty for default tone",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            items(state.highlightRules) { rule ->
                HighlightRuleItem(
                    rule = rule,
                    onToggle = { viewModel.sendEvent(SettingsEvent.OnHighlightRuleToggled(rule.id, it)) },
                    onSoundToggle = { viewModel.sendEvent(SettingsEvent.OnHighlightRuleSoundToggled(rule.id, it)) },
                    onRemove = if (!rule.id.startsWith("custom_")) null
                    else {{ viewModel.sendEvent(SettingsEvent.OnRemoveHighlightRule(rule.id)) }}
                )
            }
            item {
                var newPattern by remember { mutableStateOf("") }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newPattern,
                        onValueChange = { newPattern = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Add highlight pattern...") },
                        singleLine = true
                    )
                    Spacer(Modifier.width(8.dp))
                    FilledTonalButton(
                        onClick = {
                            if (newPattern.isNotBlank()) {
                                viewModel.sendEvent(SettingsEvent.OnAddHighlightRule(newPattern.trim()))
                                newPattern = ""
                            }
                        },
                        enabled = newPattern.isNotBlank()
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Add", modifier = Modifier.size(18.dp))
                    }
                }
            }

            // Moderation section
            item {
                SectionHeader("Moderation")
            }
            item {
                SwitchPreference(
                    title = "Confirm Mod Actions",
                    subtitle = "Show confirmation dialog before ban/timeout",
                    checked = state.confirmModActions,
                    onCheckedChange = {
                        viewModel.sendEvent(SettingsEvent.OnConfirmModActionsChanged(it))
                    }
                )
            }
            item {
                ListPreference(
                    title = "Default Timeout Duration",
                    value = formatDuration(state.defaultTimeoutDuration),
                    options = listOf("10 seconds", "1 minute", "10 minutes", "1 hour", "1 day"),
                    onSelected = { index ->
                        val durations = listOf(10, 60, 600, 3600, 86400)
                        viewModel.sendEvent(SettingsEvent.OnDefaultTimeoutChanged(durations[index]))
                    }
                )
            }

            // About section
            item {
                SectionHeader("About")
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Chatone",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Version 1.0.3",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "TG",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "https://t.me/rudionee",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 12.dp)
    )
}

@Composable
private fun SwitchPreference(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun ListPreference(
    title: String,
    value: String,
    options: List<String>,
    onSelected: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true }
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEachIndexed { index, option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelected(index)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun SliderPreference(
    title: String,
    value: Int,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    valueLabel: String,
    onValueChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = valueLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Slider(
            value = value.toFloat(),
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps
        )
    }
}

@Composable
private fun HighlightRuleItem(
    rule: HighlightRule,
    onToggle: (Boolean) -> Unit,
    onSoundToggle: (Boolean) -> Unit,
    onRemove: (() -> Unit)?
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Color indicator
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(Color(rule.color))
            )
            Spacer(Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = rule.pattern.ifEmpty { rule.id.replace("_", " ").replaceFirstChar { it.uppercase() } },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                if (rule.isRegex) {
                    Text(
                        text = "Regex",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Sound toggle
            IconButton(
                onClick = { onSoundToggle(!rule.playSound) },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    if (rule.playSound) Icons.Filled.Notifications else Icons.Outlined.Notifications,
                    contentDescription = "Toggle sound",
                    modifier = Modifier.size(18.dp),
                    tint = if (rule.playSound) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Enable/disable toggle
            Switch(
                checked = rule.enabled,
                onCheckedChange = onToggle,
                modifier = Modifier.padding(start = 4.dp)
            )

            // Remove button for custom rules
            if (onRemove != null) {
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Remove",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

private fun formatDuration(seconds: Int): String {
    return when {
        seconds < 60 -> "$seconds seconds"
        seconds < 3600 -> "${seconds / 60} minute${if (seconds / 60 > 1) "s" else ""}"
        seconds < 86400 -> "${seconds / 3600} hour${if (seconds / 3600 > 1) "s" else ""}"
        else -> "${seconds / 86400} day${if (seconds / 86400 > 1) "s" else ""}"
    }
}
