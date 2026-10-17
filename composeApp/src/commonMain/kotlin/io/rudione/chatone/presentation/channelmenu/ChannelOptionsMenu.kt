package io.rudione.chatone.presentation.channelmenu

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import io.rudione.chatone.data.repository.MentionMuteRepository
import io.rudione.chatone.icons.lucide.Bell
import io.rudione.chatone.icons.lucide.BellOff
import io.rudione.chatone.icons.lucide.Lucide
import io.rudione.chatone.icons.lucide.MessageSquare
import io.rudione.chatone.icons.lucide.Pin
import io.rudione.chatone.icons.lucide.Star
import io.rudione.chatone.icons.lucide.User
import io.rudione.chatone.presentation.components.ChatoneDropdownMenu
import io.rudione.chatone.presentation.screenchat.ScreenChatController
import io.rudione.chatone.presentation.theme.i18n.LocalStrings
import io.rudione.chatone.util.system.ChannelPanelRequestBus
import org.koin.compose.koinInject

@Immutable
class ChannelMenuActions(
    val onShowViewers: () -> Unit,
    val onSetMentionsMuted: (Boolean) -> Unit
)

@Immutable
class ChannelMenuModel(
    val channelLogin: String,
    val canSeeViewers: Boolean,
    val actions: ChannelMenuActions
)

fun busChannelMenu(channelLogin: String, canSeeViewers: Boolean): ChannelMenuModel = ChannelMenuModel(
    channelLogin = channelLogin,
    canSeeViewers = canSeeViewers,
    actions = ChannelMenuActions(
        onShowViewers = { ChannelPanelRequestBus.requestShowChatters(channelLogin) },
        onSetMentionsMuted = { muted -> ChannelPanelRequestBus.requestSetMentionsMuted(channelLogin, muted) }
    )
)

@Composable
fun ChannelOptionsMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    model: ChannelMenuModel,
    offset: DpOffset = DpOffset(0.dp, 0.dp),
    leadingItems: (@Composable ColumnScope.(close: () -> Unit) -> Unit)? = null
) {
    val strings = LocalStrings.current
    val login = model.channelLogin
    val mutes: MentionMuteRepository = koinInject()
    val screenChats: ScreenChatController = koinInject()
    val openScreenChats by screenChats.open.collectAsState()
    val muted = remember(login, expanded) { mutes.isChannelMuted(login) }
    ChatoneDropdownMenu(expanded = expanded, onDismissRequest = onDismissRequest, offset = offset) {
        if (leadingItems != null) {
            leadingItems(onDismissRequest)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        }
        if (screenChats.isSupported) {
            val open = login.lowercase() in openScreenChats
            ChannelMenuItem(
                label = if (open) strings.screenChat.menuClose else strings.screenChat.menuOpen,
                icon = Lucide.MessageSquare
            ) {
                onDismissRequest()
                screenChats.toggle(login)
            }
        }
        if (model.canSeeViewers) {
            ChannelMenuItem(strings.mainViewersList, Lucide.User) {
                onDismissRequest()
                model.actions.onShowViewers()
            }
        }
        ChannelMenuItem(
            label = if (muted) strings.unmuteMentionsForChannel else strings.muteMentionsForChannel,
            icon = if (muted) Lucide.Bell else Lucide.BellOff
        ) {
            onDismissRequest()
            model.actions.onSetMentionsMuted(!muted)
        }
        ChannelMenuItem(strings.mainChannelBadgesBits, Lucide.Star) {
            onDismissRequest()
            ChannelPanelRequestBus.requestOpenPointsBitsPanel(login)
        }
        ChannelMenuItem(strings.mainTogglePinnedMessage, Lucide.Pin) {
            onDismissRequest()
            ChannelPanelRequestBus.requestToggleHiddenEvents(login)
        }
    }
}

@Composable
fun ChannelMenuItem(label: String, icon: ImageVector?, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(label) },
        leadingIcon = icon?.let { { Icon(it, null, modifier = Modifier.size(16.dp)) } },
        onClick = onClick
    )
}
