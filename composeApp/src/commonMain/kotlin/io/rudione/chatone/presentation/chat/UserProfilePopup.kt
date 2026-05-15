package io.rudione.chatone.presentation.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
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
import io.rudione.chatone.presentation.theme.ChatoneTheme
import io.rudione.chatone.util.MessageToken
import io.rudione.chatone.presentation.theme.i18n.LocalStrings
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject

@OptIn(ExperimentalLayoutApi::class)
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
    onUnban: () -> Unit = {},
    onMod: () -> Unit = {},
    onUnmod: () -> Unit = {},
    onVip: () -> Unit = {},
    onUnvip: () -> Unit = {},
    onWhisper: () -> Unit = {},
    onDetach: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val noteRepository: UserNoteRepository = koinInject()
    val twitchApiClient: TwitchApiClient = koinInject()
    var noteText by remember { mutableStateOf("") }
    var isNoteLoaded by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current

    var fetchedAvatarUrl by remember(userId) { mutableStateOf(profileImageUrl) }
    var fetchedCreatedAt by remember(userId) { mutableStateOf(createdAt) }
    var followedAt by remember(userId) { mutableStateOf<String?>(null) }

    var selectedTab by remember { mutableIntStateOf(0) }

    val userMessages = remember(channelMessages, userId) {
        channelMessages
            .filterIsInstance<DisplayMessage.PrivMsg>()
            .filter { it.userId == userId }
            .takeLast(1000)
            .reversed()
    }

    LaunchedEffect(userId) {
        fetchedAvatarUrl = profileImageUrl
        fetchedCreatedAt = createdAt
        followedAt = null

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
            modifier = Modifier.width(320.dp).padding(8.dp),
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
                TabRow(
                    selectedTabIndex = selectedTab, containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary,
                    divider = {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outline.copy(
                                alpha = 0.2f
                            )
                        )
                    }) {
                    Tab(
                        selected = selectedTab == 0, onClick = { selectedTab = 0 },
                        text = {
                            Text(
                                LocalStrings.current.profileTabUsercard, style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (selectedTab == 0) FontWeight.SemiBold else FontWeight.Normal
                            )
                        })
                    Tab(
                        selected = selectedTab == 1, onClick = { selectedTab = 1 },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    LocalStrings.current.profileTabMessages, style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (selectedTab == 1) FontWeight.SemiBold else FontWeight.Normal
                                )
                                if (userMessages.isNotEmpty()) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = CircleShape
                                    ) {
                                        Text(
                                            "${userMessages.size}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.padding(
                                                horizontal = 5.dp,
                                                vertical = 1.dp
                                            )
                                        )
                                    }
                                }
                            }
                        })
                }

                when (selectedTab) {
                    0 -> UsercardTab(
                        userId = userId,
                        fetchedCreatedAt = fetchedCreatedAt,
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
                        onDismiss = onDismiss
                    )

                    1 -> MessagesTab(
                        messages = userMessages,
                        displayName = displayName,
                        userColor = color
                    )
                }
            }
        }
    }
}


