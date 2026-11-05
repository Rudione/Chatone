package io.rudione.chatone.presentation.settings.components

import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.cancel
import chatone.composeapp.generated.resources.save
import chatone.composeapp.generated.resources.settings_commands_add
import chatone.composeapp.generated.resources.settings_commands_col_command
import chatone.composeapp.generated.resources.settings_commands_col_hotkey
import chatone.composeapp.generated.resources.settings_commands_create_macro
import chatone.composeapp.generated.resources.settings_commands_desc
import chatone.composeapp.generated.resources.settings_commands_edit
import chatone.composeapp.generated.resources.settings_commands_empty
import chatone.composeapp.generated.resources.settings_commands_hotkey
import chatone.composeapp.generated.resources.settings_commands_hotkey_hint
import chatone.composeapp.generated.resources.settings_commands_pick_macro
import chatone.composeapp.generated.resources.settings_commands_replacement
import chatone.composeapp.generated.resources.settings_commands_replacement_hint
import chatone.composeapp.generated.resources.settings_commands_send_immediately
import chatone.composeapp.generated.resources.settings_commands_send_immediately_desc
import chatone.composeapp.generated.resources.settings_commands_title
import chatone.composeapp.generated.resources.settings_commands_trigger
import chatone.composeapp.generated.resources.settings_commands_trigger_hint
import chatone.composeapp.generated.resources.settings_commands_type_macro
import chatone.composeapp.generated.resources.settings_commands_type_text
import chatone.composeapp.generated.resources.settings_commands_whole_only
import chatone.composeapp.generated.resources.settings_commands_whole_only_desc
import chatone.composeapp.generated.resources.settings_recording
import org.jetbrains.compose.resources.stringResource
import io.rudione.chatone.presentation.components.expressive.HelpHint
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import io.rudione.chatone.icons.material.Icons
import io.rudione.chatone.icons.material.filled.Add
import io.rudione.chatone.icons.material.filled.Close
import io.rudione.chatone.icons.material.filled.Delete
import io.rudione.chatone.icons.material.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.rudione.chatone.domain.model.ChatCommand
import io.rudione.chatone.domain.model.ChatCommandKind
import io.rudione.chatone.domain.model.Macro
import io.rudione.chatone.presentation.chat.keyToChar
import io.rudione.chatone.presentation.components.ChatoneSwitch
import io.rudione.chatone.presentation.settings.SettingsEvent
import io.rudione.chatone.presentation.settings.SettingsState
import kotlin.time.Clock
import io.rudione.chatone.presentation.components.ChatoneIconButton
import io.rudione.chatone.presentation.components.ChatoneDropdownMenu
import io.rudione.chatone.presentation.components.ChatoneTextField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatCommandsSection(
    state: SettingsState,
    onEvent: (SettingsEvent) -> Unit,
    onRequestCreateMacro: () -> Unit = {},
) {
    var dialogTarget by remember { mutableStateOf<DialogTarget?>(null) }

    SectionShell {
        HeaderBlock()
        DividerThin()
        Toolbar(
            count = state.chatCommands.size,
            onAdd = { dialogTarget = DialogTarget.Create(newDraft()) }
        )
        DividerThin()
        TableHeader()
        DividerThin()
        if (state.chatCommands.isEmpty()) {
            EmptyRow()
        } else {
            val maxHeight = (state.chatCommands.size * 36 + 4).dp.coerceAtMost(360.dp)
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(min = 36.dp, max = maxHeight)
            ) {
                items(state.chatCommands, key = { it.id }, contentType = { "command" }) { cmd ->
                    CommandTableRow(
                        command = cmd,
                        macroName = cmd.macroId?.let { id -> state.macros.firstOrNull { it.id == id }?.name },
                        onClick = { dialogTarget = DialogTarget.Edit(cmd) },
                        onDelete = { onEvent(SettingsEvent.OnRemoveChatCommand(cmd.id)) }
                    )
                    DividerThin()
                }
            }
        }
    }

    dialogTarget?.let { target ->
        CommandEditDialog(
            initial = target.command,
            isNew = target is DialogTarget.Create,
            macros = state.macros,
            onSave = { cmd ->
                if (target is DialogTarget.Create) onEvent(SettingsEvent.OnAddChatCommand(cmd))
                else onEvent(SettingsEvent.OnUpdateChatCommand(cmd))
                dialogTarget = null
            },
            onDismiss = { dialogTarget = null },
            onRequestCreateMacro = {
                dialogTarget = null
                onRequestCreateMacro()
            }
        )
    }
}

private sealed class DialogTarget(val command: ChatCommand) {
    class Create(c: ChatCommand) : DialogTarget(c)
    class Edit(c: ChatCommand) : DialogTarget(c)
}

