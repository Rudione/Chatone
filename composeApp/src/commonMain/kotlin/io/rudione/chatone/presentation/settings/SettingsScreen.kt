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
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import io.rudione.chatone.domain.model.HighlightRule
import io.rudione.chatone.presentation.theme.ChatBackgroundLayer
import io.rudione.chatone.presentation.theme.LocalWallpaperController
import io.rudione.chatone.util.NotificationSoundPlayer
import io.rudione.chatone.util.WallpaperLoader
import io.rudione.chatone.util.pickAudioFile
import io.rudione.chatone.util.pickImageFile
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onThemeChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    wallpaperLoader: WallpaperLoader = koinInject(),
    viewModel: SettingsViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()

    // Берём текущий wallpaper state
    val wallpaperController = LocalWallpaperController.current
    val wallpaper by remember { derivedStateOf { wallpaperController.state } }

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
        // Фон с обоями
        ChatBackgroundLayer(
            wallpaper = wallpaper,
            darkTheme = state.darkTheme,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // ── Appearance ───────────────────────────────────────────
                item { SectionHeader("Appearance") }
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
                        options = SettingsState.FontSize.entries.map {
                            it.name.lowercase().replaceFirstChar { c -> c.uppercase() }
                        },
                        onSelected = { viewModel.sendEvent(SettingsEvent.OnFontSizeChanged(SettingsState.FontSize.entries[it])) }
                    )
                }
                item {
                    ListPreference(
                        title = "Emote Size",
                        value = state.emoteSize.name.lowercase().replaceFirstChar { it.uppercase() },
                        options = SettingsState.EmoteSize.entries.map {
                            it.name.lowercase().replaceFirstChar { c -> c.uppercase() }
                        },
                        onSelected = { viewModel.sendEvent(SettingsEvent.OnEmoteSizeChanged(SettingsState.EmoteSize.entries[it])) }
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
                        onSelected = {
                            viewModel.sendEvent(
                                SettingsEvent.OnChannelNavigationChanged(
                                    SettingsState.ChannelNavigation.entries[it]
                                )
                            )
                        }
                    )
                }

                // ── Window ───────────────────────────────────────────────
                item { SectionHeader("Window") }
                item {
                    SwitchPreference(
                        title = "Always on Top",
                        subtitle = "Keep window above other windows",
                        checked = state.alwaysOnTop,
                        onCheckedChange = { viewModel.sendEvent(SettingsEvent.OnAlwaysOnTopChanged(it)) }
                    )
                }

                // ── Chat ─────────────────────────────────────────────────
                item { SectionHeader("Chat") }
                item {
                    SwitchPreference(
                        title = "Show Timestamps",
                        subtitle = "Display message time in chat",
                        checked = state.timestampFormat != SettingsState.TimestampFormat.OFF,
                        onCheckedChange = { enabled ->
                            viewModel.sendEvent(
                                SettingsEvent.OnTimestampFormatChanged(
                                    if (enabled) SettingsState.TimestampFormat.H24 else SettingsState.TimestampFormat.OFF
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
                            onSelected = {
                                viewModel.sendEvent(
                                    SettingsEvent.OnTimestampFormatChanged(
                                        if (it == 0) SettingsState.TimestampFormat.H12 else SettingsState.TimestampFormat.H24
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
                        onCheckedChange = { viewModel.sendEvent(SettingsEvent.OnShowBadgesChanged(it)) }
                    )
                }

                // ── Wallpaper ─────────────────────────────────────────────
                item { SectionHeader("Chat Background") }
                item {
                    WallpaperPreference(
                        currentPath = state.wallpaperPath,
                        blurRadius = state.wallpaperBlur,
                        onPathChanged = { viewModel.sendEvent(SettingsEvent.OnWallpaperPathChanged(it)) },
                        onBlurChanged = { viewModel.sendEvent(SettingsEvent.OnWallpaperBlurChanged(it)) }
                    )
                }

                // ── About ─────────────────────────────────────────────────
                item { SectionHeader("About") }
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Chatone", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Version 1.0.5", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("https://t.me/rudionee", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(32.dp)) }
            }
        }
    }
}

// ─── Hotkey Recorder ────────────────────────────────────────────────────

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun HotkeyPreference(
    title: String,
    subtitle: String,
    currentHotkey: String,
    onHotkeyChanged: (String) -> Unit
) {
    var isRecording by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            // Current hotkey display / record button
            Surface(
                color = if (isRecording) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .weight(1f)
                    .onKeyEvent { event ->
                        if (!isRecording) return@onKeyEvent false
                        if (event.type != KeyEventType.KeyDown) return@onKeyEvent true

                        // Ignore lone modifier keys
                        if (event.key == Key.CtrlLeft || event.key == Key.CtrlRight ||
                            event.key == Key.AltLeft || event.key == Key.AltRight ||
                            event.key == Key.ShiftLeft || event.key == Key.ShiftRight ||
                            event.key == Key.MetaLeft || event.key == Key.MetaRight) {
                            return@onKeyEvent true
                        }

                        // Escape = clear hotkey
                        if (event.key == Key.Escape) {
                            onHotkeyChanged("")
                            isRecording = false
                            return@onKeyEvent true
                        }

                        // Build hotkey string
                        val parts = mutableListOf<String>()
                        if (event.isCtrlPressed || event.isMetaPressed) parts.add("ctrl")
                        if (event.isAltPressed) parts.add("alt")
                        if (event.isShiftPressed) parts.add("shift")

                        val keyName = keyToName(event.key)
                        if (keyName.isNotEmpty()) parts.add(keyName)

                        val hotkey = parts.joinToString("+")
                        onHotkeyChanged(hotkey)
                        isRecording = false
                        true
                    }
                    .clickable { isRecording = true }
            ) {
                Text(
                    text = when {
                        isRecording -> "Press a key combo..."
                        currentHotkey.isBlank() -> "Not set — click to record"
                        else -> currentHotkey.uppercase().replace("+", " + ")
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = when {
                        isRecording -> MaterialTheme.colorScheme.onPrimaryContainer
                        currentHotkey.isBlank() -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        else -> MaterialTheme.colorScheme.onSurface
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                )
            }

            // Clear button
            if (currentHotkey.isNotBlank() || isRecording) {
                IconButton(
                    onClick = { onHotkeyChanged(""); isRecording = false },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Clear hotkey", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
        if (isRecording) {
            Spacer(Modifier.height(4.dp))
            Text("Press Escape to clear, or any key combo to set", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun keyToName(key: Key): String = when (key) {
    Key.Spacebar -> "space"
    Key.Enter -> "enter"
    Key.Tab -> "tab"
    Key.Backspace -> "backspace"
    Key.Delete -> "delete"
    Key.MoveHome -> "home"
    Key.MoveEnd -> "end"
    Key.PageUp -> "pageup"
    Key.PageDown -> "pagedown"
    Key.DirectionUp -> "up"
    Key.DirectionDown -> "down"
    Key.DirectionLeft -> "left"
    Key.DirectionRight -> "right"
    Key.F1 -> "f1"; Key.F2 -> "f2"; Key.F3 -> "f3"; Key.F4 -> "f4"
    Key.F5 -> "f5"; Key.F6 -> "f6"; Key.F7 -> "f7"; Key.F8 -> "f8"
    Key.F9 -> "f9"; Key.F10 -> "f10"; Key.F11 -> "f11"; Key.F12 -> "f12"
    else -> {
        // Try to get a readable name from keyCode (letters/digits)
        val code = key.keyCode
        when {
            code in 65L..90L -> ('A' + (code - 65).toInt()).lowercaseChar().toString()  // A-Z
            code in 48L..57L -> (code - 48).toString()  // 0-9
            else -> ""
        }
    }
}

// ─── Shared composables ──────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String) {
    Text(text = title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(vertical = 12.dp))
}

@Composable
private fun SwitchPreference(title: String, subtitle: String? = null, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ListPreference(title: String, value: String, options: List<String>, onSelected: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Row(modifier = Modifier.fillMaxWidth().clickable { expanded = true }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.bodyLarge)
                Text(text = value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEachIndexed { index, option -> DropdownMenuItem(text = { Text(option) }, onClick = { onSelected(index); expanded = false }) }
        }
    }
}

@Composable
private fun SliderPreference(title: String, value: Int, valueRange: ClosedFloatingPointRange<Float>, steps: Int, valueLabel: String, onValueChange: (Float) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(text = valueLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Slider(value = value.toFloat(), onValueChange = onValueChange, valueRange = valueRange, steps = steps)
    }
}

@Composable
private fun HighlightRuleItem(rule: HighlightRule, onToggle: (Boolean) -> Unit, onSoundToggle: (Boolean) -> Unit, onRemove: (() -> Unit)?) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(Color(rule.color)))
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = rule.pattern.ifEmpty { rule.id.replace("_", " ").replaceFirstChar { it.uppercase() } }, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                if (rule.isRegex) Text(text = "Regex", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = { onSoundToggle(!rule.playSound) }, modifier = Modifier.size(32.dp)) {
                Icon(
                    if (rule.playSound) Icons.Filled.Notifications else Icons.Outlined.Notifications,
                    contentDescription = "Toggle sound", modifier = Modifier.size(18.dp),
                    tint = if (rule.playSound) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = rule.enabled, onCheckedChange = onToggle, modifier = Modifier.padding(start = 4.dp))
            if (onRemove != null) {
                IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Filled.Close, contentDescription = "Remove", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

private fun formatDuration(seconds: Int): String = when {
    seconds < 60 -> "$seconds seconds"
    seconds < 3600 -> "${seconds / 60} minute${if (seconds / 60 > 1) "s" else ""}"
    seconds < 86400 -> "${seconds / 3600} hour${if (seconds / 3600 > 1) "s" else ""}"
    else -> "${seconds / 86400} day${if (seconds / 86400 > 1) "s" else ""}"
}

// ─── Wallpaper Preference ────────────────────────────────────────────────

@Composable
private fun WallpaperPreference(
    currentPath: String,
    blurRadius: Float,
    onPathChanged: (String) -> Unit,
    onBlurChanged: (Float) -> Unit
) {
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxWidth()) {
        // Current file row
        if (currentPath.isNotBlank()) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = currentPath.substringAfterLast("/").substringAfterLast("\\"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    // Delete button — immediately clears wallpaper
                    IconButton(
                        onClick = { onPathChanged("") },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Remove background",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))

            // Overlay blur slider
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Chat Overlay Blur",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "${blurRadius.toInt()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Slider(
                    value = blurRadius,
                    onValueChange = onBlurChanged,
                    valueRange = 0f..40f,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Transparent", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Opaque", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        // Browse button
        OutlinedButton(
            onClick = {
                scope.launch {
                    val picked = pickImageFile()
                    if (picked != null) onPathChanged(picked)
                }
            }
        ) {
            Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (currentPath.isBlank()) "Choose background image..." else "Change image...",
                style = MaterialTheme.typography.labelMedium
            )
        }

        if (currentPath.isBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = "No background set. Supports JPG, PNG, WebP.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}