package io.rudione.chatone.presentation.main.components.sidebar

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.consumeAllChanges
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.zIndex
import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.folder_outline
import coil3.compose.AsyncImage
import io.rudione.chatone.domain.model.Channel
import io.rudione.chatone.presentation.chat.ChatScreen
import io.rudione.chatone.presentation.components.GlowSurface
import io.rudione.chatone.presentation.components.GradientButton
import io.rudione.chatone.presentation.components.LiquidGlassDropdownItem
import io.rudione.chatone.presentation.components.LiquidGlassSurface
import io.rudione.chatone.presentation.chat.pauseHotkeyMatches
import io.rudione.chatone.presentation.settings.SettingsEvent
import io.rudione.chatone.presentation.settings.SettingsScreen
import io.rudione.chatone.presentation.settings.SettingsState
import io.rudione.chatone.presentation.settings.SettingsViewModel
import io.rudione.chatone.util.system.GlobalKeyDispatcher
import io.rudione.chatone.util.system.HotkeyAction
import io.rudione.chatone.util.system.comboFor
import io.rudione.chatone.presentation.theme.ChatBackgroundLayer
import io.rudione.chatone.presentation.theme.ChatoneColors
import io.rudione.chatone.presentation.theme.ChatoneTheme
import io.rudione.chatone.presentation.theme.LocalWallpaperController
import io.rudione.chatone.presentation.theme.WallpaperGlowEdge
import io.rudione.chatone.presentation.theme.panelBlur
import io.rudione.chatone.presentation.theme.topBarBackgroundColor
import io.rudione.chatone.util.media.WallpaperLoader
import io.rudione.chatone.util.system.handleHover
import io.rudione.chatone.presentation.theme.i18n.LocalStrings
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.hypot
import io.rudione.chatone.presentation.chat.components.ChattersPanel
import io.rudione.chatone.presentation.main.components.MentionsFeed
import io.rudione.chatone.presentation.main.components.MentionTabsBar
import io.rudione.chatone.presentation.main.components.MentionToast
import io.rudione.chatone.presentation.automod.DetachedAutomodWindow
import io.rudione.chatone.presentation.settings.DetachedSettingsWindow
import io.rudione.chatone.presentation.settings.SettingsEffect
import io.rudione.chatone.presentation.chat.multichat.MultiChatRootSetup
import io.rudione.chatone.presentation.account.AccountAutoConnectEffect
import io.rudione.chatone.presentation.account.AccountManager
import io.rudione.chatone.data.repository.AuthRepository
import io.rudione.chatone.data.repository.MentionMuteRepository
import io.rudione.chatone.data.repository.MultiAccountConnectionRegistry
import io.rudione.chatone.presentation.chat.ChatViewModel
import io.rudione.chatone.presentation.chat.multichat.MainScreenChatRouter
import io.rudione.chatone.presentation.main.MainState
import io.rudione.chatone.presentation.main.MainEvent
import io.rudione.chatone.presentation.main.ChannelFolder
import io.rudione.chatone.presentation.main.ChannelTab
import io.rudione.chatone.presentation.components.ChatoneIconButton
import io.rudione.chatone.presentation.components.ChatoneDropdownMenu

