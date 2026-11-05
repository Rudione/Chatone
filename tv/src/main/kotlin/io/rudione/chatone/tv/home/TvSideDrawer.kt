package io.rudione.chatone.tv.home

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.main_add_channel
import io.rudione.chatone.icons.lucide.Plus
import chatone.composeapp.generated.resources.browse_tab_categories
import chatone.composeapp.generated.resources.tv_drawer_channels
import chatone.composeapp.generated.resources.tv_drawer_hold_hint
import chatone.composeapp.generated.resources.tv_drawer_recent
import chatone.composeapp.generated.resources.tv_nav_home
import chatone.composeapp.generated.resources.tv_nav_search
import chatone.composeapp.generated.resources.tv_nav_settings
import coil3.compose.AsyncImage
import io.rudione.chatone.domain.model.ChannelFolder
import io.rudione.chatone.domain.model.ChannelTab
import io.rudione.chatone.domain.model.TwitchAccount
import io.rudione.chatone.icons.lucide.ChevronDown
import io.rudione.chatone.icons.lucide.ChevronRight
import io.rudione.chatone.icons.lucide.Folder
import io.rudione.chatone.icons.lucide.FolderOpen
import io.rudione.chatone.icons.lucide.LayoutGrid
import io.rudione.chatone.icons.lucide.Lucide
import io.rudione.chatone.icons.lucide.MonitorPlay
import io.rudione.chatone.icons.lucide.Search
import io.rudione.chatone.icons.lucide.Settings
import io.rudione.chatone.icons.lucide.User
import io.rudione.chatone.presentation.main.components.sidebar.parseFolderColor
import io.rudione.chatone.tv.theme.TvDimens
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

enum class TvSection { HOME, CATEGORIES, SEARCH, SETTINGS }

@Immutable
private data class TvNavItem(
    val section: TvSection,
    val icon: ImageVector,
    val label: StringResource
)

private val NavItems = listOf(
    TvNavItem(TvSection.HOME, Lucide.MonitorPlay, Res.string.tv_nav_home),
    TvNavItem(TvSection.CATEGORIES, Lucide.LayoutGrid, Res.string.browse_tab_categories),
    TvNavItem(TvSection.SEARCH, Lucide.Search, Res.string.tv_nav_search),
    TvNavItem(TvSection.SETTINGS, Lucide.Settings, Res.string.tv_nav_settings)
)

internal val DrawerExpandedWidth = 300.dp
private val DrawerShape = RoundedCornerShape(topEnd = 32.dp, bottomEnd = 32.dp)
private val AvatarSize = 36.dp
private val LiveRed = Color(0xFFEB0400)
private const val OFFLINE_ALPHA = 0.5f

@Immutable
class TvDrawerChannels(
    val recent: List<ChannelTab>,
    val channels: List<ChannelTab>,
    val folders: List<ChannelFolder>
) {
    val isEmpty: Boolean get() = recent.isEmpty() && channels.isEmpty() && folders.isEmpty()
}

@Immutable
data class TvChannelTarget(val channel: ChannelTab, val recent: Boolean, val folderId: String?)

@Immutable
class TvDrawerActions(
    val onOpenChannel: (ChannelTab) -> Unit,
    val onChannelMenu: (TvChannelTarget) -> Unit,
    val onToggleFolder: (String) -> Unit,
    val onFolderMenu: (ChannelFolder) -> Unit,
    val onAddChannel: () -> Unit
)

