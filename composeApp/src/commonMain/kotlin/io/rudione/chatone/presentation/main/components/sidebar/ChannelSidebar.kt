package io.rudione.chatone.presentation.main.components.sidebar

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import io.rudione.chatone.presentation.components.RailAction
import io.rudione.chatone.presentation.components.ChatoneIconRail
import io.rudione.chatone.presentation.components.GradientButton
import io.rudione.chatone.presentation.theme.ChatoneColors
import io.rudione.chatone.presentation.theme.ChatoneTheme
import io.rudione.chatone.presentation.theme.LocalWallpaperController
import io.rudione.chatone.presentation.theme.i18n.LocalStrings
import org.koin.compose.koinInject
import io.rudione.chatone.data.repository.AiAssistantController
import io.rudione.chatone.presentation.main.MainState
import io.rudione.chatone.presentation.main.MainEvent
import io.rudione.chatone.presentation.channelmenu.ChannelMenuActions
import io.rudione.chatone.presentation.channelmenu.ChannelMenuModel
import io.rudione.chatone.presentation.channelmenu.ChannelOptionsMenu
import io.rudione.chatone.domain.model.ChannelFolder
import io.rudione.chatone.domain.model.ChannelTab
import io.rudione.chatone.presentation.components.ChatoneIconButton
import io.rudione.chatone.presentation.components.ChatoneDropdownMenu
import io.rudione.chatone.icons.lucide.AtSign
import io.rudione.chatone.icons.lucide.ChevronDown
import io.rudione.chatone.icons.lucide.ChevronLeft
import io.rudione.chatone.icons.lucide.ChevronRight
import io.rudione.chatone.icons.lucide.EllipsisVertical
import io.rudione.chatone.icons.lucide.Folder
import io.rudione.chatone.icons.lucide.FolderOpen
import io.rudione.chatone.icons.lucide.Lucide
import io.rudione.chatone.icons.lucide.MessageCircle
import io.rudione.chatone.icons.lucide.Pencil
import io.rudione.chatone.icons.lucide.Plus
import io.rudione.chatone.icons.lucide.Settings
import io.rudione.chatone.icons.lucide.Sparkles
import io.rudione.chatone.icons.lucide.Trash2
import io.rudione.chatone.icons.lucide.User
import io.rudione.chatone.icons.lucide.X

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun ChannelSidebar(
    state: MainState,
    onEvent: (MainEvent) -> Unit,
    isWideScreen: Boolean = false,
    onOpenBrowse: (() -> Unit)? = null
) {
    val extra = ChatoneTheme.extraColors

    val dropIndex = remember { SidebarDropIndex() }
    val dragState = remember { SidebarDragState() }
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    val dropTarget by remember(dropIndex, dragState, listState) {
        derivedStateOf {
            if (!dragState.isDragging) return@derivedStateOf null
            listState.firstVisibleItemIndex
            listState.firstVisibleItemScrollOffset
            dropIndex.resolve(dragState.pointInRoot)
        }
    }
    val dropTargetFolderId = dropTarget.hoveredFolderId
    LaunchedEffect(dragState.isDragging) {
        if (dragState.isDragging) dragState.autoScroll(listState, density)
    }
    val s = LocalStrings.current
    val filteredUnfoldered = remember(state.folders, state.unfolderedChannels) {
        val folderedLogins = state.folders.flatMapTo(HashSet()) { folder -> folder.channels.map { it.login } }
        state.unfolderedChannels.filter { it.login !in folderedLogins }
    }
    val glowEnabled = LocalWallpaperController.current.state.glowEffectsEnabled
    val floating = glowEnabled && isWideScreen
    val sidebarShape = when {
        floating -> RoundedCornerShape(18.dp)
        glowEnabled -> RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
        else -> RoundedCornerShape(0.dp)
    }
    val sidebarDivider = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
    val tiled = isWideScreen
    Column(
        modifier = Modifier
            .then(if (tiled) Modifier.fillMaxWidth() else Modifier.width(256.dp))
            .fillMaxHeight()
            .then(
                when {
                    floating && tiled -> Modifier.padding(start = 4.dp, top = 4.dp, bottom = 4.dp)
                    floating -> Modifier.padding(4.dp)
                    else -> Modifier
                }
            )
            .then(
                if (floating) Modifier.shadow(
                    16.dp,
                    sidebarShape,
                    ambientColor = extra.shadowColor,
                    spotColor = extra.elevatedShadow
                ) else Modifier
            )
            .then(if (tiled) Modifier else Modifier.clip(sidebarShape))
            .background(
                if (glowEnabled) extra.sidebarSurface
                else MaterialTheme.colorScheme.surfaceContainerLow
            )
            .then(
                when {
                    glowEnabled -> Modifier.border(1.dp, extra.glassBorder, sidebarShape)
                    tiled -> Modifier
                    else -> Modifier.drawBehind {
                        drawRect(
                            color = sidebarDivider,
                            topLeft = Offset(size.width - 1.dp.toPx(), 0f),
                            size = androidx.compose.ui.geometry.Size(1.dp.toPx(), size.height)
                        )
                    }
                }
            )
            .windowInsetsPadding(
                WindowInsets.safeDrawing.only(WindowInsetsSides.Top)
                    .union(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
            )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Chatone",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            if (isWideScreen) {
                ChatoneIconButton(
                    onClick = { onEvent(MainEvent.ToggleSidebarCollapsed) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        if (state.sidebarCollapsed)
                            Lucide.ChevronLeft
                        else Lucide.ChevronRight,
                        contentDescription = LocalStrings.current.mainCollapseSidebar,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else {
                ChatoneIconButton(
                    onClick = { onEvent(MainEvent.CloseSidebar) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Lucide.X,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        state.selectedAccount?.let { account ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (account.profileImageUrl.isNotEmpty()) {
                    AsyncImage(
                        model = account.profileImageUrl,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp).clip(CircleShape)
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = account.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (state.activeChannelLogin != null) {
                        Text(
                            text = "#${state.activeChannelLogin}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
                val aiControllerAcct = koinInject<AiAssistantController>()
                ChatoneIconButton(
                    onClick = { aiControllerAcct.open() },
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(
                        Lucide.Sparkles,
                        contentDescription = LocalStrings.current.aiAssistantContentDescription,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                if (state.activeChannelLogin != null) {
                    val isBroadcaster = state.activeChannelLogin.equals(account.login, ignoreCase = true)
                    val isModForActive = state.activeChatChannelId.isNotEmpty() &&
                            state.activeChatChannelId in state.moderatedChannelIds
                    Box {
                        var showChannelMenu by remember { mutableStateOf(false) }
                        ChatoneIconButton(
                            onClick = { showChannelMenu = true },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                Lucide.EllipsisVertical,
                                contentDescription = LocalStrings.current.mainChannelOptions,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        val activeLogin = state.activeChannelLogin ?: ""
                        val menuModel = remember(activeLogin, isBroadcaster, isModForActive) {
                            ChannelMenuModel(
                                channelLogin = activeLogin,
                                canSeeViewers = isBroadcaster || isModForActive,
                                actions = ChannelMenuActions(
                                    onShowViewers = { onEvent(MainEvent.ShowChattersPanel) },
                                    onSetMentionsMuted = { muted ->
                                        onEvent(
                                            if (muted) MainEvent.MuteMentionsChannel(activeLogin)
                                            else MainEvent.UnmuteMentionsChannel(activeLogin)
                                        )
                                    }
                                )
                            )
                        }
                        ChannelOptionsMenu(
                            expanded = showChannelMenu,
                            onDismissRequest = { showChannelMenu = false },
                            model = menuModel
                        )
                    }
                }
                Box(
                    modifier = Modifier.size(8.dp).clip(CircleShape)
                        .background(if (state.isConnected) ChatoneTheme.extraColors.connected else MaterialTheme.colorScheme.error)
                )
            }
        } ?: run {
            if (state.isGuest) {
                TextButton(
                    onClick = { onEvent(MainEvent.NavigateToAuth) },
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Icon(
                        Lucide.User,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(LocalStrings.current.mainLoginToTwitch)
                }
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp),
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        )

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .sidebarDragTracking(dragState, listState) { dropped, point ->
                    applySidebarDrop(
                        target = dropIndex.resolve(point),
                        draggedLogin = dropped.login,
                        sourceFolderId = dropped.folderId,
                        folders = state.folders,
                        unfoldered = filteredUnfoldered,
                        onEvent = onEvent
                    )
                },
            contentPadding = PaddingValues(vertical = 4.dp),
            verticalArrangement = Arrangement.Top
        ) {
            if (onOpenBrowse != null) {
                item(key = "browse_entry", contentType = "browse_entry") {
                    BrowseEntryItem(strings = s.explore, onClick = onOpenBrowse)
                }
            }
            itemsIndexed(
                state.folders,
                key = { _: Int, f: ChannelFolder -> "folder_${f.id}" },
                contentType = { _, _ -> "folder" }
            ) { folderIndex: Int, folder: ChannelFolder ->
                FolderItem(
                    folder = folder,
                    activeChannelLogin = state.activeChannelLogin,
                    allFolders = state.folders,
                    unfolderedChannels = filteredUnfoldered,
                    onToggle = { onEvent(MainEvent.ToggleFolder(folder.id)) },
                    onChannelSelect = { login -> onEvent(MainEvent.SelectChannel(login)) },
                    onChannelClose = { login -> onEvent(MainEvent.CloseChannel(login)) },
                    onMoveChannel = { login, targetFolderId ->
                        onEvent(MainEvent.MoveChannelToFolder(login, targetFolderId))
                    },
                    onDelete = { onEvent(MainEvent.DeleteFolder(folder.id)) },
                    onEdit = { onEvent(MainEvent.ShowEditFolderDialog(folder.id)) },
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .folderDropZone(dropIndex, folder.id),
                    isDropTarget = dropTargetFolderId == folder.id
                )

                if (folder.isExpanded) {
                    folder.channels.forEach { channel ->
                        ChannelItemWithDrag(
                            channel = channel,
                            isActive = channel.login == state.activeChannelLogin,
                            folders = state.folders,
                            currentFolderId = folder.id,
                            onClick = { onEvent(MainEvent.SelectChannel(channel.login)) },
                            onClose = { onEvent(MainEvent.CloseChannel(channel.login)) },
                            onMoveToFolder = { folderId ->
                                onEvent(MainEvent.MoveChannelToFolder(channel.login, folderId))
                            },
                            onCreateFolder = { onEvent(MainEvent.ShowCreateFolderDialog(channel.login)) },
                            liveNotifyEnabled = channel.login.lowercase() in state.liveNotifyChannels,
                            onToggleLiveNotify = { onEvent(MainEvent.ToggleLiveNotify(channel.login)) },
                            dragState = dragState,
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .channelDropZone(dropIndex, channel.login, folder.id)
                        )
                    }
                }
            }

            item(key = "unfoldered_header", contentType = "section_header") {
                val isEjectTarget = dragState.source?.folderId != null &&
                        dropTarget == SidebarDropTarget.Unfoldered
                Text(
                    LocalStrings.current.mainChannelsHeader,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isEjectTarget) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isEjectTarget) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                            else Color.Transparent
                        )
                        .unfolderedDropZone(dropIndex)
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                )
            }

            itemsIndexed(
                filteredUnfoldered,
                key = { _: Int, c: ChannelTab -> "channel_${c.login}" },
                contentType = { _, _ -> "channel" }
            ) { index: Int, channel: ChannelTab ->
                ChannelItemWithDrag(
                    channel = channel,
                    isActive = channel.login == state.activeChannelLogin,
                    folders = state.folders,
                    currentFolderId = null,
                    onClick = { onEvent(MainEvent.SelectChannel(channel.login)) },
                    onClose = { onEvent(MainEvent.CloseChannel(channel.login)) },
                    onMoveToFolder = { folderId ->
                        onEvent(MainEvent.MoveChannelToFolder(channel.login, folderId))
                    },
                    onCreateFolder = { onEvent(MainEvent.ShowCreateFolderDialog(channel.login)) },
                    liveNotifyEnabled = channel.login.lowercase() in state.liveNotifyChannels,
                    onToggleLiveNotify = { onEvent(MainEvent.ToggleLiveNotify(channel.login)) },
                    dragState = dragState,
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .channelDropZone(dropIndex, channel.login, null)
                )
            }

            if (state.recentChannels.isNotEmpty()) {
                item(key = "recent_header", contentType = "section_header") {
                    RecentChannelsHeader(strings = s.explore, modifier = Modifier.animateItem())
                }
                items(state.recentChannels, key = { "recent_${it.login}" }, contentType = { "recent" }) { channel ->
                    RecentChannelItem(
                        channel = channel,
                        isActive = channel.login == state.activeChannelLogin,
                        strings = s.explore,
                        onClick = { onEvent(MainEvent.SelectChannel(channel.login)) },
                        onKeep = { onEvent(MainEvent.KeepRecentChannel(channel.login)) },
                        onForget = { onEvent(MainEvent.ForgetRecentChannel(channel.login)) },
                        modifier = Modifier.animateItem()
                    )
                }
            }

            if (state.monitorTabs.isNotEmpty()) {
                item(key = "monitor_header", contentType = "section_header") {
                    Text(
                        LocalStrings.current.openChannelSpecialHeader,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                items(state.monitorTabs.size, key = { "monitor_${state.monitorTabs[it]}" }, contentType = { "monitor" }) { i ->
                    val login = state.monitorTabs[i]
                    MonitorTabSidebarItem(
                        login = login,
                        isActive = login == state.activeChannelLogin,
                        onClick = { onEvent(MainEvent.SelectChannel(login)) },
                        onClose = { onEvent(MainEvent.CloseMonitorTab(login)) },
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }

            item(key = "bottom_spacer", contentType = "spacer") {
                Spacer(Modifier.height(12.dp))
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {

            GradientButton(
                text = LocalStrings.current.sidebarAddChannelButton,
                onClick = { onEvent(MainEvent.ShowAddChannelDialog) },
                modifier = Modifier.weight(1f),
                gradientColors = listOf(
                    MaterialTheme.colorScheme.primary,
                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.85f)
                )
            )
            Spacer(Modifier.width(8.dp))

            GradientButton(
                text = LocalStrings.current.sidebarAddFolderButton,
                onClick = { onEvent(MainEvent.ShowCreateFolderDialog()) },
                modifier = Modifier.weight(1f),
                gradientColors = listOf(
                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.85f),
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                )
            )
        }

        ChatoneIconRail(
            actions = listOf(
                RailAction(
                    icon = Lucide.AtSign,
                    label = s.panelMentionsTitle,
                    badge = state.unreadMentionsCount,
                    onClick = { onEvent(MainEvent.ToggleMentionsFeed) }
                ),
                RailAction(
                    icon = Lucide.MessageCircle,
                    label = s.chatWhisperTab,
                    badge = state.totalUnreadWhispers,
                    onClick = { onEvent(MainEvent.ToggleWhisperPanel) }
                ),
                RailAction(
                    icon = Lucide.Settings,
                    label = s.settingsTitle,
                    onClick = { onEvent(MainEvent.ShowSettings) }
                )
            ),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
        Spacer(Modifier.height(4.dp))
    }

    val draggedLogin = dragState.source?.login
    if (dragState.isDragging && draggedLogin != null) {
        Box(
            modifier = Modifier
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints)
                    layout(placeable.width, placeable.height) {
                        val origin = coordinates?.positionInRoot() ?: Offset.Zero
                        val local = dragState.pointInRoot - origin
                        placeable.place(local.x.toInt(), local.y.toInt())
                    }
                }
                .zIndex(1000f)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.primaryContainer)
                .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .shadow(16.dp, RoundedCornerShape(8.dp))
        ) {
            val ch = filteredUnfoldered.find { it.login == draggedLogin }
                ?: state.folders.flatMap { it.channels }.find { it.login == draggedLogin }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (ch?.profileImageUrl?.isNotEmpty() == true) {
                    AsyncImage(
                        model = ch.profileImageUrl,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp).clip(CircleShape)
                    )
                    Spacer(Modifier.width(6.dp))
                }
                Text(
                    text = ch?.displayName ?: draggedLogin,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun FolderItem(
    folder: ChannelFolder,
    activeChannelLogin: String?,
    allFolders: List<ChannelFolder>,
    unfolderedChannels: List<ChannelTab> = emptyList(),
    onToggle: () -> Unit,
    onChannelSelect: (String) -> Unit,
    onChannelClose: (String) -> Unit,
    onMoveChannel: (String, String?) -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
    isDropTarget: Boolean = false
) {
    var showFolderMenu by remember { mutableStateOf(false) }
    var showAddToFolderMenu by remember { mutableStateOf(false) }

    Column {
        Box {
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 2.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        when {
                            isDropTarget -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            else -> MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.28f)
                        }
                    )
                    .combinedClickable(onClick = onToggle, onLongClick = { showFolderMenu = true })
                    .padding(horizontal = 8.dp, vertical = 6.dp)
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) {
                                    showFolderMenu = true
                                }
                            }
                        }
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (folder.isExpanded) Lucide.ChevronDown else Lucide.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    if (folder.isExpanded) Lucide.FolderOpen else Lucide.Folder,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = parseFolderColor(folder.color)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = folder.name,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${folder.channels.size}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box {
                    ChatoneIconButton(
                        onClick = { showAddToFolderMenu = true },
                        modifier = Modifier.size(22.dp)
                    ) {
                        Icon(
                            Lucide.Plus,
                            contentDescription = LocalStrings.current.mainAddChannelToFolder,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                        )
                    }
                    ChatoneDropdownMenu(
                        expanded = showAddToFolderMenu,
                        onDismissRequest = { showAddToFolderMenu = false }) {
                        if (unfolderedChannels.isEmpty()) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        LocalStrings.current.mainNoChannelsToAdd,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                onClick = { showAddToFolderMenu = false },
                                enabled = false
                            )
                        } else {
                            unfolderedChannels.forEach { ch: ChannelTab ->
                                DropdownMenuItem(
                                    text = { Text("#${ch.displayName}") },
                                    onClick = {
                                        showAddToFolderMenu = false
                                        onMoveChannel(ch.login, folder.id)
                                    },
                                    leadingIcon = {
                                        Text(
                                            "#",
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
            ChatoneDropdownMenu(
                expanded = showFolderMenu,
                onDismissRequest = { showFolderMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text(LocalStrings.current.mainEditFolder) },
                    onClick = { showFolderMenu = false; onEdit() },
                    leadingIcon = {
                        Icon(
                            Lucide.Pencil,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = parseFolderColor(folder.color)
                        )
                    }
                )
                DropdownMenuItem(
                    text = { Text(LocalStrings.current.mainDeleteFolder) },
                    onClick = { showFolderMenu = false; onDelete() },
                    leadingIcon = {
                        Icon(
                            Lucide.Trash2,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
            }
        }
    }
}

internal fun parseFolderColor(hex: String): Color {
    return try {
        val cleaned = hex.removePrefix("#")
        val colorInt = cleaned.toLong(16)
        Color(
            red = ((colorInt shr 16) and 0xFF) / 255f,
            green = ((colorInt shr 8) and 0xFF) / 255f,
            blue = (colorInt and 0xFF) / 255f
        )
    } catch (e: Exception) {
        ChatoneColors.Violet400
    }
}
