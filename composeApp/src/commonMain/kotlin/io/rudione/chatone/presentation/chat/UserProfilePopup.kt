package io.rudione.chatone.presentation.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import coil3.compose.AsyncImage
import io.rudione.chatone.data.remote.TwitchApiClient
import io.rudione.chatone.data.repository.UserNoteRepository
import io.rudione.chatone.domain.model.Badge
import io.rudione.chatone.domain.model.DisplayMessage
import io.rudione.chatone.util.Result
import io.rudione.chatone.domain.model.SevenTvCosmetics
import io.rudione.chatone.presentation.settings.SettingsState
import io.rudione.chatone.presentation.theme.ChatoneTheme
import io.rudione.chatone.util.MessageToken
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
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
    onTimeout: (Int) -> Unit = {},
    onBan: () -> Unit = {},
    onUnban: () -> Unit = {},
    onMod: () -> Unit = {},
    onUnmod: () -> Unit = {},
    onVip: () -> Unit = {},
    onUnvip: () -> Unit = {},
    onWhisper: () -> Unit = {},
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
                    val followResult = twitchApiClient.getChannelFollower(accessToken, channelId, userId)
                    if (followResult is Result.Success) {
                        followResult.data.data.firstOrNull()?.let { follower ->
                            followedAt = follower.followedAt.take(10)
                        }
                    }
                } catch (_: Exception) {}
            }
        }
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete Note") },
            text = { Text("Are you sure you want to delete this note?") },
            confirmButton = {
                TextButton(
                    onClick = { noteRepository.deleteNote(userId); noteText = ""; showDeleteConfirmation = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirmation = false }) { Text("Cancel") } }
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
               
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (fetchedAvatarUrl.isNotEmpty()) {
                            AsyncImage(model = fetchedAvatarUrl, contentDescription = displayName,
                                modifier = Modifier.size(48.dp).clip(CircleShape))
                        } else {
                            Box(modifier = Modifier.size(48.dp).clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center) {
                                Text(displayName.take(2).uppercase(), style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            val nameColor = parseHexColor(color) ?: MaterialTheme.colorScheme.primary
                            Text(displayName, style = MaterialTheme.typography.titleMedium,
                                color = nameColor, fontWeight = FontWeight.Bold)
                            if (username.lowercase() != displayName.lowercase()) {
                                Text("@$username", style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Filled.Close, null, modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    if (badges.isNotEmpty() || sevenTvBadge != null) {
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            badges.forEach { badge ->
                                if (badge.imageUrl.isNotEmpty()) AsyncImage(model = badge.imageUrl, contentDescription = badge.id, modifier = Modifier.size(20.dp))
                            }
                            sevenTvBadge?.let { stv ->
                                val badgeUrl = stv.url2x.ifEmpty { stv.url1x }
                                if (badgeUrl.isNotEmpty()) AsyncImage(model = badgeUrl, contentDescription = stv.tooltip, modifier = Modifier.size(20.dp))
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

               
                TabRow(selectedTabIndex = selectedTab, containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary,
                    divider = { HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)) }) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 },
                        text = { Text("Usercard", style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (selectedTab == 0) FontWeight.SemiBold else FontWeight.Normal) })
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Messages", style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (selectedTab == 1) FontWeight.SemiBold else FontWeight.Normal)
                                if (userMessages.isNotEmpty()) {
                                    Surface(color = MaterialTheme.colorScheme.primary, shape = CircleShape) {
                                        Text("${userMessages.size}", style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp))
                                    }
                                }
                            }
                        })
                }

                when (selectedTab) {
                    0 -> UsercardTab(
                        userId = userId, fetchedCreatedAt = fetchedCreatedAt, followedAt = followedAt,
                        noteText = noteText, isNoteLoaded = isNoteLoaded, noteRepository = noteRepository,
                        clipboardManager = clipboardManager, showModActions = showModActions,
                        isBroadcaster = isBroadcaster, isModerator = isModerator, isVip = isVip,
                        isSubscriber = isSubscriber, currentUserIsBroadcaster = currentUserIsBroadcaster,
                        onNoteChange = { noteText = it }, onShowDeleteConfirmation = { showDeleteConfirmation = true },
                        onWhisper = onWhisper, onTimeout = onTimeout, onBan = onBan, onUnban = onUnban,
                        onMod = onMod, onUnmod = onUnmod, onVip = onVip, onUnvip = onUnvip, onDismiss = onDismiss
                    )
                    1 -> MessagesTab(messages = userMessages, displayName = displayName, userColor = color)
                }
            }
        }
    }
}


