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
import io.rudione.chatone.presentation.components.ChatoneCountChip
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

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
internal fun ChannelTabBar(
    channels: List<ChannelTab>,
    activeLogin: String?,
    onSelect: (String) -> Unit,
    onClose: (String) -> Unit,
    onReorder: (Int, Int) -> Unit,
    folders: List<ChannelFolder> = emptyList(),
    onMoveToFolder: (channelLogin: String, folderId: String) -> Unit = { _, _ -> }
) {
    data class FolderRef(val id: String, val name: String, val color: Color)
    val folderByLogin: Map<String, FolderRef> = remember(folders) {
        buildMap {
            folders.forEach { f ->
                val ref = FolderRef(f.id, f.name, parseFolderColor(f.color))
                f.channels.forEach { ch ->
                    put(ch.login.lowercase().removePrefix("#"), ref)
                }
            }
        }
    }
    val collapsedFolders = remember { mutableStateListOf<String>() }
    LaunchedEffect(folders, channels) {
        io.github.aakira.napier.Napier.d(
            "ChannelTabBar: folders=${folders.size} (${folders.joinToString { it.name + "[${it.channels.size}]" }}), channels=${channels.size}, mapped=${folderByLogin.size}",
            tag = "ChannelTabBar"
        )
    }
    val extra = ChatoneTheme.extraColors
    val uriHandler = LocalUriHandler.current

    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var dropTargetIndex by remember { mutableStateOf<Int?>(null) }

    val tabCenters = remember { mutableStateListOf<Pair<Int, Offset>>() }
    val folderChipBounds = remember { mutableStateMapOf<String, androidx.compose.ui.geometry.Rect>() }
    var folderDropTargetId by remember { mutableStateOf<String?>(null) }

    val tabBarWallpaper = LocalWallpaperController.current.state
    val tabBarBlur =
        if (tabBarWallpaper.glowEffectsEnabled) tabBarWallpaper.panelColorConfig.topBarBlurRadius else 0f
    val tabBarColor =
        topBarBackgroundColor(tabBarWallpaper, MaterialTheme.colorScheme.surfaceContainer)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, extra.cardBorder)
    ) {
        if (tabBarBlur > 0f) {
            Box(modifier = Modifier.matchParentSize().background(tabBarColor).panelBlur(tabBarBlur))
        } else {
            Box(modifier = Modifier.matchParentSize().background(tabBarColor))
        }
        Box(modifier = Modifier.fillMaxWidth()) {
            val displayChannels: List<ChannelTab> = remember(channels, folders) {
                val key = { ch: ChannelTab -> ch.login.lowercase().removePrefix("#") }
                val foldered = folders.flatMap { f ->
                    val ids = f.channels.map { it.login.lowercase().removePrefix("#") }.toSet()
                    channels.filter { key(it) in ids }
                }
                val seen = foldered.map { key(it) }.toHashSet()
                val unfoldered = channels.filter { key(it) !in seen }
                foldered + unfoldered
            }
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                val folderRefByIndex: List<FolderRef?> = displayChannels.map { ch ->
                    folderByLogin[ch.login.lowercase().removePrefix("#")]
                }
                val groupStarts: Map<Int, Boolean> = run {
                    val starts = mutableMapOf<Int, Boolean>()
                    var prev: String? = "__SENTINEL__"
                    folderRefByIndex.forEachIndexed { idx, ref ->
                        val cur = ref?.id
                        if (cur != prev) {
                            starts[idx] = true
                            prev = cur
                        }
                    }
                    starts
                }
                val groupSizes: Map<Int, Int> = run {
                    val sizes = mutableMapOf<Int, Int>()
                    var runStart = -1
                    var runId: String? = "__SENTINEL__"
                    var runCount = 0
                    fun flush() {
                        if (runStart >= 0) sizes[runStart] = runCount
                    }
                    folderRefByIndex.forEachIndexed { idx, ref ->
                        val cur = ref?.id
                        if (cur != runId) {
                            flush()
                            runStart = idx
                            runId = cur
                            runCount = 1
                        } else {
                            runCount += 1
                        }
                    }
                    flush()
                    sizes
                }

                displayChannels.forEachIndexed { index: Int, channel: ChannelTab ->
                    val isActive = channel.login == activeLogin
                    val folderInfo = folderRefByIndex[index]
                    val isGroupStart = groupStarts[index] == true

                    if (isGroupStart && folderInfo != null) {
                        val isCollapsed = folderInfo.id in collapsedFolders
                        val count = groupSizes[index] ?: 0
                        TabBarFolderChip(
                            name = folderInfo.name,
                            color = folderInfo.color,
                            count = count,
                            isCollapsed = isCollapsed,
                            onClick = {
                                if (isCollapsed) collapsedFolders.remove(folderInfo.id)
                                else collapsedFolders.add(folderInfo.id)
                            },
                            isDropTarget = folderDropTargetId == folderInfo.id,
                            onPositioned = { rect -> folderChipBounds[folderInfo.id] = rect }
                        )
                    } else if (isGroupStart && folderInfo == null && index > 0) {
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                .width(1.dp)
                                .height(14.dp)
                                .background(extra.cardBorder)
                        )
                    }

                    if (folderInfo != null && folderInfo.id in collapsedFolders) {
                        return@forEachIndexed
                    }

                    val tabCenter = remember { mutableStateOf(Offset.Zero) }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isActive) MaterialTheme.colorScheme.surface else Color.Transparent)
                            .then(
                                if (dropTargetIndex == index && draggedIndex != null) Modifier.border(
                                    2.dp,
                                    MaterialTheme.colorScheme.primary,
                                    RoundedCornerShape(6.dp)
                                ) else Modifier
                            )
                            .then(
                                if (folderInfo != null) Modifier.drawBehind {
                                    val stripeHeight = 2.dp.toPx()
                                    drawRect(
                                        color = folderInfo.color,
                                        topLeft = Offset(0f, size.height - stripeHeight),
                                        size = androidx.compose.ui.geometry.Size(size.width, stripeHeight)
                                    )
                                } else Modifier
                            )
                            .clickable { onSelect(channel.login) }
                            .padding(start = 6.dp, end = 2.dp, top = 3.dp, bottom = 3.dp)
                            .onGloballyPositioned { coords ->
                                val bounds = coords.boundsInRoot()
                                tabCenter.value = Offset(
                                    bounds.left + bounds.width / 2,
                                    bounds.top + bounds.height / 2
                                )

                                tabCenters.removeAll { pair: Pair<Int, Offset> -> pair.first == index }
                                tabCenters.add(index to tabCenter.value)
                            }
                            .pointerInput(channel.login, index) {
                                detectDragGestures(
                                    onDragStart = { draggedIndex = index },
                                    onDrag = { change, delta ->
                                        change.consumeAllChanges()
                                        dragOffset += delta

                                        val currentDragPos = tabCenter.value + dragOffset

                                        val hitFolderId = folderChipBounds.entries.firstOrNull { (_, rect) ->
                                            rect.contains(currentDragPos)
                                        }?.key
                                        if (hitFolderId != null) {
                                            folderDropTargetId = hitFolderId
                                            dropTargetIndex = null
                                        } else {
                                            folderDropTargetId = null
                                            dropTargetIndex =
                                                tabCenters.minByOrNull { pair: Pair<Int, Offset> ->
                                                    hypot(
                                                        currentDragPos.x - pair.second.x,
                                                        currentDragPos.y - pair.second.y
                                                    )
                                                }?.first
                                        }
                                    },
                                    onDragEnd = {
                                        val fromDisp = draggedIndex
                                        val targetFolder = folderDropTargetId
                                        val toDisp = dropTargetIndex
                                        val fromLogin = fromDisp?.let { displayChannels.getOrNull(it)?.login }
                                        val toLogin = toDisp?.let { displayChannels.getOrNull(it)?.login }
                                        val fromOrig = fromLogin?.let { l -> channels.indexOfFirst { it.login == l } }
                                            ?.takeIf { it >= 0 }
                                        val toOrig = toLogin?.let { l -> channels.indexOfFirst { it.login == l } }
                                            ?.takeIf { it >= 0 }
                                        when {
                                            fromLogin != null && targetFolder != null -> {
                                                onMoveToFolder(fromLogin, targetFolder)
                                            }
                                            fromOrig != null && toOrig != null && fromOrig != toOrig -> {
                                                onReorder(fromOrig, toOrig)
                                            }
                                        }
                                        draggedIndex = null
                                        dragOffset = Offset.Zero
                                        dropTargetIndex = null
                                        folderDropTargetId = null
                                    },
                                    onDragCancel = {
                                        draggedIndex = null
                                        dragOffset = Offset.Zero
                                        dropTargetIndex = null
                                        folderDropTargetId = null
                                    }
                                )
                            }
                            .pointerInput(Unit) {
                                awaitPointerEventScope {
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) {
                                            try {
                                                uriHandler.openUri("https://www.twitch.tv/${channel.login}")
                                            } catch (_: Exception) {
                                            }
                                        }
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (channel.isLive) {
                                Box(
                                    modifier = Modifier.size(6.dp).clip(CircleShape)
                                        .background(ChatoneTheme.extraColors.live).border(
                                            1.dp,
                                            MaterialTheme.colorScheme.surface,
                                            CircleShape
                                        )
                                )
                                Spacer(Modifier.width(4.dp))
                            }
                            Text(
                                text = "#${channel.displayName}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isActive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                            if (channel.unreadCount > 0) {
                                Spacer(Modifier.width(3.dp))
                                ChatoneCountChip(
                                    count = channel.unreadCount,
                                    max = 99,
                                    dense = true
                                )
                            }
                            Spacer(Modifier.width(2.dp))
                            Box(
                                modifier = Modifier.size(16.dp).clip(CircleShape)
                                    .clickable { onClose(channel.login) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = "Close ${channel.displayName}",
                                    modifier = Modifier.size(10.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                            Spacer(Modifier.width(4.dp))
                        }
                    }
                }

                if (draggedIndex != null) {
                    val openFolderIds = folderRefByIndex.mapNotNull { it?.id }.toSet()
                    folders.forEach { f ->
                        if (f.id in openFolderIds) return@forEach
                        val parsed = runCatching {
                            Color(f.color.removePrefix("#").toLong(16) or 0xFF000000L)
                        }.getOrDefault(Color(0xFF9146FF))
                        TabBarFolderChip(
                            name = f.name,
                            color = parsed,
                            count = 0,
                            isCollapsed = true,
                            onClick = {  },
                            isDropTarget = folderDropTargetId == f.id,
                            onPositioned = { rect -> folderChipBounds[f.id] = rect }
                        )
                    }
                }
            }

            draggedIndex?.let { idx: Int ->
                channels.getOrNull(idx)?.let { ch: ChannelTab ->
                    Box(
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    tabCenters.getOrNull(idx)?.second?.x?.toInt()
                                        ?: 0 + dragOffset.x.toInt(),
                                    tabCenters.getOrNull(idx)?.second?.y?.toInt()
                                        ?: 0 + dragOffset.y.toInt()
                                )
                            }
                            .zIndex(10f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .border(
                                2.dp,
                                MaterialTheme.colorScheme.primary,
                                RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .shadow(8.dp, RoundedCornerShape(6.dp))
                    ) {
                        Text(
                            text = "#${ch.displayName}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun TabBarFolderChip(
    name: String,
    color: Color,
    count: Int,
    isCollapsed: Boolean,
    onClick: () -> Unit,
    isDropTarget: Boolean = false,
    onPositioned: (androidx.compose.ui.geometry.Rect) -> Unit = {}
) {
    Row(
        modifier = Modifier
            .padding(start = 3.dp, end = 1.dp, top = 2.dp, bottom = 2.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(
                if (isDropTarget) color.copy(alpha = 0.4f)
                else color.copy(alpha = 0.14f)
            )
            .then(
                if (isDropTarget) Modifier.border(
                    1.2.dp, color, RoundedCornerShape(6.dp)
                ) else Modifier
            )
            .clickable(onClick = onClick)
            .onGloballyPositioned { coords -> onPositioned(coords.boundsInRoot()) }
            .padding(horizontal = 5.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Icon(
            painter = painterResource(chatone.composeapp.generated.resources.Res.drawable.folder_outline),
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(11.dp)
        )
        Text(
            name,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = color,
            maxLines = 1,
            fontSize = 10.sp
        )
        if (count > 0) {
            Text(
                "·$count",
                style = MaterialTheme.typography.labelSmall,
                color = color.copy(alpha = 0.7f),
                fontSize = 9.sp
            )
        }
        Icon(
            if (isCollapsed) Icons.AutoMirrored.Filled.KeyboardArrowRight
            else Icons.Filled.KeyboardArrowDown,
            contentDescription = null,
            tint = color.copy(alpha = 0.7f),
            modifier = Modifier.size(10.dp)
        )
    }
}
