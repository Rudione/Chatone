package io.rudione.chatone.presentation.chat

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.rudione.chatone.data.remote.GqlUsercardMessage
import io.rudione.chatone.data.remote.SubAgeInfo
import io.rudione.chatone.data.repository.MentionMuteRepository
import io.rudione.chatone.data.repository.ModerationHistoryEntry
import io.rudione.chatone.data.repository.ModerationHistoryRepository
import io.rudione.chatone.data.repository.NicknameRepository
import io.rudione.chatone.data.repository.UserNoteRepository
import io.rudione.chatone.domain.model.ChatMessage
import io.rudione.chatone.domain.model.DisplayMessage
import io.rudione.chatone.domain.model.ModActionButton
import io.rudione.chatone.presentation.components.ChatoneChip
import io.rudione.chatone.presentation.components.ChatoneChipDefaults
import io.rudione.chatone.util.chat.plainText
import io.rudione.chatone.presentation.theme.ChatoneTheme
import io.rudione.chatone.presentation.theme.i18n.LocalStrings
import io.rudione.chatone.presentation.theme.i18n.format
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import io.rudione.chatone.presentation.components.ChatoneIconButton
import io.rudione.chatone.presentation.components.ChatoneDropdownMenu
import io.rudione.chatone.icons.lucide.Ban
import io.rudione.chatone.icons.lucide.Bell
import io.rudione.chatone.icons.lucide.BellOff
import io.rudione.chatone.icons.lucide.CircleCheck
import io.rudione.chatone.icons.lucide.Copy
import io.rudione.chatone.icons.lucide.Gem
import io.rudione.chatone.icons.lucide.History
import io.rudione.chatone.icons.lucide.LockOpen
import io.rudione.chatone.icons.lucide.Lucide
import io.rudione.chatone.icons.lucide.MessageCircle
import io.rudione.chatone.icons.lucide.MessageSquare
import io.rudione.chatone.icons.lucide.NotebookPen
import io.rudione.chatone.icons.lucide.Shield
import io.rudione.chatone.icons.lucide.ShieldCheck
import io.rudione.chatone.icons.lucide.Timer
import io.rudione.chatone.icons.lucide.TimerOff
import io.rudione.chatone.icons.lucide.Trash2
import io.rudione.chatone.icons.lucide.TriangleAlert
import io.rudione.chatone.icons.lucide.UserPen
import io.rudione.chatone.icons.lucide.UserX

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun UsercardTab(
    userId: String,
    fetchedCreatedAt: String,
    followedAt: String?,
    subAge: SubAgeInfo? = null,
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
    mentionMuteRepository: MentionMuteRepository? = null
) {
    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
        if (fetchedCreatedAt.isNotEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    ProfileIconJoined, null, modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    LocalStrings.current.profileJoined.replace("{0}", fetchedCreatedAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        followedAt?.let { date ->
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    ProfileIconFollow, null, modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    LocalStrings.current.profileFollowingSince.replace("{0}", date),
                    style = MaterialTheme.typography.bodySmall,
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
                        ProfileIconSub, null, modifier = Modifier.size(14.dp),
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

            val nicknameRepository: NicknameRepository =
                org.koin.compose.koinInject()
            var nicknameText by remember(userId) {
                mutableStateOf(nicknameRepository.getNickname(userId) ?: "")
            }

            var localTab by remember { mutableStateOf(0) }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                @Composable
                fun chip(
                    index: Int,
                    icon: androidx.compose.ui.graphics.vector.ImageVector,
                    label: String,
                    filled: Boolean
                ) {
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
                chip(
                    0,
                    Lucide.NotebookPen,
                    LocalStrings.current.profileNote,
                    noteText.isNotEmpty()
                )
                chip(
                    1,
                    Lucide.UserPen,
                    LocalStrings.current.profileNickname,
                    nicknameText.isNotEmpty()
                )
                Spacer(Modifier.weight(1f))
                if (localTab == 0 && noteText.isNotEmpty()) {
                    ChatoneIconButton(
                        onClick = { clipboardManager.setText(AnnotatedString(noteText)) },
                        modifier = Modifier.size(22.dp)
                    ) {
                        Icon(
                            Lucide.Copy, null,
                            modifier = Modifier.size(13.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    ChatoneIconButton(
                        onClick = onShowDeleteConfirmation,
                        modifier = Modifier.size(22.dp)
                    ) {
                        Icon(
                            Lucide.Trash2, null,
                            modifier = Modifier.size(13.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
                if (localTab == 1 && nicknameText.isNotEmpty()) {
                    ChatoneIconButton(
                        onClick = {
                            nicknameRepository.deleteNickname(userId)
                            nicknameText = ""
                        },
                        modifier = Modifier.size(22.dp)
                    ) {
                        Icon(
                            Lucide.Trash2, null,
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
                    if (channelLogin.isNotEmpty()) mentionMuteRepository.isUserMutedInChannel(
                        username,
                        channelLogin
                    )
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
                        if (checked) Lucide.BellOff else Lucide.Bell,
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
                    86400 to "1d",
                    604800 to "1w",
                    1209600 to "2w"
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
                    TextButton(
                        onClick = {
                            if (isBlocked) onUnblock() else onBlock()
                            showBlockConfirm = false
                            onDismiss()
                        }, colors = ButtonDefaults.textButtonColors(
                            contentColor = if (isBlocked) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.error
                        )
                    ) {
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
                icon = Lucide.MessageCircle,
                tint = MaterialTheme.colorScheme.primary,
                contentDescription = LocalStrings.current.profileSendWhisper,
                onClick = { onWhisper(); onDismiss() }
            )
            if (!isBroadcaster) {
                ModIconChip(
                    icon = if (isBlocked) Lucide.LockOpen else Lucide.UserX,
                    tint = if (isBlocked) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.error,
                    contentDescription = if (isBlocked) LocalStrings.current.profileUnblock
                    else LocalStrings.current.profileBlock,
                    onClick = { showBlockConfirm = true }
                )
            }
            if (showModActions && !isBroadcaster) {
                ModIconChip(
                    icon = Lucide.Ban,
                    tint = ChatoneTheme.extraColors.modBan,
                    contentDescription = LocalStrings.current.profileBan,
                    onClick = { onBan(); onDismiss() }
                )
                ModIconChip(
                    icon = Lucide.CircleCheck,
                    tint = ChatoneTheme.extraColors.modUnban,
                    contentDescription = LocalStrings.current.profileUnban,
                    onClick = { onUnban(); onDismiss() }
                )
                if (currentUserIsBroadcaster) {
                    ModIconChip(
                        icon = if (isModerator) Lucide.ShieldCheck else Lucide.Shield,
                        tint = ChatoneTheme.extraColors.connected,
                        contentDescription = if (isModerator) "Unmod" else "Mod",
                        onClick = { if (isModerator) onUnmod() else onMod(); onDismiss() }
                    )
                    ModIconChip(
                        icon = Lucide.Gem,
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
    userColor: String?,
    login: String = "",
    history: List<GqlUsercardMessage> = emptyList(),
    isHistoryLoading: Boolean = false,
    hasMoreHistory: Boolean = false,
    onLoadHistory: (() -> Unit)? = null,
    historyAtTop: Boolean = true,
    localHistory: List<ChatMessage> = emptyList()
) {
    val listState = rememberLazyListState()
    val nameColor = rememberNickColors().of(userColor, login.ifBlank { displayName })
    if (messages.isEmpty() && history.isEmpty() && localHistory.isEmpty() && !isHistoryLoading) {
        Box(
            modifier = Modifier.fillMaxWidth().height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Lucide.MessageSquare, null, modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
                Text(
                    LocalStrings.current.profileNoMessagesInSession,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                if (onLoadHistory != null && hasMoreHistory) {
                    TextButton(onClick = onLoadHistory) {
                        Text(LocalStrings.current.profileLoadHistory)
                    }
                }
            }
        }
    } else {
        LaunchedEffect(messages.size) {
            if (historyAtTop && messages.isNotEmpty()) {
                listState.scrollToItem(listState.layoutInfo.totalItemsCount.coerceAtLeast(1) - 1)
            }
        }
        SelectionContainer {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp, max = 360.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val localHistoryBlock: androidx.compose.foundation.lazy.LazyListScope.() -> Unit = {
                    if (localHistory.isNotEmpty()) {
                        item(key = "local-history-header") {
                            Text(
                                LocalStrings.current.profileLocalHistory,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                        items(localHistory, key = { "local_" + it.id }) { lm ->
                            LocalHistoryMessageItem(
                                message = lm,
                                displayName = displayName,
                                nameColor = nameColor
                            )
                        }
                    }
                }
                val historyBlock: androidx.compose.foundation.lazy.LazyListScope.() -> Unit = {
                    if (history.isNotEmpty() || isHistoryLoading || hasMoreHistory) {
                        item(key = "history-header") {
                            if (isHistoryLoading) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        LocalStrings.current.profileLoadingHistory,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else if (hasMoreHistory && onLoadHistory != null) {
                                TextButton(
                                    onClick = onLoadHistory,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        LocalStrings.current.profileLoadMoreHistory,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                        items(history, key = { "hist_" + it.id }) { hm ->
                            GqlHistoryMessageItem(
                                message = hm,
                                displayName = displayName,
                                nameColor = nameColor
                            )
                        }
                    }
                }
                if (historyAtTop) {
                    localHistoryBlock()
                    historyBlock()
                }
                items(messages, key = { it.id }) { msg ->
                    MessageHistoryItem(
                        message = msg,
                        nameColor = nameColor
                    )
                }
                if (!historyAtTop) {
                    historyBlock()
                    localHistoryBlock()
                }
            }
        }
    }
}

@Composable
internal fun LocalHistoryMessageItem(message: ChatMessage, displayName: String, nameColor: Color) {
    val timeText = remember(message.timestamp) { formatMessageTime(message.timestamp) }
    PastMessageLine(
        displayName = displayName,
        nameColor = nameColor,
        text = message.message,
        timeText = timeText
    )
}

@Composable
internal fun GqlHistoryMessageItem(
    message: GqlUsercardMessage,
    displayName: String,
    nameColor: Color
) {
    val timeText = remember(message.sentAtEpochMs) {
        message.sentAtEpochMs?.let { formatMessageTime(it) } ?: ""
    }
    PastMessageLine(
        displayName = displayName,
        nameColor = nameColor,
        text = message.text,
        timeText = timeText,
        isDeleted = message.isDeleted
    )
}

@Composable
internal fun MessageHistoryItem(message: DisplayMessage.PrivMsg, nameColor: Color) {
    val timeText = remember(message.timestamp) { formatMessageTime(message.timestamp) }
    val rawText = remember(message.tokens) {
        message.tokens.joinToString("") { token ->
            token.plainText()
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
        ChatoneDropdownMenu(expanded = showCopyMenu, onDismissRequest = { showCopyMenu = false }) {
            DropdownMenuItem(
                text = {
                    Text(
                        LocalStrings.current.chatCopyMessage,
                        style = MaterialTheme.typography.bodySmall
                    )
                },
                onClick = {
                    showCopyMenu = false
                    clipboard.setText(AnnotatedString(rawText))
                }
            )
            DropdownMenuItem(
                text = {
                    Text(
                        LocalStrings.current.chatCopyUsername,
                        style = MaterialTheme.typography.bodySmall
                    )
                },
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

internal const val PROFILE_ROLES_TAB = 3

@Composable
internal fun CompactProfileTabs(
    selectedTab: Int,
    onSelect: (Int) -> Unit,
    messagesCount: Int = 0,
    showHistoryTab: Boolean = false,
    historyCount: Int = 0,
    showRolesTab: Boolean = false,
    trailing: @Composable () -> Unit = {}
) {
    val s = LocalStrings.current
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
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
            if (showRolesTab) tab(PROFILE_ROLES_TAB, s.insights.tabRoles)
            if (showHistoryTab) tab(2, s.profileTabHistory, historyCount)
            Spacer(Modifier.weight(1f))
            trailing()
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
    ChatoneChip(
        label = label,
        onClick = {
            onTimeout(seconds)
            onDismiss()
        },
        selected = true,
        accent = ChatoneTheme.extraColors.modTimeout,
        height = ChatoneChipDefaults.CompactHeight
    )
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
        Icon(
            icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(15.dp),
            tint = tint
        )
    }
}

@Composable
internal fun ModerationHistoryTab(entries: List<ModerationHistoryEntry>, isLoading: Boolean = false) {
    val s = LocalStrings.current
    val bans =
        entries.count { it.action == ModerationHistoryRepository.ACTION_BAN }
    val timeouts =
        entries.count { it.action == ModerationHistoryRepository.ACTION_TIMEOUT }
    val warnings =
        entries.count { it.action == ModerationHistoryRepository.ACTION_WARN }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
        if (bans + timeouts + warnings > 0) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (bans > 0) {
                    HistoryCounterChip(
                        label = s.profileHistoryBans, count = bans,
                        icon = Lucide.Ban, tint = ChatoneTheme.extraColors.modBan,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (timeouts > 0) {
                    HistoryCounterChip(
                        label = s.profileHistoryTimeouts, count = timeouts,
                        icon = Lucide.Timer, tint = ChatoneTheme.extraColors.modTimeout,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (warnings > 0) {
                    HistoryCounterChip(
                        label = s.profileHistoryWarnings, count = warnings,
                        icon = Lucide.TriangleAlert, tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        if (entries.isEmpty() && isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth().height(160.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            }
        } else if (entries.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().height(160.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Lucide.History, null, modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Text(
                        s.profileHistoryEmpty, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        } else {
            SelectionContainer {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp, max = 320.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(entries, key = { it.id }) { entry ->
                        ModerationHistoryRow(entry)
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryCounterChip(
    label: String,
    count: Int,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(tint.copy(alpha = 0.1f))
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Icon(icon, null, modifier = Modifier.size(16.dp), tint = tint)
        Text(
            "$count",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = tint
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun ModerationHistoryRow(entry: ModerationHistoryEntry) {
    val s = LocalStrings.current
    val (icon, tint, label) = when (entry.action) {
        ModerationHistoryRepository.ACTION_BAN ->
            Triple(Lucide.Ban, ChatoneTheme.extraColors.modBan, s.profileHistoryBanEntry)

        ModerationHistoryRepository.ACTION_TIMEOUT ->
            Triple(
                Lucide.Timer, ChatoneTheme.extraColors.modTimeout,
                entry.durationSeconds?.let {
                    s.format(s.profileHistoryTimeoutEntry, ModActionButton.formatDuration(it))
                } ?: s.profileHistoryTimeoutEntryPlain
            )

        ModerationHistoryRepository.ACTION_UNBAN ->
            Triple(
                Lucide.LockOpen,
                MaterialTheme.colorScheme.primary,
                s.profileHistoryUnbanEntry
            )

        ModerationHistoryRepository.ACTION_UNTIMEOUT ->
            Triple(
                Lucide.TimerOff,
                MaterialTheme.colorScheme.primary,
                s.profileHistoryUntimeoutEntry
            )

        ModerationHistoryRepository.ACTION_DELETE ->
            Triple(
                Lucide.Trash2,
                ChatoneTheme.extraColors.modDelete,
                s.profileHistoryDeleteEntry
            )

        else ->
            Triple(
                Lucide.TriangleAlert,
                MaterialTheme.colorScheme.tertiary,
                s.profileHistoryWarnEntry
            )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, null, modifier = Modifier.size(16.dp).padding(top = 2.dp), tint = tint)
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!entry.moderatorLogin.isNullOrBlank()) {
                    Text(
                        s.format(s.profileHistoryByModerator, entry.moderatorLogin),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
            if (!entry.reason.isNullOrBlank()) {
                Text(
                    s.format(s.profileHistoryReason, entry.reason),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
        Text(
            formatModerationHistoryTimestamp(entry.timestamp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
    }
}

private fun formatModerationHistoryTimestamp(timestamp: Long): String {
    val dt =
        Instant.fromEpochMilliseconds(timestamp).toLocalDateTime(TimeZone.currentSystemDefault())
    val day = dt.day.toString().padStart(2, '0')
    val month = (dt.month.ordinal + 1).toString().padStart(2, '0')
    val hour = dt.hour.toString().padStart(2, '0')
    val minute = dt.minute.toString().padStart(2, '0')
    return "$day.$month $hour:$minute"
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