@Composable
internal fun TvSideDrawer(
    account: TwitchAccount?,
    selected: TvSection,
    expanded: Boolean,
    channels: TvDrawerChannels,
    actions: TvDrawerActions,
    onSelect: (TvSection) -> Unit,
    onFocusChanged: (Boolean) -> Unit,
    selectionFocus: FocusRequester,
    drawerFocus: FocusRequester,
    modifier: Modifier = Modifier
) {
    val width by animateDpAsState(
        targetValue = if (expanded) DrawerExpandedWidth else TvDimens.RailWidth,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 380f),
        label = "tvDrawerWidth"
    )
    val scheme = MaterialTheme.colorScheme
    LazyColumn(
        modifier
            .width(width)
            .clip(DrawerShape)
            .background(if (expanded) scheme.surface else scheme.background)
            .onFocusChanged { onFocusChanged(it.hasFocus) }
            .focusRequester(drawerFocus)
            .focusRestorer(selectionFocus)
            .focusGroup(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = TvDimens.ScreenVertical),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        item(key = "account", contentType = "account") { TvDrawerAccount(account, expanded) }
        items(NavItems, key = { "nav_${it.section}" }, contentType = { "nav" }) { item ->
            TvDrawerNavItem(
                item = item,
                selected = item.section == selected,
                expanded = expanded,
                onClick = { onSelect(item.section) },
                modifier = if (item.section == selected) Modifier.focusRequester(selectionFocus) else Modifier
            )
        }
        channelSections(channels, actions, expanded)
    }
}

private fun LazyListScope.channelSections(
    channels: TvDrawerChannels,
    actions: TvDrawerActions,
    expanded: Boolean
) {
    item(key = "channels_header", contentType = "header") { TvDrawerHeader(Res.string.tv_drawer_channels, expanded) }
    items(channels.channels, key = { "channel_${it.login}" }, contentType = { "channel" }) { channel ->
        TvDrawerChannelItem(
            channel = channel,
            expanded = expanded,
            onClick = { actions.onOpenChannel(channel) },
            onLongClick = { actions.onChannelMenu(TvChannelTarget(channel, recent = false, folderId = null)) }
        )
    }
    channels.folders.forEach { folder ->
        item(key = "folder_${folder.id}", contentType = "folder") {
            TvDrawerFolderItem(
                folder = folder,
                expanded = expanded,
                onClick = { actions.onToggleFolder(folder.id) },
                onLongClick = { actions.onFolderMenu(folder) }
            )
        }
        if (folder.isExpanded) {
            items(folder.channels, key = { "folder_${folder.id}_${it.login}" }, contentType = { "channel" }) { channel ->
                TvDrawerChannelItem(
                    channel = channel,
                    expanded = expanded,
                    indent = if (expanded) 14.dp else 0.dp,
                    onClick = { actions.onOpenChannel(channel) },
                    onLongClick = { actions.onChannelMenu(TvChannelTarget(channel, recent = false, folderId = folder.id)) }
                )
            }
        }
    }
    item(key = "add_channel", contentType = "nav") {
        TvDrawerActionItem(Lucide.Plus, Res.string.main_add_channel, expanded, actions.onAddChannel)
    }
    if (channels.recent.isNotEmpty()) {
        item(key = "recent_header", contentType = "header") { TvDrawerHeader(Res.string.tv_drawer_recent, expanded) }
        items(channels.recent, key = { "recent_${it.login}" }, contentType = { "channel" }) { channel ->
            TvDrawerChannelItem(
                channel = channel,
                expanded = expanded,
                onClick = { actions.onOpenChannel(channel) },
                onLongClick = { actions.onChannelMenu(TvChannelTarget(channel, recent = true, folderId = null)) }
            )
        }
    }
    if (expanded && !channels.isEmpty) {
        item(key = "hint", contentType = "hint") {
            Text(
                stringResource(Res.string.tv_drawer_hold_hint),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp)
            )
        }
    }
}

