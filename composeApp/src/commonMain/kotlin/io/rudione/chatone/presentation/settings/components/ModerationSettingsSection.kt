package io.rudione.chatone.presentation.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Brush
import io.github.aakira.napier.Napier
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
import io.rudione.chatone.presentation.components.SettingsCard
import io.rudione.chatone.presentation.settings.SettingsEvent
import io.rudione.chatone.presentation.settings.SettingsState
import io.rudione.chatone.presentation.theme.ChatoneTheme
import io.rudione.chatone.presentation.theme.i18n.AppStrings
import io.rudione.chatone.presentation.theme.i18n.LocalStrings
import io.rudione.chatone.presentation.components.ChatoneIconButton
import io.rudione.chatone.presentation.components.ChatoneDropdownMenu
import io.rudione.chatone.presentation.components.ChatoneTextField

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

        ModActionButtonsSection(state = state, onEvent = onEvent)

        MacrosSection(state = state, onEvent = onEvent)

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

        SettingsCard(title = s.modSavedReasonsTitle) {
            Text(
                s.modSavedReasonsDesc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            ChatoneTextField(
                value = state.savedTimeoutReason,
                onValueChange = { onEvent(SettingsEvent.OnSavedTimeoutReasonChanged(it)) },
                modifier = Modifier.fillMaxWidth(),
                label = s.modSavedTimeoutReasonLabel,
                singleLine = true
            )
            Spacer(Modifier.height(8.dp))
            ChatoneTextField(
                value = state.savedBanReason,
                onValueChange = { onEvent(SettingsEvent.OnSavedBanReasonChanged(it)) },
                modifier = Modifier.fillMaxWidth(),
                label = s.modSavedBanReasonLabel,
                singleLine = true
            )
        }
    }
}

@Composable
private fun ModActionButtonsSection(
    state: SettingsState,
    onEvent: (SettingsEvent) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingButton by remember { mutableStateOf<ModActionButton?>(null) }

    var orderedButtons by remember {
        mutableStateOf(state.allModButtons.sortedBy { it.sortOrder })
    }
    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetY by remember { mutableStateOf(0f) }
    var preDragOrder by remember { mutableStateOf<List<ModActionButton>?>(null) }
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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                    .clickable { onEvent(SettingsEvent.OnOpenThemeCreator(null)) }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Outlined.Palette,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    s.modButtonColorHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    Icons.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

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
                    key(btn.id) {
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
                                        preDragOrder = orderedButtons
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
                                        preDragOrder = null
                                        Napier.d(
                                            tag = "ModReorder",
                                            message = "[dragEnd] sending order=${orderedButtons.map { it.id }}"
                                        )
                                        onEvent(SettingsEvent.OnReorderAllModButtons(orderedButtons))
                                    },
                                    onDragCancel = {
                                        draggedIndex = null; dragOffsetY = 0f
                                        preDragOrder?.let { orderedButtons = it }
                                        preDragOrder = null
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
                            ChatoneIconButton(
                                onClick = { editingButton = btn },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.Outlined.Edit, null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            ChatoneIconButton(
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
                        io.rudione.chatone.presentation.components.ChatoneSwitch(
                            checked = btn.enabled,
                            onCheckedChange = { enabled ->
                                orderedButtons = orderedButtons.map {
                                    if (it.id == btn.id) it.copy(enabled = enabled) else it
                                }
                                onEvent(SettingsEvent.OnSetModButtonEnabled(btn.id, enabled))
                                when (btn.id) {
                                    "default_delete"  -> onEvent(SettingsEvent.OnShowDefaultDeleteChanged(enabled))
                                    "default_timeout" -> onEvent(SettingsEvent.OnShowDefaultTimeoutChanged(enabled))
                                    "default_ban"     -> onEvent(SettingsEvent.OnShowDefaultBanChanged(enabled))
                                }
                            },
                            modifier = Modifier.height(28.dp)
                        )
                    }
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
            ChatoneIconButton(onClick = onMoveUp, enabled = !isFirst, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Filled.KeyboardArrowUp, null, modifier = Modifier.size(16.dp))
            }
            ChatoneIconButton(onClick = onMoveDown, enabled = !isLast, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Filled.KeyboardArrowDown, null, modifier = Modifier.size(16.dp))
            }
            ChatoneIconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Outlined.Edit, null, modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary)
            }
            ChatoneIconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
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
                ChatoneTextField(
                    value = seconds,
                    onValueChange = { seconds = it.filter { c -> c.isDigit() } },
                    label = s.modDurationSeconds,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    trailing = {
                        secsInt?.let {
                            Text(ModActionButton.formatDuration(it),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(end = 8.dp))
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                ChatoneTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = s.modCustomLabelOptional,
                    placeholder = secsInt?.let { ModActionButton.formatDuration(it) } ?: s.modAuto,
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
