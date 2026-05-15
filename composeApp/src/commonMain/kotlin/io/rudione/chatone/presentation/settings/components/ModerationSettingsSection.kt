package io.rudione.chatone.presentation.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.zIndex
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.rudione.chatone.domain.model.Macro
import io.rudione.chatone.domain.model.MacroStep
import io.rudione.chatone.domain.model.ModActionButton
import io.rudione.chatone.presentation.automod.DetachedAutomodWindow
import io.rudione.chatone.presentation.settings.SettingsEvent
import io.rudione.chatone.presentation.settings.SettingsState
import io.rudione.chatone.presentation.theme.ChatoneTheme
import io.rudione.chatone.presentation.theme.i18n.AppStrings
import io.rudione.chatone.presentation.theme.i18n.LocalStrings


@Composable
fun ModerationSettingsSection(
    state: SettingsState,
    onEvent: (SettingsEvent) -> Unit,
    blockedUsernames: List<String> = emptyList(),
    isLoadingBlockedUsers: Boolean = false,
    blockedLoadError: String? = null,
    onUnblockUser: (String) -> Unit = {},
    onRefreshBlockedUsers: () -> Unit = {},
) {
    val extra = ChatoneTheme.extraColors
    val s = LocalStrings.current
    var showAutomod by remember { mutableStateOf(false) }

    if (showAutomod) {
        DetachedAutomodWindow(
            currentChannelLogin = null,
            onClose = { showAutomod = false }
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {

        BlockedUsersSection(
            blockedUsernames = blockedUsernames,
            showBlockedMode = state.showBlockedMode,
            isLoadingBlockedUsers = isLoadingBlockedUsers,
            loadError = blockedLoadError,
            onShowBlockedModeChange = { onEvent(SettingsEvent.OnShowBlockedModeChanged(it)) },
            onUnblockUser = onUnblockUser,
            onRefresh = onRefreshBlockedUsers
        )

        SettingsCard(title = s.modLocalAutomod) {
            Text(
                s.modLocalAutomodDesc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 10.dp)
            )
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                    .clickable { showAutomod = true }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Outlined.Build,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    s.modOpenLocalAutomodEditor,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        SettingsCard(title = s.modDefaultTimeoutDuration) {
            val options = listOf(
                60 to "1m", 300 to "5m", 600 to "10m",
                1800 to "30m", 3600 to "1h", 86400 to "1d"
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                options.forEach { (secs, label) ->
                    val active = state.defaultTimeoutDuration == secs
                    FilterChip(
                        selected = active,
                        onClick = { onEvent(SettingsEvent.OnDefaultTimeoutChanged(secs)) },
                        label = { Text(label, fontSize = 12.sp) }
                    )
                }
            }
        }

        ModActionButtonsSection(state = state, onEvent = onEvent)

        MacrosSection(state = state, onEvent = onEvent)
    }
}

@Composable
private fun ModActionButtonsSection(
    state: SettingsState,
    onEvent: (SettingsEvent) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingButton by remember { mutableStateOf<ModActionButton?>(null) }

    var orderedButtons by remember(state.allModButtons) {
        mutableStateOf(state.allModButtons.sortedBy { it.sortOrder })
    }
    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetY by remember { mutableStateOf(0f) }
    LaunchedEffect(state.allModButtons) {
        if (draggedIndex == null) {
            orderedButtons = state.allModButtons.sortedBy { it.sortOrder }
        }
    }
    val s = LocalStrings.current

    SettingsCard(title = s.modModActionButtons) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                s.modDragReorderHint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(s.modPreviewLabel, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(orderedButtons.filter { it.enabled }, key = { it.id }) { btn ->
                    val color = when (btn.id) {
                        "default_delete" -> ChatoneTheme.extraColors.modDelete
                        "default_ban"    -> ChatoneTheme.extraColors.modBan
                        else             -> ChatoneTheme.extraColors.modTimeout
                    }
                    val icon = when (btn.id) {
                        "default_delete" -> "🗑️"
                        "default_ban"    -> "🔨"
                        else             -> "⏱"
                    }
                    ModButtonPreviewChip(
                        icon = icon, label = btn.displayLabel,
                        color = color, isFixed = btn.isDefault,
                        onClick = { if (!btn.isDefault) editingButton = btn }
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

            Text(
                s.modPressDragReorder,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                orderedButtons.forEachIndexed { idx, btn ->
                    val isDragged = draggedIndex == idx
                    val btnColor = when (btn.id) {
                        "default_delete" -> ChatoneTheme.extraColors.modDelete
                        "default_ban"    -> ChatoneTheme.extraColors.modBan
                        else             -> ChatoneTheme.extraColors.modTimeout
                    }
                    val btnIcon = when (btn.id) {
                        "default_delete" -> "🗑️"
                        "default_ban"    -> "🔨"
                        else             -> "⏱"
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .zIndex(if (isDragged) 10f else 0f)
                            .graphicsLayer {
                                translationY = if (isDragged) dragOffsetY else 0f
                                alpha = if (isDragged) 0.85f else 1f
                                scaleX = if (isDragged) 1.02f else 1f
                                scaleY = if (isDragged) 1.02f else 1f
                            }
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isDragged)
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                                else
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            )
                            .border(
                                width = if (isDragged) 1.dp else 0.dp,
                                color = MaterialTheme.colorScheme.primary.copy(
                                    alpha = if (isDragged) 0.5f else 0f
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .pointerInput(btn.id) {
                                detectDragGestures(
                                    onDragStart = {
                                        val curIdx = orderedButtons.indexOfFirst { it.id == btn.id }
                                        draggedIndex = curIdx
                                        dragOffsetY = 0f
                                    },
                                    onDrag = { change, amount ->
                                        change.consume()
                                        dragOffsetY += amount.y
                                        val itemHeightPx = 52.dp.toPx()
                                        val curIdx = orderedButtons.indexOfFirst { it.id == btn.id }
                                        if (curIdx < 0) return@detectDragGestures
                                        val shift = (dragOffsetY / itemHeightPx).toInt()
                                        val target = (curIdx + shift).coerceIn(0, orderedButtons.lastIndex)
                                        if (target != curIdx) {
                                            val list = orderedButtons.toMutableList()
                                            val item = list.removeAt(curIdx)
                                            list.add(target, item)
                                            orderedButtons = list
                                            draggedIndex = target
                                            dragOffsetY -= (target - curIdx) * itemHeightPx
                                        }
                                    },
                                    onDragEnd = {
                                        draggedIndex = null; dragOffsetY = 0f
                                        onEvent(SettingsEvent.OnReorderAllModButtons(orderedButtons))
                                    },
                                    onDragCancel = {
                                        draggedIndex = null; dragOffsetY = 0f
                                        orderedButtons = state.allModButtons.sortedBy { it.sortOrder }
                                    }
                                )
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            Icons.Filled.Menu, null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                        )
                        Text(btnIcon, style = MaterialTheme.typography.bodyMedium)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                btn.displayLabel,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = btnColor
                            )
                            if (!btn.isDefault) {
                                Text(
                                    ModActionButton.formatDuration(btn.durationSeconds),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (!btn.isDefault) {
                            IconButton(
                                onClick = { editingButton = btn },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.Outlined.Edit, null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = { onEvent(SettingsEvent.OnRemoveModButton(btn.id)) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.Outlined.Delete, null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        Switch(
                            checked = btn.enabled,
                            onCheckedChange = { enabled ->
                                val updated = orderedButtons.map {
                                    if (it.id == btn.id) it.copy(enabled = enabled) else it
                                }
                                orderedButtons = updated
                                onEvent(SettingsEvent.OnReorderAllModButtons(updated))
                                when (btn.id) {
                                    "default_delete"  -> onEvent(SettingsEvent.OnShowDefaultDeleteChanged(enabled))
                                    "default_timeout" -> onEvent(SettingsEvent.OnShowDefaultTimeoutChanged(enabled))
                                    "default_ban"     -> onEvent(SettingsEvent.OnShowDefaultBanChanged(enabled))
                                }
                                if (!btn.isDefault) {
                                    onEvent(SettingsEvent.OnUpdateModButton(btn.copy(enabled = enabled)))
                                }
                            },
                            modifier = Modifier.height(28.dp)
                        )
                    }
                }
            }

            val canAdd = state.customModButtons.size < 8
            Button(
                onClick = { showAddDialog = true },
                enabled = canAdd,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 10.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(s.modAddTimeoutButtonCount.replace("{0}", state.customModButtons.size.toString()).replace("{1}", "8"))
            }
        }
    }

    if (showAddDialog) {
        AddModButtonDialog(
            onAdd = { secs, label ->
                onEvent(SettingsEvent.OnAddModButton(secs, label))
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }
    editingButton?.let { btn ->
        AddModButtonDialog(
            initial = btn,
            onAdd = { secs, label ->
                onEvent(SettingsEvent.OnUpdateModButton(btn.copy(durationSeconds = secs, label = label)))
                editingButton = null
            },
            onDismiss = { editingButton = null }
        )
    }
}

@Composable
private fun ModButtonPreviewChip(
    icon: String, label: String, color: Color,
    isFixed: Boolean, onClick: (() -> Unit)? = null
) {
    val bg = if (isFixed) color.copy(alpha = 0.12f) else color.copy(alpha = 0.18f)
    val border = if (isFixed) color.copy(alpha = 0.25f) else color.copy(alpha = 0.5f)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(8.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(icon, fontSize = 14.sp)
        Text(label, style = MaterialTheme.typography.labelSmall, color = color, fontSize = 9.sp)
    }
}

@Composable
private fun ModButtonRow(
    button: ModActionButton,
    onEdit: () -> Unit, onDelete: () -> Unit,
    onMoveUp: () -> Unit, onMoveDown: () -> Unit,
    isFirst: Boolean, isLast: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("⏱", fontSize = 16.sp)
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(button.displayLabel, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(
                "${button.durationSeconds}s = ${ModActionButton.formatDuration(button.durationSeconds)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Row {
            IconButton(onClick = onMoveUp, enabled = !isFirst, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Filled.KeyboardArrowUp, null, modifier = Modifier.size(16.dp))
            }
            IconButton(onClick = onMoveDown, enabled = !isLast, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Filled.KeyboardArrowDown, null, modifier = Modifier.size(16.dp))
            }
            IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Outlined.Edit, null, modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Outlined.Delete, null, modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun AddModButtonDialog(
    initial: ModActionButton? = null,
    onAdd: (Int, String) -> Unit,
    onDismiss: () -> Unit
) {
    var seconds by remember { mutableStateOf(initial?.durationSeconds?.toString() ?: "") }
    var label by remember { mutableStateOf(initial?.label ?: "") }
    val secsInt = seconds.trim().toIntOrNull()

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), tonalElevation = 8.dp) {
            val s = LocalStrings.current
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    if (initial == null) s.modAddTimeoutButton else s.modEditTimeoutButton,
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold
                )

                val presets = listOf(1 to "1s", 10 to "10s", 60 to "1m", 300 to "5m",
                    600 to "10m", 3600 to "1h", 86400 to "1d")
                Text(s.modQuickPresets, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(presets) { (s, l) ->
                        FilterChip(
                            selected = seconds == s.toString(),
                            onClick = { seconds = s.toString(); label = "" },
                            label = { Text(l, fontSize = 11.sp) }
                        )
                    }
                }
                OutlinedTextField(
                    value = seconds,
                    onValueChange = { seconds = it.filter { c -> c.isDigit() } },
                    label = { Text(s.modDurationSeconds) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    trailingIcon = {
                        secsInt?.let {
                            Text(ModActionButton.formatDuration(it),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(end = 8.dp))
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text(s.modCustomLabelOptional) },
                    placeholder = { Text(secsInt?.let { ModActionButton.formatDuration(it) } ?: s.modAuto) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text(s.cancel) }
                    Button(
                        onClick = { secsInt?.let { onAdd(it, label.trim()) } },
                        enabled = secsInt != null && secsInt > 0,
                        modifier = Modifier.weight(1f)
                    ) { Text(s.save) }
                }
            }
        }
    }
}


@Composable
fun MacrosSection(
    state: SettingsState,
    onEvent: (SettingsEvent) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingMacro by remember { mutableStateOf<Macro?>(null) }
    val s = LocalStrings.current

    SettingsCard(title = s.modMacros) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                s.modMacrosDesc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )


            if (state.macros.any { it.pinnedIndex >= 0 }) {
                Text(s.modQuickBar, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    (0..4).forEach { slot ->
                        val macro = state.macros.find { it.pinnedIndex == slot }
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (macro != null) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                )
                                .border(1.dp,
                                    if (macro != null) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                    RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (macro != null) {
                                Text(macro.icon, fontSize = 16.sp)
                            } else {
                                Text("${slot + 1}", style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                            }
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            }

            if (state.macros.isEmpty()) {
                Text(s.modNoMacros,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
            } else {
                state.macros.forEach { macro ->
                    MacroRow(
                        macro = macro,
                        pinnedMacros = state.pinnedMacros,
                        onEdit = { editingMacro = macro },
                        onDelete = { onEvent(SettingsEvent.OnRemoveMacro(macro.id)) },
                        onPin = { slot -> onEvent(SettingsEvent.OnPinMacro(macro.id, slot)) },
                        onUnpin = { onEvent(SettingsEvent.OnPinMacro(macro.id, -1)) }
                    )
                }
            }

            Button(
                onClick = { showAddDialog = true },
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 10.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(s.modCreateMacro)
            }
        }
    }

    if (showAddDialog) {
        MacroNameDialog(
            onConfirm = { name, icon ->
                onEvent(SettingsEvent.OnAddMacro(name, icon))
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }
    editingMacro?.let { macro ->
        MacroEditorDialog(
            macro = macro,
            onSave = { onEvent(SettingsEvent.OnUpdateMacro(it)); editingMacro = null },
            onDismiss = { editingMacro = null }
        )
    }
}

@Composable
private fun MacroRow(
    macro: Macro,
    pinnedMacros: List<Macro>,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onPin: (Int) -> Unit,
    onUnpin: () -> Unit
) {
    var showPinMenu by remember { mutableStateOf(false) }
    val isPinned = macro.pinnedIndex in 0..4
    val s = LocalStrings.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(macro.icon, fontSize = 20.sp, modifier = Modifier.size(28.dp))
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(macro.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(
                s.modStepCount.replace("{0}", macro.steps.size.toString()) + if (isPinned) s.modPinnedSlotSuffix.replace("{0}", (macro.pinnedIndex + 1).toString()) else "",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Box {
            IconButton(
                onClick = { showPinMenu = true },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    if (isPinned) Icons.Filled.Star else Icons.Outlined.Star,
                    contentDescription = s.modPinMacro,
                    modifier = Modifier.size(16.dp),
                    tint = if (isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            DropdownMenu(expanded = showPinMenu, onDismissRequest = { showPinMenu = false }) {
                if (isPinned) {
                    DropdownMenuItem(
                        text = { Text(s.modUnpinFromBar) },
                        onClick = { showPinMenu = false; onUnpin() },
                        leadingIcon = { Icon(Icons.Filled.Close, null, modifier = Modifier.size(16.dp)) }
                    )
                    HorizontalDivider()
                }
                (0..4).forEach { slot ->
                    val slotMacro = pinnedMacros.find { it.pinnedIndex == slot }
                    DropdownMenuItem(
                        text = { Text(s.modSlot.replace("{0}", (slot + 1).toString()) + (slotMacro?.let { s.modSlotName.replace("{0}", it.name) } ?: s.modSlotEmpty)) },
                        onClick = { showPinMenu = false; onPin(slot) },
                        leadingIcon = { Text("${slot + 1}", style = MaterialTheme.typography.labelMedium) }
                    )
                }
            }
        }
        IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Outlined.Edit, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Outlined.Delete, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
        }
    }
}


@Composable
private fun MacroNameDialog(
    onConfirm: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var icon by remember { mutableStateOf("⚡") }
    val emojis = listOf("⚡", "🔥", "❄️", "🎯", "🚀", "🛡️", "⚔️", "🎲", "💫", "🌊", "🎮", "📢")

    val s = LocalStrings.current
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), tonalElevation = 8.dp) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(s.modNewMacro, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text(s.modMacroName) }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(s.modChooseIcon, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(emojis) { emoji ->
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (icon == emoji) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { icon = emoji },
                            contentAlignment = Alignment.Center
                        ) { Text(emoji, fontSize = 18.sp) }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text(s.cancel) }
                    Button(
                        onClick = { if (name.isNotBlank()) onConfirm(name.trim(), icon) },
                        enabled = name.isNotBlank(), modifier = Modifier.weight(1f)
                    ) { Text(s.modCreate) }
                }
            }
        }
    }
}


@Composable
fun MacroEditorDialog(
    macro: Macro,
    onSave: (Macro) -> Unit,
    onDismiss: () -> Unit
) {
    var steps by remember { mutableStateOf(macro.steps.toMutableList()) }
    var showAddStep by remember { mutableStateOf(false) }
    var editingStepIndex by remember { mutableStateOf<Int?>(null) }
    var macroName by remember { mutableStateOf(macro.name) }
    var macroIcon by remember { mutableStateOf(macro.icon) }
    val s = LocalStrings.current

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.88f)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                Row(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(macroIcon, fontSize = 24.sp)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(s.modEditMacro, style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(macroName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, null) }
                }


                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = macroIcon,
                        onValueChange = { if (it.length <= 2) macroIcon = it },
                        label = { Text(s.modIcon) },
                        modifier = Modifier.width(72.dp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = macroName,
                        onValueChange = { macroName = it },
                        label = { Text(s.modMacroName) },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                Spacer(Modifier.height(8.dp))
                HorizontalDivider()


                LazyColumn(
                    modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    if (steps.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(s.modNoStepsYet,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                            }
                        }
                    }
                    itemsIndexed(steps) { idx, step ->
                        MacroStepRow(
                            step = step,
                            index = idx,
                            onDelete = { steps = (steps - step).toMutableList() },
                            onEdit = { editingStepIndex = idx },
                            onMoveUp = {
                                if (idx > 0) {
                                    val list = steps.toMutableList()
                                    val item = list.removeAt(idx)
                                    list.add(idx - 1, item)
                                    steps = list
                                }
                            },
                            onMoveDown = {
                                if (idx < steps.lastIndex) {
                                    val list = steps.toMutableList()
                                    val item = list.removeAt(idx)
                                    list.add(idx + 1, item)
                                    steps = list
                                }
                            },
                            isFirst = idx == 0,
                            isLast = idx == steps.lastIndex
                        )
                    }
                    item { Spacer(Modifier.height(8.dp)) }
                }

                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showAddStep = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Add, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(s.modAddStep)
                    }
                    Button(
                        onClick = { onSave(macro.copy(name = macroName, icon = macroIcon, steps = steps)) },
                        modifier = Modifier.weight(1f)
                    ) { Text(s.modSaveMacro) }
                }
            }
        }
    }


    if (showAddStep) {
        AddMacroStepDialog(
            initialStep = null,
            onAdd = { step ->
                steps = (steps + step).toMutableList()
                showAddStep = false
            },
            onDismiss = { showAddStep = false }
        )
    }


    editingStepIndex?.let { idx ->
        val stepToEdit = steps.getOrNull(idx)
        if (stepToEdit != null) {
            AddMacroStepDialog(
                initialStep = stepToEdit,
                onAdd = { updatedStep ->
                    val list = steps.toMutableList()
                    list[idx] = updatedStep
                    steps = list
                    editingStepIndex = null
                },
                onDismiss = { editingStepIndex = null }
            )
        }
    }
}

