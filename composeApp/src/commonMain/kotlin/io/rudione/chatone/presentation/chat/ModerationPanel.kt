package io.rudione.chatone.presentation.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.rudione.chatone.presentation.theme.ChatoneTheme

@Composable
fun ModerationPanel(
    roomState: RoomState,
    channelLogin: String,
    isMod: Boolean,
    onUpdateChatSettings: (Map<String, Any>) -> Unit,
    onClearChat: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val extra = ChatoneTheme.extraColors

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 220.dp),
        shape = RoundedCornerShape(0.dp),
        color = Color.Transparent
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            extra.sidebarSurface.copy(alpha = 0.97f),
                            extra.sidebarSurface.copy(alpha = 0.92f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        listOf(extra.glassBorder, extra.glassBorder.copy(alpha = 0.3f))
                    ),
                    shape = RoundedCornerShape(0.dp)
                )
        ) {
            // ── Compact Header ──────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Mod Panel",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "#$channelLogin",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                Spacer(Modifier.weight(1f))
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Close",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(color = extra.glassBorder.copy(alpha = 0.5f), thickness = 0.5.dp)

            // ── Chat Modes — single compact row ─────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ModChip("Emote Only", roomState.emoteOnly) {
                    onUpdateChatSettings(mapOf("emote_mode" to !roomState.emoteOnly))
                }
                ModChip("Sub Only", roomState.subsOnly) {
                    onUpdateChatSettings(mapOf("subscriber_mode" to !roomState.subsOnly))
                }
                ModChip("R9K", roomState.r9k) {
                    onUpdateChatSettings(mapOf("unique_chat_mode" to !roomState.r9k))
                }
                ModChip("Followers", roomState.followersOnly >= 0) {
                    if (roomState.followersOnly >= 0) {
                        onUpdateChatSettings(mapOf("follower_mode" to false))
                    } else {
                        onUpdateChatSettings(mapOf("follower_mode" to true, "follower_mode_duration" to 10))
                    }
                }
            }

            // ── Slow Mode — compact chip row ────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    "Slow:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    fontWeight = FontWeight.SemiBold
                )
                val slowOptions = listOf(0 to "Off", 3 to "3s", 5 to "5s", 10 to "10s", 30 to "30s", 60 to "1m", 120 to "2m")
                slowOptions.forEach { (seconds, label) ->
                    val isActive = if (seconds == 0) roomState.slowMode == 0 else roomState.slowMode == seconds
                    MiniChip(label, isActive) {
                        if (seconds == 0) {
                            onUpdateChatSettings(mapOf("slow_mode" to false))
                        } else {
                            onUpdateChatSettings(mapOf("slow_mode" to true, "slow_mode_wait_time" to seconds))
                        }
                    }
                }
            }

            // ── Follower Duration (conditional) ─────────────────────
            if (roomState.followersOnly >= 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        "Follow:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        fontWeight = FontWeight.SemiBold
                    )
                    val opts = listOf(0 to "Any", 10 to "10m", 30 to "30m", 60 to "1h", 1440 to "1d", 10080 to "1w")
                    opts.forEach { (min, label) ->
                        MiniChip(label, roomState.followersOnly == min) {
                            onUpdateChatSettings(mapOf("follower_mode" to true, "follower_mode_duration" to min))
                        }
                    }
                }
            }

            // ── Quick Action ────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    onClick = onClearChat,
                    shape = RoundedCornerShape(8.dp),
                    color = ChatoneTheme.extraColors.modDelete.copy(alpha = 0.1f),
                    modifier = Modifier
                        .border(1.dp, ChatoneTheme.extraColors.modDelete.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = ChatoneTheme.extraColors.modDelete
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "Clear Chat",
                            style = MaterialTheme.typography.labelMedium,
                            color = ChatoneTheme.extraColors.modDelete,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

// ─── Compact chip for chat modes ────────────────────────────────────────────

@Composable
private fun ModChip(
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val bgColor = if (isActive)
        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
    else
        ChatoneTheme.extraColors.glassOverlay.copy(alpha = 0.5f)

    val borderColor = if (isActive)
        MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
    else
        ChatoneTheme.extraColors.glassBorder

    val contentColor = if (isActive)
        MaterialTheme.colorScheme.primary
    else
        MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = bgColor,
        modifier = Modifier.border(1.dp, borderColor, RoundedCornerShape(8.dp))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1
            )
            if (isActive) {
                Spacer(Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}

// ─── Mini chip for slow mode / follower duration ────────────────────────────

@Composable
private fun MiniChip(
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(6.dp),
        color = if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
        else Color.Transparent,
        modifier = Modifier.border(
            0.5.dp,
            if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            else ChatoneTheme.extraColors.glassBorder.copy(alpha = 0.5f),
            RoundedCornerShape(6.dp)
        )
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isActive) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            fontSize = 11.sp
        )
    }
}
