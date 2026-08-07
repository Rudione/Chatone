package io.rudione.chatone.presentation.chat

import androidx.compose.runtime.Composable
import io.rudione.chatone.domain.model.DisplayMessage

@Composable
actual fun DetachedProfileWindow(
    msg: DisplayMessage.PrivMsg,
    channelMessages: List<DisplayMessage>,
    accessToken: String,
    channelId: String,
    showModActions: Boolean,
    currentUserIsBroadcaster: Boolean,
    isBlocked: Boolean,
    onBlock: () -> Unit,
    onUnblock: () -> Unit,
    onTimeout: (Int) -> Unit,
    onBan: () -> Unit,
    onUnban: () -> Unit,
    onMod: () -> Unit,
    onUnmod: () -> Unit,
    onVip: () -> Unit,
    onUnvip: () -> Unit,
    onWhisper: () -> Unit,
    onClose: () -> Unit
) {
    UserProfilePopup(
        userId = msg.userId,
        username = msg.username,
        displayName = msg.displayName,
        color = msg.color,
        accessToken = accessToken,
        channelId = channelId,
        isModerator = msg.isModerator,
        isSubscriber = msg.isSubscriber,
        isVip = msg.isVip,
        isBroadcaster = msg.isBroadcaster,
        badges = msg.badges,
        sevenTvBadge = msg.sevenTvBadge,
        channelMessages = channelMessages,
        showModActions = showModActions,
        currentUserIsBroadcaster = currentUserIsBroadcaster,
        isBlocked = isBlocked,
        onBlock = onBlock,
        onUnblock = onUnblock,
        onTimeout = onTimeout,
        onBan = onBan,
        onUnban = onUnban,
        onMod = onMod,
        onUnmod = onUnmod,
        onVip = onVip,
        onUnvip = onUnvip,
        onWhisper = onWhisper,
        channelLogin = msg.channel,
        startPinned = true,
        onDismiss = onClose
    )
}