private fun newDraft() = ChatCommand(
    id = "cmd_${Clock.System.now().toEpochMilliseconds()}",
    trigger = "",
    kind = ChatCommandKind.TEXT,
    replacement = "",
)

@Composable
private fun SectionShell(content: @Composable ColumnScope.() -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        scheme.surfaceContainerHigh.copy(alpha = 0.75f),
                        scheme.surfaceContainerLow.copy(alpha = 0.60f)
                    )
                )
            )
            .border(
                1.dp,
                Brush.verticalGradient(
                    listOf(
                        scheme.outline.copy(alpha = 0.20f),
                        scheme.outline.copy(alpha = 0.06f)
                    )
                ),
                RoundedCornerShape(16.dp)
            )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) { content() }
    }
}

@Composable
private fun DividerThin() {
    HorizontalDivider(
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
    )
}

@Composable
private fun HeaderBlock() {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicText(
            stringResource(Res.string.settings_commands_title),
            modifier = Modifier.weight(1f, fill = false),
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.SemiBold,
                color = scheme.onSurface
            )
        )
        HelpHint(title = stringResource(Res.string.settings_commands_title), text = stringResource(Res.string.settings_commands_desc), size = 28.dp)
    }
}

@Composable
private fun Toolbar(count: Int, onAdd: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        FilledTonalButton(
            onClick = onAdd,
            modifier = Modifier.height(30.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(Icons.Filled.Add, null, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            BasicText(
                stringResource(Res.string.settings_commands_add),
                style = MaterialTheme.typography.labelSmall.copy(color = scheme.onSecondaryContainer)
            )
        }
        Spacer(Modifier.weight(1f))
        BasicText(
            "$count",
            style = MaterialTheme.typography.labelSmall.copy(color = scheme.onSurfaceVariant)
        )
    }
}

@Composable
private fun TableHeader() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TableHeaderCell(stringResource(Res.string.settings_commands_trigger), Modifier.weight(0.9f))
        TableHeaderCell(stringResource(Res.string.settings_commands_col_command), Modifier.weight(2.2f))
        TableHeaderCell(stringResource(Res.string.settings_commands_col_hotkey), Modifier.weight(1.0f))
        Spacer(Modifier.width(28.dp))
    }
}

@Composable
private fun TableHeaderCell(text: String, modifier: Modifier = Modifier) {
    BasicText(
        text,
        modifier = modifier,
        style = MaterialTheme.typography.labelSmall.copy(
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp
        )
    )
}

@Composable
private fun EmptyRow() {
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        BasicText(
            stringResource(Res.string.settings_commands_empty),
            style = MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
    }
}