@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun UsercardTab(
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
                Icon(Icons.Outlined.DateRange, null, modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(4.dp))
                Text("Joined $fetchedCreatedAt", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        followedAt?.let { date ->
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Favorite, null, modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
                Spacer(Modifier.width(4.dp))
                Text("Following since $date", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        if (isNoteLoaded) {
            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.Edit, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(4.dp))
                Text("Note", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.weight(1f))
                if (noteText.isNotEmpty()) {
                    TextButton(onClick = { clipboardManager.setText(AnnotatedString(noteText)) },
                        modifier = Modifier.height(24.dp), contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)) {
                        Text("Copy", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = onShowDeleteConfirmation, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Outlined.Delete, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.error)
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
                placeholder = { Text("Add a note about this user...", style = MaterialTheme.typography.bodySmall) },
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

        TextButton(onClick = { onWhisper(); onDismiss() }, modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) {
            Icon(Icons.Outlined.MailOutline, null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text("Whisper", modifier = Modifier.weight(1f))
        }

        if (showModActions && !isBroadcaster) {
            Spacer(Modifier.height(4.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            Spacer(Modifier.height(8.dp))

            Text("MODERATION", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 8.dp))

           
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(1 to "1s", 30 to "30s", 60 to "1m", 600 to "10m", 3600 to "1h", 86400 to "1d").forEach { (sec, label) ->
                    TimeoutChip(label, sec, onTimeout, onDismiss)
                }
            }

            Spacer(Modifier.height(8.dp))

           
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                    Text("Ban", style = MaterialTheme.typography.labelMedium)
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
                    Text("Unban", style = MaterialTheme.typography.labelMedium)
                }
            }

           
            if (currentUserIsBroadcaster) {
                Spacer(Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { if (isModerator) onUnmod() else onMod(); onDismiss() },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Text(if (isModerator) "Unmod" else "Mod", style = MaterialTheme.typography.labelMedium)
                    }
                    OutlinedButton(
                        onClick = { if (isVip) onUnvip() else onVip(); onDismiss() },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Text(if (isVip) "Un-VIP" else "VIP", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}


@Composable
private fun MessagesTab(messages: List<DisplayMessage.PrivMsg>, displayName: String, userColor: String?) {
    val listState = rememberLazyListState()
    val nameColor = parseHexColor(userColor) ?: MaterialTheme.colorScheme.primary
    if (messages.isEmpty()) {
        Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.MailOutline, null, modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                Text("No messages in this session", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
            }
        }
    } else {
        LazyColumn(state = listState, modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp, max = 360.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(messages, key = { it.id }) { msg -> MessageHistoryItem(message = msg, nameColor = nameColor) }
        }
    }
}

@Composable
private fun MessageHistoryItem(message: DisplayMessage.PrivMsg, nameColor: Color) {
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
    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp))
        .background(if (message.isDeleted) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.20f)
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f))
        .padding(horizontal = 8.dp, vertical = 5.dp), verticalAlignment = Alignment.Top) {
        Text(timeText, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.padding(top = 1.dp, end = 6.dp))
        Text(rawText.ifEmpty { if (message.isDeleted) "message deleted" else "" },
            style = if (message.isDeleted) MaterialTheme.typography.bodySmall.copy(textDecoration = TextDecoration.LineThrough)
            else MaterialTheme.typography.bodySmall,
            color = if (message.isDeleted) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f)
            else if (message.isAction) nameColor else MaterialTheme.colorScheme.onSurface)
    }
}

private fun formatMessageTime(timestamp: Long): String {
    val instant = Instant.fromEpochMilliseconds(timestamp)
    val dt = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    return "${dt.hour.toString().padStart(2, '0')}:${dt.minute.toString().padStart(2, '0')}"
}


@Composable
private fun RoleBadge(label: String, color: Color) {
    Surface(color = color.copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = color,
            fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
    }
}

@Composable
private fun TimeoutChip(label: String, seconds: Int, onTimeout: (Int) -> Unit, onDismiss: () -> Unit) {
    Surface(
        onClick = { onTimeout(seconds); onDismiss() },
        shape = RoundedCornerShape(8.dp),
        color = ChatoneTheme.extraColors.modTimeout.copy(alpha = 0.1f),
        modifier = Modifier.border(0.5.dp, ChatoneTheme.extraColors.modTimeout.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = ChatoneTheme.extraColors.modTimeout, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
    }
}

private fun parseHexColor(hexColor: String?): Color? {
    if (hexColor == null || !hexColor.startsWith("#")) return null
    return try {
        val colorInt = hexColor.substring(1).toLong(16)
        Color(red = ((colorInt shr 16) and 0xFF) / 255f,
            green = ((colorInt shr 8) and 0xFF) / 255f,
            blue = (colorInt and 0xFF) / 255f)
    } catch (e: Exception) { null }
}