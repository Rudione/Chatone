package io.rudione.chatone.presentation.chat

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.SideEffect
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.rudione.chatone.data.repository.EmoteRepository
import io.rudione.chatone.data.repository.MentionMuteRepository
import io.rudione.chatone.domain.model.DisplayMessage
import io.rudione.chatone.domain.model.GenericEmote
import io.rudione.chatone.domain.model.GifSearchItem
import io.rudione.chatone.domain.model.Macro
import io.rudione.chatone.presentation.components.DockHost
import io.rudione.chatone.presentation.components.DockPanel
import io.rudione.chatone.presentation.components.evictedFrom
import io.rudione.chatone.presentation.components.insetBottom
import io.rudione.chatone.presentation.theme.i18n.LocalStrings
import org.koin.compose.koinInject
import io.rudione.chatone.icons.lucide.Lucide
import io.rudione.chatone.icons.lucide.MessagesSquare

internal typealias OpenWhisperCallback =
    (userId: String, username: String, displayName: String, avatarUrl: String, color: String?) -> Unit

@Composable
internal fun DockEvictionEffects(
    dockHost: DockHost?,
    onModerationEvicted: () -> Unit,
    onAutomodEvicted: () -> Unit,
    onEmotesEvicted: () -> Unit,
    onPointsEvicted: () -> Unit
) {
    DockEvictionEffect(dockHost, DockPanel.Moderation, onModerationEvicted)
    DockEvictionEffect(dockHost, DockPanel.Automod, onAutomodEvicted)
    DockEvictionEffect(dockHost, DockPanel.Emotes, onEmotesEvicted)
    DockEvictionEffect(dockHost, DockPanel.Points, onPointsEvicted)
}

@Composable
private fun DockEvictionEffect(dockHost: DockHost?, panel: DockPanel, onEvicted: () -> Unit) {
    var wasDocked by remember { mutableStateOf(false) }
    val evicted by rememberUpdatedState(onEvicted)
    SideEffect(dockHost?.panel) {
        if (dockHost.evictedFrom(panel, wasDocked)) {
            wasDocked = false
            evicted()
        } else if (dockHost?.panel == panel) {
            wasDocked = true
        }
    }
}

@Composable
internal fun ChatModerationPanelSection(
    state: ChatState,
    viewModel: ChatViewModel,
    channelLogin: String,
    pinnedMacros: List<Macro>,
    onOpenLocalAutomod: () -> Unit,
    onClose: () -> Unit
) {
    ModerationPanel(
        roomState = state.roomState,
        channelLogin = channelLogin,
        isMod = state.canModerate,
        pinnedMacros = pinnedMacros,
        onUpdateChatSettings = { settings -> viewModel.sendEvent(ChatEvent.OnUpdateChatSettings(settings)) },
        onClearChat = { viewModel.sendEvent(ChatEvent.OnClearChat) },
        onSendAnnouncement = { message, color -> viewModel.sendEvent(ChatEvent.OnSendAnnouncement(message, color)) },
        onStartRaid = { targetLogin -> viewModel.sendEvent(ChatEvent.OnStartRaid(targetLogin)) },
        onCancelRaid = { viewModel.sendEvent(ChatEvent.OnCancelRaid) },
        onExecuteMacro = { macro -> viewModel.sendEvent(ChatEvent.OnExecuteMacro(macro)) },
        onSendPinMessage = { msg -> viewModel.sendEvent(ChatEvent.OnSendMessageText("/pin $msg")) },
        onShoutout = { target -> viewModel.sendEvent(ChatEvent.OnSendMessageText("/shoutout $target")) },
        onSendRawCommand = { cmd -> viewModel.sendEvent(ChatEvent.OnSendMessageText(cmd)) },
        onOpenLocalAutomod = onOpenLocalAutomod,
        onClose = onClose,
        accessToken = state.currentAccessToken,
        channelId = state.channelId,
        isBroadcaster = state.isBroadcaster
    )
}

