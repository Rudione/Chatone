package io.rudione.chatone.presentation.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.rudione.chatone.domain.model.Macro
import io.rudione.chatone.domain.model.MacroStep
import io.rudione.chatone.presentation.components.ChatoneDropdownMenu
import io.rudione.chatone.presentation.components.ChatoneIconButton
import io.rudione.chatone.presentation.components.ChatoneTextField
import io.rudione.chatone.presentation.components.SettingsCard
import io.rudione.chatone.presentation.settings.SettingsEvent
import io.rudione.chatone.presentation.settings.SettingsState
import io.rudione.chatone.presentation.theme.i18n.AppStrings
import io.rudione.chatone.presentation.theme.i18n.LocalStrings

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
                Text(
                    s.modQuickBar,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    (0 until Macro.MAX_MACRO_SLOTS).forEach { slot ->
                        val macro = state.macros.find { it.pinnedIndex == slot }
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(
                                    if (macro != null) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                )
                                .border(
                                    1.dp,
                                    if (macro != null) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                    RoundedCornerShape(9.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (macro != null) {
                                MacroIcon(
                                    macro.icon,
                                    size = 18.dp,
                                    fontSize = 14.sp,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            } else {
                                Text(
                                    "${slot + 1}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                )
                            }
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            }

            if (state.macros.isEmpty()) {
                Text(
                    s.modNoMacros,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
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
    val isPinned = macro.pinnedIndex in 0 until Macro.MAX_MACRO_SLOTS
    val s = LocalStrings.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MacroIcon(macro.icon, size = 22.dp, fontSize = 20.sp)
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(macro.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(
                s.modStepCount.replace("{0}", macro.steps.size.toString()) +
                    if (isPinned) s.modPinnedSlotSuffix.replace("{0}", (macro.pinnedIndex + 1).toString()) else "",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Box {
            ChatoneIconButton(onClick = { showPinMenu = true }, modifier = Modifier.size(32.dp)) {
                Icon(
                    if (isPinned) Icons.Filled.Star else Icons.Outlined.Star,
                    contentDescription = s.modPinMacro,
                    modifier = Modifier.size(16.dp),
                    tint = if (isPinned) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            ChatoneDropdownMenu(expanded = showPinMenu, onDismissRequest = { showPinMenu = false }) {
                if (isPinned) {
                    DropdownMenuItem(
                        text = { Text(s.modUnpinFromBar) },
                        onClick = { showPinMenu = false; onUnpin() },
                        leadingIcon = { Icon(Icons.Filled.Close, null, modifier = Modifier.size(16.dp)) }
                    )
                    HorizontalDivider()
                }
                (0 until Macro.MAX_MACRO_SLOTS).forEach { slot ->
                    val slotMacro = pinnedMacros.find { it.pinnedIndex == slot }
                    DropdownMenuItem(
                        text = {
                            Text(
                                s.modSlot.replace("{0}", (slot + 1).toString()) +
                                    (slotMacro?.let { s.modSlotName.replace("{0}", it.name) } ?: s.modSlotEmpty)
                            )
                        },
                        onClick = { showPinMenu = false; onPin(slot) },
                        leadingIcon = { Text("${slot + 1}", style = MaterialTheme.typography.labelMedium) }
                    )
                }
            }
        }
        ChatoneIconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Outlined.Edit, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
        }
        ChatoneIconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Outlined.Delete, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun MacroIconPicker(
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val s = LocalStrings.current
    var showVectors by remember { mutableStateOf(!MacroIcons.isEmojiChoice(selected)) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = showVectors,
                onClick = { showVectors = true },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
            ) { Text(s.modIconTabMaterial, style = MaterialTheme.typography.labelMedium) }
            SegmentedButton(
                selected = !showVectors,
                onClick = { showVectors = false },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
            ) { Text(s.modIconTabEmoji, style = MaterialTheme.typography.labelMedium) }
        }

        if (showVectors) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(44.dp),
                modifier = Modifier.fillMaxWidth().heightIn(max = 168.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(MacroIcons.CATALOG, key = { it.first }, contentType = { "macro-icon" }) { (name, _) ->
                    val token = MacroIcons.token(name)
                    MacroIconCell(token = token, isSelected = selected == token) { onSelect(token) }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(44.dp),
                modifier = Modifier.fillMaxWidth().heightIn(max = 168.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(MacroIcons.EMOJI, key = { it }, contentType = { "macro-emoji" }) { emoji ->
                    MacroIconCell(token = emoji, isSelected = selected == emoji) { onSelect(emoji) }
                }
            }
        }
    }
}

@Composable
private fun MacroIconCell(token: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
            .border(
                1.dp,
                if (isSelected) MaterialTheme.colorScheme.primary
                else Color.Transparent,
                RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        MacroIcon(
            token,
            size = 20.dp,
            fontSize = 18.sp,
            tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun MacroPreviewChip(icon: String, name: String, stepCount: Int) {
    val s = LocalStrings.current
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
            .border(
                0.5.dp,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                RoundedCornerShape(10.dp)
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MacroIcon(icon, size = 16.dp, fontSize = 14.sp, tint = MaterialTheme.colorScheme.onPrimaryContainer)
        Spacer(Modifier.width(8.dp))
        Column {
            Text(
                name.ifBlank { s.modMacroName },
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                s.modStepCount.replace("{0}", stepCount.toString()),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun MacroNameDialog(
    onConfirm: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var icon by remember { mutableStateOf(MacroIcons.DEFAULT) }
    val s = LocalStrings.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(s.modNewMacro, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                ChatoneTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = s.modMacroName,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    s.modChooseIcon,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                MacroIconPicker(selected = icon, onSelect = { icon = it })
                Text(
                    s.modPreviewLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                MacroPreviewChip(icon = icon, name = name, stepCount = 0)
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(name.trim(), icon) },
                enabled = name.isNotBlank()
            ) { Text(s.modCreate) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(s.cancel) } }
    )
}

@Composable
fun MacroEditorDialog(
    macro: Macro,
    onSave: (Macro) -> Unit,
    onDismiss: () -> Unit
) {
    var steps by remember { mutableStateOf(macro.steps.toList()) }
    var showAddStep by remember { mutableStateOf(false) }
    var editingStepIndex by remember { mutableStateOf<Int?>(null) }
    var macroName by remember { mutableStateOf(macro.name) }
    var macroIcon by remember { mutableStateOf(macro.icon) }
    var showIconPicker by remember { mutableStateOf(false) }
    val s = LocalStrings.current

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            tonalElevation = 8.dp,
            modifier = Modifier.fillMaxWidth(0.9f).fillMaxHeight(0.88f)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                Row(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            s.modEditMacro,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(6.dp))
                        MacroPreviewChip(icon = macroIcon, name = macroName, stepCount = steps.size)
                    }
                    ChatoneIconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, null) }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(onClick = { showIconPicker = !showIconPicker }) {
                        MacroIcon(macroIcon, size = 18.dp, fontSize = 16.sp)
                        Spacer(Modifier.width(6.dp))
                        Text(s.modIcon, style = MaterialTheme.typography.labelMedium)
                    }
                    ChatoneTextField(
                        value = macroName,
                        onValueChange = { macroName = it },
                        label = s.modMacroName,
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (showIconPicker) {
                    MacroIconPicker(
                        selected = macroIcon,
                        onSelect = { macroIcon = it },
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
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
                        item(contentType = "empty") {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    s.modNoStepsYet,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                    itemsIndexed(steps, contentType = { _, _ -> "step" }) { idx, step ->
                        MacroStepRow(
                            step = step,
                            index = idx,
                            onDelete = { steps = steps.filterIndexed { i, _ -> i != idx } },
                            onEdit = { editingStepIndex = idx },
                            onMoveUp = {
                                if (idx > 0) {
                                    steps = steps.toMutableList().apply { add(idx - 1, removeAt(idx)) }
                                }
                            },
                            onMoveDown = {
                                if (idx < steps.lastIndex) {
                                    steps = steps.toMutableList().apply { add(idx + 1, removeAt(idx)) }
                                }
                            },
                            isFirst = idx == 0,
                            isLast = idx == steps.lastIndex
                        )
                    }
                    item(contentType = "spacer") { Spacer(Modifier.height(8.dp)) }
                }

                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(onClick = { showAddStep = true }, modifier = Modifier.weight(1f)) {
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
                steps = steps + step
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
                    steps = steps.toMutableList().apply { set(idx, updatedStep) }
                    editingStepIndex = null
                },
                onDismiss = { editingStepIndex = null }
            )
        }
    }
}

@Composable
private fun MacroStepRow(
    step: MacroStep,
    index: Int,
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
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "${index + 1}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(icon, fontSize = 16.sp)
        Spacer(Modifier.width(8.dp))
        Text(description, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f), maxLines = 2)
        Row {
            ChatoneIconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                Icon(
                    Icons.Outlined.Edit, null, modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                )
            }
            ChatoneIconButton(onClick = onMoveUp, enabled = !isFirst, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Filled.KeyboardArrowUp, null, modifier = Modifier.size(14.dp))
            }
            ChatoneIconButton(onClick = onMoveDown, enabled = !isLast, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Filled.KeyboardArrowDown, null, modifier = Modifier.size(14.dp))
            }
            ChatoneIconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                Icon(
                    Icons.Outlined.Delete, null, modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                )
            }
        }
    }
}

internal fun stepDescription(step: MacroStep, s: AppStrings): Pair<String, String> = when (step) {
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
    is MacroStep.Delay -> "⏳" to if (step.repeatCount > 1)
        s.modStepDescDelay.replace("{0}", (step.seconds * step.repeatCount).toString())
    else s.modStepDescDelay.replace("{0}", step.seconds.toString())

    is MacroStep.ClearChat -> "🗑️" to s.modStepDescClear
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddMacroStepDialog(
    initialStep: MacroStep?,
    onAdd: (MacroStep) -> Unit,
    onDismiss: () -> Unit
) {
    fun typeOf(step: MacroStep?) = when (step) {
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
        mutableStateOf(
            when (initialStep) {
                is MacroStep.SendMessage -> "${initialStep.repeatCount}"
                is MacroStep.InsertText -> "${initialStep.repeatCount}"
                is MacroStep.Delay -> "${initialStep.repeatCount}"
                else -> "1"
            }
        )
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

    val repeatValue = repeatCount.toIntOrNull()?.coerceAtLeast(1) ?: 1
    val draftStep = buildStep(
        selected, messageText, boolState,
        slowSeconds, followerMinutes, raidTarget, pinMessage, delaySeconds, repeatValue
    )

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            tonalElevation = 8.dp,
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
                    Text(
                        title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(stepTypes, key = { it.first }, contentType = { "step-type" }) { (key, label) ->
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
                                Text(
                                    label,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    modifier = Modifier.weight(1f)
                                )
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
                    modifier = Modifier.weight(1f).fillMaxHeight().padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(
                        modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (selected == null) {
                            Text(
                                s.modSelectActionType,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }

                        when (selected) {
                            "send" -> ChatoneTextField(
                                value = messageText,
                                onValueChange = { messageText = it },
                                label = s.modMessageText,
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            "insert" -> ChatoneTextField(
                                value = messageText,
                                onValueChange = { messageText = it },
                                label = s.modTextToInsert,
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            "sub", "emote", "r9k" -> MacroToggleRow(boolState) { boolState = it }

                            "slow" -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                MacroToggleRow(boolState) { boolState = it }
                                if (boolState) ChatoneTextField(
                                    value = slowSeconds,
                                    onValueChange = { slowSeconds = it.filter { c -> c.isDigit() } },
                                    label = s.modSlowModeSeconds,
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            "followers" -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                MacroToggleRow(boolState) { boolState = it }
                                if (boolState) ChatoneTextField(
                                    value = followerMinutes,
                                    onValueChange = { followerMinutes = it.filter { c -> c.isDigit() } },
                                    label = s.modDurationMinutes,
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            "raid" -> ChatoneTextField(
                                value = raidTarget,
                                onValueChange = { raidTarget = it },
                                label = s.modChannelToRaid,
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            "pin" -> ChatoneTextField(
                                value = pinMessage,
                                onValueChange = { pinMessage = it },
                                label = s.modMessageToPin,
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            "delay" -> ChatoneTextField(
                                value = delaySeconds,
                                onValueChange = { delaySeconds = it.filter { c -> c.isDigit() } },
                                label = s.modWaitSeconds,
                                singleLine = true,
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
                                Text(
                                    s.modRepeat,
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.width(60.dp)
                                )
                                ChatoneTextField(
                                    value = repeatCount,
                                    onValueChange = { v ->
                                        val n = v.filter { it.isDigit() }
                                        repeatCount = if (n.isEmpty()) "1" else n
                                    },
                                    singleLine = true,
                                    modifier = Modifier.width(80.dp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                                Text(
                                    s.modRepeatTimes,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (selected == "send" && repeatValue > 1) {
                                Text(
                                    s.modRepeatDuplicateHint,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (draftStep != null) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            Text(
                                s.modPreviewLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val (previewIcon, previewText) = stepDescription(draftStep, s)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Outlined.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(previewIcon, fontSize = 15.sp)
                                Spacer(Modifier.width(8.dp))
                                Text(previewText, style = MaterialTheme.typography.bodySmall, maxLines = 3)
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                            Text(s.cancel)
                        }
                        Button(
                            onClick = { draftStep?.let(onAdd) },
                            enabled = draftStep != null,
                            modifier = Modifier.weight(1f)
                        ) { Text(confirmLabel) }
                    }
                }
            }
        }
    }
}

@Composable
private fun MacroToggleRow(enabled: Boolean, onChange: (Boolean) -> Unit) {
    val s = LocalStrings.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(s.modAction, style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = enabled, onClick = { onChange(true) }, label = { Text(s.modEnable) })
            FilterChip(selected = !enabled, onClick = { onChange(false) }, label = { Text(s.modDisable) })
        }
    }
}

internal fun buildStep(
    type: String?, msg: String, bool: Boolean,
    slow: String, follow: String, raid: String, pin: String, delay: String,
    repeatCount: Int = 1
): MacroStep? = when (type) {
    "send" -> if (msg.isNotBlank()) MacroStep.SendMessage(msg, repeatCount.coerceAtLeast(1)) else null
    "insert" -> if (msg.isNotBlank()) MacroStep.InsertText(msg, repeatCount.coerceAtLeast(1)) else null
    "sub" -> MacroStep.SubMode(bool)
    "emote" -> MacroStep.EmoteMode(bool)
    "slow" -> if (!bool || (slow.toIntOrNull() ?: 0) > 0) MacroStep.SlowMode(bool, slow.toIntOrNull() ?: 30) else null
    "followers" -> if (!bool || follow.toIntOrNull() != null) MacroStep.FollowerMode(bool, follow.toIntOrNull() ?: 10) else null
    "r9k" -> MacroStep.R9KMode(bool)
    "raid" -> if (raid.isNotBlank()) MacroStep.StartRaid(raid.trim()) else null
    "pin" -> if (pin.isNotBlank()) MacroStep.PinMessage(pin) else null
    "delay" -> delay.toIntOrNull()?.takeIf { it > 0 }?.let { MacroStep.Delay(it, repeatCount.coerceAtLeast(1)) }
    "clear" -> MacroStep.ClearChat()
    else -> null
}
