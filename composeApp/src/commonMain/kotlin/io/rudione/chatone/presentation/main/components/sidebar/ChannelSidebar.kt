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
import io.rudione.chatone.presentation.components.RailAction
import io.rudione.chatone.presentation.components.ChatoneIconRail
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

internal sealed interface SidebarDropTarget {
    data class Folder(val folderId: String) : SidebarDropTarget
    data class Channel(val login: String, val folderId: String?) : SidebarDropTarget
    data object Unfoldered : SidebarDropTarget
}

internal class SidebarDropIndex {
    val folderRects = mutableStateMapOf<String, Rect>()
    val channelRects = mutableStateMapOf<String, Rect>()
    val channelFolderIds = mutableStateMapOf<String, String?>()
    var unfolderedSection by mutableStateOf<Rect?>(null)

    fun putChannel(login: String, folderId: String?, rect: Rect) {
        channelRects[login] = rect
        channelFolderIds[login] = folderId
    }

    fun forget(login: String) {
        channelRects.remove(login)
        channelFolderIds.remove(login)
    }

    fun resolve(pointInRoot: Offset): SidebarDropTarget? {
        folderRects.entries.firstOrNull { it.value.contains(pointInRoot) }?.let {
            return SidebarDropTarget.Folder(it.key)
        }
        channelRects.entries.firstOrNull { it.value.contains(pointInRoot) }?.let {
            return SidebarDropTarget.Channel(it.key, channelFolderIds[it.key])
        }
        if (unfolderedSection?.contains(pointInRoot) == true) return SidebarDropTarget.Unfoldered
        return null
    }