@Composable
private fun MacroStepRow(
    step: MacroStep, index: Int,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    isFirst: Boolean,
    isLast: Boolean
) {
    val s = LocalStrings.current
    val (icon, description) = stepDescription(step, s)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Box(
            modifier = Modifier.size(28.dp).clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Text("${index + 1}", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(8.dp))
        Text(icon, fontSize = 16.sp)
        Spacer(Modifier.width(8.dp))
        Text(description, style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f), maxLines = 2)
        Row {

            IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Outlined.Edit, null, modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f))
            }
            IconButton(onClick = onMoveUp, enabled = !isFirst, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Filled.KeyboardArrowUp, null, modifier = Modifier.size(14.dp))
            }
            IconButton(onClick = onMoveDown, enabled = !isLast, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Filled.KeyboardArrowDown, null, modifier = Modifier.size(14.dp))
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Outlined.Delete, null, modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
            }
        }
    }
}

private fun stepDescription(step: MacroStep, s: AppStrings): Pair<String, String> = when (step) {
    is MacroStep.SendMessage -> "💬" to if (step.repeatCount > 1)
        s.modStepDescSendMulti.replace("{0}", step.repeatCount.toString()).replace("{1}", step.text)
    else s.modStepDescSend.replace("{0}", step.text)
    is MacroStep.InsertText -> "✏️" to if (step.repeatCount > 1)
        s.modStepDescInsertMulti.replace("{0}", step.repeatCount.toString()).replace("{1}", step.text)
    else s.modStepDescInsert.replace("{0}", step.text)
    is MacroStep.SubMode -> "⭐" to if (step.enable) s.modStepDescSubEnable else s.modStepDescSubDisable
    is MacroStep.EmoteMode -> "😊" to if (step.enable) s.modStepDescEmoteEnable else s.modStepDescEmoteDisable
    is MacroStep.SlowMode -> "🐢" to if (step.enable)
        s.modStepDescSlowEnable.replace("{0}", step.seconds.toString()) else s.modStepDescSlowDisable
    is MacroStep.FollowerMode -> "❤️" to if (step.enable)
        s.modStepDescFollowEnable.replace("{0}", step.minutes.toString()) else s.modStepDescFollowDisable
    is MacroStep.R9KMode -> "🔒" to if (step.enable) s.modStepDescR9kEnable else s.modStepDescR9kDisable
    is MacroStep.StartRaid -> "🚀" to s.modStepDescRaid.replace("{0}", step.targetLogin)
    is MacroStep.PinMessage -> "📌" to s.modStepDescPin.replace("{0}", step.message)
    is MacroStep.Delay -> "⏳" to s.modStepDescDelay.replace("{0}", step.seconds.toString())
    is MacroStep.ClearChat -> "🗑️" to s.modStepDescClear
}


