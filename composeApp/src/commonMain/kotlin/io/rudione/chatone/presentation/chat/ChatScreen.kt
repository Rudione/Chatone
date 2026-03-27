package io.rudione.chatone.presentation.chat

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import io.rudione.chatone.data.repository.EmoteRepository
import io.rudione.chatone.domain.model.DisplayMessage
import io.rudione.chatone.domain.model.SevenTvCosmetics
import io.rudione.chatone.presentation.settings.SettingsState
import io.rudione.chatone.presentation.settings.SettingsViewModel
import io.rudione.chatone.presentation.theme.ChatoneTheme
import io.rudione.chatone.util.MessageToken
import io.rudione.chatone.util.NotificationSoundPlayer
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    channelLogin: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    accessToken: String = "",
    currentUserId: String = "",
    currentUserLogin: String = "",
    currentDisplayName: String = "",
    onMentionDetected: (String) -> Unit = {},
    viewModel: ChatViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val settingsViewModel: SettingsViewModel = koinViewModel()
    val settingsState by settingsViewModel.state.collectAsState()
    val listState = rememberLazyListState()
    var showEmotePicker by remember { mutableStateOf(false) }
    var showModPanel by remember { mutableStateOf(false) }
    var profilePopupUserId by remember { mutableStateOf<String?>(null) }
    var profilePopupMessage by remember { mutableStateOf<DisplayMessage.PrivMsg?>(null) }
    var pendingModAction by remember { mutableStateOf<PendingModAction?>(null) }
    val emoteRepository: EmoteRepository = koinInject()

    LaunchedEffect(channelLogin, accessToken) {
        viewModel.sendEvent(ChatEvent.OnInit(channelLogin, accessToken, currentUserId, currentUserLogin, currentDisplayName))
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is ChatEffect.ShowError -> {}
                ChatEffect.ScrollToBottom -> {
                    if (state.messages.isNotEmpty()) {
                        listState.animateScrollToItem(state.messages.size - 1)
                    }
                }
                is ChatEffect.MentionDetected -> {
                    if (settingsState.mentionSoundEnabled) {
                        NotificationSoundPlayer.playMentionSound()
                    }
                    onMentionDetected(effect.channelLogin)
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ── Top Bar ─────────────────────────────────────────────────
        ChatTopBar(
            channelLogin = channelLogin,
            connectionStatus = state.connectionStatus,
            isConnected = state.isConnected,
            roomState = state.roomState,
            isMod = state.isMod,
            modModeEnabled = state.modModeEnabled,
            onBack = onNavigateBack,
            onToggleModMode = { viewModel.sendEvent(ChatEvent.OnToggleModMode) },
            onOpenModPanel = { showModPanel = !showModPanel }
        )

        // ── Message List ────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary
                )
            } else if (state.messages.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Outlined.MailOutline,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Waiting for messages...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    items(
                        items = state.messages,
                        key = { it.id }
                    ) { message ->
                        when (message) {
                            is DisplayMessage.PrivMsg -> PrivMsgItem(
                                message = message,
                                showModActions = state.modModeEnabled,
                                timestampFormat = settingsState.timestampFormat,
                                showBadges = settingsState.showBadges,
                                onUsernameClick = {
                                    profilePopupMessage = message
                                    profilePopupUserId = message.userId
                                },
                                onTimeout = {
                                    if (settingsState.confirmModActions) {
                                        pendingModAction = PendingModAction.Timeout(message.userId, message.displayName, settingsState.defaultTimeoutDuration)
                                    } else {
                                        viewModel.sendEvent(ChatEvent.OnTimeoutUser(message.userId, settingsState.defaultTimeoutDuration))
                                    }
                                },
                                onBan = {
                                    if (settingsState.confirmModActions) {
                                        pendingModAction = PendingModAction.Ban(message.userId, message.displayName)
                                    } else {
                                        viewModel.sendEvent(ChatEvent.OnBanUser(message.userId))
                                    }
                                },
                                onDelete = { viewModel.sendEvent(ChatEvent.OnDeleteMessage(message.id)) }
                            )
                            is DisplayMessage.SystemMsg -> SystemMsgItem(message = message)
                            is DisplayMessage.UserNoticeMsg -> UserNoticeMsgItem(message = message)
                            is DisplayMessage.ModerationMsg -> ModerationMsgItem(message = message)
                        }
                    }
                }
            }

            // Scroll to bottom FAB
            val isAtBottom = remember {
                derivedStateOf {
                    val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                    lastVisible >= state.messages.size - 3
                }
            }
            val coroutineScope = rememberCoroutineScope()
            val showScrollButton = !isAtBottom.value && state.messages.isNotEmpty()
            if (showScrollButton) {
                SmallFloatingActionButton(
                    onClick = {
                        if (state.messages.isNotEmpty()) {
                            coroutineScope.launch {
                                listState.animateScrollToItem(state.messages.size - 1)
                            }
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp),
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Scroll to bottom")
                }
            }
        }

        // ── Mod Panel ─────────────────────────────────────────────────
        AnimatedVisibility(
            visible = showModPanel && state.isMod,
            enter = expandVertically(tween(250)) + fadeIn(tween(200)),
            exit = shrinkVertically(tween(250)) + fadeOut(tween(200))
        ) {
            ModerationPanel(
                roomState = state.roomState,
                channelLogin = channelLogin,
                isMod = state.isMod,
                onUpdateChatSettings = { settings ->
                    viewModel.sendEvent(ChatEvent.OnUpdateChatSettings(settings))
                },
                onClearChat = {
                    viewModel.sendEvent(ChatEvent.OnClearChat)
                },
                onClose = { showModPanel = false }
            )
        }

        // ── Emote Autocomplete ────────────────────────────────────────
        if (state.showEmoteCompletions && state.emoteCompletions.isNotEmpty()) {
            EmoteAutocompleteRow(
                emotes = state.emoteCompletions,
                onSelect = { viewModel.sendEvent(ChatEvent.OnSelectEmoteCompletion(it)) },
                onDismiss = { viewModel.sendEvent(ChatEvent.OnDismissCompletions) }
            )
        }

        // ── Message Input ───────────────────────────────────────────
        MessageInput(
            value = state.messageInput,
            onValueChange = { viewModel.sendEvent(ChatEvent.OnMessageInputChanged(it)) },
            onSend = { viewModel.sendEvent(ChatEvent.OnSendMessage) },
            onEmotePickerClick = { showEmotePicker = true },
            enabled = state.isConnected
        )
    }

    // ── Emote Picker ────────────────────────────────────────────────
    if (showEmotePicker) {
        val resolvedEmotes = emoteRepository.getResolvedEmotes(channelLogin)
        EmotePickerSheet(
            emotes = resolvedEmotes.all,
            onEmoteSelected = { emote ->
                val current = state.messageInput
                val newInput = if (current.isEmpty() || current.endsWith(" ")) {
                    "$current${emote.code} "
                } else {
                    "$current ${emote.code} "
                }
                viewModel.sendEvent(ChatEvent.OnMessageInputChanged(newInput))
                showEmotePicker = false
            },
            onDismiss = { showEmotePicker = false }
        )
    }

    // ── User Profile Popup ──────────────────────────────────────────
    profilePopupMessage?.let { msg ->
        UserProfilePopup(
            userId = msg.userId,
            username = msg.username,
            displayName = msg.displayName,
            color = msg.color,
            isModerator = msg.isModerator,
            isSubscriber = msg.isSubscriber,
            isVip = msg.isVip,
            isBroadcaster = msg.isBroadcaster,
            badges = msg.badges,
            sevenTvBadge = msg.sevenTvBadge,
            showModActions = state.modModeEnabled || state.isMod,
            onTimeout = { seconds ->
                viewModel.sendEvent(ChatEvent.OnTimeoutUser(msg.userId, seconds))
            },
            onBan = { viewModel.sendEvent(ChatEvent.OnBanUser(msg.userId)) },
            onUnban = { viewModel.sendEvent(ChatEvent.OnUnbanUser(msg.userId)) },
            onMod = { viewModel.sendEvent(ChatEvent.OnModUser(msg.userId)) },
            onUnmod = { viewModel.sendEvent(ChatEvent.OnUnmodUser(msg.userId)) },
            onVip = { viewModel.sendEvent(ChatEvent.OnVipUser(msg.userId)) },
            onUnvip = { viewModel.sendEvent(ChatEvent.OnUnvipUser(msg.userId)) },
            onWhisper = { viewModel.sendEvent(ChatEvent.OnWhisper(msg.username)) },
            onDismiss = {
                profilePopupMessage = null
                profilePopupUserId = null
            }
        )
    }

    // ── Mod Action Confirmation ──────────────────────────────────────
    pendingModAction?.let { action ->
        ModActionConfirmDialog(
            action = action,
            onConfirm = {
                when (action) {
                    is PendingModAction.Timeout -> viewModel.sendEvent(ChatEvent.OnTimeoutUser(action.userId, action.duration))
                    is PendingModAction.Ban -> viewModel.sendEvent(ChatEvent.OnBanUser(action.userId))
                }
                pendingModAction = null
            },
            onDismiss = { pendingModAction = null }
        )
    }
}

