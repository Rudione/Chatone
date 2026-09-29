package io.rudione.chatone.presentation.settings.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import io.rudione.chatone.icons.material.Icons
import io.rudione.chatone.icons.material.filled.Add
import io.rudione.chatone.icons.material.filled.Close
import io.rudione.chatone.icons.material.filled.Star
import io.rudione.chatone.icons.material.outlined.Delete
import io.rudione.chatone.icons.material.outlined.Edit
import io.rudione.chatone.icons.material.outlined.Star
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.rudione.chatone.domain.model.Macro
import io.rudione.chatone.presentation.components.ChatoneButton
import io.rudione.chatone.presentation.components.ChatoneButtonTone
import io.rudione.chatone.presentation.components.ChatoneDialog
import io.rudione.chatone.presentation.components.ChatoneDropdownMenu
import io.rudione.chatone.presentation.components.ChatoneFieldLabel
import io.rudione.chatone.presentation.components.ChatoneIconButton
import io.rudione.chatone.presentation.components.ChatoneListItem
import io.rudione.chatone.presentation.components.ChatoneSegmentedControl
import io.rudione.chatone.presentation.components.ChatoneTextField
import io.rudione.chatone.presentation.components.SettingsCard
import io.rudione.chatone.presentation.settings.SettingsDestination
import io.rudione.chatone.presentation.settings.SettingsEvent
import io.rudione.chatone.presentation.settings.SettingsNavigator
import io.rudione.chatone.presentation.settings.SettingsState
import io.rudione.chatone.presentation.theme.i18n.LocalStrings
import org.koin.compose.koinInject

private val MacroTileShape = RoundedCornerShape(8.dp)