@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun UsercardTab(
    userId: String,
    fetchedCreatedAt: String,
    followedAt: String?,
    noteText: String,
    isNoteLoaded: Boolean,
    noteRepository: UserNoteRepository,
    clipboardManager: androidx.compose.ui.platform.ClipboardManager,
    showModActions: Boolean,
    isBroadcaster: Boolean,
    isModerator: Boolean,
    isVip: Boolean,
    isSubscriber: Boolean,
    currentUserIsBroadcaster: Boolean,
    isBlocked: Boolean = false,
    onBlock: () -> Unit = {},
    onUnblock: () -> Unit = {},
    onNoteChange: (String) -> Unit,
    onShowDeleteConfirmation: () -> Unit,
    onWhisper: () -> Unit,
    onTimeout: (Int) -> Unit,
    onBan: () -> Unit,
    onUnban: () -> Unit,
    onMod: () -> Unit,
    onUnmod: () -> Unit,
    onVip: () -> Unit,
    onUnvip: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        if (fetchedCreatedAt.isNotEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.DateRange, null, modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    LocalStrings.current.profileJoined.replace("{0}", fetchedCreatedAt), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        followedAt?.let { date ->
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Favorite, null, modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    LocalStrings.current.profileFollowingSince.replace("{0}", date), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (isNoteLoaded) {
            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            Spacer(Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Outlined.Edit,
                    null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    LocalStrings.current.profileNote,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.weight(1f))
                if (noteText.isNotEmpty()) {
                    TextButton(
                        onClick = { clipboardManager.setText(AnnotatedString(noteText)) },
                        modifier = Modifier.height(24.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                    ) {
                        Text(
                            LocalStrings.current.profileNoteCopy,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = onShowDeleteConfirmation,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Delete,
                            null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(
                value = noteText,
                onValueChange = { newText ->
                    onNoteChange(newText)
                    if (newText.isNotBlank()) noteRepository.saveNote(userId, newText)
                    else if (newText.isEmpty()) noteRepository.deleteNote(userId)
                },
                modifier = Modifier.fillMaxWidth().heightIn(min = 16.dp, max = 72.dp),
                placeholder = {
                    Text(
                        LocalStrings.current.profileNotePlaceholder,
                        style = MaterialTheme.typography.bodySmall
                    )
                },
                textStyle = MaterialTheme.typography.bodySmall,
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                    focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                )
            )
        }

        Spacer(Modifier.height(12.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        Spacer(Modifier.height(8.dp))

        TextButton(
            onClick = { onWhisper(); onDismiss() }, modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Icon(Icons.Outlined.MailOutline, null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(LocalStrings.current.profileSendWhisper, modifier = Modifier.weight(1f))
        }

        if (!isBroadcaster) {
            var showBlockConfirm by remember { mutableStateOf(false) }
            if (showBlockConfirm) {
                val confirmTitle = if (isBlocked)
                    LocalStrings.current.profileUnblockConfirmTitle
                else
                    LocalStrings.current.profileBlockConfirmTitle
                val confirmText = ""
                AlertDialog(
                    onDismissRequest = { showBlockConfirm = false },
                    title = { Text(confirmTitle, style = MaterialTheme.typography.titleSmall) },
                    confirmButton = {
                        TextButton(onClick = {
                            if (isBlocked) onUnblock() else onBlock()
                            showBlockConfirm = false
                            onDismiss()
                        }, colors = ButtonDefaults.textButtonColors(
                            contentColor = if (isBlocked) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.error
                        )) {
                            Text(if (isBlocked) LocalStrings.current.unblockUser else LocalStrings.current.blockUser)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showBlockConfirm = false }) {
                            Text(LocalStrings.current.cancel)
                        }
                    }
                )
            }
            TextButton(
                onClick = { showBlockConfirm = true },
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = if (isBlocked) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                )
            ) {
                Icon(
                    if (isBlocked) Icons.Outlined.LockOpen else Icons.Outlined.Block,
                    null, modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (isBlocked) LocalStrings.current.profileUnblock else LocalStrings.current.profileBlock,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (showModActions && !isBroadcaster) {
            Spacer(Modifier.height(4.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            Spacer(Modifier.height(8.dp))

            Text(
                LocalStrings.current.profileSectionModeration, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )


            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(
                    1 to "1s",
                    30 to "30s",
                    60 to "1m",
                    300 to "5m",
                    600 to "10m",
                    3600 to "1h",
                    21600 to "6h",
                    86400 to "1d",
                    604800 to "7d",
                    1209600 to "14d"
                ).forEach { (sec, label) ->
                    TimeoutChip(label, sec, onTimeout, onDismiss)
                }
            }

            Spacer(Modifier.height(8.dp))


            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = { onBan(); onDismiss() },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = ChatoneTheme.extraColors.modBan.copy(alpha = 0.15f),
                        contentColor = ChatoneTheme.extraColors.modBan
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Filled.Close, null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(LocalStrings.current.profileBan, style = MaterialTheme.typography.labelMedium)
                }
                FilledTonalButton(
                    onClick = { onUnban(); onDismiss() },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = ChatoneTheme.extraColors.modUnban.copy(alpha = 0.15f),
                        contentColor = ChatoneTheme.extraColors.modUnban
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Outlined.CheckCircle, null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(LocalStrings.current.profileUnban, style = MaterialTheme.typography.labelMedium)
                }
            }


            if (currentUserIsBroadcaster) {
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { if (isModerator) onUnmod() else onMod(); onDismiss() },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Text(
                            if (isModerator) "Unmod" else "Mod",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    OutlinedButton(
                        onClick = { if (isVip) onUnvip() else onVip(); onDismiss() },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Text(
                            if (isVip) "Un-VIP" else "VIP",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
    }
}


@Composable
internal fun MessagesTab(
    messages: List<DisplayMessage.PrivMsg>,
    displayName: String,
    userColor: String?
) {
    val listState = rememberLazyListState()
    val nameColor = parseHexColor(userColor) ?: MaterialTheme.colorScheme.primary
    if (messages.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxWidth().height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Outlined.MailOutline, null, modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
                Text(
                    LocalStrings.current.profileNoMessagesInSession, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    } else {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp, max = 360.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                MessageHistoryItem(
                    message = msg,
                    nameColor = nameColor
                )
            }
        }
    }
}

@Composable
internal fun MessageHistoryItem(message: DisplayMessage.PrivMsg, nameColor: Color) {
    val timeText = remember(message.timestamp) { formatMessageTime(message.timestamp) }
    val rawText = remember(message.tokens) {
        message.tokens.joinToString("") { token ->
            when (token) {
                is MessageToken.Text -> token.text
                is MessageToken.TwitchEmoteToken -> token.name
                is MessageToken.ThirdPartyEmoteToken -> token.emote.code
                is MessageToken.Link -> token.displayText
                is MessageToken.Mention -> token.username
            }
        }
    }
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp))
            .background(
                if (message.isDeleted) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.20f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
            )
            .padding(horizontal = 8.dp, vertical = 5.dp), verticalAlignment = Alignment.Top
    ) {
        Text(
            timeText, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.padding(top = 1.dp, end = 6.dp)
        )
        Text(
            rawText.ifEmpty { if (message.isDeleted) "message deleted" else "" },
            style = if (message.isDeleted) MaterialTheme.typography.bodySmall.copy(textDecoration = TextDecoration.LineThrough)
            else MaterialTheme.typography.bodySmall,
            color = if (message.isDeleted) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f)
            else if (message.isAction) nameColor else MaterialTheme.colorScheme.onSurface
        )
    }
}

internal fun formatMessageTime(timestamp: Long): String {
    val instant = Instant.fromEpochMilliseconds(timestamp)
    val dt = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    return "${dt.hour.toString().padStart(2, '0')}:${dt.minute.toString().padStart(2, '0')}"
}


@Composable
internal fun RoleBadge(label: String, color: Color) {
    Surface(color = color.copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
internal fun TimeoutChip(
    label: String,
    seconds: Int,
    onTimeout: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        onClick = { onTimeout(seconds); onDismiss() },
        shape = RoundedCornerShape(8.dp),
        color = ChatoneTheme.extraColors.modTimeout.copy(alpha = 0.1f),
        modifier = Modifier.border(
            0.5.dp,
            ChatoneTheme.extraColors.modTimeout.copy(alpha = 0.3f),
            RoundedCornerShape(8.dp)
        )
    ) {
        Text(
            label, style = MaterialTheme.typography.labelSmall,
            color = ChatoneTheme.extraColors.modTimeout, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

internal fun parseHexColor(hexColor: String?): Color? {
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
    var noteText by remember { mutableStateOf("") }
    var isNoteLoaded by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current

    var fetchedAvatarUrl by remember(msg.userId) { mutableStateOf("") }
    var fetchedCreatedAt by remember(msg.userId) { mutableStateOf("") }
    var followedAt by remember(msg.userId) { mutableStateOf<String?>(null) }
    var selectedTab by remember { mutableIntStateOf(0) }

    val userMessages = remember(channelMessages, msg.userId) {
        channelMessages.filterIsInstance<DisplayMessage.PrivMsg>()
            .filter { it.userId == msg.userId }.takeLast(1000).reversed()
    }

    LaunchedEffect(msg.userId) {
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

        TabRow(
            selectedTabIndex = selectedTab, containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary,
            divider = { HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)) }) {
            Tab(
                selected = selectedTab == 0, onClick = { selectedTab = 0 },
                text = { Text(LocalStrings.current.profileTabUsercard, style = MaterialTheme.typography.labelMedium) })
            Tab(
                selected = selectedTab == 1, onClick = { selectedTab = 1 },
                text = {
                    Text(
                        LocalStrings.current.profileTabMessagesCount.replace("{0}", userMessages.size.toString()),
                        style = MaterialTheme.typography.labelMedium
                    )
                })
        }
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
                onDismiss = onDismiss
            )

            1 -> MessagesTab(
                messages = userMessages,
                displayName = msg.displayName,
                userColor = msg.color
            )
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

    Column(modifier = Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (avatarUrl.isNotEmpty()) {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = displayName,
                    modifier = Modifier.size(48.dp).clip(CircleShape)
                )
            } else {
                Box(
                    modifier = Modifier.size(48.dp).clip(CircleShape)
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
                    IconButton(
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
                    IconButton(
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
                IconButton(onClick = onDetach, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.Outlined.OpenInNew, null, modifier = Modifier.size(15.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (onDismiss != null) {
                if (showIconDetached) {
                    IconButton(
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