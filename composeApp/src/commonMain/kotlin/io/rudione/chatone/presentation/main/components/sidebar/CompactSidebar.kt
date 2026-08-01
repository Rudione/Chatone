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
internal fun MonitorTabSidebarItem(
    login: String,
    isActive: Boolean,
    onClick: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val s = LocalStrings.current
    val label = if (login == "/live") s.openChannelLiveLabel else s.openChannelAutomodLabel
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                else Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            if (login == "/live") Icons.Outlined.Wifi else Icons.Outlined.Shield,
            contentDescription = null,
            tint = if (isActive) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(15.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isActive) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            modifier = Modifier.weight(1f)
        )
        ChatoneIconButton(onClick = onClose, modifier = Modifier.size(20.dp)) {
            Icon(
                Icons.Filled.Close,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(12.dp)
            )
        }
    }
}

@Composable
internal fun CompactSidebar(
    state: MainState,
    onEvent: (MainEvent) -> Unit
) {
    val extra = ChatoneTheme.extraColors
    val expandedFolders = remember { mutableStateListOf<String>() }
    val glowEnabled = LocalWallpaperController.current.state.glowEffectsEnabled
    val compactShape =
        if (glowEnabled) RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
        else RoundedCornerShape(0.dp)
    val compactDivider = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)

    Column(
        modifier = Modifier
            .width(40.dp)
            .fillMaxHeight()
            .then(
                if (glowEnabled) Modifier.shadow(
                    12.dp,
                    compactShape,
                    ambientColor = extra.shadowColor,
                    spotColor = extra.elevatedShadow
                ) else Modifier
            )
            .clip(compactShape)
            .background(
                if (glowEnabled) extra.sidebarSurface
                else MaterialTheme.colorScheme.surfaceContainerLow
            )
            .then(
                if (glowEnabled) Modifier.border(1.dp, extra.glassBorder, compactShape)
                else Modifier.drawBehind {
                    drawRect(
                        color = compactDivider,
                        topLeft = Offset(size.width - 1.dp.toPx(), 0f),
                        size = androidx.compose.ui.geometry.Size(1.dp.toPx(), size.height)
                    )
                }
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(6.dp))
        ChatoneIconButton(
            onClick = { onEvent(MainEvent.ToggleSidebarCollapsed) },
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = LocalStrings.current.mainExpandSidebar,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp)
            )
        }
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 3.dp, horizontal = 6.dp),
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
        )

        val folderedLogins = state.folders.flatMap { it.channels }.map { it.login }.toSet()
        val filteredUnfoldered = state.unfolderedChannels.filter { it.login !in folderedLogins }
        val s = LocalStrings.current

        LazyColumn(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(vertical = 3.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            itemsIndexed(state.folders, key = { _, f -> "cf_${f.id}" }) { _, folder ->
                val isExpanded = folder.id in expandedFolders
                val folderColor = try {
                    val c = folder.color.removePrefix("#").toLong(16)
                    Color(
                        red = ((c shr 16) and 0xFF) / 255f,
                        green = ((c shr 8) and 0xFF) / 255f,
                        blue = (c and 0xFF) / 255f
                    )
                } catch (_: Exception) {
                    ChatoneColors.Violet400
                }

                val hasFolderActive = folder.channels.any { it.login == state.activeChannelLogin }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(36.dp)
                ) {
                    Box(
                        modifier = Modifier.size(28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(7.dp))
                                .background(
                                    if (hasFolderActive) folderColor.copy(alpha = 0.2f)
                                    else if (isExpanded) MaterialTheme.colorScheme.surfaceVariant.copy(
                                        alpha = 0.5f
                                    )
                                    else Color.Transparent
                                )
                                .then(
                                    if (hasFolderActive) Modifier.border(
                                        1.5.dp,
                                        folderColor,
                                        RoundedCornerShape(7.dp)
                                    )
                                    else Modifier
                                )
                                .clickable {
                                    if (isExpanded) expandedFolders.remove(folder.id)
                                    else expandedFolders.add(folder.id)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painterResource(Res.drawable.folder_outline),
                                contentDescription = folder.name,
                                modifier = Modifier.size(14.dp),
                                tint = folderColor
                            )
                        }
                        if (folder.channels.count { it.isLive } > 0) {
                            Box(
                                modifier = Modifier.align(Alignment.TopEnd)
                                    .offset(x = 1.5.dp, y = (-1.5).dp)
                                    .size(7.dp).clip(CircleShape)
                                    .background(ChatoneTheme.extraColors.live)
                                    .border(1.dp, MaterialTheme.colorScheme.surface, CircleShape)
                            )
                        }
                    }

                    Text(
                        text = folder.name.take(6),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 6.sp),
                        color = if (hasFolderActive) folderColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(
                            alpha = 0.7f
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp)
                    )
                }

                if (isExpanded) {
                    folder.channels.forEach { channel ->
                        val isActive = channel.login == state.activeChannelLogin
                        CompactChannelAvatar(
                            channel = channel,
                            isActive = isActive,
                            onClick = { onEvent(MainEvent.SelectChannel(channel.login)) },
                            indented = true
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 7.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                    )
                    Spacer(Modifier.height(2.dp))
                }
            }

            items(filteredUnfoldered, key = { "cu_${it.login}" }) { channel ->
                val isActive = channel.login == state.activeChannelLogin
                CompactChannelAvatar(
                    channel = channel,
                    isActive = isActive,
                    onClick = { onEvent(MainEvent.SelectChannel(channel.login)) },
                    indented = false
                )
            }

            if (state.monitorTabs.isNotEmpty()) {
                items(state.monitorTabs.size, key = { "cmonitor_${state.monitorTabs[it]}" }) { i ->
                    val login = state.monitorTabs[i]
                    val isActive = login == state.activeChannelLogin
                    val label = if (login == "/live") s.openChannelLiveLabel else s.openChannelAutomodLabel
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(
                                if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                else Color.Transparent
                            )
                            .then(
                                if (isActive) Modifier.border(
                                    1.5.dp,
                                    MaterialTheme.colorScheme.primary,
                                    RoundedCornerShape(7.dp)
                                ) else Modifier
                            )
                            .clickable { onEvent(MainEvent.SelectChannel(login)) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (login == "/live") Icons.Outlined.Wifi else Icons.Outlined.Shield,
                            contentDescription = label,
                            tint = if (isActive) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(6.dp)) }
        }

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 6.dp),
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
        )

        Spacer(Modifier.height(3.dp))
        Box(modifier = Modifier.size(28.dp), contentAlignment = Alignment.Center) {
            ChatoneIconButton(
                onClick = { onEvent(MainEvent.ToggleMentionsFeed) },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    Icons.Filled.Notifications,
                    contentDescription = s.chatMentionsTab,
                    tint = if (state.unreadMentionsCount > 0) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
            }
            if (state.unreadMentionsCount > 0) {
                Box(
                    modifier = Modifier.align(Alignment.TopEnd).offset(x = 1.dp, y = (-1).dp)
                        .size(10.dp).clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error)
                        .border(1.dp, MaterialTheme.colorScheme.surface, CircleShape),
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
        Box(modifier = Modifier.size(28.dp), contentAlignment = Alignment.Center) {
            ChatoneIconButton(
                onClick = { onEvent(MainEvent.ToggleWhisperPanel) },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    Icons.Filled.MailOutline,
                    contentDescription = s.chatWhisperTab,
                    tint = if (state.totalUnreadWhispers > 0) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
            }
            if (state.totalUnreadWhispers > 0) {
                Box(
                    modifier = Modifier.align(Alignment.TopEnd).offset(x = 1.dp, y = (-1).dp)
                        .size(10.dp).clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error)
                        .border(1.dp, MaterialTheme.colorScheme.surface, CircleShape),
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
        ChatoneIconButton(
            onClick = { onEvent(MainEvent.ShowSettings) },
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                Icons.Outlined.Settings,
                contentDescription = s.settingsTitle,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp)
            )
        }
        Spacer(Modifier.height(6.dp))
    }
}
