package io.rudione.chatone.presentation.main

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import io.rudione.chatone.domain.model.WhisperConversation
import io.rudione.chatone.domain.model.WhisperMessage
import io.rudione.chatone.presentation.components.LiquidGlassSurface
import io.rudione.chatone.presentation.theme.ChatoneTheme
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime


@Composable
fun WhisperPanel(
    state: MainState,
    onEvent: (MainEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val extra = ChatoneTheme.extraColors
    val activeConversation = state.activeWhisperUserId?.let { uid ->
        state.whisperConversations.find { it.userId == uid }
    }

    LiquidGlassSurface(
        modifier = modifier
            .width(340.dp)
            .heightIn(max = 520.dp),
        shape = RoundedCornerShape(20.dp),
        contentPadding = PaddingValues(0.dp),
        backgroundAlphaHigh = 0.97f,
        backgroundAlphaLow = 0.92f,
        borderAlphaHigh = 0.22f,
        borderAlphaLow = 0.08f
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
           
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                            Color.Transparent
                        ))
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (activeConversation != null) {
                    IconButton(
                        onClick = { onEvent(MainEvent.OpenWhisperWith("", "", "")) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.width(8.dp))
                   
                    WhisperAvatar(conv = activeConversation, size = 32)
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(activeConversation.displayName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1)
                        Text("@${activeConversation.username}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1)
                    }
                } else {
                    Icon(Icons.Filled.MailOutline, null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(10.dp))
                    Text("Whispers",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f))
                    if (state.totalUnreadWhispers > 0) {
                        Surface(color = MaterialTheme.colorScheme.error, shape = CircleShape) {
                            Text("${state.totalUnreadWhispers}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                        Spacer(Modifier.width(6.dp))
                    }
                }
                IconButton(
                    onClick = {
                       
                        onEvent(MainEvent.HideWhisperPanel)
                       
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Filled.Close,
                        null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

           
            if (activeConversation != null) {
                WhisperChat(
                    conversation = activeConversation,
                    onSend = { text ->
                        onEvent(MainEvent.SendWhisper(
                            toUserId = activeConversation.userId,
                            toUsername = activeConversation.username,
                            text = text
                        ))
                    },
                    modifier = Modifier.weight(1f, fill = false)
                )
            } else {
                if (state.whisperConversations.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.MailOutline, null,
                                modifier = Modifier.size(40.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                            Spacer(Modifier.height(8.dp))
                            Text("No whispers yet",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                            Text("Click on a username → Whisper",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f))
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(state.whisperConversations, key = { it.userId }) { conv ->
                            WhisperConversationRow(
                                conv = conv,
                                onClick = {
                                    onEvent(MainEvent.OpenWhisperWith(
                                        userId = conv.userId,
                                        username = conv.username,
                                        displayName = conv.displayName,
                                        avatarUrl = conv.avatarUrl,
                                        color = conv.color
                                    ))
                                    onEvent(MainEvent.MarkWhisperRead(conv.userId))
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun WhisperConversationRow(conv: WhisperConversation, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        WhisperAvatar(conv = conv, size = 40)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(conv.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                conv.lastMessage?.let {
                    Text(
                        formatWhisperTime(it.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
            conv.lastMessage?.let { msg ->
                Text(
                    text = if (msg.isOwn) "You: ${msg.text}" else msg.text,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (conv.unreadCount > 0) {
            Spacer(Modifier.width(8.dp))
            Surface(color = MaterialTheme.colorScheme.error, shape = CircleShape) {
                Text("${conv.unreadCount}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
            }
        }
    }
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 14.dp),
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
    )
}


@Composable
private fun WhisperChat(
    conversation: WhisperConversation,
    onSend: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(conversation.messages.size) {
        if (conversation.messages.isNotEmpty()) {
            listState.animateScrollToItem(conversation.messages.lastIndex)
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
       
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxWidth().heightIn(min = 160.dp, max = 360.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(conversation.messages, key = { it.id }) { msg ->
                WhisperBubble(msg = msg)
            }
        }

        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

       
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text("Message @${conversation.username}…",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                },
                textStyle = MaterialTheme.typography.bodySmall,
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                    focusedContainerColor = Color.White.copy(alpha = 0.04f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.02f)
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    if (inputText.isNotBlank()) { onSend(inputText.trim()); inputText = "" }
                })
            )
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(if (inputText.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                    .clickable(enabled = inputText.isNotBlank()) {
                        onSend(inputText.trim()); inputText = ""
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, null,
                    modifier = Modifier.size(18.dp),
                    tint = if (inputText.isNotBlank()) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
            }
        }
    }
}


@Composable
private fun WhisperBubble(msg: WhisperMessage) {
    val isOwn = msg.isOwn
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isOwn) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 14.dp,
                topEnd = 14.dp,
                bottomStart = if (isOwn) 14.dp else 4.dp,
                bottomEnd = if (isOwn) 4.dp else 14.dp
            ),
            color = if (isOwn) MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.widthIn(max = 240.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)) {
                Text(
                    msg.text,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isOwn) Color.White else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    formatWhisperTime(msg.timestamp),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = if (isOwn) Color.White.copy(alpha = 0.6f)
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.align(Alignment.End).padding(top = 2.dp)
                )
            }
        }
    }
}


@Composable
private fun WhisperAvatar(conv: WhisperConversation, size: Int) {
    if (conv.avatarUrl.isNotEmpty()) {
        AsyncImage(
            model = conv.avatarUrl,
            contentDescription = conv.displayName,
            modifier = Modifier.size(size.dp).clip(CircleShape)
        )
    } else {
        Box(
            modifier = Modifier.size(size.dp).clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                conv.displayName.take(2).uppercase(),
                style = if (size >= 40) MaterialTheme.typography.titleSmall else MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun formatWhisperTime(timestamp: Long): String {
    val instant = Instant.fromEpochMilliseconds(timestamp)
    val dt = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    return "${dt.hour.toString().padStart(2, '0')}:${dt.minute.toString().padStart(2, '0')}"
}