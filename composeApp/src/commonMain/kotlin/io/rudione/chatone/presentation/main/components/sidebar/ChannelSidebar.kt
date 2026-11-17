package io.rudione.chatone.presentation.main.components.sidebar


import io.rudione.chatone.presentation.components.motion.IconMotion
import io.rudione.chatone.util.system.isDesktopPlatform
import chatone.composeapp.generated.resources.lab_title
import io.rudione.chatone.icons.lucide.Activity
import io.rudione.chatone.icons.lucide.Download
import chatone.composeapp.generated.resources.update_now
import androidx.compose.ui.platform.LocalUriHandler
import io.rudione.chatone.util.system.appStoreUrl
import io.rudione.chatone.util.system.rememberStoreUpdateAvailable
import io.rudione.chatone.util.BuildConfig
import io.rudione.chatone.presentation.components.AnimatedMenuIcon
import io.rudione.chatone.presentation.components.LocalSidebarExpanded
import io.rudione.chatone.presentation.components.ChatConnectionIndicator
import chatone.composeapp.generated.resources.ui_close
import chatone.composeapp.generated.resources.ai_assistant_content_description
import chatone.composeapp.generated.resources.channel_page_my_profile
import chatone.composeapp.generated.resources.chat_whisper_tab
import chatone.composeapp.generated.resources.main_add_channel_to_folder
import chatone.composeapp.generated.resources.main_channel_options
import chatone.composeapp.generated.resources.main_channels_header
import chatone.composeapp.generated.resources.main_collapse_sidebar
import chatone.composeapp.generated.resources.main_delete_folder
import chatone.composeapp.generated.resources.main_edit_folder
import chatone.composeapp.generated.resources.main_login_to_twitch
import chatone.composeapp.generated.resources.main_no_channels_to_add
import chatone.composeapp.generated.resources.open_channel_special_header
import chatone.composeapp.generated.resources.panel_mentions_title
import chatone.composeapp.generated.resources.settings_title
import chatone.composeapp.generated.resources.sidebar_add_channel_button
import chatone.composeapp.generated.resources.sidebar_add_folder_button
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.points_streak_description
import io.rudione.chatone.presentation.points.StreakBadge
import io.rudione.chatone.presentation.points.rememberWatchStreak
import org.jetbrains.compose.resources.stringResource
import androidx.compose.foundation.clickable
import io.rudione.chatone.presentation.profile.PageNavigator
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
import io.rudione.chatone.presentation.components.TonalButton
import io.rudione.chatone.presentation.theme.ChatoneColors
import io.rudione.chatone.presentation.theme.ChatoneTheme
import io.rudione.chatone.presentation.theme.LocalWallpaperController
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
import androidx.compose.ui.geometry.Size