@Composable
fun MacrosSection(
    state: SettingsState,
    onEvent: (SettingsEvent) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingMacro by remember { mutableStateOf<Macro?>(null) }
    val s = LocalStrings.current
    val settingsNavigator: SettingsNavigator = koinInject()
    val pendingDestination by settingsNavigator.pending.collectAsState()

    LaunchedEffect(pendingDestination, state.macros) {
        val destination = pendingDestination as? SettingsDestination.EditMacro ?: return@LaunchedEffect
        val target = state.macros.firstOrNull { it.id == destination.macroId } ?: return@LaunchedEffect
        editingMacro = target
        settingsNavigator.consume(destination)
    }

    SettingsCard(title = s.modMacros) {
        Text(
            s.modMacrosDesc,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (state.macros.any { it.pinnedIndex in 0 until Macro.MAX_MACRO_SLOTS }) {
            ChatoneFieldLabel(s.modQuickBar)
            MacroQuickBarPreview(macros = state.macros, onEdit = { editingMacro = it })
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        }

        if (state.macros.isEmpty()) {
            Text(
                s.modNoMacros,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                state.macros.forEach { macro ->
                    key(macro.id) {
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
            }
        }

        ChatoneButton(
            text = s.modCreateMacro,
            onClick = { showAddDialog = true },
            tone = ChatoneButtonTone.Neutral,
            icon = Icons.Filled.Add,
            modifier = Modifier.fillMaxWidth()
        )
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
            onSave = {
                onEvent(SettingsEvent.OnUpdateMacro(it))
                editingMacro = null
            },
            onDismiss = { editingMacro = null }
        )
    }
}

@Composable
private fun MacroQuickBarPreview(macros: List<Macro>, onEdit: (Macro) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        (0 until Macro.MAX_MACRO_SLOTS).forEach { slot ->
            val macro = macros.firstOrNull { it.pinnedIndex == slot }
            if (macro != null) {
                MacroIconTile(icon = macro.icon, onClick = { onEdit(macro) })
            } else {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(MacroTileShape)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f), MacroTileShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "${slot + 1}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                    )
                }
            }
        }
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

    ChatoneListItem(
        onClick = onEdit,
        leading = { MacroIconTile(icon = macro.icon, onClick = null, size = 28.dp) },
        trailing = {
            Box {
                ChatoneIconButton(onClick = { showPinMenu = true }, modifier = Modifier.size(26.dp)) {
                    Icon(
                        if (isPinned) Icons.Filled.Star else Icons.Outlined.Star,
                        contentDescription = s.modPinMacro,
                        modifier = Modifier.size(15.dp),
                        tint = if (isPinned) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                ChatoneDropdownMenu(expanded = showPinMenu, onDismissRequest = { showPinMenu = false }) {
                    if (isPinned) {
                        DropdownMenuItem(
                            text = { Text(s.modUnpinFromBar) },
                            onClick = {
                                showPinMenu = false
                                onUnpin()
                            },
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
                            onClick = {
                                showPinMenu = false
                                onPin(slot)
                            },
                            leadingIcon = { Text("${slot + 1}", style = MaterialTheme.typography.labelMedium) }
                        )
                    }
                }
            }
            ChatoneIconButton(onClick = onEdit, modifier = Modifier.size(26.dp)) {
                Icon(
                    Icons.Outlined.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
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
            macro.name,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            s.modStepCount.replace("{0}", macro.steps.size.toString()) +
                if (isPinned) s.modPinnedSlotSuffix.replace("{0}", (macro.pinnedIndex + 1).toString()) else "",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
internal fun MacroIconTile(
    icon: String,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    size: Dp = 30.dp,
    selected: Boolean = false
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val accent = MaterialTheme.colorScheme.primary
    val active = hovered && onClick != null
    val container by animateColorAsState(
        accent.copy(
            alpha = when {
                selected -> 0.32f
                active -> 0.24f
                else -> 0.14f
            }
        ),
        tween(140),
        label = "macroTileContainer"
    )
    val border by animateColorAsState(
        accent.copy(alpha = if (selected || active) 0.65f else 0.28f),
        tween(140),
        label = "macroTileBorder"
    )
    Box(
        modifier = modifier
            .size(size)
            .clip(MacroTileShape)
            .background(container)
            .border(1.dp, border, MacroTileShape)
            .then(
                if (onClick != null) {
                    Modifier
                        .clickable(interactionSource = interaction, indication = null, onClick = onClick)
                        .pointerHoverIcon(PointerIcon.Hand)
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        MacroIcon(
            icon,
            size = size * 0.56f,
            fontSize = (size.value * 0.46f).sp,
            tint = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
internal fun MacroIconPicker(
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val s = LocalStrings.current
    var showVectors by remember { mutableStateOf(!MacroIcons.isEmojiChoice(selected)) }
    val tokens = remember(showVectors) {
        if (showVectors) MacroIcons.CATALOG.map { MacroIcons.token(it.first) } else MacroIcons.EMOJI
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ChatoneSegmentedControl(
            options = listOf(true to s.modIconTabMaterial, false to s.modIconTabEmoji),
            selected = showVectors,
            onSelect = { showVectors = it },
            fillWidth = true
        )
        LazyVerticalGrid(
            columns = GridCells.Adaptive(36.dp),
            modifier = Modifier.fillMaxWidth().heightIn(max = 156.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(tokens, key = { it }, contentType = { "macro-icon" }) { token ->
                MacroIconTile(
                    icon = token,
                    onClick = { onSelect(token) },
                    size = 34.dp,
                    selected = selected == token
                )
            }
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

    ChatoneDialog(
        onDismissRequest = onDismiss,
        title = s.modNewMacro,
        maxWidth = 420.dp,
        actions = {
            ChatoneButton(text = s.cancel, onClick = onDismiss, tone = ChatoneButtonTone.Neutral)
            ChatoneButton(
                text = s.modCreate,
                onClick = { onConfirm(name.trim(), icon) },
                enabled = name.isNotBlank()
            )
        }
    ) {
        ChatoneTextField(
            value = name,
            onValueChange = { name = it },
            label = s.modMacroName,
            singleLine = true,
            leading = { MacroIconTile(icon = icon, onClick = null, size = 24.dp) },
            modifier = Modifier.fillMaxWidth()
        )
        ChatoneFieldLabel(s.modChooseIcon)
        MacroIconPicker(selected = icon, onSelect = { icon = it })
    }
}
