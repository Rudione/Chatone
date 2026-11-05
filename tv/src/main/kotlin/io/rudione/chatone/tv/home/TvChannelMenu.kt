package io.rudione.chatone.tv.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.ListItem
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.ListItemDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.browse_forget_channel
import chatone.composeapp.generated.resources.browse_keep_channel
import chatone.composeapp.generated.resources.cancel
import chatone.composeapp.generated.resources.main_create
import chatone.composeapp.generated.resources.main_create_folder_title
import chatone.composeapp.generated.resources.main_delete_folder
import chatone.composeapp.generated.resources.main_folder_name_placeholder
import chatone.composeapp.generated.resources.main_new_folder
import chatone.composeapp.generated.resources.main_remove_from_folder
import chatone.composeapp.generated.resources.tv_channel_watch
import chatone.composeapp.generated.resources.tv_menu_collapse
import chatone.composeapp.generated.resources.tv_menu_expand
import chatone.composeapp.generated.resources.tv_menu_move
import chatone.composeapp.generated.resources.tv_menu_profile
import chatone.composeapp.generated.resources.tv_menu_remove_channel
import coil3.compose.AsyncImage
import io.rudione.chatone.domain.model.ChannelFolder
import io.rudione.chatone.domain.model.ChannelTab
import io.rudione.chatone.icons.lucide.ChevronDown
import io.rudione.chatone.icons.lucide.ChevronRight
import io.rudione.chatone.icons.lucide.Folder
import io.rudione.chatone.icons.lucide.FolderPlus
import io.rudione.chatone.icons.lucide.History
import io.rudione.chatone.icons.lucide.Lucide
import io.rudione.chatone.icons.lucide.Pin
import io.rudione.chatone.icons.lucide.PinOff
import io.rudione.chatone.icons.lucide.Play
import io.rudione.chatone.icons.lucide.Trash2
import io.rudione.chatone.icons.lucide.User
import io.rudione.chatone.icons.lucide.X
import io.rudione.chatone.presentation.components.SystemBackHandler
import io.rudione.chatone.presentation.main.MainEvent
import io.rudione.chatone.presentation.main.components.sidebar.FolderColors
import io.rudione.chatone.presentation.main.components.sidebar.parseFolderColor
import io.rudione.chatone.tv.components.TvButtonTone
import io.rudione.chatone.tv.components.TvPillButton
import io.rudione.chatone.tv.components.claim
import io.rudione.chatone.tv.components.tvFocusTrap
import org.jetbrains.compose.resources.stringResource

@Immutable
sealed interface TvMenuTarget {
    data class Channel(val target: TvChannelTarget) : TvMenuTarget
    data class Folder(val folder: ChannelFolder) : TvMenuTarget
    data class AddChannel(val openLogins: Set<String>) : TvMenuTarget
}

@Immutable
class TvChannelMenuActions(
    val onWatch: (ChannelTab) -> Unit,
    val onOpenProfile: (String) -> Unit,
    val send: (MainEvent) -> Unit
)

private enum class MenuStep { ACTIONS, FOLDERS, NEW_FOLDER }

private val MenuShape = RoundedCornerShape(36.dp)
private val MenuWidth = 460.dp