// ─── Mod Action Confirmation ────────────────────────────────────────────

private sealed class PendingModAction {
    data class Timeout(val userId: String, val displayName: String, val duration: Int) : PendingModAction()
    data class Ban(val userId: String, val displayName: String) : PendingModAction()
}

@Composable
private fun ModActionConfirmDialog(
    action: PendingModAction,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val (title, text) = when (action) {
        is PendingModAction.Timeout -> {
            val durationText = when {
                action.duration < 60 -> "${action.duration}s"
                action.duration < 3600 -> "${action.duration / 60}m"
                action.duration < 86400 -> "${action.duration / 3600}h"
                else -> "${action.duration / 86400}d"
            }
            "Timeout ${action.displayName}?" to "Timeout for $durationText"
        }
        is PendingModAction.Ban -> "Ban ${action.displayName}?" to "This will permanently ban the user from chat."
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.SemiBold) },
        text = { Text(text) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (action is PendingModAction.Ban) ChatoneTheme.extraColors.modBan
                    else ChatoneTheme.extraColors.modTimeout
                )
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// ─── Top Bar ────────────────────────────────────────────────────────────

@Composable
private fun ChatTopBar(
    channelLogin: String,
    connectionStatus: String,
    isConnected: Boolean,
    roomState: RoomState,
    isMod: Boolean,
    modModeEnabled: Boolean,
    onBack: () -> Unit,
    onToggleModMode: () -> Unit,
    onOpenModPanel: () -> Unit = {}
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Filled.Menu,
                        contentDescription = "Menu",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Column(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
                    Text(
                        text = "#$channelLogin",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isConnected) ChatoneTheme.extraColors.connected
                                    else MaterialTheme.colorScheme.error
                                )
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = connectionStatus,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Mod mode toggle
                if (isMod) {
                    FilledIconToggleButton(
                        checked = modModeEnabled,
                        onCheckedChange = { onToggleModMode() },
                        modifier = Modifier.size(36.dp),
                        colors = IconButtonDefaults.filledIconToggleButtonColors(
                            checkedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            checkedContentColor = MaterialTheme.colorScheme.primary,
                            containerColor = Color.Transparent,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Icon(
                            Icons.Filled.Star,
                            contentDescription = "Mod Mode",
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Mod panel button
                    IconButton(
                        onClick = onOpenModPanel,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Build,
                            contentDescription = "Mod Panel",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Room state chips
            val roomChips = buildList {
                if (roomState.emoteOnly) add("Emote-only")
                if (roomState.subsOnly) add("Sub-only")
                if (roomState.slowMode > 0) add("Slow: ${roomState.slowMode}s")
                if (roomState.followersOnly >= 0) {
                    add(if (roomState.followersOnly == 0) "Followers" else "Followers: ${roomState.followersOnly}m")
                }
                if (roomState.r9k) add("R9K")
            }

            if (roomChips.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    roomChips.forEach { chip ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = chip,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(2.dp))
            }
        }
    }
}

// ─── Message Items ──────────────────────────────────────────────────────

@Composable
private fun PrivMsgItem(
    message: DisplayMessage.PrivMsg,
    showModActions: Boolean = false,
    timestampFormat: SettingsState.TimestampFormat = SettingsState.TimestampFormat.H24,
    showBadges: Boolean = true,
    onUsernameClick: () -> Unit = {},
    onTimeout: () -> Unit = {},
    onBan: () -> Unit = {},
    onDelete: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val extraColors = ChatoneTheme.extraColors
    val backgroundColor = when {
        message.isDeleted -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
        message.isMention && message.highlightColor != null -> Color(message.highlightColor).copy(alpha = 0.15f)
        message.isMention -> extraColors.mentionHighlight
        else -> Color.Transparent
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Mod actions (compact icons)
        if (showModActions) {
            Column(
                modifier = Modifier.padding(end = 4.dp, top = 2.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(18.dp)
                ) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = "Delete",
                        modifier = Modifier.size(13.dp),
                        tint = extraColors.modDelete.copy(alpha = 0.7f)
                    )
                }
                IconButton(
                    onClick = onTimeout,
                    modifier = Modifier.size(18.dp)
                ) {
                    Icon(
                        Icons.Outlined.Refresh,
                        contentDescription = "Timeout",
                        modifier = Modifier.size(13.dp),
                        tint = extraColors.modTimeout.copy(alpha = 0.7f)
                    )
                }
                IconButton(
                    onClick = onBan,
                    modifier = Modifier.size(18.dp)
                ) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Ban",
                        modifier = Modifier.size(13.dp),
                        tint = extraColors.modBan.copy(alpha = 0.7f)
                    )
                }
            }
        }

        // Timestamp
        if (timestampFormat != SettingsState.TimestampFormat.OFF) {
            Text(
                text = formatTimestamp(message.timestamp, timestampFormat),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.padding(end = 4.dp, top = 1.dp)
            )
        }

        // Badges
        if (showBadges) {
            // Twitch badges
            message.badges.forEach { badge ->
                if (badge.imageUrl.isNotEmpty()) {
                    AsyncImage(
                        model = badge.imageUrl,
                        contentDescription = badge.id,
                        modifier = Modifier
                            .size(18.dp)
                            .padding(end = 2.dp)
                    )
                }
            }

            // 7TV badge
            message.sevenTvBadge?.let { stvBadge ->
                val badgeUrl = stvBadge.url2x.ifEmpty { stvBadge.url1x }
                if (badgeUrl.isNotEmpty()) {
                    AsyncImage(
                        model = badgeUrl,
                        contentDescription = stvBadge.tooltip,
                        modifier = Modifier
                            .size(18.dp)
                            .padding(end = 2.dp)
                    )
                }
            }

            if (message.badges.isNotEmpty() || message.sevenTvBadge != null) {
                Spacer(modifier = Modifier.width(2.dp))
            }
        }

        // Message content with inline emotes
        val userColor = parseColor(message.color) ?: MaterialTheme.colorScheme.primary
        val paintBrush = message.sevenTvPaint?.let { createPaintBrush(it) }
        val inlineContent = mutableMapOf<String, InlineTextContent>()
        var emoteCounter = 0

        val annotatedString = buildAnnotatedString {
            if (message.isDeleted) {
                // Username clickable
                pushStringAnnotation("username", message.userId)
                if (paintBrush != null) {
                    withStyle(SpanStyle(brush = paintBrush, fontWeight = FontWeight.Bold)) {
                        append(message.displayName)
                    }
                } else {
                    withStyle(SpanStyle(color = userColor, fontWeight = FontWeight.Bold)) {
                        append(message.displayName)
                    }
                }
                pop()
                append(": ")
                withStyle(SpanStyle(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                    fontStyle = FontStyle.Italic
                )) {
                    append("<message deleted>")
                }
            } else {
                // Username with optional 7TV paint
                pushStringAnnotation("username", message.userId)
                if (message.isAction) {
                    if (paintBrush != null) {
                        withStyle(SpanStyle(brush = paintBrush, fontStyle = FontStyle.Italic, fontWeight = FontWeight.SemiBold)) {
                            append(message.displayName)
                            append(" ")
                        }
                    } else {
                        withStyle(SpanStyle(color = userColor, fontStyle = FontStyle.Italic, fontWeight = FontWeight.SemiBold)) {
                            append(message.displayName)
                            append(" ")
                        }
                    }
                } else {
                    if (paintBrush != null) {
                        withStyle(SpanStyle(brush = paintBrush, fontWeight = FontWeight.Bold)) {
                            append(message.displayName)
                        }
                    } else {
                        withStyle(SpanStyle(color = userColor, fontWeight = FontWeight.Bold)) {
                            append(message.displayName)
                        }
                    }
                    append(": ")
                }
                pop()

                // Message tokens
                val messageColor = if (message.isAction) userColor else Color.Unspecified
                message.tokens.forEach { token ->
                    when (token) {
                        is MessageToken.Text -> {
                            if (message.isAction) {
                                withStyle(SpanStyle(color = messageColor, fontStyle = FontStyle.Italic)) {
                                    append(token.text)
                                }
                            } else {
                                append(token.text)
                            }
                        }
                        is MessageToken.TwitchEmoteToken -> {
                            val key = "emote_${emoteCounter++}"
                            appendInlineContent(key, token.name)
                            inlineContent[key] = InlineTextContent(
                                Placeholder(24.sp, 24.sp, PlaceholderVerticalAlign.TextCenter)
                            ) {
                                AnimatedEmoteImage(
                                    url = token.url,
                                    contentDescription = token.name,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                        is MessageToken.ThirdPartyEmoteToken -> {
                            val key = "emote_${emoteCounter++}"
                            appendInlineContent(key, token.emote.code)
                            inlineContent[key] = InlineTextContent(
                                Placeholder(24.sp, 24.sp, PlaceholderVerticalAlign.TextCenter)
                            ) {
                                Box {
                                    AnimatedEmoteImage(
                                        url = token.emote.url2x,
                                        contentDescription = token.emote.code,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    token.overlays.forEach { overlay ->
                                        AnimatedEmoteImage(
                                            url = overlay.url2x,
                                            contentDescription = overlay.code,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }
                            }
                        }
                        is MessageToken.Link -> {
                            withStyle(SpanStyle(
                                color = MaterialTheme.colorScheme.primary,
                                textDecoration = TextDecoration.Underline
                            )) {
                                append(token.displayText)
                            }
                        }
                        is MessageToken.Mention -> {
                            withStyle(SpanStyle(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )) {
                                append(token.username)
                            }
                        }
                    }
                }
            }
        }

        Text(
            text = annotatedString,
            inlineContent = inlineContent,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .weight(1f)
                .clickable { onUsernameClick() }
        )
    }
}

@Composable
private fun SystemMsgItem(message: DisplayMessage.SystemMsg) {
    Row(
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Outlined.Info,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = ChatoneTheme.extraColors.systemMessage
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = message.text,
            style = MaterialTheme.typography.bodySmall,
            color = ChatoneTheme.extraColors.systemMessage
        )
    }
}

@Composable
private fun UserNoticeMsgItem(message: DisplayMessage.UserNoticeMsg) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = message.systemText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
        message.innerMessage?.let {
            Spacer(modifier = Modifier.height(2.dp))
            PrivMsgItem(message = it)
        }
    }
}

@Composable
private fun ModerationMsgItem(message: DisplayMessage.ModerationMsg) {
    Row(
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val (icon, color) = when (message.action) {
            DisplayMessage.ModerationMsg.ModerationAction.BAN ->
                Icons.Filled.Close to ChatoneTheme.extraColors.modBan
            DisplayMessage.ModerationMsg.ModerationAction.TIMEOUT ->
                Icons.Outlined.Refresh to ChatoneTheme.extraColors.modTimeout
            DisplayMessage.ModerationMsg.ModerationAction.DELETE ->
                Icons.Outlined.Delete to ChatoneTheme.extraColors.modDelete
            DisplayMessage.ModerationMsg.ModerationAction.CLEAR ->
                Icons.Outlined.Clear to ChatoneTheme.extraColors.modDelete
            DisplayMessage.ModerationMsg.ModerationAction.UNBAN ->
                Icons.Outlined.CheckCircle to ChatoneTheme.extraColors.modUnban
        }

        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = color.copy(alpha = 0.7f)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = message.text,
            style = MaterialTheme.typography.bodySmall,
            color = color.copy(alpha = 0.7f),
            fontStyle = FontStyle.Italic
        )
    }
}

// ─── Message Input ──────────────────────────────────────────────────────

@Composable
private fun MessageInput(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    onEmotePickerClick: () -> Unit = {},
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = ChatoneTheme.extraColors.chatInputSurface,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Emote picker button
            IconButton(
                onClick = onEmotePickerClick,
                enabled = enabled,
                modifier = Modifier.size(36.dp)
            ) {
                Text(
                    text = ":)",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (enabled) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            }

            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        "Send a message...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                },
                enabled = enabled,
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSend() }),
                textStyle = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.width(4.dp))

            // Send button
            FilledIconButton(
                onClick = onSend,
                enabled = enabled && value.isNotBlank(),
                modifier = Modifier.size(36.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                    disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// ─── Emote Autocomplete ─────────────────────────────────────────────────

@Composable
private fun EmoteAutocompleteRow(
    emotes: List<io.rudione.chatone.domain.model.GenericEmote>,
    onSelect: (io.rudione.chatone.domain.model.GenericEmote) -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            emotes.forEach { emote ->
                Surface(
                    onClick = { onSelect(emote) },
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    tonalElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        AnimatedEmoteImage(
                            url = emote.url2x,
                            contentDescription = emote.code,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = emote.code,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

// ─── 7TV Paint ──────────────────────────────────────────────────────────

private fun createPaintBrush(paint: SevenTvCosmetics.Paint): Brush? {
    if (paint.stops.isEmpty()) {
        // Single color paint
        paint.color?.let { argb ->
            val color = argbToColor(argb)
            return Brush.linearGradient(listOf(color, color))
        }
        return null
    }

    val colorStops = paint.stops.map { stop ->
        stop.at to argbToColor(stop.color)
    }.toTypedArray()

    return when (paint.function) {
        "LINEAR_GRADIENT" -> Brush.linearGradient(colorStops = colorStops)
        "RADIAL_GRADIENT" -> Brush.radialGradient(colorStops = colorStops)
        else -> {
            // Fallback to linear gradient
            if (colorStops.isNotEmpty()) Brush.linearGradient(colorStops = colorStops) else null
        }
    }
}

private fun argbToColor(argb: Int): Color {
    val a = ((argb shr 24) and 0xFF) / 255f
    val r = ((argb shr 16) and 0xFF) / 255f
    val g = ((argb shr 8) and 0xFF) / 255f
    val b = (argb and 0xFF) / 255f
    return Color(r, g, b, if (a == 0f) 1f else a)
}

// ─── Helpers ────────────────────────────────────────────────────────────

private fun formatTimestamp(
    timestamp: Long,
    format: SettingsState.TimestampFormat = SettingsState.TimestampFormat.H24
): String {
    val instant = Instant.fromEpochMilliseconds(timestamp)
    val dateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    return when (format) {
        SettingsState.TimestampFormat.H24 ->
            "${dateTime.hour.toString().padStart(2, '0')}:${dateTime.minute.toString().padStart(2, '0')}"
        SettingsState.TimestampFormat.H12 -> {
            val hour12 = if (dateTime.hour == 0) 12 else if (dateTime.hour > 12) dateTime.hour - 12 else dateTime.hour
            val amPm = if (dateTime.hour < 12) "AM" else "PM"
            "$hour12:${dateTime.minute.toString().padStart(2, '0')} $amPm"
        }
        SettingsState.TimestampFormat.OFF -> ""
    }
}

private fun parseColor(hexColor: String?): Color? {
    if (hexColor == null || !hexColor.startsWith("#")) return null
    return try {
        val colorInt = hexColor.substring(1).toLong(16)
        Color(
            red = ((colorInt shr 16) and 0xFF) / 255f,
            green = ((colorInt shr 8) and 0xFF) / 255f,
            blue = (colorInt and 0xFF) / 255f
        )
    } catch (e: Exception) {
        null
    }
}