@Composable
private fun AddMacroStepDialog(
    initialStep: MacroStep?,
    onAdd: (MacroStep) -> Unit,
    onDismiss: () -> Unit
) {

    fun typeOf(s: MacroStep?) = when (s) {
        is MacroStep.SendMessage -> "send"
        is MacroStep.InsertText -> "insert"
        is MacroStep.SubMode -> "sub"
        is MacroStep.EmoteMode -> "emote"
        is MacroStep.SlowMode -> "slow"
        is MacroStep.FollowerMode -> "followers"
        is MacroStep.R9KMode -> "r9k"
        is MacroStep.StartRaid -> "raid"
        is MacroStep.PinMessage -> "pin"
        is MacroStep.Delay -> "delay"
        is MacroStep.ClearChat -> "clear"
        null -> null
    }

    var selected by remember { mutableStateOf(typeOf(initialStep)) }
    var messageText by remember {
        mutableStateOf(
            when (initialStep) {
                is MacroStep.SendMessage -> initialStep.text
                is MacroStep.InsertText -> initialStep.text
                else -> ""
            }
        )
    }
    var delaySeconds by remember {
        mutableStateOf(if (initialStep is MacroStep.Delay) "${initialStep.seconds}" else "5")
    }
    var slowSeconds by remember {
        mutableStateOf(if (initialStep is MacroStep.SlowMode) "${initialStep.seconds}" else "30")
    }
    var followerMinutes by remember {
        mutableStateOf(if (initialStep is MacroStep.FollowerMode) "${initialStep.minutes}" else "10")
    }
    var raidTarget by remember {
        mutableStateOf(if (initialStep is MacroStep.StartRaid) initialStep.targetLogin else "")
    }
    var pinMessage by remember {
        mutableStateOf(if (initialStep is MacroStep.PinMessage) initialStep.message else "")
    }
    var boolState by remember {
        mutableStateOf(
            when (initialStep) {
                is MacroStep.SubMode -> initialStep.enable
                is MacroStep.EmoteMode -> initialStep.enable
                is MacroStep.SlowMode -> initialStep.enable
                is MacroStep.FollowerMode -> initialStep.enable
                is MacroStep.R9KMode -> initialStep.enable
                else -> true
            }
        )
    }

    var repeatCount by remember {
        mutableStateOf(when (initialStep) {
            is MacroStep.SendMessage -> "${initialStep.repeatCount}"
            is MacroStep.InsertText  -> "${initialStep.repeatCount}"
            is MacroStep.Delay       -> "${initialStep.repeatCount}"
            else -> "1"
        })
    }

    val s = LocalStrings.current
    val isEditMode = initialStep != null
    val title = if (isEditMode) s.modEditStep else s.modAddStep
    val confirmLabel = if (isEditMode) s.modSaveChanges else s.modAddStep

    val stepTypes = listOf(
        "send" to s.modStepTypeSend,
        "insert" to s.modStepTypeInsert,
        "sub" to s.modStepTypeSub,
        "emote" to s.modStepTypeEmote,
        "slow" to s.modStepTypeSlow,
        "followers" to s.modStepTypeFollowers,
        "r9k" to s.modStepTypeR9k,
        "raid" to s.modStepTypeRaid,
        "pin" to s.modStepTypePin,
        "delay" to s.modStepTypeDelay,
        "clear" to s.modStepTypeClear
    )

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(shape = RoundedCornerShape(20.dp), tonalElevation = 8.dp,
            modifier = Modifier.fillMaxWidth(0.92f).fillMaxHeight(0.85f)
        ) {
            Row(modifier = Modifier.fillMaxSize()) {


                Column(
                    modifier = Modifier
                        .width(200.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .padding(top = 20.dp, bottom = 12.dp)
                ) {
                    Text(title, style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                    Spacer(Modifier.height(8.dp))
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(stepTypes) { (key, label) ->
                            val isSelected = selected == key
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                                        else Color.Transparent
                                    )
                                    .clickable { selected = key }
                                    .padding(horizontal = 10.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(label, style = MaterialTheme.typography.bodySmall,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    modifier = Modifier.weight(1f))
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary)
                                    )
                                }
                            }
                        }
                    }
                }


                VerticalDivider()


                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    Column(
                        modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (selected == null) {
                            Text(s.modSelectActionType,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                        }

                        when (selected) {
                            "send" -> OutlinedTextField(
                                value = messageText, onValueChange = { messageText = it },
                                label = { Text(s.modMessageText) }, singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            "insert" -> OutlinedTextField(
                                value = messageText, onValueChange = { messageText = it },
                                label = { Text(s.modTextToInsert) }, singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            "sub", "emote", "r9k" -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(s.modAction, style = MaterialTheme.typography.labelMedium)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilterChip(selected = boolState, onClick = { boolState = true }, label = { Text(s.modEnable) })
                                    FilterChip(selected = !boolState, onClick = { boolState = false }, label = { Text(s.modDisable) })
                                }
                            }
                            "slow" -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(s.modAction, style = MaterialTheme.typography.labelMedium)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilterChip(selected = boolState, onClick = { boolState = true }, label = { Text(s.modEnable) })
                                    FilterChip(selected = !boolState, onClick = { boolState = false }, label = { Text(s.modDisable) })
                                }
                                if (boolState) OutlinedTextField(
                                    value = slowSeconds,
                                    onValueChange = { slowSeconds = it.filter { c -> c.isDigit() } },
                                    label = { Text(s.modSlowModeSeconds) }, singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            "followers" -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(s.modAction, style = MaterialTheme.typography.labelMedium)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilterChip(selected = boolState, onClick = { boolState = true }, label = { Text(s.modEnable) })
                                    FilterChip(selected = !boolState, onClick = { boolState = false }, label = { Text(s.modDisable) })
                                }
                                if (boolState) OutlinedTextField(
                                    value = followerMinutes,
                                    onValueChange = { followerMinutes = it.filter { c -> c.isDigit() } },
                                    label = { Text(s.modDurationMinutes) }, singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            "raid" -> OutlinedTextField(
                                value = raidTarget, onValueChange = { raidTarget = it },
                                label = { Text(s.modChannelToRaid) }, singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            "pin" -> OutlinedTextField(
                                value = pinMessage, onValueChange = { pinMessage = it },
                                label = { Text(s.modMessageToPin) }, singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            "delay" -> OutlinedTextField(
                                value = delaySeconds,
                                onValueChange = { delaySeconds = it.filter { c -> c.isDigit() } },
                                label = { Text(s.modWaitSeconds) }, singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                            "clear" -> Text(
                                s.modClearWarning,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }


                        if (selected in listOf("send", "insert", "delay")) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(s.modRepeat, style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.width(60.dp))
                                OutlinedTextField(
                                    value = repeatCount,
                                    onValueChange = { v ->
                                        val n = v.filter { it.isDigit() }
                                        repeatCount = if (n.isEmpty()) "1" else n
                                    },
                                    singleLine = true,
                                    modifier = Modifier.width(80.dp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                                Text(s.modRepeatTimes, style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }


                    val canConfirm = when (selected) {
                        "send" -> messageText.isNotBlank()
                        "insert" -> messageText.isNotBlank()
                        "raid" -> raidTarget.isNotBlank()
                        "pin" -> pinMessage.isNotBlank()
                        "delay" -> delaySeconds.toIntOrNull() != null && delaySeconds.toInt() > 0
                        "slow" -> !boolState || (slowSeconds.toIntOrNull() != null && slowSeconds.toInt() > 0)
                        "followers" -> !boolState || (followerMinutes.toIntOrNull() != null)
                        null -> false
                        else -> true
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                            Text(s.cancel)
                        }
                        Button(
                            onClick = {
                                val count = repeatCount.toIntOrNull()?.coerceAtLeast(1) ?: 1
                                val baseStep = buildStep(
                                    selected, messageText, boolState,
                                    slowSeconds, followerMinutes, raidTarget, pinMessage, delaySeconds, count
                                )
                                if (baseStep != null) {
                                    onAdd(baseStep)
                                }
                            },
                            enabled = canConfirm && selected != null,
                            modifier = Modifier.weight(1f)
                        ) { Text(confirmLabel) }
                    }
                }
            }
        }
    }
}

private fun buildStep(
    type: String?, msg: String, bool: Boolean,
    slow: String, follow: String, raid: String, pin: String, delay: String,
    repeatCount: Int = 1
): MacroStep? = when (type) {
    "send" -> if (msg.isNotBlank()) MacroStep.SendMessage(msg, repeatCount.coerceAtLeast(1)) else null
    "insert" -> if (msg.isNotBlank()) MacroStep.InsertText(msg, repeatCount.coerceAtLeast(1)) else null
    "sub" -> MacroStep.SubMode(bool)
    "emote" -> MacroStep.EmoteMode(bool)
    "slow" -> MacroStep.SlowMode(bool, slow.toIntOrNull() ?: 30)
    "followers" -> MacroStep.FollowerMode(bool, follow.toIntOrNull() ?: 10)
    "r9k" -> MacroStep.R9KMode(bool)
    "raid" -> if (raid.isNotBlank()) MacroStep.StartRaid(raid.trim()) else null
    "pin" -> if (pin.isNotBlank()) MacroStep.PinMessage(pin) else null
    "delay" -> delay.toIntOrNull()?.takeIf { it > 0 }?.let { MacroStep.Delay(it, repeatCount.coerceAtLeast(1)) }
    "clear" -> MacroStep.ClearChat()
    else -> null
}


@Composable
fun SettingsCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary)
            content()
        }
    }
}