@Composable
fun TvChannelMenu(
    target: TvMenuTarget,
    folders: List<ChannelFolder>,
    actions: TvChannelMenuActions,
    onDismiss: () -> Unit
) {
    var step by remember(target) { mutableStateOf(MenuStep.ACTIONS) }
    SystemBackHandler {
        if (step == MenuStep.ACTIONS) onDismiss() else step = MenuStep.ACTIONS
    }
    val done: (MainEvent) -> Unit = { event ->
        actions.send(event)
        onDismiss()
    }
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier
                .width(MenuWidth)
                .clip(MenuShape)
                .background(MaterialTheme.colorScheme.surface)
                .padding(24.dp)
                .tvFocusTrap()
                .focusGroup(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            when (target) {
                is TvMenuTarget.Channel -> {
                    val channel = target.target.channel
                    TvMenuHeader(channel)
                    when (step) {
                        MenuStep.ACTIONS -> TvChannelActionsList(target.target, actions, done, onDismiss) {
                            step = MenuStep.FOLDERS
                        }

                        MenuStep.FOLDERS -> TvFolderPicker(
                            folders = folders,
                            currentFolderId = target.target.folderId,
                            onPick = { done(MainEvent.MoveChannelToFolder(channel.login, it)) },
                            onNewFolder = { step = MenuStep.NEW_FOLDER }
                        )

                        MenuStep.NEW_FOLDER -> TvNewFolderForm(
                            onCreate = { name, color ->
                                actions.send(MainEvent.ShowCreateFolderDialog(channel.login))
                                actions.send(MainEvent.UpdateNewFolderName(name))
                                actions.send(MainEvent.UpdateNewFolderColor(color))
                                done(MainEvent.CreateFolder)
                            },
                            onCancel = { step = MenuStep.FOLDERS }
                        )
                    }
                }

                is TvMenuTarget.AddChannel -> TvAddChannelPanel(
                    openLogins = target.openLogins,
                    onAdd = { requests -> done(MainEvent.AddChannels(requests)) },
                    onCancel = onDismiss
                )

                is TvMenuTarget.Folder -> {
                    val folder = target.folder
                    Text(
                        folder.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = parseFolderColor(folder.color),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    TvMenuList(
                        listOf(
                            TvMenuEntry(
                                if (folder.isExpanded) Lucide.ChevronDown else Lucide.ChevronRight,
                                stringResource(if (folder.isExpanded) Res.string.tv_menu_collapse else Res.string.tv_menu_expand)
                            ) { done(MainEvent.ToggleFolder(folder.id)) },
                            TvMenuEntry(Lucide.Trash2, stringResource(Res.string.main_delete_folder), danger = true) {
                                done(MainEvent.DeleteFolder(folder.id))
                            },
                            TvMenuEntry(Lucide.X, stringResource(Res.string.cancel), onClick = onDismiss)
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun TvMenuHeader(channel: ChannelTab) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        AsyncImage(
            model = channel.profileImageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
        Text(
            channel.displayName.ifBlank { channel.login },
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Immutable
internal class TvMenuEntry(
    val icon: ImageVector,
    val label: String,
    val danger: Boolean = false,
    val supporting: String? = null,
    val onClick: () -> Unit
)

@Composable
private fun TvChannelActionsList(
    target: TvChannelTarget,
    actions: TvChannelMenuActions,
    done: (MainEvent) -> Unit,
    onDismiss: () -> Unit,
    onMove: () -> Unit
) {
    val channel = target.channel
    val entries = buildList {
        add(TvMenuEntry(Lucide.Play, stringResource(Res.string.tv_channel_watch)) {
            onDismiss()
            actions.onWatch(channel)
        })
        add(TvMenuEntry(Lucide.User, stringResource(Res.string.tv_menu_profile)) {
            onDismiss()
            actions.onOpenProfile(channel.login)
        })
        if (target.recent) {
            add(TvMenuEntry(Lucide.Pin, stringResource(Res.string.browse_keep_channel)) {
                done(MainEvent.KeepRecentChannel(channel.login))
            })
        }
        add(TvMenuEntry(Lucide.Folder, stringResource(Res.string.tv_menu_move), onClick = onMove))
        if (target.recent) {
            add(TvMenuEntry(Lucide.History, stringResource(Res.string.browse_forget_channel), danger = true) {
                done(MainEvent.ForgetRecentChannel(channel.login))
            })
        } else {
            add(TvMenuEntry(Lucide.PinOff, stringResource(Res.string.tv_menu_remove_channel), danger = true) {
                if (target.folderId != null) actions.send(MainEvent.MoveChannelToFolder(channel.login, null))
                done(MainEvent.CloseChannel(channel.login))
            })
        }
    }
    TvMenuList(entries)
}

@Composable
private fun TvFolderPicker(
    folders: List<ChannelFolder>,
    currentFolderId: String?,
    onPick: (String?) -> Unit,
    onNewFolder: () -> Unit
) {
    val entries = buildList {
        folders.filter { it.id != currentFolderId }.forEach { folder ->
            add(TvMenuEntry(Lucide.Folder, folder.name) { onPick(folder.id) })
        }
        if (currentFolderId != null) {
            add(TvMenuEntry(Lucide.X, stringResource(Res.string.main_remove_from_folder)) { onPick(null) })
        }
        add(TvMenuEntry(Lucide.FolderPlus, stringResource(Res.string.main_new_folder), onClick = onNewFolder))
    }
    TvMenuList(entries)
}

@Composable
internal fun TvMenuList(entries: List<TvMenuEntry>) {
    val first = remember { FocusRequester() }
    LazyColumn(
        Modifier.heightIn(max = 420.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(entries.size) { index ->
            val entry = entries[index]
            val scheme = MaterialTheme.colorScheme
            ListItem(
                selected = false,
                onClick = entry.onClick,
                modifier = if (index == 0) Modifier.focusRequester(first) else Modifier,
                shape = ListItemDefaults.shape(RoundedCornerShape(20.dp)),
                leadingContent = {
                    Icon(
                        entry.icon,
                        contentDescription = null,
                        tint = if (entry.danger) scheme.error else LocalContentColor.current,
                        modifier = Modifier.size(ListItemDefaults.IconSize)
                    )
                },
                headlineContent = {
                    Text(
                        entry.label,
                        color = if (entry.danger) scheme.error else Color.Unspecified,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                supportingContent = entry.supporting?.let { { Text(it, maxLines = 2) } }
            )
        }
    }
    LaunchedEffect(entries.size) { first.claim() }
}

@Composable
private fun TvNewFolderForm(onCreate: (String, String) -> Unit, onCancel: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var color by remember { mutableStateOf(FolderColors.DEFAULT_HEX) }
    val fieldFocus = remember { FocusRequester() }
    val textFocus = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val scheme = MaterialTheme.colorScheme
    Text(stringResource(Res.string.main_create_folder_title), style = MaterialTheme.typography.titleMedium)
    Surface(
        onClick = { textFocus.requestFocus() },
        modifier = Modifier.fillMaxWidth().focusRequester(fieldFocus),
        shape = ClickableSurfaceDefaults.shape(CircleShape),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.01f),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(BorderStroke(2.dp, scheme.onSurface), shape = CircleShape)
        ),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = scheme.surfaceVariant,
            focusedContainerColor = scheme.surfaceVariant
        )
    ) {
        BasicTextField(
            value = name,
            onValueChange = { name = it.take(MAX_FOLDER_NAME) },
            singleLine = true,
            textStyle = MaterialTheme.typography.titleMedium.copy(color = scheme.onSurface),
            cursorBrush = SolidColor(scheme.primary),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { fieldFocus.requestFocus() }),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 14.dp)
                .focusRequester(textFocus)
                .onPreviewKeyEvent { event ->
                    when {
                        event.type != KeyEventType.KeyDown -> false
                        event.key == Key.DirectionDown -> focusManager.moveFocus(FocusDirection.Down)
                        event.key == Key.DirectionUp -> fieldFocus.requestFocus()
                        else -> false
                    }
                },
            decorationBox = { field ->
                if (name.isEmpty()) {
                    Text(
                        stringResource(Res.string.main_folder_name_placeholder),
                        style = MaterialTheme.typography.titleMedium,
                        color = scheme.onSurfaceVariant
                    )
                }
                field()
            }
        )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        FolderColors.options.forEach { option ->
            val selected = option.hex == color
            Surface(
                onClick = { color = option.hex },
                modifier = Modifier.size(40.dp),
                shape = ClickableSurfaceDefaults.shape(CircleShape),
                scale = ClickableSurfaceDefaults.scale(focusedScale = 1.2f),
                border = ClickableSurfaceDefaults.border(
                    border = if (selected) Border(BorderStroke(3.dp, scheme.onSurface), shape = CircleShape) else Border.None,
                    focusedBorder = Border(BorderStroke(3.dp, scheme.onSurface), shape = CircleShape)
                ),
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = option.color,
                    focusedContainerColor = option.color
                )
            ) {}
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        TvPillButton(
            stringResource(Res.string.main_create),
            onClick = { if (name.isNotBlank()) onCreate(name.trim(), color) },
            enabled = name.isNotBlank()
        )
        TvPillButton(stringResource(Res.string.cancel), onClick = onCancel, tone = TvButtonTone.Tonal)
    }
    LaunchedEffect(Unit) { fieldFocus.claim() }
}

private const val MAX_FOLDER_NAME = 32
