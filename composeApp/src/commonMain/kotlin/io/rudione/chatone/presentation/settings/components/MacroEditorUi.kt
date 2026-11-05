package io.rudione.chatone.presentation.settings.components

import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.cancel
import chatone.composeapp.generated.resources.mod_choose_icon
import chatone.composeapp.generated.resources.mod_create
import chatone.composeapp.generated.resources.mod_create_macro
import chatone.composeapp.generated.resources.mod_icon_tab_emoji
import chatone.composeapp.generated.resources.mod_icon_tab_material
import chatone.composeapp.generated.resources.mod_macro_name
import chatone.composeapp.generated.resources.mod_macros
import chatone.composeapp.generated.resources.mod_macros_desc
import chatone.composeapp.generated.resources.mod_new_macro
import chatone.composeapp.generated.resources.mod_no_macros
import chatone.composeapp.generated.resources.mod_pin_macro
import chatone.composeapp.generated.resources.mod_pinned_slot_suffix
import chatone.composeapp.generated.resources.mod_quick_bar
import chatone.composeapp.generated.resources.mod_slot
import chatone.composeapp.generated.resources.mod_slot_empty
import chatone.composeapp.generated.resources.mod_slot_name
import chatone.composeapp.generated.resources.mod_step_count
import chatone.composeapp.generated.resources.mod_unpin_from_bar
import org.jetbrains.compose.resources.stringResource
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.SideEffect
import io.rudione.chatone.presentation.components.animatedBorder
import androidx.compose.ui.draw.drawBehind
import io.rudione.chatone.presentation.components.expressive.ExpressiveTabs
import io.rudione.chatone.presentation.components.expressive.ExpressiveTabSize
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import io.rudione.chatone.presentation.components.ChatoneTextField
import io.rudione.chatone.presentation.components.SettingsCard
import io.rudione.chatone.presentation.settings.SettingsDestination
import io.rudione.chatone.presentation.settings.SettingsEvent
import io.rudione.chatone.presentation.settings.SettingsNavigator
import io.rudione.chatone.presentation.settings.SettingsState
import org.koin.compose.koinInject

private val MacroTileShape = RoundedCornerShape(8.dp)

@Composable
fun MacrosSection(
    state: SettingsState,
    onEvent: (SettingsEvent) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingMacro by remember { mutableStateOf<Macro?>(null) }
    val settingsNavigator: SettingsNavigator = koinInject()
    val pendingDestination by settingsNavigator.pending.collectAsState()

    SideEffect(pendingDestination, state.macros) {
        val destination = pendingDestination as? SettingsDestination.EditMacro ?: return@SideEffect
        val target = state.macros.firstOrNull { it.id == destination.macroId } ?: return@SideEffect
        editingMacro = target
        settingsNavigator.consume(destination)
    }

    SettingsCard(title = stringResource(Res.string.mod_macros), help = stringResource(Res.string.mod_macros_desc)) {
        if (state.macros.any { it.pinnedIndex in 0 until Macro.MAX_MACRO_SLOTS }) {
            ChatoneFieldLabel(stringResource(Res.string.mod_quick_bar))
            MacroQuickBarPreview(macros = state.macros, onEdit = { editingMacro = it })
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        }

        if (state.macros.isEmpty()) {
            Text(
                stringResource(Res.string.mod_no_macros),
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
            text = stringResource(Res.string.mod_create_macro),
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

    ChatoneListItem(
        onClick = onEdit,
        leading = { MacroIconTile(icon = macro.icon, onClick = null, size = 28.dp) },
        trailing = {
            Box {
                ChatoneIconButton(onClick = { showPinMenu = true }, modifier = Modifier.size(26.dp)) {
                    Icon(
                        if (isPinned) Icons.Filled.Star else Icons.Outlined.Star,
                        contentDescription = stringResource(Res.string.mod_pin_macro),
                        modifier = Modifier.size(15.dp),
                        tint = if (isPinned) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                ChatoneDropdownMenu(expanded = showPinMenu, onDismissRequest = { showPinMenu = false }) {
                    if (isPinned) {
                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.mod_unpin_from_bar)) },
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
                                    stringResource(Res.string.mod_slot, (slot + 1)) +
                                        (slotMacro?.let { stringResource(Res.string.mod_slot_name, (it.name)) } ?: stringResource(Res.string.mod_slot_empty))
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
            stringResource(Res.string.mod_step_count, macro.steps.size) +
                if (isPinned) stringResource(Res.string.mod_pinned_slot_suffix, (macro.pinnedIndex + 1)) else "",
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
            .drawBehind { drawRect(container) }
            .animatedBorder(1.dp, MacroTileShape) { border }
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
    val modIconTabMaterialText = stringResource(Res.string.mod_icon_tab_material)
    val modIconTabEmojiText = stringResource(Res.string.mod_icon_tab_emoji)
    var showVectors by remember { mutableStateOf(!MacroIcons.isEmojiChoice(selected)) }
    val tokens = remember(showVectors) {
        if (showVectors) MacroIcons.CATALOG.map { MacroIcons.token(it.first) } else MacroIcons.EMOJI
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ExpressiveTabs(
            tabs = ICON_TABS,
            selected = showVectors,
            onSelect = { showVectors = it },
            label = { if (it) modIconTabMaterialText else modIconTabEmojiText },
            size = ExpressiveTabSize.Compact
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

    ChatoneDialog(
        onDismissRequest = onDismiss,
        title = stringResource(Res.string.mod_new_macro),
        maxWidth = 420.dp,
        actions = {
            ChatoneButton(text = stringResource(Res.string.cancel), onClick = onDismiss, tone = ChatoneButtonTone.Neutral)
            ChatoneButton(
                text = stringResource(Res.string.mod_create),
                onClick = { onConfirm(name.trim(), icon) },
                enabled = name.isNotBlank()
            )
        }
    ) {
        ChatoneTextField(
            value = name,
            onValueChange = { name = it },
            label = stringResource(Res.string.mod_macro_name),
            singleLine = true,
            leading = { MacroIconTile(icon = icon, onClick = null, size = 24.dp) },
            modifier = Modifier.fillMaxWidth()
        )
        ChatoneFieldLabel(stringResource(Res.string.mod_choose_icon))
        MacroIconPicker(selected = icon, onSelect = { icon = it })
    }
}

private val ICON_TABS = listOf(true, false)
