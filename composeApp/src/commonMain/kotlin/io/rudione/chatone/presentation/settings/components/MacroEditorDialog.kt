package io.rudione.chatone.presentation.settings.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import io.rudione.chatone.icons.material.Icons
import io.rudione.chatone.icons.material.filled.Add
import io.rudione.chatone.icons.material.filled.KeyboardArrowDown
import io.rudione.chatone.icons.material.filled.KeyboardArrowUp
import io.rudione.chatone.icons.material.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.rudione.chatone.domain.model.Macro
import io.rudione.chatone.domain.model.MacroStep
import io.rudione.chatone.presentation.components.ChatoneButton
import io.rudione.chatone.presentation.components.ChatoneButtonTone
import io.rudione.chatone.presentation.components.ChatoneDialogHeader
import io.rudione.chatone.presentation.components.ChatoneDialogPanel
import io.rudione.chatone.presentation.components.ChatoneIconButton
import io.rudione.chatone.presentation.components.ChatoneListItem
import io.rudione.chatone.presentation.components.ChatoneTextField
import io.rudione.chatone.presentation.theme.i18n.LocalStrings

@Composable
fun MacroEditorDialog(
    macro: Macro,
    onSave: (Macro) -> Unit,
    onDismiss: () -> Unit
) {
    var steps by remember(macro.id) { mutableStateOf(macro.steps) }
    var macroName by remember(macro.id) { mutableStateOf(macro.name) }
    var macroIcon by remember(macro.id) { mutableStateOf(macro.icon) }
    var showIconPicker by remember { mutableStateOf(false) }
    var showAddStep by remember { mutableStateOf(false) }
    var editingStepIndex by remember { mutableStateOf<Int?>(null) }
    val s = LocalStrings.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        ChatoneDialogPanel(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxWidth(0.92f)
                .heightIn(max = 640.dp)
        ) {
            ChatoneDialogHeader(
                title = s.modEditMacro,
                subtitle = s.modStepCount.replace("{0}", steps.size.toString()),
                onClose = onDismiss
            )
            ChatoneTextField(
                value = macroName,
                onValueChange = { macroName = it },
                label = s.modMacroName,
                singleLine = true,
                leading = {
                    MacroIconTile(
                        icon = macroIcon,
                        onClick = { showIconPicker = !showIconPicker },
                        size = 24.dp,
                        selected = showIconPicker
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )
            AnimatedVisibility(visible = showIconPicker) {
                MacroIconPicker(selected = macroIcon, onSelect = { macroIcon = it })
            }
            LazyColumn(
                modifier = Modifier.weight(1f, fill = false).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (steps.isEmpty()) {
                    item(contentType = "empty") {
                        Text(
                            s.modNoStepsYet,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp)
                        )
                    }
                }
                itemsIndexed(steps, contentType = { _, _ -> "step" }) { index, step ->
                    MacroStepRow(
                        step = step,
                        index = index,
                        isFirst = index == 0,
                        isLast = index == steps.lastIndex,
                        onEdit = { editingStepIndex = index },
                        onMoveUp = {
                            if (index > 0) steps =
                                steps.toMutableList().apply { add(index - 1, removeAt(index)) }
                        },
                        onMoveDown = {
                            if (index < steps.lastIndex) {
                                steps =
                                    steps.toMutableList().apply { add(index + 1, removeAt(index)) }
                            }
                        },
                        onDelete = { steps = steps.filterIndexed { i, _ -> i != index } }
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ChatoneButton(
                    text = s.modAddStep,
                    onClick = { showAddStep = true },
                    tone = ChatoneButtonTone.Neutral,
                    icon = Icons.Filled.Add,
                    modifier = Modifier.weight(1f)
                )
                ChatoneButton(
                    text = s.modSaveMacro,
                    onClick = {
                        onSave(
                            macro.copy(
                                name = macroName.trim().ifBlank { macro.name },
                                icon = macroIcon,
                                steps = steps
                            )
                        )
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    if (showAddStep) {
        MacroStepDialog(
            initialStep = null,
            onConfirm = { step ->
                steps = steps + step
                showAddStep = false
            },
            onDismiss = { showAddStep = false }
        )
    }

    editingStepIndex?.let { index ->
        steps.getOrNull(index)?.let { step ->
            MacroStepDialog(
                initialStep = step,
                onConfirm = { updated ->
                    steps = steps.toMutableList().apply { set(index, updated) }
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
    isFirst: Boolean,
    isLast: Boolean,
    onEdit: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit
) {
    val s = LocalStrings.current
    ChatoneListItem(
        onClick = onEdit,
        leading = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    "${index + 1}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.End,
                    modifier = Modifier.widthIn(min = 14.dp)
                )
                MacroStepBadge(MacroStepKind.of(step))
            }
        },
        trailing = {
            ChatoneIconButton(
                onClick = onMoveUp,
                enabled = !isFirst,
                modifier = Modifier.size(26.dp)
            ) {
                Icon(
                    Icons.Filled.KeyboardArrowUp,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
            ChatoneIconButton(
                onClick = onMoveDown,
                enabled = !isLast,
                modifier = Modifier.size(26.dp)
            ) {
                Icon(
                    Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
            ChatoneIconButton(onClick = onDelete, modifier = Modifier.size(26.dp)) {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    ) {
        Text(
            macroStepDescription(step, s),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
