package io.rudione.chatone.presentation.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
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
import io.rudione.chatone.data.remote.TwitchApiClient
import io.rudione.chatone.data.remote.dto.ChatterData
import io.rudione.chatone.presentation.components.LiquidGlassSurface
import io.rudione.chatone.presentation.theme.ChatoneTheme
import io.rudione.chatone.util.Result
import io.rudione.chatone.presentation.theme.i18n.LocalStrings
import org.koin.compose.koinInject
import io.rudione.chatone.presentation.components.ChatoneIconButton
import io.rudione.chatone.presentation.components.ChatoneTextField

@Composable
fun ChattersPanel(
    channelLogin: String,
    channelId: String,
    accessToken: String,
    currentUserId: String,
    onUserClick: (userId: String, username: String, displayName: String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val apiClient: TwitchApiClient = koinInject()
    val extra = ChatoneTheme.extraColors

    var chatters by remember { mutableStateOf<List<ChatterData>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var totalCount by remember { mutableStateOf(0) }

    LaunchedEffect(channelId) {
        isLoading = true
        if (accessToken.isNotEmpty() && channelId.isNotEmpty()) {
            when (val result = apiClient.getChatters(accessToken, channelId, currentUserId, first = 100)) {
                is Result.Success -> {
                    chatters = result.data.data
                    totalCount = result.data.total
                }
                else -> {}
            }
        }
        isLoading = false
    }

    val filtered = remember(chatters, searchQuery) {
        if (searchQuery.isBlank()) chatters
        else chatters.filter { it.userName.contains(searchQuery, ignoreCase = true) || it.userLogin.contains(searchQuery, ignoreCase = true) }
    }

    LiquidGlassSurface(
        modifier = modifier.width(280.dp).heightIn(max = 480.dp),
        shape = RoundedCornerShape(16.dp),
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
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Person, null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    val s = LocalStrings.current
                    Text(s.panelViewers, style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    if (totalCount > 0) {
                        Text(s.panelViewersInChat.replace("{0}", totalCount.toString()), style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                ChatoneIconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Filled.Close, null, modifier = Modifier.size(15.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            ChatoneTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                placeholder = LocalStrings.current.panelSearchViewers,
                leading = { Icon(Icons.Filled.Search, null, modifier = Modifier.size(16.dp)) },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {})
            )

            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

            when {
                isLoading -> {
                    Box(modifier = Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    }
                }
                filtered.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        Text(if (searchQuery.isBlank()) "No viewers found" else "No results for \"$searchQuery\"",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 340.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(filtered, key = { it.userId }) { chatter ->
                            ChatterRow(
                                chatter = chatter,
                                onClick = { onUserClick(chatter.userId, chatter.userLogin, chatter.userName) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatterRow(chatter: ChatterData, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(28.dp).clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                chatter.userName.take(1).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 11.sp
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(chatter.userName,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (chatter.userLogin != chatter.userName.lowercase()) {
                Text("@${chatter.userLogin}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    maxLines = 1)
            }
        }
    }
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 12.dp),
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.06f)
    )
}
