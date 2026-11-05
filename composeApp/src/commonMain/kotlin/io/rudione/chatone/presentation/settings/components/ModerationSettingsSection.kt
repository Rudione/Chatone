package io.rudione.chatone.presentation.settings.components

import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.cancel
import chatone.composeapp.generated.resources.mod_add_timeout_button
import chatone.composeapp.generated.resources.mod_add_timeout_button_count
import chatone.composeapp.generated.resources.mod_button_color_hint
import chatone.composeapp.generated.resources.mod_button_colors
import chatone.composeapp.generated.resources.mod_buttons_on_hover
import chatone.composeapp.generated.resources.mod_buttons_on_hover_desc
import chatone.composeapp.generated.resources.mod_custom_label_optional
import chatone.composeapp.generated.resources.mod_drag_reorder_hint
import chatone.composeapp.generated.resources.mod_duration
import chatone.composeapp.generated.resources.mod_edit_timeout_button
import chatone.composeapp.generated.resources.mod_mod_action_buttons
import chatone.composeapp.generated.resources.mod_press_drag_reorder
import chatone.composeapp.generated.resources.mod_preview_label
import chatone.composeapp.generated.resources.mod_quick_presets
import chatone.composeapp.generated.resources.mod_slider_buttons_in_chat
import chatone.composeapp.generated.resources.mod_slider_buttons_in_chat_help
import chatone.composeapp.generated.resources.mod_slider_buttons_in_chat_off
import chatone.composeapp.generated.resources.save
import org.jetbrains.compose.resources.stringResource
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.SideEffect
import io.rudione.chatone.presentation.components.expressive.HelpHint
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import io.rudione.chatone.icons.material.Icons
import io.rudione.chatone.icons.material.automirrored.filled.KeyboardArrowRight
import io.rudione.chatone.icons.material.filled.Add
import io.rudione.chatone.icons.material.filled.DragIndicator
import io.rudione.chatone.icons.material.outlined.Block
import io.rudione.chatone.icons.material.outlined.Delete
import io.rudione.chatone.icons.material.outlined.Edit
import io.rudione.chatone.icons.material.outlined.Palette
import io.rudione.chatone.icons.material.outlined.Timer
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import io.github.aakira.napier.Napier
import io.rudione.chatone.domain.model.ModActionButton
import io.rudione.chatone.presentation.components.ChatoneButton
import io.rudione.chatone.presentation.components.ChatoneButtonTone
import io.rudione.chatone.presentation.components.ChatoneChip
import io.rudione.chatone.presentation.components.ChatoneDialog
import io.rudione.chatone.presentation.components.ChatoneDurationField
import io.rudione.chatone.presentation.components.ChatoneFieldLabel
import io.rudione.chatone.presentation.components.ChatoneIconButton
import io.rudione.chatone.presentation.components.ChatoneListItem
import io.rudione.chatone.presentation.components.ChatoneSwitch
import io.rudione.chatone.presentation.components.ChatoneTextField
import io.rudione.chatone.presentation.components.SettingsCard
import io.rudione.chatone.presentation.settings.SettingsEvent
import io.rudione.chatone.presentation.settings.SettingsState
import io.rudione.chatone.presentation.settings.settingsAnchor
import io.rudione.chatone.presentation.settings.theme_settings.ThemeCreatorSection
import io.rudione.chatone.presentation.theme.ChatoneTheme
import io.rudione.chatone.util.platform.rememberInputCapabilities

private val TIMEOUT_BUTTON_PRESETS = listOf(
    1 to "1s", 10 to "10s", 60 to "1m", 300 to "5m", 600 to "10m",
    3600 to "1h", 86400 to "1d", 604800 to "1w", ModActionButton.MAX_TIMEOUT_SECONDS to "2w"
)