@Composable
private fun CommandTableRow(
    command: ChatCommand,
    macroName: String?,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = scheme.primary.copy(alpha = 0.10f),
            shape = RoundedCornerShape(5.dp),
            border = BorderStroke(1.dp, scheme.primary.copy(alpha = 0.25f)),
            modifier = Modifier.weight(0.9f)
        ) {
            BasicText(
                command.trigger.ifBlank { "—" },
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                style = MaterialTheme.typography.labelSmall.copy(
                    color = scheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(8.dp))
        BasicText(
            when (command.kind) {
                ChatCommandKind.TEXT -> command.replacement.ifBlank { "—" }
                ChatCommandKind.MACRO -> "⚡ ${macroName ?: "—"}"
            },
            modifier = Modifier.weight(2.2f).padding(end = 8.dp),
            style = MaterialTheme.typography.bodySmall.copy(
                color = scheme.onSurface.copy(alpha = 0.85f),
                fontSize = 12.sp
            ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        if (command.hotkey.isNotBlank()) {
            HotkeyChip(command.hotkey, modifier = Modifier.weight(1f))
        } else {
            BasicText(
                "—",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelSmall.copy(
                    color = scheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            )
        }
        ChatoneIconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
            Icon(
                Icons.Filled.Delete, null,
                modifier = Modifier.size(14.dp),
                tint = scheme.error.copy(alpha = 0.75f)
            )
        }
    }
}

@Composable
private fun HotkeyChip(hotkey: String, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        color = scheme.surfaceContainerHighest.copy(alpha = 0.7f),
        shape = RoundedCornerShape(5.dp),
        border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = modifier
    ) {
        BasicText(
            hotkey.uppercase().replace("+", " + "),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall.copy(
                color = scheme.onSurface,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CommandEditDialog(
    initial: ChatCommand,
    isNew: Boolean,
    macros: List<Macro>,
    onSave: (ChatCommand) -> Unit,
    onDismiss: () -> Unit,
    onRequestCreateMacro: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    var draft by remember(initial.id) { mutableStateOf(initial) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.92f).heightIn(max = 640.dp),
            shape = RoundedCornerShape(20.dp),
            color = scheme.surface,
            border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.35f)),
            tonalElevation = 4.dp
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BasicText(
                        if (isNew) stringResource(Res.string.settings_commands_add) else stringResource(Res.string.settings_commands_edit),
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = scheme.onSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    Spacer(Modifier.weight(1f))
                    ChatoneIconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Filled.Close, null, modifier = Modifier.size(16.dp))
                    }
                }

                CompactField(label = stringResource(Res.string.settings_commands_trigger)) {
                    ChatoneTextField(
                        value = draft.trigger,
                        onValueChange = { draft = draft.copy(trigger = it.trim()) },
                        placeholder = stringResource(Res.string.settings_commands_trigger_hint),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SegmentedChip(
                        selected = draft.kind == ChatCommandKind.TEXT,
                        label = stringResource(Res.string.settings_commands_type_text),
                        onClick = { draft = draft.copy(kind = ChatCommandKind.TEXT) },
                        modifier = Modifier.weight(1f)
                    )
                    SegmentedChip(
                        selected = draft.kind == ChatCommandKind.MACRO,
                        label = stringResource(Res.string.settings_commands_type_macro),
                        onClick = { draft = draft.copy(kind = ChatCommandKind.MACRO) },
                        modifier = Modifier.weight(1f)
                    )
                }

                if (draft.kind == ChatCommandKind.TEXT) {
                    CompactField(label = stringResource(Res.string.settings_commands_replacement)) {
                        ChatoneTextField(
                            value = draft.replacement,
                            onValueChange = { draft = draft.copy(replacement = it) },
                            placeholder = stringResource(Res.string.settings_commands_replacement_hint),
                            singleLine = false,
                            maxLines = 6,
                            textStyle = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 72.dp, max = 160.dp)
                        )
                    }
                } else {
                    CompactField(label = stringResource(Res.string.settings_commands_pick_macro)) {
                        MacroPicker(
                            selectedId = draft.macroId,
                            macros = macros,
                            onPick = { draft = draft.copy(macroId = it) },
                            onCreate = onRequestCreateMacro
                        )
                    }
                }

                CompactField(label = stringResource(Res.string.settings_commands_hotkey)) {
                    HotkeyCapture(
                        hotkey = draft.hotkey,
                        onChanged = { draft = draft.copy(hotkey = it) }
                    )
                }

                ToggleRow(
                    label = stringResource(Res.string.settings_commands_whole_only),
                    description = stringResource(Res.string.settings_commands_whole_only_desc),
                    checked = draft.wholeMessageOnly,
                    onCheckedChange = { draft = draft.copy(wholeMessageOnly = it) }
                )
                ToggleRow(
                    label = stringResource(Res.string.settings_commands_send_immediately),
                    description = stringResource(Res.string.settings_commands_send_immediately_desc),
                    checked = draft.sendImmediately,
                    onCheckedChange = { draft = draft.copy(sendImmediately = it) }
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(38.dp),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        BasicText(
                            stringResource(Res.string.cancel),
                            style = MaterialTheme.typography.labelMedium.copy(color = scheme.primary)
                        )
                    }
                    Button(
                        onClick = { onSave(draft) },
                        modifier = Modifier.weight(1f).height(38.dp),
                        shape = RoundedCornerShape(10.dp),
                        enabled = draft.trigger.isNotBlank() && (
                            (draft.kind == ChatCommandKind.TEXT && draft.replacement.isNotBlank()) ||
                            (draft.kind == ChatCommandKind.MACRO && draft.macroId != null)
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        BasicText(
                            stringResource(Res.string.save),
                            style = MaterialTheme.typography.labelMedium.copy(color = scheme.onPrimary)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactField(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.fillMaxWidth()) {
        BasicText(
            label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
        )
        content()
    }
}

@Composable
private fun SegmentedChip(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        color = if (selected) scheme.primary.copy(alpha = 0.18f) else scheme.surfaceContainerHighest.copy(alpha = 0.4f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(
            1.dp,
            if (selected) scheme.primary.copy(alpha = 0.6f) else scheme.outlineVariant.copy(alpha = 0.4f)
        ),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
            BasicText(
                label,
                style = MaterialTheme.typography.labelMedium.copy(
                    color = if (selected) scheme.primary else scheme.onSurfaceVariant,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                )
            )
        }
    }
}

@Composable
private fun MacroPicker(
    selectedId: String?,
    macros: List<Macro>,
    onPick: (String) -> Unit,
    onCreate: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val selectedMacro = macros.firstOrNull { it.id == selectedId }
    var expanded by remember { mutableStateOf(false) }

    Box {
        Surface(
            color = scheme.surfaceContainerHighest.copy(alpha = 0.5f),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth().clickable { expanded = true }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (selectedMacro != null) {
                    BasicText(selectedMacro.icon, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.width(8.dp))
                    BasicText(
                        selectedMacro.name,
                        style = MaterialTheme.typography.bodySmall.copy(color = scheme.onSurface),
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    BasicText(
                        stringResource(Res.string.settings_commands_pick_macro),
                        style = MaterialTheme.typography.bodySmall.copy(color = scheme.onSurfaceVariant),
                        modifier = Modifier.weight(1f)
                    )
                }
                Icon(Icons.Filled.KeyboardArrowDown, null, modifier = Modifier.size(18.dp), tint = scheme.onSurfaceVariant)
            }
        }
        ChatoneDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            macros.forEach { macro ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            BasicText(macro.icon, style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.width(8.dp))
                            BasicText(macro.name, style = MaterialTheme.typography.bodySmall.copy(color = scheme.onSurface))
                        }
                    },
                    onClick = { onPick(macro.id); expanded = false }
                )
            }
            HorizontalDivider()
            DropdownMenuItem(
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Add, null, modifier = Modifier.size(14.dp), tint = scheme.primary)
                        Spacer(Modifier.width(6.dp))
                        BasicText(stringResource(Res.string.settings_commands_create_macro), style = MaterialTheme.typography.bodySmall.copy(color = scheme.primary))
                    }
                },
                onClick = { expanded = false; onCreate() }
            )
        }
    }
}