internal data class ItemBounds(val id: String, val rect: Rect)

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun ChannelSidebar(
    state: MainState,
    onEvent: (MainEvent) -> Unit,
    isWideScreen: Boolean = false,
    mentionMuteRepository: MentionMuteRepository? = null
) {
    val extra = ChatoneTheme.extraColors
    val density = LocalDensity.current

    var draggedChannelLogin by remember { mutableStateOf<String?>(null) }
    var draggedChannelSourceFolderId by remember { mutableStateOf<String?>(null) }
    var dragOffsetPx by remember { mutableStateOf(Offset.Zero) }
    var dragStartOffsetPx by remember { mutableStateOf(Offset.Zero) }
    var isDragging by remember { mutableStateOf(false) }
    var dropTargetFolderId by remember { mutableStateOf<String?>(null) }
    var dragStartIndex by remember { mutableStateOf<Int?>(null) }
    var dragOverIndex by remember { mutableStateOf<Int?>(null) }
    var sidebarTopLeftInRoot by remember { mutableStateOf(Offset.Zero) }

    val folderBounds = remember { mutableStateListOf<ItemBounds>() }
    val channelRectMap = remember { mutableStateMapOf<String, Rect>() }
    val channelBounds = remember { mutableStateListOf<ItemBounds>() }
    val s = LocalStrings.current
    val folderedLogins = state.folders.flatMap { it.channels }.map { it.login }.toSet()
    val filteredUnfoldered = state.unfolderedChannels.filter { it.login !in folderedLogins }
    val glowEnabled = LocalWallpaperController.current.state.glowEffectsEnabled
    val floating = glowEnabled && isWideScreen
    val sidebarShape = when {
        floating -> RoundedCornerShape(18.dp)
        glowEnabled -> RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
        else -> RoundedCornerShape(0.dp)
    }
    val sidebarDivider = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
    Column(
        modifier = Modifier
            .width(256.dp)
            .fillMaxHeight()
            .then(if (floating) Modifier.padding(8.dp) else Modifier)
            .then(
                if (floating) Modifier.shadow(
                    16.dp,
                    sidebarShape,
                    ambientColor = extra.shadowColor,
                    spotColor = extra.elevatedShadow
                ) else Modifier
            )
            .clip(sidebarShape)
            .background(
                if (glowEnabled) extra.sidebarSurface
                else MaterialTheme.colorScheme.surfaceContainerLow
            )
            .then(
                if (glowEnabled) Modifier.border(1.dp, extra.glassBorder, sidebarShape)
                else Modifier.drawBehind {
                    drawRect(
                        color = sidebarDivider,
                        topLeft = Offset(size.width - 1.dp.toPx(), 0f),
                        size = androidx.compose.ui.geometry.Size(1.dp.toPx(), size.height)
                    )
                }
            )
            .onGloballyPositioned { sidebarTopLeftInRoot = it.positionInRoot() }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
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
                            Icons.AutoMirrored.Filled.KeyboardArrowLeft
                        else Icons.AutoMirrored.Default.KeyboardArrowRight,
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
                        Icons.Filled.Close,
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
                val aiControllerAcct = org.koin.compose.koinInject<io.rudione.chatone.data.repository.AiAssistantController>()
                ChatoneIconButton(
                    onClick = { aiControllerAcct.open() },
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(
                        Icons.Filled.AutoAwesome,
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
                                Icons.Filled.MoreVert,
                                contentDescription = LocalStrings.current.mainChannelOptions,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        val activeLogin = state.activeChannelLogin ?: ""
                        val isChannelMuted = remember(activeLogin) {
                            mentionMuteRepository?.isChannelMuted(activeLogin) ?: false
                        }
                        var channelMuted by remember(activeLogin) { mutableStateOf(isChannelMuted) }
                        ChatoneDropdownMenu(
                            expanded = showChannelMenu,
                            onDismissRequest = { showChannelMenu = false }) {
                            if (isBroadcaster || isModForActive) {
                                DropdownMenuItem(
                                    text = { Text(LocalStrings.current.mainViewersList) },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Filled.Person,
                                            null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    onClick = {
                                        showChannelMenu = false; onEvent(MainEvent.ShowChattersPanel)
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        if (channelMuted)
                                            LocalStrings.current.unmuteMentionsForChannel
                                        else
                                            LocalStrings.current.muteMentionsForChannel
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        if (channelMuted) Icons.Filled.Notifications
                                        else Icons.Outlined.NotificationsOff,
                                        null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                onClick = {
                                    showChannelMenu = false
                                    if (channelMuted) {
                                        onEvent(MainEvent.UnmuteMentionsChannel(activeLogin))
                                        channelMuted = false
                                    } else {
                                        onEvent(MainEvent.MuteMentionsChannel(activeLogin))
                                        channelMuted = true
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(LocalStrings.current.mainChannelBadgesBits) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Filled.Star,
                                        null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                onClick = {
                                    showChannelMenu = false
                                    io.rudione.chatone.util.system.ChannelPanelRequestBus
                                        .requestOpenPointsBitsPanel(activeLogin)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(LocalStrings.current.mainTogglePinnedMessage) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Filled.PushPin,
                                        null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                onClick = {
                                    showChannelMenu = false
                                    io.rudione.chatone.util.system.ChannelPanelRequestBus
                                        .requestToggleHidePin(activeLogin)
                                }
                            )
                        }
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
                        Icons.Filled.Person,
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
            modifier = Modifier
                .weight(1f)
                .pointerInput(isDragging) {
                    if (isDragging) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                event.changes.forEach { it.consume() }
                            }
                        }
                    }
                },
            contentPadding = PaddingValues(vertical = 4.dp),
            verticalArrangement = Arrangement.Top
        ) {
            itemsIndexed(
                state.folders,
                key = { _: Int, f: ChannelFolder -> "folder_${f.id}" }
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
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .onGloballyPositioned { coords ->
                            val rect = coords.boundsInRoot()
                            folderBounds.removeAll { it.id == folder.id }
                            folderBounds.add(ItemBounds(folder.id, rect))
                        },
                    isDropTarget = dropTargetFolderId == folder.id && draggedChannelLogin != null
                )

                if (folder.isExpanded) {
                    folder.channels.forEachIndexed { channelIndex, channel ->
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
                            liveNotifyEnabled = channel.login.lowercase() in state.liveNotifyChannels,
                            onToggleLiveNotify = { onEvent(MainEvent.ToggleLiveNotify(channel.login)) },
                            index = channelIndex,
                            onDragStart = { rootPos ->
                                draggedChannelLogin = channel.login
                                draggedChannelSourceFolderId = folder.id
                                dragStartOffsetPx = rootPos - sidebarTopLeftInRoot
                                dragOffsetPx = Offset.Zero
                                isDragging = true
                                dragStartIndex = channelIndex
                                dragOverIndex = null
                            },
                            onDrag = { delta ->
                                dragOffsetPx += delta
                                val currentPos = dragStartOffsetPx + dragOffsetPx

                                dropTargetFolderId = folderBounds.find { it.rect.contains(currentPos) }?.id

                                val hoveredLogin = channelRectMap.entries
                                    .firstOrNull { (_, rect) -> rect.contains(currentPos) }?.key
                                dragOverIndex = if (hoveredLogin != null) {
                                    folder.channels.indexOfFirst { it.login == hoveredLogin }
                                        .takeIf { it >= 0 }
                                } else null
                            },
                            onDragEnd = {
                                val currentPos = dragStartOffsetPx + dragOffsetPx
                                val hitFolder = folderBounds.find { it.rect.contains(currentPos) }
                                when {
                                    hitFolder != null && hitFolder.id != folder.id && draggedChannelLogin != null -> {
                                        onEvent(MainEvent.MoveChannelToFolder(draggedChannelLogin!!, hitFolder.id))
                                    }
                                    dragOverIndex != null && dragStartIndex != null && dragOverIndex != dragStartIndex -> {
                                        onEvent(MainEvent.ReorderFolderChannels(folder.id, dragStartIndex!!, dragOverIndex!!))
                                    }
                                    hitFolder == null && dragOverIndex == null && draggedChannelLogin != null -> {
                                        onEvent(MainEvent.MoveChannelToFolder(draggedChannelLogin!!, null))
                                    }
                                }
                                draggedChannelLogin = null
                                draggedChannelSourceFolderId = null
                                dropTargetFolderId = null
                                dragOffsetPx = Offset.Zero
                                dragStartIndex = null
                                dragOverIndex = null
                                isDragging = false
                            },
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .onGloballyPositioned { coords ->
                                    channelRectMap[channel.login] = coords.boundsInRoot()
                                }
                        )
                    }
                }
            }

            if (filteredUnfoldered.isNotEmpty()) {
                item(key = "unfoldered_header") {
                    Text(
                        LocalStrings.current.mainChannelsHeader,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp,
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }

            itemsIndexed(
                filteredUnfoldered,
                key = { _: Int, c: ChannelTab -> "channel_${c.login}" }
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
                    liveNotifyEnabled = channel.login.lowercase() in state.liveNotifyChannels,
                    onToggleLiveNotify = { onEvent(MainEvent.ToggleLiveNotify(channel.login)) },
                    index = index,
                    onDragStart = { rootPos ->
                        draggedChannelLogin = channel.login
                        dragStartOffsetPx = rootPos - sidebarTopLeftInRoot
                        dragOffsetPx = Offset.Zero
                        isDragging = true
                        dragStartIndex =
                            filteredUnfoldered.indexOfFirst { it.login == channel.login }
                        dragOverIndex = null
                    },
                    onDrag = { delta ->
                        dragOffsetPx += delta
                        val currentPos = dragStartOffsetPx + dragOffsetPx

                        dropTargetFolderId = folderBounds.find { it.rect.contains(currentPos) }?.id

                        val hoveredLogin = channelRectMap.entries
                            .firstOrNull { (_, rect) -> rect.contains(currentPos) }?.key
                        dragOverIndex = if (hoveredLogin != null) {
                            filteredUnfoldered.indexOfFirst { it.login == hoveredLogin }
                                .takeIf { it >= 0 }
                        } else null
                    },
                    onDragEnd = {
                        val currentPos = dragStartOffsetPx + dragOffsetPx
                        val hitFolder = folderBounds.find { it.rect.contains(currentPos) }
                        if (hitFolder != null && draggedChannelLogin != null) {
                            onEvent(
                                MainEvent.DropChannelOnFolder(
                                    draggedChannelLogin!!,
                                    hitFolder.id
                                )
                            )
                        } else if (dragOverIndex != null && dragStartIndex != null && dragOverIndex != dragStartIndex) {
                            onEvent(
                                MainEvent.ReorderUnfolderedChannels(
                                    dragStartIndex!!,
                                    dragOverIndex!!
                                )
                            )
                        }
                        draggedChannelLogin = null
                        dropTargetFolderId = null
                        dragOffsetPx = Offset.Zero
                        dragStartIndex = null
                        dragOverIndex = null
                        isDragging = false
                    },
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .onGloballyPositioned { coords ->
                            val rect = coords.boundsInRoot()
                            channelRectMap[channel.login] = rect
                            channelBounds.removeAll { it.id == "unfoldered_${channel.login}" }
                            channelBounds.add(ItemBounds("unfoldered_${channel.login}", rect))
                        }
                )
            }

            if (state.monitorTabs.isNotEmpty()) {
                item(key = "monitor_header") {
                    Text(
                        LocalStrings.current.openChannelSpecialHeader,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                items(state.monitorTabs.size, key = { "monitor_${state.monitorTabs[it]}" }) { i ->
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

            item(key = "bottom_spacer") {
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
                onClick = { onEvent(MainEvent.ShowCreateFolderDialog) },
                modifier = Modifier.weight(1f),
                gradientColors = listOf(
                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.85f),
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                )
            )
        }

        TextButton(
            onClick = { onEvent(MainEvent.ToggleMentionsFeed) },
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).height(38.dp)
        ) {
            Box {
                Icon(
                    Icons.Filled.Notifications,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                if (state.unreadMentionsCount > 0) {
                    Box(
                        modifier = Modifier.align(Alignment.TopEnd).offset(x = 2.dp, y = (-2).dp)
                            .size(10.dp).clip(CircleShape)
                            .background(MaterialTheme.colorScheme.error),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (state.unreadMentionsCount > 9) "9+" else "${state.unreadMentionsCount}",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 6.sp),
                            color = Color.White
                        )
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            Text(s.panelMentionsTitle)
            Spacer(Modifier.weight(1f))
            if (state.unreadMentionsCount > 0) {
                Surface(color = MaterialTheme.colorScheme.error, shape = CircleShape) {
                    Text(
                        "${state.unreadMentionsCount}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                    )
                }
            }
        }
        TextButton(
            onClick = { onEvent(MainEvent.ToggleWhisperPanel) },
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).height(38.dp)
        ) {
            Box {
                Icon(
                    Icons.Filled.MailOutline,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                if (state.totalUnreadWhispers > 0) {
                    Box(
                        modifier = Modifier.align(Alignment.TopEnd).offset(x = 2.dp, y = (-2).dp)
                            .size(10.dp).clip(CircleShape)
                            .background(MaterialTheme.colorScheme.error),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (state.totalUnreadWhispers > 9) "9+" else "${state.totalUnreadWhispers}",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 6.sp),
                            color = Color.White
                        )
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            Text(s.chatWhisperTab)
            Spacer(Modifier.weight(1f))
            if (state.totalUnreadWhispers > 0) {
                Surface(color = MaterialTheme.colorScheme.error, shape = CircleShape) {
                    Text(
                        "${state.totalUnreadWhispers}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                    )
                }
            }
        }
        TextButton(
            onClick = { onEvent(MainEvent.ShowSettings) },
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).height(38.dp)
        ) {
            Icon(
                Icons.Outlined.Settings,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(s.settingsTitle)
            Spacer(Modifier.weight(1f))
        }
        Spacer(Modifier.height(4.dp))
    }

    if (isDragging && draggedChannelLogin != null) {
        Box(
            modifier = Modifier
                .offset {
                    with(density) {
                        IntOffset(
                            (dragStartOffsetPx.x + dragOffsetPx.x).toInt(),
                            (dragStartOffsetPx.y + dragOffsetPx.y).toInt()
                        )
                    }
                }
                .zIndex(1000f)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.primaryContainer)
                .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .shadow(16.dp, RoundedCornerShape(8.dp))
        ) {
            val ch = filteredUnfoldered.find { it.login == draggedChannelLogin }
                ?: state.folders.flatMap { it.channels }.find { it.login == draggedChannelLogin }
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
                    text = ch?.displayName ?: draggedChannelLogin!!,
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
                    if (folder.isExpanded) Icons.Filled.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    painterResource(if (folder.isExpanded) Res.drawable.folder_outline else Res.drawable.folder_outline),
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
                            Icons.Filled.Add,
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
                    text = { Text(LocalStrings.current.mainDeleteFolder) },
                    onClick = { showFolderMenu = false; onDelete() },
                    leadingIcon = {
                        Icon(
                            Icons.Outlined.Delete,
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
        val colorInt = hex.removePrefix("#").toLong(16)
        Color(
            red = ((colorInt shr 16) and 0xFF) / 255f,
            green = ((colorInt shr 8) and 0xFF) / 255f,
            blue = (colorInt and 0xFF) / 255f
        )
    } catch (e: Exception) {
        ChatoneColors.Violet400
    }
}