private const val MAX_CUSTOM_MOD_BUTTONS = 8
private const val DEFAULT_NEW_BUTTON_SECONDS = 600
private const val PREVIEW_BUTTON_ID = "preview"
private val ROW_SPACING = 4.dp
private val ModTagShape = RoundedCornerShape(8.dp)

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ModActionButtonsSection(
    state: SettingsState,
    onEvent: (SettingsEvent) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingButton by remember { mutableStateOf<ModActionButton?>(null) }

    var orderedButtons by remember {
        mutableStateOf(state.allModButtons.sortedBy { it.sortOrder })
    }
    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var preDragOrder by remember { mutableStateOf<List<ModActionButton>?>(null) }
    var rowPitchPx by remember { mutableIntStateOf(0) }
    SideEffect(state.allModButtons) {
        if (draggedIndex == null) {
            orderedButtons = state.allModButtons.sortedBy { it.sortOrder }
        }
    }
    val rowSpacingPx = with(LocalDensity.current) { ROW_SPACING.roundToPx() }

    SettingsCard(title = stringResource(Res.string.mod_mod_action_buttons), help = stringResource(Res.string.mod_drag_reorder_hint)) {
        ChatoneListItem(
            onClick = {
                onEvent(SettingsEvent.OnOpenThemeCreator(null, ThemeCreatorSection.MODERATION_COLORS))
            },
            leading = {
                Icon(
                    Icons.Outlined.Palette,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            trailing = {
                HelpHint(title = stringResource(Res.string.mod_button_colors), text = stringResource(Res.string.mod_button_color_hint), size = 28.dp)
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        ) {
            Text(
                stringResource(Res.string.mod_button_colors),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        ModButtonsSwitchItem(
            title = stringResource(Res.string.mod_slider_buttons_in_chat),
            help = stringResource(Res.string.mod_slider_buttons_in_chat_help),
            subtitle = stringResource(Res.string.mod_slider_buttons_in_chat_off).takeUnless { state.modButtonsInChat },
            checked = state.modButtonsInChat,
            onCheckedChange = { onEvent(SettingsEvent.OnModButtonsInChatChanged(it)) }
        )

        if (state.modButtonsInChat && rememberInputCapabilities().pointer) {
            ModButtonsSwitchItem(
                title = stringResource(Res.string.mod_buttons_on_hover),
                help = stringResource(Res.string.mod_buttons_on_hover_desc),
                checked = state.modButtonsOnHover,
                onCheckedChange = { onEvent(SettingsEvent.OnModButtonsOnHoverChanged(it)) }
            )
        }

        ChatoneFieldLabel(stringResource(Res.string.mod_preview_label))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            orderedButtons.filter { it.enabled }.forEach { button ->
                key(button.id) {
                    ModButtonTag(
                        button = button,
                        onClick = if (button.isDefault) null else ({ editingButton = button })
                    )
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

        ChatoneFieldLabel(stringResource(Res.string.mod_press_drag_reorder))
        Column(verticalArrangement = Arrangement.spacedBy(ROW_SPACING)) {
            orderedButtons.forEachIndexed { index, button ->
                key(button.id) {
                    val isDragged = draggedIndex == index
                    ModButtonRow(
                        button = button,
                        highlighted = isDragged,
                        onEdit = { editingButton = button },
                        onRemove = { onEvent(SettingsEvent.OnRemoveModButton(button.id)) },
                        onToggle = { enabled ->
                            orderedButtons = orderedButtons.map {
                                if (it.id == button.id) it.copy(enabled = enabled) else it
                            }
                            onEvent(SettingsEvent.OnSetModButtonEnabled(button.id, enabled))
                            when (button.id) {
                                ModActionButton.DEFAULT_DELETE.id ->
                                    onEvent(SettingsEvent.OnShowDefaultDeleteChanged(enabled))
                                ModActionButton.DEFAULT_TIMEOUT.id ->
                                    onEvent(SettingsEvent.OnShowDefaultTimeoutChanged(enabled))
                                ModActionButton.DEFAULT_BAN.id ->
                                    onEvent(SettingsEvent.OnShowDefaultBanChanged(enabled))
                            }
                        },
                        modifier = Modifier
                            .onSizeChanged { rowPitchPx = it.height + rowSpacingPx }
                            .zIndex(if (isDragged) 10f else 0f)
                            .graphicsLayer {
                                translationY = if (isDragged) dragOffsetY else 0f
                                alpha = if (isDragged) 0.9f else 1f
                                scaleX = if (isDragged) 1.02f else 1f
                                scaleY = if (isDragged) 1.02f else 1f
                            }
                            .pointerInput(button.id) {
                                detectDragGestures(
                                    onDragStart = {
                                        preDragOrder = orderedButtons
                                        draggedIndex = orderedButtons.indexOfFirst { it.id == button.id }
                                        dragOffsetY = 0f
                                    },
                                    onDrag = { change, amount ->
                                        change.consume()
                                        dragOffsetY += amount.y
                                        val pitch = rowPitchPx.takeIf { it > 0 }?.toFloat()
                                            ?: return@detectDragGestures
                                        val current = orderedButtons.indexOfFirst { it.id == button.id }
                                        if (current < 0) return@detectDragGestures
                                        val target = (current + (dragOffsetY / pitch).toInt())
                                            .coerceIn(0, orderedButtons.lastIndex)
                                        if (target != current) {
                                            orderedButtons = orderedButtons.toMutableList().apply {
                                                add(target, removeAt(current))
                                            }
                                            draggedIndex = target
                                            dragOffsetY -= (target - current) * pitch
                                        }
                                    },
                                    onDragEnd = {
                                        draggedIndex = null
                                        dragOffsetY = 0f
                                        preDragOrder = null
                                        Napier.d(
                                            tag = "ModReorder",
                                            message = "[dragEnd] sending order=${orderedButtons.map { it.id }}"
                                        )
                                        onEvent(SettingsEvent.OnReorderAllModButtons(orderedButtons))
                                    },
                                    onDragCancel = {
                                        draggedIndex = null
                                        dragOffsetY = 0f
                                        preDragOrder?.let { orderedButtons = it }
                                        preDragOrder = null
                                    }
                                )
                            }
                    )
                }
            }
        }

        ChatoneButton(
            text = stringResource(Res.string.mod_add_timeout_button_count, state.customModButtons.size, MAX_CUSTOM_MOD_BUTTONS),
            onClick = { showAddDialog = true },
            enabled = state.customModButtons.size < MAX_CUSTOM_MOD_BUTTONS,
            tone = ChatoneButtonTone.Neutral,
            icon = Icons.Filled.Add,
            modifier = Modifier.fillMaxWidth()
        )
    }

    if (showAddDialog) {
        ModButtonEditorDialog(
            initial = null,
            onSave = { seconds, label ->
                onEvent(SettingsEvent.OnAddModButton(seconds, label))
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }
    editingButton?.let { button ->
        ModButtonEditorDialog(
            initial = button,
            onSave = { seconds, label ->
                onEvent(SettingsEvent.OnUpdateModButton(button.copy(durationSeconds = seconds, label = label)))
                editingButton = null
            },
            onDismiss = { editingButton = null }
        )
    }
}

@Composable
private fun ModButtonsSwitchItem(
    title: String,
    help: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    subtitle: String? = null
) {
    ChatoneListItem(
        modifier = Modifier.settingsAnchor(title),
        onClick = { onCheckedChange(!checked) },
        trailing = {
            ChatoneSwitch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                modifier = Modifier.height(28.dp)
            )
        }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                title,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f, fill = false)
            )
            HelpHint(title = title, text = help, size = 28.dp)
        }
        if (subtitle != null) {
            Text(
                subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private data class ModButtonVisual(val icon: ImageVector, val tint: Color)

@Composable
private fun modButtonVisual(button: ModActionButton): ModButtonVisual {
    val extra = ChatoneTheme.extraColors
    return when (button.id) {
        ModActionButton.DEFAULT_DELETE.id -> ModButtonVisual(Icons.Outlined.Delete, extra.modDelete)
        ModActionButton.DEFAULT_BAN.id -> ModButtonVisual(Icons.Outlined.Block, extra.modBan)
        else -> ModButtonVisual(Icons.Outlined.Timer, extra.modTimeout)
    }
}

@Composable
private fun ModButtonRow(
    button: ModActionButton,
    highlighted: Boolean,
    onEdit: () -> Unit,
    onRemove: () -> Unit,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val visual = modButtonVisual(button)
    ChatoneListItem(
        modifier = modifier,
        highlighted = highlighted,
        leading = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    Icons.Filled.DragIndicator,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(visual.tint.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(visual.icon, contentDescription = null, tint = visual.tint, modifier = Modifier.size(14.dp))
                }
            }
        },
        trailing = {
            if (!button.isDefault) {
                ChatoneIconButton(onClick = onEdit, modifier = Modifier.size(26.dp)) {
                    Icon(
                        Icons.Outlined.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                ChatoneIconButton(onClick = onRemove, modifier = Modifier.size(26.dp)) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
            ChatoneSwitch(
                checked = button.enabled,
                onCheckedChange = onToggle,
                modifier = Modifier.height(28.dp)
            )
        }
    ) {
        Text(
            button.displayLabel,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = visual.tint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (!button.isDefault && button.label.isNotBlank()) {
            Text(
                ModActionButton.formatDuration(button.durationSeconds),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ModButtonTag(button: ModActionButton, onClick: (() -> Unit)?) {
    val visual = modButtonVisual(button)
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val interactive = onClick != null
    Row(
        modifier = Modifier
            .height(24.dp)
            .clip(ModTagShape)
            .background(visual.tint.copy(alpha = if (hovered && interactive) 0.26f else 0.14f))
            .border(1.dp, visual.tint.copy(alpha = if (hovered && interactive) 0.6f else 0.35f), ModTagShape)
            .then(
                if (onClick != null) {
                    Modifier
                        .clickable(interactionSource = interaction, indication = null, onClick = onClick)
                        .pointerHoverIcon(PointerIcon.Hand)
                } else Modifier
            )
            .padding(horizontal = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(visual.icon, contentDescription = null, tint = visual.tint, modifier = Modifier.size(13.dp))
        Text(
            button.displayLabel,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = visual.tint,
            maxLines = 1
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ModButtonEditorDialog(
    initial: ModActionButton?,
    onSave: (Int, String) -> Unit,
    onDismiss: () -> Unit
) {
    val timeoutTint = ChatoneTheme.extraColors.modTimeout
    var seconds by remember {
        mutableIntStateOf(initial?.durationSeconds?.takeIf { it > 0 } ?: DEFAULT_NEW_BUTTON_SECONDS)
    }
    var label by remember { mutableStateOf(initial?.label.orEmpty()) }
    val preview = ModActionButton(id = PREVIEW_BUTTON_ID, durationSeconds = seconds, label = label.trim())

    ChatoneDialog(
        onDismissRequest = onDismiss,
        title = if (initial == null) stringResource(Res.string.mod_add_timeout_button) else stringResource(Res.string.mod_edit_timeout_button),
        maxWidth = 400.dp,
        actions = {
            ChatoneButton(text = stringResource(Res.string.cancel), onClick = onDismiss, tone = ChatoneButtonTone.Neutral)
            ChatoneButton(text = stringResource(Res.string.save), onClick = { onSave(seconds, label.trim()) })
        }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ChatoneFieldLabel(stringResource(Res.string.mod_preview_label))
            ModButtonTag(button = preview, onClick = null)
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ChatoneFieldLabel(stringResource(Res.string.mod_quick_presets))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TIMEOUT_BUTTON_PRESETS.forEach { (presetSeconds, presetLabel) ->
                    ChatoneChip(
                        label = presetLabel,
                        onClick = {
                            seconds = presetSeconds
                            label = ""
                        },
                        selected = seconds == presetSeconds,
                        accent = timeoutTint,
                        height = 26.dp
                    )
                }
            }
        }
        ChatoneDurationField(
            totalSeconds = seconds,
            onValueChange = { seconds = it },
            label = stringResource(Res.string.mod_duration),
            maxSeconds = ModActionButton.MAX_TIMEOUT_SECONDS,
            modifier = Modifier.fillMaxWidth()
        )
        ChatoneTextField(
            value = label,
            onValueChange = { label = it },
            label = stringResource(Res.string.mod_custom_label_optional),
            placeholder = ModActionButton.formatDuration(seconds),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
