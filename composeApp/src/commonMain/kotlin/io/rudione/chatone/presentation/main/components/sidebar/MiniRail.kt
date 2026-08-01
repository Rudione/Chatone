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

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun MiniRail(
    state: MainState,
    onEvent: (MainEvent) -> Unit,
    modifier: Modifier = Modifier,
    onCollapseRail: () -> Unit = {}
) {
    val extra = ChatoneTheme.extraColors
    val uriHandler = LocalUriHandler.current
    val coroutineScope = rememberCoroutineScope()
    val railScrollState = rememberLazyListState()
    val density = LocalDensity.current

    var railDragLogin by remember { mutableStateOf<String?>(null) }
    var railDragFromIndex by remember { mutableStateOf<Int?>(null) }
    var railDragOffset by remember { mutableStateOf(Offset.Zero) }
    var railDragStartCenterX by remember { mutableStateOf(0f) }
    var railDropTargetLogin by remember { mutableStateOf<String?>(null) }
    val s = LocalStrings.current
    val railItemCenters = remember { mutableStateMapOf<String, Float>() }
    val miniRailCollapsedFolders = remember { mutableStateListOf<String>() }
    val tooltipOffsetYPx = with(density) { 28.dp.roundToPx() }
    val glowEnabled = LocalWallpaperController.current.state.glowEffectsEnabled

    LiquidGlassSurface(
        modifier = modifier.padding(horizontal = 4.dp, vertical = 22.dp).then(
            if (glowEnabled) Modifier.shadow(
                8.dp,
                RoundedCornerShape(16.dp),
                ambientColor = extra.shadowColor,
                spotColor = extra.elevatedShadow
            ) else Modifier
        ),
        contentPadding = PaddingValues(2.dp),
        backgroundAlphaHigh = 0.95f,
        backgroundAlphaLow = 0.88f,
        forceGlass = true
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(1.dp),
            modifier = Modifier.padding(horizontal = 2.dp, vertical = 2.dp)
        ) {
            ChatoneIconButton(
                onClick = onCollapseRail,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = LocalStrings.current.mainCollapseMiniRail,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(18.dp)
                )
            }
            ChatoneIconButton(
                onClick = { onEvent(MainEvent.ToggleSidebar) },
                modifier = Modifier.size(34.dp)
            ) {
                Icon(
                    Icons.Filled.Menu,
                    contentDescription = LocalStrings.current.mainOpenSidebar,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(18.dp)
                )
            }
            LazyRow(
                state = railScrollState,
                contentPadding = PaddingValues(horizontal = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .height(36.dp)
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                if (event.type == PointerEventType.Scroll) {
                                    val delta = event.changes.firstOrNull()?.scrollDelta?.y ?: 0f
                                    if (delta != 0f) {
                                        coroutineScope.launch {
                                            railScrollState.scrollBy(delta * 5f)
                                        }
                                        event.changes.forEach { it.consume() }
                                    }
                                }
                            }
                        }
                    }
            ) {
                val railNodes: List<Any> = run {
                    val out = mutableListOf<Any>()
                    val openByKey = state.openChannels.associateBy { it.login.lowercase().removePrefix("#") }
                    val emitted = mutableSetOf<String>()
                    state.folders.forEach { f ->
                        out.add(f)
                        if (f.id !in miniRailCollapsedFolders) {
                            f.channels.forEach { fc ->
                                val key = fc.login.lowercase().removePrefix("#")
                                openByKey[key]?.let { out.add(it) }
                                emitted.add(key)
                            }
                        } else {
                            f.channels.forEach { emitted.add(it.login.lowercase().removePrefix("#")) }
                        }
                    }
                    state.openChannels.forEach { ch ->
                        val key = ch.login.lowercase().removePrefix("#")
                        if (key !in emitted) out.add(ch)
                    }
                    out
                }
                val channelFolderTint: Map<String, Color> = run {
                    val map = mutableMapOf<String, Color>()
                    state.folders.forEach { f ->
                        val tint = runCatching {
                            Color(f.color.removePrefix("#").toLong(16) or 0xFF000000L)
                        }.getOrDefault(Color(0xFF9146FF))
                        f.channels.forEach { fc ->
                            map[fc.login.lowercase().removePrefix("#")] = tint
                        }
                    }
                    map
                }
                items(
                    railNodes,
                    key = { node ->
                        when (node) {
                            is ChannelFolder -> "rail_folder_${node.id}"
                            is ChannelTab -> "rail_ch_${node.login}"
                            else -> node.hashCode().toString()
                        }
                    }
                ) { node ->
                    if (node is ChannelFolder) {
                        val folder = node
                        val isCollapsed = folder.id in miniRailCollapsedFolders
                        val parsedColor = runCatching {
                            Color(folder.color.removePrefix("#").toLong(16) or 0xFF000000L)
                        }.getOrDefault(Color(0xFF9146FF))
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .width(34.dp)
                                .height(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    if (isCollapsed) miniRailCollapsedFolders.remove(folder.id)
                                    else miniRailCollapsedFolders.add(folder.id)
                                }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(23.dp)
                                    .clip(CircleShape)
                                    .background(parsedColor.copy(alpha = 0.18f))
                                    .border(1.dp, parsedColor.copy(alpha = 0.6f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(
                                        Res.drawable.folder_outline
                                    ),
                                    contentDescription = folder.name,
                                    tint = parsedColor,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                            Text(
                                text = folder.name.take(5),
                                style = MaterialTheme.typography.labelSmall,
                                color = parsedColor,
                                fontSize = 7.sp,
                                maxLines = 1
                            )
                        }
                        return@items
                    }
                    val channel = node as ChannelTab
                    val isActive = channel.login == state.activeChannelLogin
                    var showTooltip by remember { mutableStateOf(false) }
                    var tooltipOffset by remember { mutableStateOf(IntOffset.Zero) }
                    val isDraggedItem = railDragLogin == channel.login
                    val isDragTarget = railDropTargetLogin == channel.login && !isDraggedItem
                    val itemCenterX = remember { mutableStateOf(0f) }
                    val folderTint = channelFolderTint[channel.login.lowercase().removePrefix("#")]

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .then(
                                if (folderTint != null) Modifier.drawBehind {
                                    val lineHeight = 2.dp.toPx()
                                    drawRect(
                                        color = folderTint,
                                        topLeft = Offset(0f, size.height - lineHeight),
                                        size = androidx.compose.ui.geometry.Size(size.width, lineHeight)
                                    )
                                } else Modifier
                            )
                            .onGloballyPositioned { coords ->
                                val bounds = coords.boundsInRoot()
                                val cx = bounds.left + bounds.width / 2f
                                railItemCenters[channel.login] = cx
                                itemCenterX.value = cx
                            }
                            .pointerInput(channel.login) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        railDragLogin = channel.login
                                        railDragOffset = Offset.Zero
                                        railDragStartCenterX = itemCenterX.value
                                        railDropTargetLogin = null
                                        railDragFromIndex =
                                            state.openChannels.indexOfFirst { it.login == channel.login }
                                    },
                                    onDrag = { change, delta ->
                                        change.consume()
                                        railDragOffset += delta
                                        val currentX = railDragStartCenterX + railDragOffset.x
                                        val itemHalfWidthPx = with(density) { 18.dp.toPx() }
                                        val closest = railItemCenters.entries
                                            .filter { (login, _) -> login != channel.login }
                                            .minByOrNull { (_, cx) -> kotlin.math.abs(cx - currentX) }?.key
                                        val nearestCx = closest?.let { railItemCenters[it] }
                                        railDropTargetLogin =
                                            if (closest != null && nearestCx != null &&
                                                kotlin.math.abs(nearestCx - currentX) < itemHalfWidthPx
                                            ) closest else null
                                    },
                                    onDragEnd = {
                                        val fromIdx = railDragFromIndex
                                        val toLogin = railDropTargetLogin
                                        val toIdx = if (toLogin != null)
                                            state.openChannels.indexOfFirst { it.login == toLogin }
                                                .takeIf { it >= 0 }
                                        else null
                                        if (fromIdx != null && toIdx != null && toIdx != fromIdx) {
                                            onEvent(MainEvent.ReorderChannels(fromIdx, toIdx))
                                        }
                                        railDragLogin = null
                                        railDragOffset = Offset.Zero
                                        railDragStartCenterX = 0f
                                        railDropTargetLogin = null
                                        railDragFromIndex = null
                                    },
                                    onDragCancel = {
                                        railDragLogin = null
                                        railDragOffset = Offset.Zero
                                        railDragStartCenterX = 0f
                                        railDropTargetLogin = null
                                        railDragFromIndex = null
                                    }
                                )
                            }
                            .pointerInput(channel.login) {
                                awaitPointerEventScope {
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        when (event.type) {
                                            PointerEventType.Enter -> {
                                                val pos = event.changes.firstOrNull()?.position
                                                    ?: Offset.Zero
                                                tooltipOffset = IntOffset(
                                                    pos.x.toInt() - 35,
                                                    pos.y.toInt() - tooltipOffsetYPx
                                                )
                                                showTooltip = true
                                            }

                                            PointerEventType.Move -> {
                                                val pos = event.changes.firstOrNull()?.position
                                                    ?: Offset.Zero
                                                tooltipOffset = IntOffset(
                                                    pos.x.toInt() - 35,
                                                    pos.y.toInt() - tooltipOffsetYPx
                                                )
                                            }

                                            PointerEventType.Exit -> showTooltip = false
                                            PointerEventType.Press -> if (event.buttons.isSecondaryPressed) {
                                                try {
                                                    uriHandler.openUri("https://www.twitch.tv/${channel.login}")
                                                } catch (_: Exception) {
                                                }
                                            }
                                        }
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isDraggedItem -> MaterialTheme.colorScheme.primary.copy(
                                            alpha = 0.35f
                                        )

                                        isDragTarget -> MaterialTheme.colorScheme.secondary.copy(
                                            alpha = 0.25f
                                        )

                                        isActive -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                        else -> Color.Transparent
                                    }
                                )
                                .clickable { onEvent(MainEvent.SelectChannel(channel.login)) }
                                .then(
                                    if (isActive || isDraggedItem) Modifier.border(
                                        2.dp,
                                        MaterialTheme.colorScheme.primary,
                                        CircleShape
                                    ) else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (channel.profileImageUrl.isNotEmpty()) {
                                AsyncImage(
                                    model = channel.profileImageUrl,
                                    contentDescription = channel.displayName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(24.dp).clip(CircleShape)
                                )
                            } else {
                                Text(
                                    text = channel.displayName.take(2).uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (channel.isLive) {
                            Box(
                                modifier = Modifier.align(Alignment.TopEnd)
                                    .offset(x = 1.dp, y = (-1).dp).size(9.dp).clip(CircleShape)
                                    .background(ChatoneTheme.extraColors.live)
                                    .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
                                    .zIndex(2f)
                            )
                        }
                        if (channel.unreadCount > 0) {
                            Box(
                                modifier = Modifier.align(Alignment.BottomEnd)
                                    .offset(x = 1.dp, y = 1.dp).size(11.dp).clip(CircleShape)
                                    .background(Color.Red)
                                    .border(1.dp, MaterialTheme.colorScheme.surface, CircleShape)
                                    .zIndex(2f), contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (channel.unreadCount > 9) "9+" else "${channel.unreadCount}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 6.sp),
                                    color = Color.White
                                )
                            }
                        }
                        if (showTooltip) {
                            Popup(
                                alignment = Alignment.TopStart,
                                offset = tooltipOffset,
                                properties = androidx.compose.ui.window.PopupProperties(
                                    focusable = false,
                                    dismissOnBackPress = false,
                                    dismissOnClickOutside = false
                                )
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.98f))
                                        .border(
                                            1.dp,
                                            MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                                            RoundedCornerShape(6.dp)
                                        )
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                        .shadow(4.dp, RoundedCornerShape(6.dp))
                                        .pointerInput(Unit) {
                                            awaitPointerEventScope {
                                                while (true) {
                                                    val event = awaitPointerEvent()
                                                    event.changes.forEach { it.consumeAllChanges() }
                                                }
                                            }
                                        }
                                ) {
                                    Text(
                                        text = channel.displayName.ifBlank { channel.login.replaceFirstChar { char -> char.uppercaseChar() } },
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Box(modifier = Modifier.size(32.dp), contentAlignment = Alignment.Center) {
                ChatoneIconButton(
                    onClick = { onEvent(MainEvent.ToggleMentionsFeed) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Filled.Notifications,
                        contentDescription = s.chatMentionsTab,
                        tint = if (state.unreadMentionsCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
                if (state.unreadMentionsCount > 0) {
                    Box(
                        modifier = Modifier.align(Alignment.TopEnd).offset(x = 1.dp, y = (-1).dp)
                            .size(14.dp).clip(CircleShape)
                            .background(MaterialTheme.colorScheme.error)
                            .border(1.dp, MaterialTheme.colorScheme.surface, CircleShape)
                            .zIndex(2f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (state.unreadMentionsCount > 9) "9+" else "${state.unreadMentionsCount}",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.sp),
                            color = Color.White
                        )
                    }
                }
            }

            Box(modifier = Modifier.size(32.dp), contentAlignment = Alignment.Center) {
                ChatoneIconButton(
                    onClick = { onEvent(MainEvent.ToggleWhisperPanel) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Filled.MailOutline,
                        contentDescription = s.chatWhisperTab,
                        tint = if (state.totalUnreadWhispers > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
                if (state.totalUnreadWhispers > 0) {
                    Box(
                        modifier = Modifier.align(Alignment.TopEnd).offset(x = 1.dp, y = (-1).dp)
                            .size(14.dp).clip(CircleShape)
                            .background(MaterialTheme.colorScheme.error)
                            .border(1.dp, MaterialTheme.colorScheme.surface, CircleShape)
                            .zIndex(2f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (state.totalUnreadWhispers > 9) "9+" else "${state.totalUnreadWhispers}",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.sp),
                            color = Color.White
                        )
                    }
                }
            }
            ChatoneIconButton(
                onClick = { onEvent(MainEvent.ShowAddChannelDialog) },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = s.addChannel,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
