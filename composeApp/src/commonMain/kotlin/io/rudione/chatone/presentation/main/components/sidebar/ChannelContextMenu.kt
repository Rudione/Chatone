package io.rudione.chatone.presentation.main.components.sidebar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.channel_page_menu_profile
import chatone.composeapp.generated.resources.main_close_channel
import chatone.composeapp.generated.resources.main_folder_with_channel
import chatone.composeapp.generated.resources.main_live_notify_off
import chatone.composeapp.generated.resources.main_live_notify_on
import chatone.composeapp.generated.resources.main_move_to_folder
import chatone.composeapp.generated.resources.main_new_folder
import chatone.composeapp.generated.resources.main_open_on_twitch
import chatone.composeapp.generated.resources.main_remove_from_folder
import chatone.composeapp.generated.resources.main_watch_in_player
import chatone.composeapp.generated.resources.mute_mentions_for_channel
import chatone.composeapp.generated.resources.screen_chat_menu_copy_link
import chatone.composeapp.generated.resources.unmute_mentions_for_channel
import io.rudione.chatone.domain.model.ChannelFolder
import io.rudione.chatone.domain.model.ChannelTab
import io.rudione.chatone.icons.lucide.AtSign
import io.rudione.chatone.icons.lucide.Bell
import io.rudione.chatone.icons.lucide.BellRing
import io.rudione.chatone.icons.lucide.CircleUser
import io.rudione.chatone.icons.lucide.ExternalLink
import io.rudione.chatone.icons.lucide.Folder
import io.rudione.chatone.icons.lucide.FolderOpen
import io.rudione.chatone.icons.lucide.FolderPlus
import io.rudione.chatone.icons.lucide.Link
import io.rudione.chatone.icons.lucide.Lucide
import io.rudione.chatone.icons.lucide.Play
import io.rudione.chatone.icons.lucide.X
import io.rudione.chatone.presentation.components.LiquidGlassDropdownItem
import io.rudione.chatone.presentation.components.LiquidGlassSurface
import io.rudione.chatone.presentation.components.PortalPopup
import io.rudione.chatone.presentation.main.MainEvent
import io.rudione.chatone.presentation.profile.PageNavigator
import io.rudione.chatone.presentation.stream.StreamOpenQuality
import io.rudione.chatone.presentation.stream.rememberStreamLauncher
import io.rudione.chatone.util.link.TwitchLinks
import io.rudione.chatone.util.platform.setText
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@Immutable
internal class ChannelMenuActions(
    val onSelect: () -> Unit,
    val onMoveToFolder: (String?) -> Unit,
    val onCreateFolder: () -> Unit,
    val onToggleLiveNotify: () -> Unit,
    val onSetMentionsMuted: (Boolean) -> Unit,
    val onClose: () -> Unit
)

internal fun sidebarChannelActions(login: String, onEvent: (MainEvent) -> Unit): ChannelMenuActions =
    ChannelMenuActions(
        onSelect = { onEvent(MainEvent.SelectChannel(login)) },
        onMoveToFolder = { folderId -> onEvent(MainEvent.MoveChannelToFolder(login, folderId)) },
        onCreateFolder = { onEvent(MainEvent.ShowCreateFolderDialog(login)) },
        onToggleLiveNotify = { onEvent(MainEvent.ToggleLiveNotify(login)) },
        onSetMentionsMuted = { muted ->
            onEvent(if (muted) MainEvent.MuteMentionsChannel(login) else MainEvent.UnmuteMentionsChannel(login))
        },
        onClose = { onEvent(MainEvent.CloseChannel(login)) }
    )

@Composable
internal fun rememberChannelMenuActions(login: String, onEvent: (MainEvent) -> Unit): ChannelMenuActions =
    remember(login, onEvent) { sidebarChannelActions(login, onEvent) }

internal sealed interface ChannelMenuAnchor {
    data object Button : ChannelMenuAnchor
    data class Pointer(val position: IntOffset) : ChannelMenuAnchor
}

@Composable
internal fun ChannelContextMenu(
    channel: ChannelTab,
    folders: List<ChannelFolder>,
    currentFolderId: String?,
    liveNotifyEnabled: Boolean,
    anchor: ChannelMenuAnchor,
    actions: ChannelMenuActions,
    onDismiss: () -> Unit
) {
    val density = LocalDensity.current
    val position = remember(anchor, density) {
        with(density) { ChannelMenuPosition(anchor, 24.dp.roundToPx(), 8.dp.roundToPx()) }
    }
    val appearance = remember { MutableTransitionState(false).apply { targetState = true } }
    PortalPopup(
        popupPositionProvider = position,
        properties = PopupProperties(focusable = true, dismissOnBackPress = true, dismissOnClickOutside = true),
        onDismissRequest = onDismiss
    ) {
        AnimatedVisibility(
            visibleState = appearance,
            enter = fadeIn(tween(110)) + scaleIn(
                animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
                initialScale = 0.82f,
                transformOrigin = if (anchor is ChannelMenuAnchor.Pointer) TransformOrigin(0f, 0f) else TransformOrigin(1f, 0f)
            )
        ) {
            LiquidGlassSurface(
                modifier = Modifier
                    .widthIn(min = 216.dp, max = 300.dp)
                    .width(IntrinsicSize.Max)
                    .shadow(16.dp, RoundedCornerShape(14.dp)),
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(4.dp),
                backgroundAlphaHigh = 0.99f
            ) {
                ChannelMenuItems(
                    channel = channel,
                    folders = folders,
                    currentFolderId = currentFolderId,
                    liveNotifyEnabled = liveNotifyEnabled,
                    foldersOpen = anchor == ChannelMenuAnchor.Button,
                    actions = actions,
                    onDismiss = onDismiss
                )
            }
        }
    }
}

