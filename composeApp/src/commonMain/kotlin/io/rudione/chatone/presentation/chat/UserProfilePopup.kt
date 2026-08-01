package io.rudione.chatone.presentation.chat

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.copy_check
import chatone.composeapp.generated.resources.ic_copy
import chatone.composeapp.generated.resources.ic_twitch
import coil3.compose.AsyncImage
import io.rudione.chatone.data.remote.TwitchApiClient
import io.rudione.chatone.data.repository.UserNoteRepository
import io.rudione.chatone.domain.model.Badge
import io.rudione.chatone.domain.model.DisplayMessage
import io.rudione.chatone.util.Result
import io.rudione.chatone.domain.model.SevenTvCosmetics
import io.rudione.chatone.presentation.components.ExpressiveCheckbox
import io.rudione.chatone.presentation.theme.ChatoneTheme
import io.rudione.chatone.util.chat.MessageToken
import io.rudione.chatone.presentation.theme.i18n.LocalStrings
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import io.rudione.chatone.presentation.components.ChatoneIconButton
import io.rudione.chatone.presentation.components.ChatoneTextField

@OptIn(ExperimentalLayoutApi::class, ExperimentalComposeUiApi::class)
@Composable
fun UserProfilePopup(
    userId: String,
    username: String,
    displayName: String,
    color: String?,
    accessToken: String = "",
    channelId: String = "",
    profileImageUrl: String = "",
    createdAt: String = "",
    isModerator: Boolean = false,
    isSubscriber: Boolean = false,
    isVip: Boolean = false,
    isBroadcaster: Boolean = false,
    badges: List<Badge> = emptyList(),
    sevenTvBadge: SevenTvCosmetics.Badge? = null,
    channelMessages: List<DisplayMessage> = emptyList(),
    showModActions: Boolean = false,

    currentUserIsBroadcaster: Boolean = false,
    isBlocked: Boolean = false,
    onBlock: () -> Unit = {},
    onUnblock: () -> Unit = {},
    onTimeout: (Int) -> Unit = {},
    onBan: () -> Unit = {},
    onBanWithReason: (String) -> Unit = { onBan() },
    onUnban: () -> Unit = {},
    onMod: () -> Unit = {},
    onUnmod: () -> Unit = {},
    onVip: () -> Unit = {},
    onUnvip: () -> Unit = {},
    onWhisper: () -> Unit = {},
    onDetach: (() -> Unit)? = null,
    channelLogin: String = "",
    mentionMuteRepository: io.rudione.chatone.data.repository.MentionMuteRepository? = null,
    onDismiss: () -> Unit
) {
    val noteRepository: UserNoteRepository = koinInject()
    val twitchApiClient: TwitchApiClient = koinInject()
    val twitchGqlClient: io.rudione.chatone.data.remote.TwitchGqlClient = koinInject()
    val chatRepository: io.rudione.chatone.data.repository.ChatRepository = koinInject()
    val moderationHistoryRepository: io.rudione.chatone.data.repository.ModerationHistoryRepository = koinInject()
    var noteText by remember { mutableStateOf("") }
    var isNoteLoaded by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current

    var moderationHistory by remember(userId) { mutableStateOf<List<io.rudione.chatone.data.repository.ModerationHistoryEntry>>(emptyList()) }

    var localHistory by remember(userId) { mutableStateOf<List<io.rudione.chatone.domain.model.ChatMessage>>(emptyList()) }

    var historyMessages by remember(userId) { mutableStateOf<List<io.rudione.chatone.data.remote.GqlUsercardMessage>>(emptyList()) }
    var historyCursor by remember(userId) { mutableStateOf<String?>(null) }
    var hasMoreHistory by remember(userId) { mutableStateOf(false) }
    var isHistoryLoading by remember(userId) { mutableStateOf(false) }
    var historyLoadFailed by remember(userId) { mutableStateOf(false) }
    val historyScope = rememberCoroutineScope()
    val loadHistory: () -> Unit = {
        if (!isHistoryLoading && accessToken.isNotEmpty() && channelId.isNotEmpty() && userId.isNotEmpty()) {
            isHistoryLoading = true
            historyLoadFailed = false
            historyScope.launch {
                val page = twitchGqlClient.getUsercardMessagesBySender(
                    channelId, userId, historyCursor, accessToken
                )
                if (page != null) {
                    historyMessages = historyMessages + page.messages
                    historyCursor = page.nextCursor
                    hasMoreHistory = page.hasNextPage
                } else {
                    historyLoadFailed = true
                }
                isHistoryLoading = false
            }
        }
    }

    var fetchedAvatarUrl by remember(userId) { mutableStateOf(profileImageUrl) }
    var fetchedCreatedAt by remember(userId) { mutableStateOf(createdAt) }
    var followedAt by remember(userId) { mutableStateOf<String?>(null) }
    var subAge by remember(userId) { mutableStateOf<io.rudione.chatone.data.remote.SubAgeInfo?>(null) }
    val ivrApiClient: io.rudione.chatone.data.remote.IvrApiClient = koinInject()

    var selectedTab by remember { mutableIntStateOf(0) }
    var banReasonPrompt by remember { mutableStateOf(false) }
    var banReasonText by remember { mutableStateOf("") }

    val userMessages = remember(channelMessages, userId) {
        channelMessages
            .filterIsInstance<DisplayMessage.PrivMsg>()
            .filter { it.userId == userId }
            .takeLast(1000)
    }

    LaunchedEffect(userId) {
        fetchedAvatarUrl = profileImageUrl
        fetchedCreatedAt = createdAt
        followedAt = null
        subAge = null
        if (username.isNotEmpty() && channelLogin.isNotEmpty()) {
            launch { subAge = ivrApiClient.getSubAge(username, channelLogin) }
        }

        if (channelId.isNotEmpty() && userId.isNotEmpty()) {
            launch {
                val sessionIds = userMessages.map { it.id }.toSet()
                localHistory = chatRepository.getLocalHistoryForUser(channelId, userId)
                    .filterNot { it.id in sessionIds }
            }
            moderationHistory = moderationHistoryRepository.getHistoryForUser(channelId, userId)
        }

        val existing = noteRepository.getNote(userId)
        noteText = existing ?: ""
        isNoteLoaded = true

        if (accessToken.isNotEmpty() && userId.isNotEmpty()) {
            val result = twitchApiClient.getUsers(accessToken, ids = listOf(userId))
            if (result is Result.Success) {
                result.data.data.firstOrNull()?.let { userData ->
                    fetchedAvatarUrl = userData.profileImageUrl
                    fetchedCreatedAt = userData.createdAt.take(10)
                }
            }
            if (channelId.isNotEmpty()) {
                try {
                    val followResult =
                        twitchApiClient.getChannelFollower(accessToken, channelId, userId)
                    if (followResult is Result.Success) {
                        followResult.data.data.firstOrNull()?.let { follower ->
                            followedAt = follower.followedAt.take(10)
                        }
                    }
                } catch (_: Exception) {
                }
            }
        }
    }

    if (showDeleteConfirmation) {
        val sd = LocalStrings.current
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text(sd.profileDeleteNote) },
            text = { Text(sd.profileDeleteNoteConfirm) },
            confirmButton = {
                TextButton(
                    onClick = {
                        noteRepository.deleteNote(userId); noteText = ""; showDeleteConfirmation =
                        false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text(sd.delete) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteConfirmation = false
                }) { Text(sd.cancel) }
            }
        )
    }

    Popup(onDismissRequest = onDismiss, properties = PopupProperties(focusable = true)) {
        Card(
            modifier = Modifier.width(320.dp).padding(8.dp)
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown || !event.isAltPressed) return@onPreviewKeyEvent false
                    if (!showModActions || isBroadcaster) return@onPreviewKeyEvent false
                    when (event.key) {
                        Key.B -> {
                            if (event.isShiftPressed) { banReasonPrompt = true }
                            else { onBan(); onDismiss() }
                            true
                        }
                        Key.U -> { onUnban(); onDismiss(); true }
                        Key.W -> { onWhisper(); onDismiss(); true }
                        Key.K -> { if (isBlocked) onUnblock() else onBlock(); onDismiss(); true }
                        Key.M -> if (currentUserIsBroadcaster) {
                            if (isModerator) onUnmod() else onMod(); onDismiss(); true
                        } else false
                        Key.V -> if (currentUserIsBroadcaster) {
                            if (isVip) onUnvip() else onVip(); onDismiss(); true
                        } else false
                        else -> false
                    }
                },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column {
                UserProfileHeader(
                    avatarUrl = fetchedAvatarUrl,
                    displayName = displayName,
                    username = username,
                    color = color,
                    badges = badges,
                    sevenTvBadge = sevenTvBadge,
                    isBroadcaster = isBroadcaster,
                    isModerator = isModerator,
                    isVip = isVip,
                    isSubscriber = isSubscriber,
                    onDetach = onDetach,
                    onDismiss = onDismiss
                )
                CompactProfileTabs(
                    selectedTab = selectedTab,
                    onSelect = { selectedTab = it },
                    messagesCount = userMessages.size,
                    showHistoryTab = showModActions,
                    historyCount = moderationHistory.size
                )

                when (selectedTab) {
                    0 -> UsercardTab(
                        userId = userId,
                        fetchedCreatedAt = fetchedCreatedAt,
                        subAge = subAge,
                        followedAt = followedAt,
                        noteText = noteText,
                        isNoteLoaded = isNoteLoaded,
                        noteRepository = noteRepository,
                        clipboardManager = clipboardManager,
                        showModActions = showModActions,
                        isBroadcaster = isBroadcaster,
                        isModerator = isModerator,
                        isVip = isVip,
                        isSubscriber = isSubscriber,
                        currentUserIsBroadcaster = currentUserIsBroadcaster,
                        isBlocked = isBlocked,
                        onBlock = onBlock,
                        onUnblock = onUnblock,
                        onNoteChange = { noteText = it },
                        onShowDeleteConfirmation = { showDeleteConfirmation = true },
                        onWhisper = onWhisper,
                        onTimeout = onTimeout,
                        onBan = onBan,
                        onUnban = onUnban,
                        onMod = onMod,
                        onUnmod = onUnmod,
                        onVip = onVip,
                        onUnvip = onUnvip,
                        onDismiss = onDismiss,
                        channelLogin = channelLogin,
                        mentionMuteRepository = mentionMuteRepository,
                        username = username
                    )

                    1 -> MessagesTab(
                        messages = userMessages,
                        displayName = displayName,
                        userColor = color,
                        history = historyMessages,
                        isHistoryLoading = isHistoryLoading,
                        hasMoreHistory = hasMoreHistory,
                        historyLoadFailed = historyLoadFailed,
                        onLoadHistory = loadHistory,
                        localHistory = localHistory
                    )

                    2 -> ModerationHistoryTab(entries = moderationHistory)
                }
            }
        }
    }

    if (banReasonPrompt) {
        AlertDialog(
            onDismissRequest = { banReasonPrompt = false },
            title = { Text("${LocalStrings.current.profileBan}: $displayName") },
            text = {
                Column {
                    Text(
                        LocalStrings.current.chatBanReasonHint,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    ChatoneTextField(
                        value = banReasonText,
                        onValueChange = { banReasonText = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = LocalStrings.current.chatBanReasonPlaceholder,
                        singleLine = false
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    onBanWithReason(banReasonText.trim())
                    banReasonPrompt = false
                    onDismiss()
                }) { Text(LocalStrings.current.profileBan) }
            },
            dismissButton = {
                TextButton(onClick = { banReasonPrompt = false }) { Text(LocalStrings.current.cancel) }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UserProfileContent(
    msg: DisplayMessage.PrivMsg,
    channelMessages: List<DisplayMessage>,
    accessToken: String,
    channelId: String,
    showModActions: Boolean,
    currentUserIsBroadcaster: Boolean,
    isBlocked: Boolean = false,
    onBlock: () -> Unit = {},
    onUnblock: () -> Unit = {},
    onTimeout: (Int) -> Unit,
    onBan: () -> Unit,
    onUnban: () -> Unit,
    onMod: () -> Unit,
    onUnmod: () -> Unit,
    onVip: () -> Unit,
    onUnvip: () -> Unit,
    onWhisper: () -> Unit,
    onDismiss: () -> Unit
) {
    val noteRepository: UserNoteRepository = koinInject()
    val twitchApiClient: TwitchApiClient = koinInject()
    val twitchGqlClient: io.rudione.chatone.data.remote.TwitchGqlClient = koinInject()
    val chatRepository: io.rudione.chatone.data.repository.ChatRepository = koinInject()
    val moderationHistoryRepository: io.rudione.chatone.data.repository.ModerationHistoryRepository = koinInject()
    var noteText by remember { mutableStateOf("") }
    var isNoteLoaded by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current
    var localHistory by remember(msg.userId) { mutableStateOf<List<io.rudione.chatone.domain.model.ChatMessage>>(emptyList()) }
    var moderationHistory by remember(msg.userId) { mutableStateOf<List<io.rudione.chatone.data.repository.ModerationHistoryEntry>>(emptyList()) }

    var fetchedAvatarUrl by remember(msg.userId) { mutableStateOf("") }
    var fetchedCreatedAt by remember(msg.userId) { mutableStateOf("") }
    var followedAt by remember(msg.userId) { mutableStateOf<String?>(null) }
    var selectedTab by remember { mutableIntStateOf(0) }

    var historyMessages by remember(msg.userId) { mutableStateOf<List<io.rudione.chatone.data.remote.GqlUsercardMessage>>(emptyList()) }
    var historyCursor by remember(msg.userId) { mutableStateOf<String?>(null) }
    var hasMoreHistory by remember(msg.userId) { mutableStateOf(false) }
    var isHistoryLoading by remember(msg.userId) { mutableStateOf(false) }
    var historyLoadFailed by remember(msg.userId) { mutableStateOf(false) }
    val historyScope = rememberCoroutineScope()
    val loadHistory: () -> Unit = {
        if (!isHistoryLoading && accessToken.isNotEmpty() && channelId.isNotEmpty()) {
            isHistoryLoading = true
            historyLoadFailed = false
            historyScope.launch {
                val page = twitchGqlClient.getUsercardMessagesBySender(
                    channelId, msg.userId, historyCursor, accessToken
                )
                if (page != null) {
                    historyMessages = historyMessages + page.messages
                    historyCursor = page.nextCursor
                    hasMoreHistory = page.hasNextPage
                } else {
                    historyLoadFailed = true
                }
                isHistoryLoading = false
            }
        }
    }

    val userMessages = remember(channelMessages, msg.userId) {
        channelMessages.filterIsInstance<DisplayMessage.PrivMsg>()
            .filter { it.userId == msg.userId }.takeLast(1000).reversed()
    }

    LaunchedEffect(msg.userId) {
        if (channelId.isNotEmpty()) {
            launch {
                val sessionIds = userMessages.map { it.id }.toSet()
                localHistory = chatRepository.getLocalHistoryForUser(channelId, msg.userId)
                    .filterNot { it.id in sessionIds }
            }
            moderationHistory = moderationHistoryRepository.getHistoryForUser(channelId, msg.userId)
        }

        val existing = noteRepository.getNote(msg.userId)
        noteText = existing ?: ""
        isNoteLoaded = true
        if (accessToken.isNotEmpty()) {
            val result = twitchApiClient.getUsers(accessToken, ids = listOf(msg.userId))
            if (result is Result.Success) {
                result.data.data.firstOrNull()?.let { userData ->
                    fetchedAvatarUrl = userData.profileImageUrl
                    fetchedCreatedAt = userData.createdAt.take(10)
                }
            }
            if (channelId.isNotEmpty()) {
                try {
                    val fr = twitchApiClient.getChannelFollower(accessToken, channelId, msg.userId)
                    if (fr is Result.Success) followedAt =
                        fr.data.data.firstOrNull()?.followedAt?.take(10)
                } catch (_: Exception) {
                }
            }
        }
    }

    if (showDeleteConfirmation) {
        val sd2 = LocalStrings.current
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text(sd2.profileDeleteNote) },
            text = { Text(sd2.profileDeleteNoteConfirm) },
            confirmButton = {
                TextButton(
                    onClick = {
                        noteRepository.deleteNote(msg.userId); noteText =
                        ""; showDeleteConfirmation = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text(sd2.delete) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteConfirmation = false
                }) { Text(sd2.cancel) }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        UserProfileHeader(
            avatarUrl = fetchedAvatarUrl,
            displayName = msg.displayName,
            username = msg.username,
            color = msg.color,
            badges = msg.badges,
            sevenTvBadge = msg.sevenTvBadge,
            isBroadcaster = msg.isBroadcaster,
            isModerator = msg.isModerator,
            isVip = msg.isVip,
            isSubscriber = msg.isSubscriber,
            onDetach = null,
            onDismiss = onDismiss,
            showIconDetached = false
        )

        CompactProfileTabs(
            selectedTab = selectedTab,
            onSelect = { selectedTab = it },
            messagesCount = userMessages.size,
            showHistoryTab = showModActions,
            historyCount = moderationHistory.size
        )
        when (selectedTab) {
            0 -> UsercardTab(
                userId = msg.userId,
                fetchedCreatedAt = fetchedCreatedAt,
                followedAt = followedAt,
                noteText = noteText,
                isNoteLoaded = isNoteLoaded,
                noteRepository = noteRepository,
                clipboardManager = clipboardManager,
                showModActions = showModActions,
                isBroadcaster = msg.isBroadcaster,
                isModerator = msg.isModerator,
                isVip = msg.isVip,
                isSubscriber = msg.isSubscriber,
                currentUserIsBroadcaster = currentUserIsBroadcaster,
                isBlocked = isBlocked,
                onBlock = onBlock,
                onUnblock = onUnblock,
                onNoteChange = { noteText = it },
                onShowDeleteConfirmation = { showDeleteConfirmation = true },
                onWhisper = onWhisper,
                onTimeout = onTimeout,
                onBan = onBan,
                onUnban = onUnban,
                onMod = onMod,
                onUnmod = onUnmod,
                onVip = onVip,
                onUnvip = onUnvip,
                onDismiss = onDismiss,
                username = msg.username
            )

            1 -> MessagesTab(
                messages = userMessages,
                displayName = msg.displayName,
                userColor = msg.color,
                history = historyMessages,
                isHistoryLoading = isHistoryLoading,
                hasMoreHistory = hasMoreHistory,
                historyLoadFailed = historyLoadFailed,
                onLoadHistory = loadHistory,
                historyAtTop = false,
                localHistory = localHistory
            )

            2 -> ModerationHistoryTab(entries = moderationHistory)
        }
    }
}

@Composable
fun UserProfileHeader(
    avatarUrl: String,
    displayName: String,
    username: String,
    color: String?,
    badges: List<Badge>,
    sevenTvBadge: SevenTvCosmetics.Badge?,
    isBroadcaster: Boolean,
    isModerator: Boolean,
    isVip: Boolean,
    isSubscriber: Boolean,
    onDetach: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
    showIconDetached: Boolean = true
) {
    val clipboardManager = LocalClipboardManager.current
    var showCopied by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current

    LaunchedEffect(showCopied) {
        if (showCopied) {
            kotlinx.coroutines.delay(1500)
            showCopied = false
        }
    }

    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (avatarUrl.isNotEmpty()) {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = displayName,
                    modifier = Modifier.size(44.dp).clip(CircleShape)
                )
            } else {
                Box(
                    modifier = Modifier.size(44.dp).clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        displayName.take(2).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                val nameColor = parseHexColor(color) ?: MaterialTheme.colorScheme.primary
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        displayName, style = MaterialTheme.typography.titleMedium,
                        color = nameColor, fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(4.dp))
                    ChatoneIconButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(username))
                            showCopied = true
                        },
                        modifier = Modifier.size(22.dp)
                    ) {
                        Icon(
                            if (showCopied) painterResource(Res.drawable.copy_check)
                            else painterResource(
                                Res.drawable.ic_copy
                            ),
                            contentDescription = "Copy username",
                            modifier = Modifier.size(14.dp),
                            tint = if (showCopied)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                    ChatoneIconButton(
                        onClick = {
                            val twitchUrl = "https://www.twitch.tv/${username.lowercase()}"
                            uriHandler.openUri(twitchUrl)
                        },
                        modifier = Modifier.size(22.dp)
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_twitch),
                            contentDescription = "Open Twitch profile",
                            modifier = Modifier.size(14.dp),
                            tint = Color(0xFF9146FF)
                        )
                    }
                }
                if (username.lowercase() != displayName.lowercase()) {
                    Text(
                        "@$username", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (onDetach != null) {
                ChatoneIconButton(onClick = onDetach, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.Outlined.OpenInNew, null, modifier = Modifier.size(15.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (onDismiss != null) {
                if (showIconDetached) {
                    ChatoneIconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp),
                    ) {
                        Icon(
                            Icons.Filled.Close, null, modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        if (badges.isNotEmpty() || sevenTvBadge != null) {
            Spacer(Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                badges.forEach { badge ->
                    if (badge.imageUrl.isNotEmpty()) AsyncImage(
                        model = badge.imageUrl,
                        contentDescription = badge.id,
                        modifier = Modifier.size(20.dp)
                    )
                }
                sevenTvBadge?.let { stv ->
                    val badgeUrl = stv.url2x.ifEmpty { stv.url1x }
                    if (badgeUrl.isNotEmpty()) AsyncImage(
                        model = badgeUrl,
                        contentDescription = stv.tooltip,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        val hasRoles = isBroadcaster || isModerator || isVip || isSubscriber
        if (hasRoles) {
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (isBroadcaster) RoleBadge("Broadcaster", MaterialTheme.colorScheme.error)
                if (isModerator) RoleBadge("Mod", ChatoneTheme.extraColors.connected)
                if (isVip) RoleBadge("VIP", Color(0xFFE005B9))
                if (isSubscriber) RoleBadge("Sub", MaterialTheme.colorScheme.primary)
            }
        }
    }
}