@OptIn(ExperimentalComposeUiApi::class, ExperimentalLayoutApi::class)
@Composable
internal fun ChannelSidebar(
    state: MainState,
    onEvent: (MainEvent) -> Unit,
    isWideScreen: Boolean = false,
    onOpenBrowse: (() -> Unit)? = null
) {
    val pages = koinInject<PageNavigator>()
    val extra = ChatoneTheme.extraColors

    val dropIndex = remember { SidebarDropIndex() }
    val dragState = remember { SidebarDragState() }
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    val compactBrowseThreshold = with(density) { 48.dp.roundToPx() }
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
    val filteredUnfoldered = remember(state.folders, state.unfolderedChannels) {
        val folderedLogins =
            state.folders.flatMapTo(HashSet()) { folder -> folder.channels.map { it.login } }
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
    val aiControllerAcct = koinInject<AiAssistantController>()
    Column(
        modifier = Modifier
            .then(if (isWideScreen) Modifier.fillMaxWidth() else Modifier.width(256.dp))
            .fillMaxHeight()
            .then(
                when {
                    floating && isWideScreen -> Modifier.padding(
                        start = 4.dp,
                        top = 4.dp,
                        bottom = 4.dp
                    )

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
            .then(if (isWideScreen) Modifier else Modifier.clip(sidebarShape))
            .background(
                if (glowEnabled) extra.sidebarSurface
                else MaterialTheme.colorScheme.surfaceContainerLow
            )
            .then(
                when {
                    glowEnabled -> Modifier.border(1.dp, extra.glassBorder, sidebarShape)
                    isWideScreen -> Modifier
                    else -> Modifier.drawBehind {
                        drawRect(
                            color = sidebarDivider,
                            topLeft = Offset(size.width - 1.dp.toPx(), 0f),
                            size = Size(1.dp.toPx(), size.height)
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
            Text(
                text = "v${BuildConfig.VERSION}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            val storeUrl = appStoreUrl
            val updateAvailable = rememberStoreUpdateAvailable()
            val uriHandler = LocalUriHandler.current
            if (storeUrl != null && updateAvailable) {
                ChatoneIconButton(
                    onClick = { uriHandler.openUri(storeUrl) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Lucide.Download,
                        contentDescription = stringResource(Res.string.update_now),
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (isWideScreen) {
                ChatoneIconButton(
                    onClick = { onEvent(MainEvent.ToggleSidebarCollapsed) },
                    modifier = Modifier.size(32.dp)
                ) {
                    AnimatedMenuIcon(
                        expanded = !state.sidebarCollapsed,
                        contentDescription = stringResource(Res.string.main_collapse_sidebar),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else {
                ChatoneIconButton(
                    onClick = { onEvent(MainEvent.CloseSidebar) },
                    modifier = Modifier.size(32.dp)
                ) {
                    AnimatedMenuIcon(
                        expanded = LocalSidebarExpanded.current,
                        contentDescription = stringResource(Res.string.ui_close),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        state.selectedAccount?.let { account ->
            val profiles: PageNavigator = koinInject()
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
                        .clickable(enabled = profiles.isSupported) {
                            onEvent(MainEvent.CloseSidebar)
                            profiles.openChannel(account.login)
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (account.profileImageUrl.isNotEmpty()) {
                        AsyncImage(
                            model = account.profileImageUrl,
                            contentDescription = stringResource(Res.string.channel_page_my_profile),
                            modifier = Modifier.size(24.dp).clip(CircleShape)
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        val streak by rememberWatchStreak(state.activeChannelLogin)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                            itemVerticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = account.displayName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            val activeLogin = state.activeChannelLogin
                            streak?.takeIf { activeLogin != null && !it.countedThisStream }?.let { current ->
                                StreakBadge(
                                    count = current.current,
                                    description = stringResource(
                                        Res.string.points_streak_description,
                                        activeLogin.orEmpty(),
                                        current.current
                                    )
                                )
                            }
                        }
                        if (state.activeChannelLogin != null) {
                            Text(
                                text = "#${state.activeChannelLogin}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
                ChatoneIconButton(
                    onClick = { aiControllerAcct.open() },
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(
                        Lucide.Sparkles,
                        contentDescription = stringResource(Res.string.ai_assistant_content_description),
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (state.activeChannelLogin != null) {
                    val isBroadcaster =
                        state.activeChannelLogin.equals(account.login, ignoreCase = true)
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
                                contentDescription = stringResource(Res.string.main_channel_options),
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
                ChatConnectionIndicator(
                    state = state.connectionState,
                    onReconnect = { onEvent(MainEvent.ReconnectChat) }
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
                    Text(stringResource(Res.string.main_login_to_twitch))
                }
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp),
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        )

        if (onOpenBrowse != null) {
            val compactBrowse by remember(compactBrowseThreshold) {
                derivedStateOf {
                    listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > compactBrowseThreshold
                }
            }
            BrowseEntryItem(onClick = onOpenBrowse, compact = compactBrowse)
        }

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
                            liveNotifyEnabled = channel.login.lowercase() in state.liveNotifyChannels,
                            actions = rememberChannelMenuActions(channel.login, onEvent),
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
                    stringResource(Res.string.main_channels_header),
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
                    liveNotifyEnabled = channel.login.lowercase() in state.liveNotifyChannels,
                    actions = rememberChannelMenuActions(channel.login, onEvent),
                    dragState = dragState,
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .channelDropZone(dropIndex, channel.login, null)
                )
            }

            if (state.recentChannels.isNotEmpty()) {
                item(key = "recent_header", contentType = "section_header") {
                    RecentChannelsHeader(modifier = Modifier.animateItem())
                }
                items(
                    state.recentChannels,
                    key = { "recent_${it.login}" },
                    contentType = { "recent" }) { channel ->
                    RecentChannelItem(
                        channel = channel,
                        isActive = channel.login == state.activeChannelLogin,
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
                        stringResource(Res.string.open_channel_special_header),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                items(
                    state.monitorTabs.size,
                    key = { "monitor_${state.monitorTabs[it]}" },
                    contentType = { "monitor" }) { i ->
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

            TonalButton(
                text = stringResource(Res.string.sidebar_add_channel_button),
                onClick = { onEvent(MainEvent.ShowAddChannelDialog) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))

            TonalButton(
                text = stringResource(Res.string.sidebar_add_folder_button),
                onClick = { onEvent(MainEvent.ShowCreateFolderDialog()) },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.weight(1f)
            )
        }

        ChatoneIconRail(
            actions = listOfNotNull(
                RailAction(
                    icon = Lucide.AtSign,
                    label = stringResource(Res.string.panel_mentions_title),
                    badge = state.unreadMentionsCount,
                    motion = IconMotion.Mention,
                    pulse = state.unreadMentionsCount,
                    onClick = { onEvent(MainEvent.ToggleMentionsFeed) }
                ),
                RailAction(
                    icon = Lucide.MessageCircle,
                    label = stringResource(Res.string.chat_whisper_tab),
                    badge = state.totalUnreadWhispers,
                    motion = IconMotion.Bubble,
                    pulse = state.totalUnreadWhispers,
                    onClick = { onEvent(MainEvent.ToggleWhisperPanel) }
                ),
                if (!isDesktopPlatform) RailAction(
                    icon = Lucide.Activity,
                    label = stringResource(Res.string.lab_title),
                    motion = IconMotion.Tilt,
                    onClick = { onEvent(MainEvent.CloseSidebar); pages.openLaboratory() }
                ) else null,
                RailAction(
                    icon = Lucide.Settings,
                    label = stringResource(Res.string.settings_title),
                    motion = IconMotion.Gear,
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
                            contentDescription = stringResource(Res.string.main_add_channel_to_folder),
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                    ChatoneDropdownMenu(
                        expanded = showAddToFolderMenu,
                        onDismissRequest = { showAddToFolderMenu = false }) {
                        if (unfolderedChannels.isEmpty()) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        stringResource(Res.string.main_no_channels_to_add),
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
                    text = { Text(stringResource(Res.string.main_edit_folder)) },
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
                    text = { Text(stringResource(Res.string.main_delete_folder)) },
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

fun parseFolderColor(hex: String): Color {
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
