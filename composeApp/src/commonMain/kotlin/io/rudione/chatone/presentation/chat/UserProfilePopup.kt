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
import io.rudione.chatone.util.MessageToken
import io.rudione.chatone.presentation.theme.i18n.LocalStrings
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject

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
    var noteText by remember { mutableStateOf("") }
    var isNoteLoaded by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current

    var fetchedAvatarUrl by remember(userId) { mutableStateOf(profileImageUrl) }
    var fetchedCreatedAt by remember(userId) { mutableStateOf(createdAt) }
    var followedAt by remember(userId) { mutableStateOf<String?>(null) }
    var subAge by remember(userId) { mutableStateOf<io.rudione.chatone.data.remote.SubAgeInfo?>(null) }
    val ivrApiClient: io.rudione.chatone.data.remote.IvrApiClient = koinInject()

    var selectedTab by remember { mutableIntStateOf(0) }

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
                        Key.B -> { onBan(); onDismiss(); true }
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
                    messagesCount = userMessages.size
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
    subAge: io.rudione.chatone.data.remote.SubAgeInfo? = null,
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
    onDismiss: () -> Unit,
    channelLogin: String = "",
    username: String = "",
    mentionMuteRepository: io.rudione.chatone.data.repository.MentionMuteRepository? = null
) {
    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
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
        subAge?.let { sa ->
            val subLine = when {
                sa.hidden -> LocalStrings.current.profileSubAgeHidden
                sa.cumulativeMonths > 0 -> buildString {
                    append(
                        LocalStrings.current.profileSubAgeMonths
                            .replace("{0}", sa.cumulativeMonths.toString())
                    )
                    sa.tier?.let { append(" · Tier $it") }
                    if (sa.streakMonths > 1) {
                        append(" · ")
                        append(
                            LocalStrings.current.profileSubAgeStreak
                                .replace("{0}", sa.streakMonths.toString())
                        )
                    }
                }
                else -> null
            }
            if (subLine != null) {
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Star, null, modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.8f)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        subLine, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (isNoteLoaded) {
            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            Spacer(Modifier.height(6.dp))

            val nicknameRepository: io.rudione.chatone.data.repository.NicknameRepository =
                org.koin.compose.koinInject()
            var nicknameText by remember(userId) {
                mutableStateOf(nicknameRepository.getNickname(userId) ?: "")
            }
            // Note and local nickname share one compact field, switched by the chip row —
            // two stacked 56dp OutlinedTextFields made the card twice as tall as needed.
            var localTab by remember { mutableStateOf(0) }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                @Composable
                fun chip(index: Int, icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, filled: Boolean) {
                    val selected = localTab == index
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                else Color.Transparent
                            )
                            .border(
                                1.dp,
                                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { localTab = index }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            icon, null,
                            modifier = Modifier.size(12.dp),
                            tint = if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (filled) {
                            Box(
                                Modifier.size(5.dp).clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                        }
                    }
                }
                chip(0, Icons.Outlined.Edit, LocalStrings.current.profileNote, noteText.isNotEmpty())
                chip(1, Icons.Outlined.Person, LocalStrings.current.profileNickname, nicknameText.isNotEmpty())
                Spacer(Modifier.weight(1f))
                if (localTab == 0 && noteText.isNotEmpty()) {
                    IconButton(
                        onClick = { clipboardManager.setText(AnnotatedString(noteText)) },
                        modifier = Modifier.size(22.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Email, null,
                            modifier = Modifier.size(13.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onShowDeleteConfirmation, modifier = Modifier.size(22.dp)) {
                        Icon(
                            Icons.Outlined.Delete, null,
                            modifier = Modifier.size(13.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
                if (localTab == 1 && nicknameText.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            nicknameRepository.deleteNickname(userId)
                            nicknameText = ""
                        },
                        modifier = Modifier.size(22.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Delete, null,
                            modifier = Modifier.size(13.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            Spacer(Modifier.height(5.dp))
            if (localTab == 0) {
                CompactModField(
                    value = noteText,
                    onValueChange = { newText ->
                        onNoteChange(newText)
                        if (newText.isNotBlank()) noteRepository.saveNote(userId, newText)
                        else if (newText.isEmpty()) noteRepository.deleteNote(userId)
                    },
                    placeholder = LocalStrings.current.profileNotePlaceholder,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                CompactModField(
                    value = nicknameText,
                    onValueChange = { newText ->
                        nicknameText = newText
                        if (newText.isNotBlank()) nicknameRepository.saveNickname(userId, newText)
                        else nicknameRepository.deleteNickname(userId)
                    },
                    placeholder = LocalStrings.current.profileNicknamePlaceholder,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (mentionMuteRepository != null && username.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            Spacer(Modifier.height(6.dp))

            var isMutedGlobally by remember(username) {
                mutableStateOf(mentionMuteRepository.isUserMuted(username))
            }
            var isMutedInChannel by remember(username, channelLogin) {
                mutableStateOf(
                    if (channelLogin.isNotEmpty()) mentionMuteRepository.isUserMutedInChannel(username, channelLogin)
                    else false
                )
            }

            val s = LocalStrings.current

            @Composable
            fun muteChip(label: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(7.dp))
                        .background(
                            if (checked) MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
                            else Color.Transparent
                        )
                        .border(
                            1.dp,
                            if (checked) MaterialTheme.colorScheme.error.copy(alpha = 0.35f)
                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                            RoundedCornerShape(7.dp)
                        )
                        .clickable { onToggle(!checked) }
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Icon(
                        if (checked) Icons.Outlined.NotificationsOff else Icons.Outlined.Notifications,
                        null,
                        modifier = Modifier.size(11.dp),
                        tint = if (checked) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (checked) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                muteChip(s.profileMuteChipGlobal, isMutedGlobally) { checked ->
                    isMutedGlobally = checked
                    if (checked) mentionMuteRepository.muteUser(username)
                    else mentionMuteRepository.unmuteUser(username)
                }
                if (channelLogin.isNotEmpty()) {
                    muteChip(s.profileMuteChipChannel, isMutedInChannel) { checked ->
                        isMutedInChannel = checked
                        if (checked) mentionMuteRepository.muteUserInChannel(username, channelLogin)
                        else mentionMuteRepository.unmuteUserInChannel(username, channelLogin)
                    }
                }
            }
        }

        Spacer(Modifier.height(6.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        Spacer(Modifier.height(5.dp))

        if (showModActions && !isBroadcaster) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
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
            Spacer(Modifier.height(5.dp))
        }

        var showBlockConfirm by remember { mutableStateOf(false) }
        if (showBlockConfirm) {
            val confirmTitle = if (isBlocked)
                LocalStrings.current.profileUnblockConfirmTitle
            else
                LocalStrings.current.profileBlockConfirmTitle
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

        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            ModIconChip(
                icon = Icons.Outlined.MailOutline,
                tint = MaterialTheme.colorScheme.primary,
                contentDescription = LocalStrings.current.profileSendWhisper,
                onClick = { onWhisper(); onDismiss() }
            )
            if (!isBroadcaster) {
                ModIconChip(
                    icon = if (isBlocked) Icons.Outlined.LockOpen else Icons.Outlined.Block,
                    tint = if (isBlocked) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.error,
                    contentDescription = if (isBlocked) LocalStrings.current.profileUnblock
                    else LocalStrings.current.profileBlock,
                    onClick = { showBlockConfirm = true }
                )
            }
            if (showModActions && !isBroadcaster) {
                ModIconChip(
                    icon = Icons.Filled.Close,
                    tint = ChatoneTheme.extraColors.modBan,
                    contentDescription = LocalStrings.current.profileBan,
                    onClick = { onBan(); onDismiss() }
                )
                ModIconChip(
                    icon = Icons.Outlined.CheckCircle,
                    tint = ChatoneTheme.extraColors.modUnban,
                    contentDescription = LocalStrings.current.profileUnban,
                    onClick = { onUnban(); onDismiss() }
                )
                if (currentUserIsBroadcaster) {
                    ModIconChip(
                        icon = if (isModerator) Icons.Filled.Shield else Icons.Outlined.Shield,
                        tint = ChatoneTheme.extraColors.connected,
                        contentDescription = if (isModerator) "Unmod" else "Mod",
                        onClick = { if (isModerator) onUnmod() else onMod(); onDismiss() }
                    )
                    ModIconChip(
                        icon = if (isVip) Icons.Filled.WorkspacePremium else Icons.Outlined.WorkspacePremium,
                        tint = Color(0xFFE005B9),
                        contentDescription = if (isVip) "Un-VIP" else "VIP",
                        onClick = { if (isVip) onUnvip() else onVip(); onDismiss() }
                    )
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
        LaunchedEffect(messages.size) {
            if (messages.isNotEmpty()) listState.scrollToItem(messages.lastIndex)
        }
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
    val bodyColor = if (message.isDeleted) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f)
    else if (message.isAction) nameColor else MaterialTheme.colorScheme.onSurface
    val deletedLabel = LocalStrings.current.profileMessageDeleted
    val line = buildAnnotatedString {
        withStyle(SpanStyle(color = nameColor, fontWeight = FontWeight.SemiBold)) {
            append(message.displayName)
        }
        withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))) {
            append(": ")
        }
        withStyle(
            SpanStyle(
                color = bodyColor,
                textDecoration = if (message.isDeleted) TextDecoration.LineThrough else null
            )
        ) {
            append(rawText.ifEmpty { if (message.isDeleted) deletedLabel else "" })
        }
    }
    var showCopyMenu by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current
    Box {
        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp))
                .background(
                    if (message.isDeleted) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.20f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                )
                .pointerInput(message.id) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.type == PointerEventType.Press &&
                                event.buttons.isSecondaryPressed
                            ) {
                                showCopyMenu = true
                            }
                        }
                    }
                }
                .padding(horizontal = 8.dp, vertical = 5.dp), verticalAlignment = Alignment.Top
        ) {
            Text(
                timeText, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.padding(top = 1.dp, end = 6.dp)
            )
            Text(line, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
        }
        DropdownMenu(expanded = showCopyMenu, onDismissRequest = { showCopyMenu = false }) {
            DropdownMenuItem(
                text = { Text(LocalStrings.current.chatCopyMessage, style = MaterialTheme.typography.bodySmall) },
                onClick = {
                    showCopyMenu = false
                    clipboard.setText(AnnotatedString(rawText))
                }
            )
            DropdownMenuItem(
                text = { Text(LocalStrings.current.chatCopyUsername, style = MaterialTheme.typography.bodySmall) },
                onClick = {
                    showCopyMenu = false
                    clipboard.setText(AnnotatedString(message.displayName))
                }
            )
        }
    }
}

internal fun formatMessageTime(timestamp: Long): String {
    val instant = Instant.fromEpochMilliseconds(timestamp)
    val dt = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    return "${dt.hour.toString().padStart(2, '0')}:${dt.minute.toString().padStart(2, '0')}"
}


@Composable
internal fun CompactProfileTabs(
    selectedTab: Int,
    onSelect: (Int) -> Unit,
    messagesCount: Int = 0
) {
    val s = LocalStrings.current
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            @Composable
            fun tab(index: Int, label: String, badge: Int = 0) {
                val selected = selectedTab == index
                val labelColor by animateColorAsState(
                    if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                val indicatorWidth by animateDpAsState(if (selected) 16.dp else 0.dp)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                        .clickable { onSelect(index) }
                        .padding(horizontal = 10.dp)
                        .padding(top = 5.dp, bottom = 2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                            color = labelColor
                        )
                        if (badge > 0) {
                            Text(
                                "$badge",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                color = if (selected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(
                                        if (selected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f)
                                    )
                                    .padding(horizontal = 4.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(3.dp))
                    Box(
                        Modifier
                            .width(indicatorWidth)
                            .height(2.dp)
                            .clip(RoundedCornerShape(1.dp))
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }
            tab(0, s.profileTabUsercard)
            tab(1, s.profileTabMessages, messagesCount)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
    }
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
    // Custom clickable Box instead of Surface(onClick): Surface enforces a 48dp
    // minimum touch target, which blew the chips apart with invisible gaps.
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(ChatoneTheme.extraColors.modTimeout.copy(alpha = 0.14f))
            .clickable { onTimeout(seconds); onDismiss() }
            .padding(horizontal = 6.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label, style = MaterialTheme.typography.labelSmall,
            color = ChatoneTheme.extraColors.modTimeout, fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
internal fun MiniActionButton(
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.14f),
        contentColor = color,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(icon, null, modifier = Modifier.size(14.dp), tint = color)
                Spacer(Modifier.width(4.dp))
            }
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
internal fun ModIconChip(
    icon: ImageVector,
    tint: Color,
    contentDescription: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .size(26.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(tint.copy(alpha = 0.14f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(15.dp), tint = tint)
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

        CompactProfileTabs(
            selectedTab = selectedTab,
            onSelect = { selectedTab = it },
            messagesCount = userMessages.size
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