@Composable
private fun HotkeyCapture(hotkey: String, onChanged: (String) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    var recording by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Surface(
            color = if (recording) scheme.primaryContainer else scheme.surfaceContainerHighest.copy(alpha = 0.5f),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(
                1.dp,
                if (recording) scheme.primary else scheme.outlineVariant.copy(alpha = 0.4f)
            ),
            modifier = Modifier.weight(1f)
                .onKeyEvent { event ->
                    if (!recording) return@onKeyEvent false
                    if (event.type != KeyEventType.KeyDown) return@onKeyEvent true
                    if (event.key == Key.Escape) {
                        onChanged(""); recording = false; return@onKeyEvent true
                    }
                    if (event.key in listOf(
                            Key.CtrlLeft, Key.CtrlRight, Key.MetaLeft, Key.MetaRight,
                            Key.AltLeft, Key.AltRight, Key.ShiftLeft, Key.ShiftRight
                        )
                    ) return@onKeyEvent true
                    val parts = mutableListOf<String>()
                    if (event.isCtrlPressed || event.isMetaPressed) parts.add("ctrl")
                    if (event.isAltPressed) parts.add("alt")
                    if (event.isShiftPressed) parts.add("shift")
                    if (parts.isEmpty()) return@onKeyEvent true
                    val keyName = keyToName(event.key) ?: return@onKeyEvent true
                    parts.add(keyName)
                    onChanged(parts.joinToString("+"))
                    recording = false
                    true
                }
                .clickable { recording = true }
        ) {
            BasicText(
                when {
                    recording -> stringResource(Res.string.settings_recording)
                    hotkey.isBlank() -> stringResource(Res.string.settings_commands_hotkey_hint)
                    else -> hotkey.uppercase().replace("+", " + ")
                },
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                style = MaterialTheme.typography.labelMedium.copy(
                    color = when {
                        recording -> scheme.onPrimaryContainer
                        hotkey.isBlank() -> scheme.onSurfaceVariant.copy(alpha = 0.6f)
                        else -> scheme.onSurface
                    },
                    fontWeight = if (hotkey.isNotBlank()) FontWeight.Medium else FontWeight.Normal
                )
            )
        }
        if (hotkey.isNotBlank() || recording) {
            ChatoneIconButton(
                onClick = { onChanged(""); recording = false },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(Icons.Filled.Close, null, modifier = Modifier.size(13.dp), tint = scheme.error)
            }
        }
    }
}

@Composable
private fun ToggleRow(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            BasicText(
                label,
                modifier = Modifier.weight(1f, fill = false),
                style = MaterialTheme.typography.bodySmall.copy(color = scheme.onSurface, fontWeight = FontWeight.Medium)
            )
            HelpHint(title = label, text = description, size = 28.dp)
        }
        ChatoneSwitch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

private fun keyToName(key: Key): String? = when (key) {
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
    else -> keyToChar[key]?.lowercaseChar()?.toString()
}