@Composable
private fun ChannelMenuItems(
    channel: ChannelTab,
    folders: List<ChannelFolder>,
    currentFolderId: String?,
    liveNotifyEnabled: Boolean,
    foldersOpen: Boolean,
    actions: ChannelMenuActions,
    onDismiss: () -> Unit
) {
    val pages = koinInject<PageNavigator>()
    val streamLauncher = rememberStreamLauncher()
    val uriHandler = LocalUriHandler.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val channelUrl = TwitchLinks.channelUrl(channel.login)
    var showQualities by remember { mutableStateOf(false) }
    var showFolders by remember { mutableStateOf(foldersOpen) }

    fun run(action: () -> Unit) {
        onDismiss()
        action()
    }

    Column {
        if (pages.isSupported) {
            LiquidGlassDropdownItem(
                text = stringResource(Res.string.channel_page_menu_profile),
                icon = Lucide.CircleUser,
                onClick = { run { pages.openChannel(channel.login) } }
            )
        }
        if (streamLauncher.opensInApp) {
            LiquidGlassDropdownItem(
                text = stringResource(Res.string.main_watch_in_player),
                icon = Lucide.Play,
                onClick = { showQualities = !showQualities }
            )
            ExpandedGroup(showQualities) {
                StreamOpenQuality.entries.forEach { quality ->
                    LiquidGlassDropdownItem(
                        text = quality.label,
                        icon = Lucide.Play,
                        onClick = {
                            run {
                                actions.onSelect()
                                streamLauncher.open(channel.login, quality)
                            }
                        }
                    )
                }
            }
        }
        if (channelUrl != null) {
            LiquidGlassDropdownItem(
                text = stringResource(Res.string.main_open_on_twitch),
                icon = Lucide.ExternalLink,
                onClick = { run { uriHandler.openUri(channelUrl) } }
            )
            LiquidGlassDropdownItem(
                text = stringResource(Res.string.screen_chat_menu_copy_link),
                icon = Lucide.Link,
                onClick = { run { scope.launch(start = CoroutineStart.UNDISPATCHED) { clipboard.setText(channelUrl) } } }
            )
        }
        MenuDivider()
        if (folders.isEmpty()) {
            LiquidGlassDropdownItem(
                text = stringResource(Res.string.main_folder_with_channel),
                icon = Lucide.FolderPlus,
                iconTint = MaterialTheme.colorScheme.primary,
                onClick = { run(actions.onCreateFolder) }
            )
        } else {
            LiquidGlassDropdownItem(
                text = stringResource(Res.string.main_move_to_folder),
                icon = Lucide.Folder,
                onClick = { showFolders = !showFolders }
            )
            ExpandedGroup(showFolders) {
                folders.filter { it.id != currentFolderId }.forEach { folder ->
                    LiquidGlassDropdownItem(
                        text = folder.name,
                        icon = Lucide.Folder,
                        iconTint = parseFolderColor(folder.color),
                        onClick = { run { actions.onMoveToFolder(folder.id) } }
                    )
                }
                LiquidGlassDropdownItem(
                    text = stringResource(Res.string.main_new_folder),
                    icon = Lucide.FolderPlus,
                    onClick = { run(actions.onCreateFolder) }
                )
            }
        }
        if (currentFolderId != null) {
            LiquidGlassDropdownItem(
                text = stringResource(Res.string.main_remove_from_folder),
                icon = Lucide.FolderOpen,
                onClick = { run { actions.onMoveToFolder(null) } }
            )
        }
        MenuDivider()
        LiquidGlassDropdownItem(
            text = stringResource(if (liveNotifyEnabled) Res.string.main_live_notify_off else Res.string.main_live_notify_on),
            icon = if (liveNotifyEnabled) Lucide.BellRing else Lucide.Bell,
            onClick = { run(actions.onToggleLiveNotify) }
        )
        LiquidGlassDropdownItem(
            text = stringResource(if (channel.notificationsMuted) Res.string.unmute_mentions_for_channel else Res.string.mute_mentions_for_channel),
            icon = Lucide.AtSign,
            onClick = { run { actions.onSetMentionsMuted(!channel.notificationsMuted) } }
        )
        MenuDivider()
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.error) {
            LiquidGlassDropdownItem(
                text = stringResource(Res.string.main_close_channel),
                icon = Lucide.X,
                iconTint = MaterialTheme.colorScheme.error,
                onClick = { run(actions.onClose) }
            )
        }
    }
}

@Composable
private fun ExpandedGroup(visible: Boolean, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = expandVertically(spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn()
    ) {
        Column(Modifier.padding(start = 12.dp)) { content() }
    }
}

@Composable
private fun MenuDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 4.dp),
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
    )
}

private class ChannelMenuPosition(
    private val anchor: ChannelMenuAnchor,
    private val buttonGap: Int,
    private val margin: Int
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        val width = popupContentSize.width
        val height = popupContentSize.height
        val (x, below, above) = when (anchor) {
            ChannelMenuAnchor.Button -> Triple(
                anchorBounds.right - width,
                anchorBounds.top + buttonGap,
                anchorBounds.top - height
            )
            is ChannelMenuAnchor.Pointer -> {
                val point = anchorBounds.topLeft + anchor.position
                val left = if (point.x + width > windowSize.width - margin) point.x - width else point.x
                Triple(left, point.y, point.y - height)
            }
        }
        val y = if (below + height > windowSize.height - margin && above >= margin) above else below
        return IntOffset(
            x.coerceIn(margin, (windowSize.width - width - margin).coerceAtLeast(margin)),
            y.coerceIn(margin, (windowSize.height - height - margin).coerceAtLeast(margin))
        )
    }
}