@Composable
internal fun ChatDetachedProfileSection(
    msg: DisplayMessage.PrivMsg,
    state: ChatState,
    viewModel: ChatViewModel,
    channelLogin: String,
    onOpenWhisper: OpenWhisperCallback,
    onClose: () -> Unit
) {
    val roles = state.rolesOf(msg)
    val roleActions = rememberRoleActions(viewModel, msg, roles)
    DetachedProfileWindow(
        msg = msg.copy(isModerator = roles.moderator, isVip = roles.vip, badges = msg.badges.matching(roles)),
        channelMessages = state.messages,
        accessToken = state.currentAccessToken,
        channelId = state.channelId,
        showModActions = state.canModerate,
        canManageRoles = state.canManageRoles && msg.userId != state.currentUserId,
        isBlocked = msg.userId in state.blockedUserIds,
        onBlock = { viewModel.sendEvent(ChatEvent.OnBlockUser(msg.userId, msg.username)) },
        onUnblock = { viewModel.sendEvent(ChatEvent.OnUnblockUser(msg.userId, msg.username)) },
        onTimeout = { seconds -> viewModel.sendEvent(ChatEvent.OnTimeoutUser(msg.userId, seconds)) },
        onBan = { viewModel.sendEvent(ChatEvent.OnBanUser(msg.userId)) },
        onUnban = { viewModel.sendEvent(ChatEvent.OnUnbanUser(msg.userId)) },
        onMod = roleActions.onMod,
        onUnmod = roleActions.onUnmod,
        onVip = roleActions.onVip,
        onUnvip = roleActions.onUnvip,
        onWhisper = {
            onOpenWhisper(msg.userId, msg.username, msg.displayName, "", msg.color)
            onClose()
        },
        onClose = onClose
    )
}

@Composable
internal fun ChatProfilePopupSection(
    msg: DisplayMessage.PrivMsg,
    state: ChatState,
    viewModel: ChatViewModel,
    channelLogin: String,
    currentUserId: String,
    mentionMuteRepository: MentionMuteRepository?,
    onOpenWhisper: OpenWhisperCallback,
    onDetach: (DisplayMessage.PrivMsg) -> Unit,
    onPinnedChange: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val target = rememberResolvedProfileMessage(msg)
    val roles = state.rolesOf(target)
    val roleActions = rememberRoleActions(viewModel, target, roles)
    key(currentUserId) {
        UserProfilePopup(
            userId = target.userId,
            username = target.username,
            displayName = target.displayName,
            color = target.color,
            channelMessages = state.messages,
            accessToken = state.currentAccessToken,
            channelId = state.channelId,
            isModerator = roles.moderator,
            isSubscriber = target.isSubscriber,
            isVip = roles.vip,
            isBroadcaster = target.isBroadcaster,
            badges = target.badges.matching(roles),
            sevenTvBadge = target.sevenTvBadge,
            showModActions = state.canModerate,
            canManageRoles = state.canManageRoles && target.userId != state.currentUserId,
            isBlocked = target.userId in state.blockedUserIds,
            onBlock = { viewModel.sendEvent(ChatEvent.OnBlockUser(target.userId, target.username)) },
            onUnblock = { viewModel.sendEvent(ChatEvent.OnUnblockUser(target.userId, target.username)) },
            onTimeout = { seconds -> viewModel.sendEvent(ChatEvent.OnTimeoutUser(target.userId, seconds)) },
            onBan = { viewModel.sendEvent(ChatEvent.OnBanUser(target.userId)) },
            onBanWithReason = { reason -> viewModel.sendEvent(ChatEvent.OnBanUser(target.userId, reason)) },
            onUnban = { viewModel.sendEvent(ChatEvent.OnUnbanUser(target.userId)) },
            onMod = roleActions.onMod,
            onUnmod = roleActions.onUnmod,
            onVip = roleActions.onVip,
            onUnvip = roleActions.onUnvip,
            onWhisper = {
                onOpenWhisper(target.userId, target.username, target.displayName, "", target.color)
                onDismiss()
            },
            onDetach = { onDetach(target) },
            channelLogin = channelLogin,
            mentionMuteRepository = mentionMuteRepository,
            onPinnedChange = onPinnedChange,
            onDismiss = onDismiss
        )
    }
}

