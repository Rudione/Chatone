package io.rudione.chatone.presentation.chat

import androidx.compose.runtime.Composable
import io.rudione.chatone.data.repository.UserNoteRepository
import io.rudione.chatone.domain.model.DisplayMessage
import org.koin.compose.koinInject

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
    val noteRepository: UserNoteRepository = koinInject()
    val sessionMessages = channelMessages
        .filterIsInstance<DisplayMessage.PrivMsg>()
        .filter { it.userId == msg.userId }
        .takeLast(1000)

    DesktopUserProfilePopup(
        userId = msg.userId,
        username = msg.username,
        displayName = msg.displayName,
        color = msg.color,
        initialAvatarUrl = "",
        initialCreatedAt = "",
        badges = msg.badges,
        sevenTvBadge = msg.sevenTvBadge,
        isBroadcaster = msg.isBroadcaster,
        isModerator = msg.isModerator,
        isVip = msg.isVip,
        isSubscriber = msg.isSubscriber,
        sessionMessages = sessionMessages,
        canModerate = showModActions,
        currentUserIsBroadcaster = currentUserIsBroadcaster,
        isBlocked = isBlocked,
        channelId = channelId,
        channelLogin = msg.channel,
        accessToken = accessToken,
        startPinned = true,
        mentionMuteRepository = null,
        noteRepository = noteRepository,
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
        onBanWithReason = { onBan() },
        onDismiss = onClose
    )
}
