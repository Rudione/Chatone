package io.rudione.chatone.presentation.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import io.rudione.chatone.domain.model.Macro
import io.rudione.chatone.domain.model.MacroStep
import io.rudione.chatone.domain.model.ModActionButton
import io.rudione.chatone.presentation.theme.ChatoneTheme


@Composable
fun ModerationSettingsSection(
    state: SettingsState,
    onEvent: (SettingsEvent) -> Unit
) {
    val extra = ChatoneTheme.extraColors

    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {


        SettingsCard(title = "Default Timeout Duration") {
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


        SettingsCard(title = "Safety") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Confirm mod actions", style = MaterialTheme.typography.bodyMedium)
                    Text("Show confirmation dialog before ban/timeout",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = state.confirmModActions,
                    onCheckedChange = { onEvent(SettingsEvent.OnConfirmModActionsChanged(it)) }
                )
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

    SettingsCard(title = "Mod Action Buttons") {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "Configure quick-action buttons shown left of the username when Mod Mode (★) is active. " +
                        "Delete and Ban are always present. Add custom timeout durations between them.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )


            Text("Preview:", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                item {
                    ModButtonPreviewChip(
                        icon = "🗑️", label = "Del",
                        color = ChatoneTheme.extraColors.modDelete, isFixed = true
                    )
                }

                items(state.customModButtons.filter { it.enabled }, key = { it.id }) { btn ->
                    ModButtonPreviewChip(
                        icon = "⏱", label = btn.displayLabel,
                        color = ChatoneTheme.extraColors.modTimeout, isFixed = false,
                        onClick = { editingButton = btn }
                    )
                }

                item {
                    ModButtonPreviewChip(
                        icon = "⏱", label = "10m",
                        color = ChatoneTheme.extraColors.modTimeout, isFixed = true
                    )
                }

                item {
                    ModButtonPreviewChip(
                        icon = "🔨", label = "Ban",
                        color = ChatoneTheme.extraColors.modBan, isFixed = true
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))


            if (state.customModButtons.isEmpty()) {
                Text(
                    "No custom buttons yet. Add timeout durations below.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            } else {
                state.customModButtons.forEachIndexed { idx, btn ->
                    ModButtonRow(
                        button = btn,
                        onEdit = { editingButton = btn },
                        onDelete = { onEvent(SettingsEvent.OnRemoveModButton(btn.id)) },
                        onMoveUp = { if (idx > 0) onEvent(SettingsEvent.OnReorderModButtons(idx, idx - 1)) },
                        onMoveDown = { if (idx < state.customModButtons.lastIndex) onEvent(SettingsEvent.OnReorderModButtons(idx, idx + 1)) },
                        isFirst = idx == 0,
                        isLast = idx == state.customModButtons.lastIndex
                    )
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
                Text("Add timeout button (${state.customModButtons.size}/8)")
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
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    if (initial == null) "Add Timeout Button" else "Edit Timeout Button",
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold
                )

                val presets = listOf(1 to "1s", 10 to "10s", 60 to "1m", 300 to "5m",
                    600 to "10m", 3600 to "1h", 86400 to "1d")
                Text("Quick presets:", style = MaterialTheme.typography.labelSmall,
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
                    label = { Text("Duration (seconds)") },
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
                    label = { Text("Custom label (optional)") },
                    placeholder = { Text(secsInt?.let { ModActionButton.formatDuration(it) } ?: "auto") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
                    Button(
                        onClick = { secsInt?.let { onAdd(it, label.trim()) } },
                        enabled = secsInt != null && secsInt > 0,
                        modifier = Modifier.weight(1f)
                    ) { Text("Save") }
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

    SettingsCard(title = "Macros") {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "Create macros that execute multiple chat actions in sequence. " +
                        "Pin up to 5 macros to the quick-access bar above ★ and ⚙.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )


            if (state.macros.any { it.pinnedIndex >= 0 }) {
                Text("Quick bar (drag & drop in chat):", style = MaterialTheme.typography.labelSmall,
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
                Text("No macros yet. Create your first macro below.",
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
                Text("Create macro")
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
                "${macro.steps.size} step(s)" + if (isPinned) " · Slot ${macro.pinnedIndex + 1}" else "",
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
                    contentDescription = "Pin macro",
                    modifier = Modifier.size(16.dp),
                    tint = if (isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            DropdownMenu(expanded = showPinMenu, onDismissRequest = { showPinMenu = false }) {
                if (isPinned) {
                    DropdownMenuItem(
                        text = { Text("Unpin from bar") },
                        onClick = { showPinMenu = false; onUnpin() },
                        leadingIcon = { Icon(Icons.Filled.Close, null, modifier = Modifier.size(16.dp)) }
                    )
                    HorizontalDivider()
                }
                (0..4).forEach { slot ->
                    val slotMacro = pinnedMacros.find { it.pinnedIndex == slot }
                    DropdownMenuItem(
                        text = { Text("Slot ${slot + 1}" + (slotMacro?.let { " (${it.name})" } ?: " (empty)")) },
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

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), tonalElevation = 8.dp) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("New Macro", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Macro name") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Choose icon:", style = MaterialTheme.typography.labelSmall,
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
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
                    Button(
                        onClick = { if (name.isNotBlank()) onConfirm(name.trim(), icon) },
                        enabled = name.isNotBlank(), modifier = Modifier.weight(1f)
                    ) { Text("Create") }
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
    var macroName by remember { mutableStateOf(macro.name) }
    var macroIcon by remember { mutableStateOf(macro.icon) }

    Dialog(onDismissRequest = onDismiss, properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
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
                        Text("Edit Macro", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(macroName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, null) }
                }
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
                                Text("No steps yet. Add actions below.",
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
                        Text("Add step")
                    }
                    Button(
                        onClick = { onSave(macro.copy(name = macroName, icon = macroIcon, steps = steps)) },
                        modifier = Modifier.weight(1f)
                    ) { Text("Save macro") }
                }
            }
        }
    }

    if (showAddStep) {
        AddMacroStepDialog(
            onAdd = { step ->
                steps = (steps + step).toMutableList()
                showAddStep = false
            },
            onDismiss = { showAddStep = false }
        )
    }
}

@Composable
private fun MacroStepRow(
    step: MacroStep, index: Int,
    onDelete: () -> Unit, onMoveUp: () -> Unit, onMoveDown: () -> Unit,
    isFirst: Boolean, isLast: Boolean
) {
    val (icon, description) = stepDescription(step)
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

private fun stepDescription(step: MacroStep): Pair<String, String> = when (step) {
    is MacroStep.SendMessage -> "💬" to "Send: \"${step.text}\""
    is MacroStep.SubMode -> "⭐" to if (step.enable) "Enable Sub-only mode" else "Disable Sub-only mode"
    is MacroStep.EmoteMode -> "😊" to if (step.enable) "Enable Emote-only mode" else "Disable Emote-only mode"
    is MacroStep.SlowMode -> "🐢" to if (step.enable) "Enable Slow mode (${step.seconds}s)" else "Disable Slow mode"
    is MacroStep.FollowerMode -> "❤️" to if (step.enable) "Enable Follower mode (${step.minutes}m)" else "Disable Follower mode"
    is MacroStep.R9KMode -> "🔒" to if (step.enable) "Enable R9K / Unique mode" else "Disable R9K mode"
    is MacroStep.StartRaid -> "🚀" to "Start raid → ${step.targetLogin}"
    is MacroStep.PinMessage -> "📌" to "Pin: \"${step.message}\""
    is MacroStep.Delay -> "⏳" to "Wait ${step.seconds}s"
    is MacroStep.ClearChat -> "🗑️" to "Clear chat"
}


@Composable
private fun AddMacroStepDialog(
    onAdd: (MacroStep) -> Unit,
    onDismiss: () -> Unit
) {
    var selected by remember { mutableStateOf<String?>(null) }

    var messageText by remember { mutableStateOf("") }
    var delaySeconds by remember { mutableStateOf("5") }
    var slowSeconds by remember { mutableStateOf("30") }
    var followerMinutes by remember { mutableStateOf("10") }
    var raidTarget by remember { mutableStateOf("") }
    var pinMessage by remember { mutableStateOf("") }
    var boolState by remember { mutableStateOf(true) }

    val stepTypes = listOf(
        "send" to "💬  Send message",
        "sub" to "⭐  Sub-only mode",
        "emote" to "😊  Emote-only mode",
        "slow" to "🐢  Slow mode",
        "followers" to "❤️  Follower mode",
        "r9k" to "🔒  R9K / Unique mode",
        "raid" to "🚀  Start raid",
        "pin" to "📌  Pin message",
        "delay" to "⏳  Delay / Wait",
        "clear" to "🗑️  Clear chat"
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), tonalElevation = 8.dp) {
            Column(modifier = Modifier.padding(20.dp).widthIn(min = 320.dp, max = 480.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Add Step", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)


                LazyColumn(modifier = Modifier.heightIn(max = 240.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(stepTypes) { (key, label) ->
                        val isSelected = selected == key
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                .clickable { selected = key }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(label, style = MaterialTheme.typography.bodyMedium,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f))
                            if (isSelected) Icon(Icons.Filled.Check, null, modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }


                when (selected) {
                    "send" -> OutlinedTextField(value = messageText, onValueChange = { messageText = it },
                        label = { Text("Message text") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth())
                    "sub", "emote", "r9k" -> Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Action:", style = MaterialTheme.typography.bodyMedium)
                        FilterChip(selected = boolState, onClick = { boolState = true }, label = { Text("Enable") })
                        FilterChip(selected = !boolState, onClick = { boolState = false }, label = { Text("Disable") })
                    }
                    "slow" -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            FilterChip(selected = boolState, onClick = { boolState = true }, label = { Text("Enable") })
                            FilterChip(selected = !boolState, onClick = { boolState = false }, label = { Text("Disable") })
                        }
                        if (boolState) OutlinedTextField(value = slowSeconds, onValueChange = { slowSeconds = it.filter { c -> c.isDigit() } },
                            label = { Text("Slow mode (seconds)") }, singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth())
                    }
                    "followers" -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            FilterChip(selected = boolState, onClick = { boolState = true }, label = { Text("Enable") })
                            FilterChip(selected = !boolState, onClick = { boolState = false }, label = { Text("Disable") })
                        }
                        if (boolState) OutlinedTextField(value = followerMinutes, onValueChange = { followerMinutes = it.filter { c -> c.isDigit() } },
                            label = { Text("Follow duration (minutes)") }, singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth())
                    }
                    "raid" -> OutlinedTextField(value = raidTarget, onValueChange = { raidTarget = it },
                        label = { Text("Channel to raid") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth())
                    "pin" -> OutlinedTextField(value = pinMessage, onValueChange = { pinMessage = it },
                        label = { Text("Message to pin") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth())
                    "delay" -> OutlinedTextField(value = delaySeconds, onValueChange = { delaySeconds = it.filter { c -> c.isDigit() } },
                        label = { Text("Wait duration (seconds)") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth())
                    "clear" -> Text("This will clear the chat.", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error)
                }

                val canConfirm = when (selected) {
                    "send" -> messageText.isNotBlank()
                    "raid" -> raidTarget.isNotBlank()
                    "pin" -> pinMessage.isNotBlank()
                    "delay" -> delaySeconds.toIntOrNull() != null && delaySeconds.toInt() > 0
                    "slow" -> !boolState || (slowSeconds.toIntOrNull() != null && slowSeconds.toInt() > 0)
                    "followers" -> !boolState || (followerMinutes.toIntOrNull() != null)
                    null -> false
                    else -> true
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
                    Button(
                        onClick = {
                            val step = buildStep(selected, messageText, boolState, slowSeconds, followerMinutes, raidTarget, pinMessage, delaySeconds)
                            if (step != null) onAdd(step)
                        },
                        enabled = canConfirm && selected != null,
                        modifier = Modifier.weight(1f)
                    ) { Text("Add step") }
                }
            }
        }
    }
}

private fun buildStep(
    type: String?, msg: String, bool: Boolean,
    slow: String, follow: String, raid: String, pin: String, delay: String
): MacroStep? = when (type) {
    "send" -> if (msg.isNotBlank()) MacroStep.SendMessage(msg) else null
    "sub" -> MacroStep.SubMode(bool)
    "emote" -> MacroStep.EmoteMode(bool)
    "slow" -> MacroStep.SlowMode(bool, slow.toIntOrNull() ?: 30)
    "followers" -> MacroStep.FollowerMode(bool, follow.toIntOrNull() ?: 10)
    "r9k" -> MacroStep.R9KMode(bool)
    "raid" -> if (raid.isNotBlank()) MacroStep.StartRaid(raid.trim()) else null
    "pin" -> if (pin.isNotBlank()) MacroStep.PinMessage(pin) else null
    "delay" -> delay.toIntOrNull()?.takeIf { it > 0 }?.let { MacroStep.Delay(it) }
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