@Composable
internal fun ChatEmotePickerSection(
    state: ChatState,
    viewModel: ChatViewModel,
    channelLogin: String,
    closeOnMouseLeave: Boolean,
    dockHost: DockHost?,
    inputHeightPx: () -> Int
) {
    val emoteRepository: EmoteRepository = koinInject()
    val resolvedEmotes = emoteRepository.getResolvedEmotes(channelLogin).copy(
        twitchEmotes = state.twitchChannelEmotes,
        twitchGlobal = state.twitchGlobalEmotes
    )
    val personalEmotes = remember(state.currentUserId, state.twitchSubscriberEmotes) {
        val sevenTv = if (state.currentUserId.isNotEmpty()) {
            emoteRepository.getCachedPersonalEmotes(state.currentUserId)
        } else {
            emptyList()
        }
        (sevenTv + state.twitchSubscriberEmotes).distinctBy { "${it.provider.name}_${it.id}" }
    }
    val onEmotePicked: (GenericEmote) -> Unit = { emote ->
        val current = state.messageInput
        val newInput = if (current.isEmpty() || current.endsWith(" ")) "$current${emote.code} " else "$current ${emote.code} "
        viewModel.sendEvent(ChatEvent.OnMessageInputChanged(newInput))
    }
    val onEmojiPicked: (String) -> Unit = { emoji ->
        val current = state.messageInput
        val newInput = if (current.isEmpty() || current.endsWith(" ")) "$current$emoji" else "$current $emoji"
        viewModel.sendEvent(ChatEvent.OnMessageInputChanged(newInput))
    }
    val onGifPicked: (GifSearchItem) -> Unit = { gif -> viewModel.sendEvent(ChatEvent.OnSendGif(gif)) }
    val onRefreshGifAccess: () -> Unit = { viewModel.sendEvent(ChatEvent.OnRefreshGifAccess) }
    val onDismiss = { viewModel.sendEvent(ChatEvent.OnToggleEmotePicker) }

    if (dockHost == null) {
        Box(modifier = Modifier.fillMaxSize().insetBottom(inputHeightPx)) {
            EmotePickerSheet(
                channelEmotes = resolvedEmotes,
                personalEmotes = personalEmotes,
                closeOnMouseLeave = closeOnMouseLeave,
                onEmoteSelected = onEmotePicked,
                onEmojiSelected = onEmojiPicked,
                onGifSelected = onGifPicked,
                canSendGifs = state.ownSubTier >= 2,
                gifSendError = state.gifSendError,
                gifAccess = state.gifAccess,
                onRefreshGifAccess = onRefreshGifAccess,
                onDismiss = onDismiss
            )
        }
    } else {
        val emoteBody = rememberUpdatedState<@Composable () -> Unit> {
            EmotePickerSheet(
                channelEmotes = resolvedEmotes,
                personalEmotes = personalEmotes,
                closeOnMouseLeave = false,
                onEmoteSelected = onEmotePicked,
                onEmojiSelected = onEmojiPicked,
                onGifSelected = onGifPicked,
                canSendGifs = state.ownSubTier >= 2,
                gifSendError = state.gifSendError,
                gifAccess = state.gifAccess,
                onRefreshGifAccess = onRefreshGifAccess,
                onDismiss = onDismiss,
                docked = true
            )
        }
        LaunchedEffect(Unit) {
            dockHost.openWith(DockPanel.Emotes) { emoteBody.value() }
        }
        DisposableEffect(Unit) {
            onDispose { dockHost.closeIf(DockPanel.Emotes) }
        }
    }
}

@Composable
internal fun ChatWaitingState(isLoading: Boolean) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (isLoading) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Lucide.MessagesSquare,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    LocalStrings.current.chatWaitingForMessages,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
internal fun ChatFileDropHint() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
            .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            LocalStrings.current.uploaderDropHint,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
    }
}
