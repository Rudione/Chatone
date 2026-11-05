package io.rudione.chatone.presentation.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.automod_allow
import chatone.composeapp.generated.resources.automod_deny
import chatone.composeapp.generated.resources.open_channel_automod_desc
import coil3.compose.AsyncImage
import io.rudione.chatone.data.repository.AutoModHub
import io.rudione.chatone.domain.model.ChannelTab
import io.rudione.chatone.domain.model.IrcEvent
import io.rudione.chatone.icons.lucide.Check
import io.rudione.chatone.icons.lucide.Lucide
import io.rudione.chatone.icons.lucide.X
import io.rudione.chatone.presentation.chat.autoModCategoryLabel
import io.rudione.chatone.presentation.chat.rememberNickColors
import io.rudione.chatone.presentation.components.tactilePress
import io.rudione.chatone.presentation.theme.ChatoneTheme
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@Composable
internal fun AutoModMonitorList(channelMeta: Map<String, ChannelTab>, modifier: Modifier = Modifier) {
    val hub: AutoModHub = koinInject()
    LaunchedEffect(hub) { hub.ensureRunning() }
    val pending by hub.pending.collectAsState()
    var decided by remember { mutableStateOf(emptySet<String>()) }
    val items = remember(pending, decided) {
        pending.values.flatten().filter { it.msgId !in decided }.sortedBy { it.heldAtMs }
    }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    LaunchedEffect(items.size) { if (items.isNotEmpty()) listState.animateScrollToItem(items.lastIndex) }

    if (items.isEmpty()) {
        Box(modifier, contentAlignment = Alignment.Center) {
            Text(
                stringResource(Res.string.open_channel_automod_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }
    LazyColumn(state = listState, modifier = modifier, contentPadding = PaddingValues(vertical = 4.dp)) {
        items(items, key = { it.msgId }, contentType = { "automod" }) { held ->
            AutoModMonitorRow(
                held = held,
                channel = channelMeta[held.channelLogin.lowercase()],
                onDecide = { allow ->
                    decided = decided + held.msgId
                    scope.launch { if (hub.resolve(held.msgId, allow).isError) decided = decided - held.msgId }
                }
            )
        }
    }
}

@Composable
private fun AutoModMonitorRow(held: IrcEvent.AutoModHeld, channel: ChannelTab?, onDecide: (Boolean) -> Unit) {
    val extra = ChatoneTheme.extraColors
    val nameColor = rememberNickColors().of(held.color, held.username)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 3.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(extra.modTimeout.copy(alpha = 0.07f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        if (!channel?.profileImageUrl.isNullOrEmpty()) {
            AsyncImage(
                model = channel?.profileImageUrl,
                contentDescription = null,
                modifier = Modifier.size(20.dp).clip(CircleShape)
            )
            Spacer(Modifier.width(8.dp))
        }
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "#${channel?.displayName ?: held.channelLogin.ifEmpty { held.channel }}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                autoModCategoryLabel(held.reasonCategory)?.let { label ->
                    Spacer(Modifier.width(6.dp))
                    Text(
                        label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = extra.modTimeout,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .clip(RoundedCornerShape(5.dp))
                            .background(extra.modTimeout.copy(alpha = 0.15f))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    )
                }
            }
            Text(
                held.displayName.ifBlank { held.username },
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = nameColor,
                maxLines = 1
            )
            Text(
                held.message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 6,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            DecisionIcon(Lucide.Check, stringResource(Res.string.automod_allow), extra.modUnban) { onDecide(true) }
            DecisionIcon(Lucide.X, stringResource(Res.string.automod_deny), extra.modBan) { onDecide(false) }
        }
    }
}

@Composable
private fun DecisionIcon(icon: ImageVector, label: String, tint: Color, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .size(30.dp)
            .tactilePress(interaction, pressedScale = 0.9f)
            .clip(RoundedCornerShape(8.dp))
            .background(tint.copy(alpha = 0.15f))
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick)
            .pointerHoverIcon(PointerIcon.Hand),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(16.dp))
    }
}