@Composable
private fun TvDrawerAccount(account: TwitchAccount?, expanded: Boolean) {
    Row(
        Modifier.padding(start = 6.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (account != null && account.profileImageUrl.isNotBlank()) {
            AsyncImage(
                model = account.profileImageUrl,
                contentDescription = account.displayName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(36.dp).clip(CircleShape)
            )
        } else {
            Icon(Lucide.User, contentDescription = null, modifier = Modifier.size(36.dp).padding(6.dp))
        }
        if (expanded && account != null) {
            Text(
                account.displayName,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun TvDrawerHeader(label: StringResource, expanded: Boolean) {
    if (expanded) {
        Text(
            stringResource(label),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            modifier = Modifier.padding(start = 10.dp, top = 14.dp, bottom = 2.dp)
        )
    } else {
        Box(
            Modifier
                .padding(vertical = 10.dp, horizontal = 14.dp)
                .fillMaxWidth()
                .height(1.dp)
                .background(MaterialTheme.colorScheme.borderVariant)
        )
    }
}

@Composable
private fun drawerItemColors(selected: Boolean) = MaterialTheme.colorScheme.let { scheme ->
    ClickableSurfaceDefaults.colors(
        containerColor = if (selected) scheme.primaryContainer else Color.Transparent,
        contentColor = if (selected) scheme.onPrimaryContainer else scheme.onSurfaceVariant,
        focusedContainerColor = scheme.onSurface,
        focusedContentColor = scheme.surface,
        pressedContainerColor = scheme.primaryContainer,
        pressedContentColor = scheme.onPrimaryContainer
    )
}

@Composable
private fun TvDrawerNavItem(
    item: TvNavItem,
    selected: Boolean,
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = ClickableSurfaceDefaults.shape(CircleShape),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.04f, pressedScale = 0.97f),
        colors = drawerItemColors(selected)
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            val label = stringResource(item.label)
            Icon(item.icon, contentDescription = label, modifier = Modifier.size(24.dp))
            if (expanded) Text(label, style = MaterialTheme.typography.titleSmall, maxLines = 1)
        }
    }
}

@Composable
private fun TvDrawerActionItem(icon: ImageVector, label: StringResource, expanded: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(CircleShape),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.04f, pressedScale = 0.97f),
        colors = drawerItemColors(selected = false)
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            val text = stringResource(label)
            Icon(icon, contentDescription = text, modifier = Modifier.size(24.dp))
            if (expanded) Text(text, style = MaterialTheme.typography.titleSmall, maxLines = 1)
        }
    }
}

@Composable
private fun TvDrawerChannelItem(
    channel: ChannelTab,
    expanded: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    indent: Dp = 0.dp
) {
    Surface(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = Modifier.padding(start = indent),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(18.dp)),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.04f, pressedScale = 0.97f),
        colors = drawerItemColors(selected = false)
    ) {
        Row(
            Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TvChannelAvatar(channel)
            if (expanded) {
                Column(Modifier.weight(1f)) {
                    Text(
                        channel.displayName.ifBlank { channel.login },
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (channel.isLive) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(LiveRed))
                }
            }
        }
    }
}

@Composable
private fun TvChannelAvatar(channel: ChannelTab) {
    val ring = if (channel.isLive) LiveRed else Color.Transparent
    Box(
        Modifier
            .size(AvatarSize)
            .border(BorderStroke(2.dp, ring), CircleShape)
            .padding(3.dp)
    ) {
        AsyncImage(
            model = channel.profileImageUrl,
            contentDescription = channel.displayName,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(AvatarSize)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .alpha(if (channel.isLive) 1f else OFFLINE_ALPHA)
        )
    }
}

@Composable
private fun TvDrawerFolderItem(
    folder: ChannelFolder,
    expanded: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val color = parseFolderColor(folder.color)
    val live = folder.channels.count { it.isLive }
    Surface(
        onClick = onClick,
        onLongClick = onLongClick,
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(18.dp)),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.04f, pressedScale = 0.97f),
        colors = drawerItemColors(selected = false)
    ) {
        Row(
            Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                Modifier.size(AvatarSize).clip(RoundedCornerShape(12.dp)).background(color.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (folder.isExpanded) Lucide.FolderOpen else Lucide.Folder,
                    contentDescription = folder.name,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
            if (expanded) {
                Column(Modifier.weight(1f)) {
                    Text(folder.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (live > 0) {
                        Text(
                            "$live / ${folder.channels.size}",
                            style = MaterialTheme.typography.labelSmall,
                            color = LiveRed
                        )
                    }
                }
                Icon(
                    if (folder.isExpanded) Lucide.ChevronDown else Lucide.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