    fun hoveredFolderId(pointInRoot: Offset): String? = when (val t = resolve(pointInRoot)) {
        is SidebarDropTarget.Folder -> t.folderId
        is SidebarDropTarget.Channel -> t.folderId
        else -> null
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun ChannelSidebar(
    state: MainState,
    onEvent: (MainEvent) -> Unit,
    isWideScreen: Boolean = false,
    mentionMuteRepository: MentionMuteRepository? = null
) {
    val extra = ChatoneTheme.extraColors

    var draggedChannelLogin by remember { mutableStateOf<String?>(null) }
    var draggedChannelSourceFolderId by remember { mutableStateOf<String?>(null) }
    var dragOffsetPx by remember { mutableStateOf(Offset.Zero) }
    var dragStartRootPx by remember { mutableStateOf(Offset.Zero) }
    var isDragging by remember { mutableStateOf(false) }
    var dropTargetFolderId by remember { mutableStateOf<String?>(null) }
    var sidebarTopLeftInRoot by remember { mutableStateOf(Offset.Zero) }

    val dropIndex = remember { SidebarDropIndex() }
    val dragPointInRoot = dragStartRootPx + dragOffsetPx

    LaunchedEffect(state.folders, state.unfolderedChannels) {
        val alive = buildSet {
            state.folders.forEach { f -> f.channels.forEach { add(it.login) } }
            state.unfolderedChannels.forEach { add(it.login) }
        }
        dropIndex.channelRects.keys.toList()
            .filterNot { it in alive }
            .forEach { dropIndex.forget(it) }
        dropIndex.folderRects.keys.toList()
            .filterNot { id -> state.folders.any { it.id == id } }
            .forEach { dropIndex.folderRects.remove(it) }
    }
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
            .onGloballyPositioned { sidebarTopLeftInRoot = it.positionInRoot() }
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
                    onEdit = { onEvent(MainEvent.ShowEditFolderDialog(folder.id)) },
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .onGloballyPositioned { coords ->
                            dropIndex.folderRects[folder.id] = coords.boundsInRoot()
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
                                dragStartRootPx = rootPos
                                dragOffsetPx = Offset.Zero
                                isDragging = true
                            },
                            onDrag = { delta ->
                                dragOffsetPx += delta
                                dropTargetFolderId =
                                    dropIndex.hoveredFolderId(dragStartRootPx + dragOffsetPx)
                            },
                            onDragEnd = {
                                applySidebarDrop(
                                    target = dropIndex.resolve(dragStartRootPx + dragOffsetPx),
                                    draggedLogin = channel.login,
                                    sourceFolderId = folder.id,
                                    folders = state.folders,
                                    unfoldered = filteredUnfoldered,
                                    onEvent = onEvent
                                )
                                draggedChannelLogin = null
                                draggedChannelSourceFolderId = null
                                dropTargetFolderId = null
                                dragOffsetPx = Offset.Zero
                                isDragging = false
                            },
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .onGloballyPositioned { coords ->
                                    dropIndex.putChannel(
                                        channel.login, folder.id, coords.boundsInRoot()
                                    )
                                }
                        )
                    }
                }
            }

            item(key = "unfoldered_header") {
                val isEjectTarget = isDragging && draggedChannelSourceFolderId != null &&
                        dropIndex.resolve(dragPointInRoot) == SidebarDropTarget.Unfoldered
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
                        .onGloballyPositioned { coords ->
                            dropIndex.unfolderedSection = coords.boundsInRoot()
                        }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                )
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
                        draggedChannelSourceFolderId = null
                        dragStartRootPx = rootPos
                        dragOffsetPx = Offset.Zero
                        isDragging = true
                    },
                    onDrag = { delta ->
                        dragOffsetPx += delta
                        dropTargetFolderId =
                            dropIndex.hoveredFolderId(dragStartRootPx + dragOffsetPx)
                    },
                    onDragEnd = {
                        applySidebarDrop(
                            target = dropIndex.resolve(dragStartRootPx + dragOffsetPx),
                            draggedLogin = channel.login,
                            sourceFolderId = null,
                            folders = state.folders,
                            unfoldered = filteredUnfoldered,
                            onEvent = onEvent
                        )
                        draggedChannelLogin = null
                        draggedChannelSourceFolderId = null
                        dropTargetFolderId = null
                        dragOffsetPx = Offset.Zero
                        isDragging = false
                    },
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .onGloballyPositioned { coords ->
                            dropIndex.putChannel(channel.login, null, coords.boundsInRoot())
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

        ChatoneIconRail(
            actions = listOf(
                RailAction(
                    icon = Icons.Filled.Notifications,
                    label = s.panelMentionsTitle,
                    badge = state.unreadMentionsCount,
                    onClick = { onEvent(MainEvent.ToggleMentionsFeed) }
                ),
                RailAction(
                    icon = Icons.Filled.MailOutline,
                    label = s.chatWhisperTab,
                    badge = state.totalUnreadWhispers,
                    onClick = { onEvent(MainEvent.ToggleWhisperPanel) }
                ),
                RailAction(
                    icon = Icons.Outlined.Settings,
                    label = s.settingsTitle,
                    onClick = { onEvent(MainEvent.ShowSettings) }
                )
            ),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
        Spacer(Modifier.height(4.dp))
    }

    if (isDragging && draggedChannelLogin != null) {
        Box(
            modifier = Modifier
                .offset {
                    val local = dragPointInRoot - sidebarTopLeftInRoot
                    IntOffset(local.x.toInt(), local.y.toInt())
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

internal fun applySidebarDrop(
    target: SidebarDropTarget?,
    draggedLogin: String,
    sourceFolderId: String?,
    folders: List<ChannelFolder>,
    unfoldered: List<ChannelTab>,
    onEvent: (MainEvent) -> Unit
) {
    when (target) {
        null -> return

        is SidebarDropTarget.Folder -> {
            if (target.folderId != sourceFolderId) {
                onEvent(MainEvent.MoveChannelToFolder(draggedLogin, target.folderId))
            }
        }

        SidebarDropTarget.Unfoldered -> {
            if (sourceFolderId != null) {
                onEvent(MainEvent.MoveChannelToFolder(draggedLogin, null))
            }
        }

        is SidebarDropTarget.Channel -> {
            if (target.login.equals(draggedLogin, ignoreCase = true)) return
            if (target.folderId != sourceFolderId) {
                onEvent(MainEvent.MoveChannelToFolder(draggedLogin, target.folderId))
                return
            }
            val siblings = if (sourceFolderId == null) unfoldered
            else folders.firstOrNull { it.id == sourceFolderId }?.channels ?: return
            val from = siblings.indexOfFirst { it.login.equals(draggedLogin, ignoreCase = true) }
            val to = siblings.indexOfFirst { it.login.equals(target.login, ignoreCase = true) }
            if (from < 0 || to < 0 || from == to) return
            if (sourceFolderId == null) {
                onEvent(MainEvent.ReorderUnfolderedChannels(from, to))
            } else {
                onEvent(MainEvent.ReorderFolderChannels(sourceFolderId, from, to))
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
                    text = { Text(LocalStrings.current.mainEditFolder) },
                    onClick = { showFolderMenu = false; onEdit() },
                    leadingIcon = {
                        Icon(
                            Icons.Outlined.Edit,
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
