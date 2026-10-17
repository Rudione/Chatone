package io.rudione.chatone.presentation.main.components.sidebar

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.rudione.chatone.presentation.theme.ChatoneTheme
import io.rudione.chatone.presentation.theme.LocalWallpaperController
import io.rudione.chatone.presentation.theme.i18n.LocalStrings
import io.rudione.chatone.presentation.main.MainState
import io.rudione.chatone.presentation.main.MainEvent
import io.rudione.chatone.presentation.components.ChatoneIconButton
import io.rudione.chatone.presentation.components.ChatoneTileDefaults
import io.rudione.chatone.presentation.components.ChatoneCountBadge
import io.rudione.chatone.icons.lucide.AtSign
import io.rudione.chatone.icons.lucide.ChevronRight
import io.rudione.chatone.icons.lucide.Folder
import io.rudione.chatone.icons.lucide.Lucide
import io.rudione.chatone.icons.lucide.MessageCircle
import io.rudione.chatone.icons.lucide.Radio
import io.rudione.chatone.icons.lucide.Settings
import io.rudione.chatone.icons.lucide.Shield
import io.rudione.chatone.icons.lucide.X

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
            if (login == "/live") Lucide.Radio else Lucide.Shield,
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
                Lucide.X,
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
    val glowEnabled = LocalWallpaperController.current.state.glowEffectsEnabled
    val compactShape = RoundedCornerShape(ChatoneTileDefaults.radius)

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
                else Modifier
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(6.dp))
        ChatoneIconButton(
            onClick = { onEvent(MainEvent.ToggleSidebarCollapsed) },
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                Lucide.ChevronRight,
                contentDescription = LocalStrings.current.mainExpandSidebar,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp)
            )
        }
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 3.dp, horizontal = 6.dp),
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
        )

        val filteredUnfoldered = remember(state.folders, state.unfolderedChannels) {
            val folderedLogins = state.folders.flatMapTo(HashSet()) { folder -> folder.channels.map { it.login } }
            state.unfolderedChannels.filter { it.login !in folderedLogins }
        }
        val s = LocalStrings.current

        LazyColumn(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(vertical = 3.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            itemsIndexed(state.folders, key = { _, f -> "cf_${f.id}" }, contentType = { _, _ -> "folder" }) { _, folder ->
                val isExpanded = folder.isExpanded
                val folderColor = parseFolderColor(folder.color)

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
                                .clickable { onEvent(MainEvent.ToggleFolder(folder.id)) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Lucide.Folder,
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

            items(filteredUnfoldered, key = { "cu_${it.login}" }, contentType = { "channel" }) { channel ->
                val isActive = channel.login == state.activeChannelLogin
                CompactChannelAvatar(
                    channel = channel,
                    isActive = isActive,
                    onClick = { onEvent(MainEvent.SelectChannel(channel.login)) },
                    indented = false
                )
            }

            if (state.monitorTabs.isNotEmpty()) {
                items(state.monitorTabs.size, key = { "cmonitor_${state.monitorTabs[it]}" }, contentType = { "monitor" }) { i ->
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
                            if (login == "/live") Lucide.Radio else Lucide.Shield,
                            contentDescription = label,
                            tint = if (isActive) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }

            item(key = "bottom_spacer", contentType = "spacer") { Spacer(Modifier.height(6.dp)) }
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
                    Lucide.AtSign,
                    contentDescription = s.chatMentionsTab,
                    tint = if (state.unreadMentionsCount > 0) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
            }
            ChatoneCountBadge(
                count = state.unreadMentionsCount,
                modifier = Modifier.align(Alignment.TopEnd).offset(x = 1.dp, y = (-1).dp)
            )
        }
        Box(modifier = Modifier.size(28.dp), contentAlignment = Alignment.Center) {
            ChatoneIconButton(
                onClick = { onEvent(MainEvent.ToggleWhisperPanel) },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    Lucide.MessageCircle,
                    contentDescription = s.chatWhisperTab,
                    tint = if (state.totalUnreadWhispers > 0) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
            }
            ChatoneCountBadge(
                count = state.totalUnreadWhispers,
                modifier = Modifier.align(Alignment.TopEnd).offset(x = 1.dp, y = (-1).dp)
            )
        }
        ChatoneIconButton(
            onClick = { onEvent(MainEvent.ShowSettings) },
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                Lucide.Settings,
                contentDescription = s.settingsTitle,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp)
            )
        }
        Spacer(Modifier.height(6.dp))
    }
}
