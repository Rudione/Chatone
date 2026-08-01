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

@Composable
internal fun CompactChannelAvatar(
    channel: ChannelTab,
    isActive: Boolean,
    onClick: () -> Unit,
    indented: Boolean = false
) {
    var showTooltip by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier.fillMaxWidth().padding(start = if (indented) 6.dp else 0.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(modifier = Modifier.size(28.dp), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(
                        if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        else Color.Transparent
                    )
                    .then(
                        if (isActive) Modifier.border(
                            1.5.dp,
                            MaterialTheme.colorScheme.primary,
                            CircleShape
                        )
                        else Modifier
                    )
                    .clickable { onClick() }
                    .pointerInput(channel.login) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                when (event.type) {
                                    PointerEventType.Enter -> showTooltip = true
                                    PointerEventType.Exit -> showTooltip = false
                                }
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (channel.profileImageUrl.isNotEmpty()) {
                    AsyncImage(
                        model = channel.profileImageUrl,
                        contentDescription = channel.displayName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(20.dp).clip(CircleShape)
                    )
                } else {
                    Text(
                        text = channel.displayName.take(2).uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                        fontWeight = FontWeight.Bold,
                        color = if (isActive) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (channel.isLive) {
                Box(
                    modifier = Modifier.align(Alignment.TopEnd)
                        .offset(x = 1.5.dp, y = (-1.5).dp)
                        .size(7.dp).clip(CircleShape)
                        .background(ChatoneTheme.extraColors.live)
                        .border(1.dp, MaterialTheme.colorScheme.surface, CircleShape)
                )
            }
            if (channel.unreadCount > 0) {
                Box(
                    modifier = Modifier.align(Alignment.BottomEnd)
                        .offset(x = 1.5.dp, y = 1.5.dp)
                        .size(10.dp).clip(CircleShape)
                        .background(Color.Red)
                        .border(1.dp, MaterialTheme.colorScheme.surface, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (channel.unreadCount > 9) "9+" else "${channel.unreadCount}",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 6.sp),
                        color = Color.White
                    )
                }
            }
        }
        if (showTooltip) {
            Popup(
                alignment = Alignment.CenterEnd,
                offset = IntOffset(20, 0),
                properties = androidx.compose.ui.window.PopupProperties(focusable = false)
            ) {
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.98f))
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                            RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = channel.displayName.ifBlank { channel.login.replaceFirstChar { it.uppercaseChar() } },
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
internal fun ChannelItemWithDrag(
    modifier: Modifier,
    channel: ChannelTab,
    isActive: Boolean,
    folders: List<ChannelFolder> = emptyList(),
    currentFolderId: String? = null,
    onClick: () -> Unit,
    onClose: () -> Unit,
    onMoveToFolder: (String?) -> Unit = {},
    liveNotifyEnabled: Boolean = false,
    onToggleLiveNotify: () -> Unit = {},
    onDragStart: (Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onReorder: (Int, Int) -> Unit = { _, _ -> },
    index: Int = -1
) {
    val uriHandler = LocalUriHandler.current
    var showContextMenu by remember { mutableStateOf(false) }
    var itemBounds by remember { mutableStateOf<Rect?>(null) }

    Box(
        modifier = modifier
            .pointerInput(channel.login) {
                detectDragGestures(
                    onDragStart = { localOffset ->
                        val itemTopLeft = itemBounds?.topLeft ?: Offset.Zero
                        onDragStart(itemTopLeft + localOffset)
                    },
                    onDrag = { change, amount ->
                        change.consumeAllChanges()
                        onDrag(amount)
                    },
                    onDragEnd = { onDragEnd() },
                    onDragCancel = { onDragEnd() }
                )
            }
            .onGloballyPositioned { coords ->
                itemBounds = coords.boundsInRoot()
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 3.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    if (isActive) ChatoneTheme.extraColors.sidebarSelected else Color.Transparent
                )
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { showContextMenu = true }
                )
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
                }
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (channel.isLive) {
                Box(
                    modifier = Modifier.size(8.dp).clip(CircleShape)
                        .background(ChatoneTheme.extraColors.live)
                        .border(1.dp, MaterialTheme.colorScheme.surface, CircleShape)
                )
                Spacer(Modifier.width(6.dp))
            }
            if (channel.profileImageUrl.isNotEmpty()) {
                AsyncImage(
                    model = channel.profileImageUrl,
                    contentDescription = channel.displayName,
                    modifier = Modifier.size(22.dp).clip(CircleShape)
                )
            } else {
                Text(
                    text = "#",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = channel.displayName,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isActive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (channel.unreadCount > 0) {
                Surface(
                    color = ChatoneTheme.extraColors.live,
                    shape = CircleShape,
                    modifier = Modifier.padding(start = 4.dp)
                ) {
                    Text(
                        text = if (channel.unreadCount > 99) "99+" else "${channel.unreadCount}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            if (folders.isNotEmpty()) {
                Box {
                    ChatoneIconButton(
                        onClick = { showContextMenu = true },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Outlined.List,
                            contentDescription = LocalStrings.current.mainMoveToFolder,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                    if (showContextMenu) {
                        Popup(
                            alignment = Alignment.TopEnd,
                            offset = IntOffset(0, 24),
                            properties = androidx.compose.ui.window.PopupProperties(
                                focusable = true,
                                dismissOnBackPress = true,
                                dismissOnClickOutside = true
                            ),
                            onDismissRequest = { showContextMenu = false }
                        ) {
                            LiquidGlassSurface(
                                modifier = Modifier
                                    .width(200.dp)
                                    .zIndex(100f)
                                    .shadow(16.dp, RoundedCornerShape(12.dp)),
                                contentPadding = PaddingValues(4.dp),
                                backgroundAlphaHigh = 0.99f
                            ) {
                                Column {
                                    if (currentFolderId != null) {
                                        LiquidGlassDropdownItem(
                                            text = LocalStrings.current.mainRemoveFromFolder,
                                            icon = Icons.Outlined.Close,
                                            onClick = {
                                                showContextMenu = false
                                                onMoveToFolder(null)
                                            }
                                        )
                                        HorizontalDivider(
                                            modifier = Modifier.padding(vertical = 4.dp),
                                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                                        )
                                    }
                                    folders.filter { it.id != currentFolderId }.forEach { folder ->
                                        LiquidGlassDropdownItem(
                                            text = folder.name,
                                            icon = Icons.AutoMirrored.Outlined.List,
                                            onClick = {
                                                showContextMenu = false
                                                onMoveToFolder(folder.id)
                                            }
                                        )
                                    }
                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 4.dp),
                                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                                    )
                                    LiquidGlassDropdownItem(
                                        text = if (liveNotifyEnabled)
                                            LocalStrings.current.mainLiveNotifyOff
                                        else LocalStrings.current.mainLiveNotifyOn,
                                        icon = if (liveNotifyEnabled)
                                            Icons.Filled.Notifications
                                        else Icons.Outlined.Notifications,
                                        onClick = {
                                            showContextMenu = false
                                            onToggleLiveNotify()
                                        }
                                    )
                                    var showQualityMenu by remember { mutableStateOf(false) }
                                    if (!showQualityMenu) {
                                        LiquidGlassDropdownItem(
                                            text = LocalStrings.current.mainOpenInPlayer,
                                            icon = Icons.Outlined.PlayArrow,
                                            onClick = { showQualityMenu = true }
                                        )
                                    } else {
                                        listOf("best", "720p60", "480p", "audio_only").forEach { q ->
                                            LiquidGlassDropdownItem(
                                                text = "▶ $q",
                                                icon = Icons.Outlined.PlayArrow,
                                                onClick = {
                                                    showContextMenu = false
                                                    showQualityMenu = false
                                                    io.rudione.chatone.util.system.openInStreamlink(channel.login, q)
                                                }
                                            )
                                        }
                                    }
                                    LiquidGlassDropdownItem(
                                        text = LocalStrings.current.mainCloseChannel,
                                        icon = Icons.Outlined.Close,
                                        onClick = {
                                            showContextMenu = false
                                            onClose()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.width(2.dp))
            }
            if (isActive) {
                ChatoneIconButton(onClick = onClose, modifier = Modifier.size(20.dp)) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Close",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
