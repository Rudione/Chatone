package io.rudione.chatone.presentation.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
    onSendAnnouncement: (message: String, color: String) -> Unit,
    onStartRaid: (targetLogin: String) -> Unit,
    onCancelRaid: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val extra = ChatoneTheme.extraColors

    // Glass morphism background
    val glassBg = Brush.verticalGradient(
        listOf(
            extra.sidebarSurface.copy(alpha = 0.85f),
            extra.sidebarSurface.copy(alpha = 0.75f),
            extra.sidebarSurface.copy(alpha = 0.85f)
        )
    )
    val glassBorderBrush = Brush.verticalGradient(
        listOf(
            Color.White.copy(alpha = 0.15f),
            Color.White.copy(alpha = 0.05f),
            Color.White.copy(alpha = 0.1f)
        )
    )

    // Expandable sections state
    var showAnnouncement by remember { mutableStateOf(false) }
    var announcementText by remember { mutableStateOf("") }
    var announcementColor by remember { mutableStateOf("primary") }
    var showRaid by remember { mutableStateOf(false) }
    var raidTarget by remember { mutableStateOf("") }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 400.dp),
        shape = RoundedCornerShape(0.dp),
        color = Color.Transparent
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(glassBg)
                .border(width = 0.5.dp, brush = glassBorderBrush, shape = RoundedCornerShape(0.dp))
                .verticalScroll(rememberScrollState())
        ) {
            // ── Header ───────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                Color.Transparent
                            )
                        )
                    )
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Build,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Moderation",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "#$channelLogin",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                        .clickable(onClick = onClose),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Close",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // ── Glass divider ────────────────────────────────────────
            GlassDivider()

            // ── Chat Modes Section ───────────────────────────────────
            SectionLabel("Chat Modes")

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                GlassChip(
                    label = "Emote Only",
                    icon = Icons.Outlined.Face,
                    isActive = roomState.emoteOnly
                ) {
                    onUpdateChatSettings(mapOf("emote_mode" to !roomState.emoteOnly))
                }
                GlassChip(
                    label = "Sub Only",
                    icon = Icons.Outlined.Star,
                    isActive = roomState.subsOnly
                ) {
                    onUpdateChatSettings(mapOf("subscriber_mode" to !roomState.subsOnly))
                }
                GlassChip(
                    label = "R9K / Unique",
                    icon = Icons.Outlined.Lock,
                    isActive = roomState.r9k
                ) {
                    onUpdateChatSettings(mapOf("unique_chat_mode" to !roomState.r9k))
                }
                GlassChip(
                    label = "Followers",
                    icon = Icons.Outlined.Favorite,
                    isActive = roomState.followersOnly >= 0
                ) {
                    if (roomState.followersOnly >= 0) {
                        onUpdateChatSettings(mapOf("follower_mode" to false))
                    } else {
                        onUpdateChatSettings(mapOf("follower_mode" to true, "follower_mode_duration" to 10))
                    }
                }
            }

            // ── Slow Mode ────────────────────────────────────────────
            GlassDivider()
            SectionLabel("Slow Mode")

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val slowOptions = listOf(0 to "Off", 3 to "3s", 5 to "5s", 10 to "10s", 30 to "30s", 60 to "1m", 120 to "2m")
                slowOptions.forEach { (seconds, label) ->
                    val isActive = if (seconds == 0) roomState.slowMode == 0 else roomState.slowMode == seconds
                    GlassMiniChip(label, isActive) {
                        if (seconds == 0) {
                            onUpdateChatSettings(mapOf("slow_mode" to false))
                        } else {
                            onUpdateChatSettings(mapOf("slow_mode" to true, "slow_mode_wait_time" to seconds))
                        }
                    }
                }
            }

            // ── Follower Duration (conditional) ──────────────────────
            AnimatedVisibility(
                visible = roomState.followersOnly >= 0,
                enter = expandVertically(tween(200)),
                exit = shrinkVertically(tween(200))
            ) {
                Column {
                    GlassDivider()
                    SectionLabel("Follower Duration")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val opts = listOf(0 to "Any", 10 to "10m", 30 to "30m", 60 to "1h", 1440 to "1d", 10080 to "1w")
                        opts.forEach { (min, label) ->
                            GlassMiniChip(label, roomState.followersOnly == min) {
                                onUpdateChatSettings(mapOf("follower_mode" to true, "follower_mode_duration" to min))
                            }
                        }
                    }
                }
            }

            // ── Quick Actions ────────────────────────────────────────
            GlassDivider()
            SectionLabel("Quick Actions")

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Clear Chat
                GlassActionButton(
                    label = "Clear Chat",
                    icon = Icons.Outlined.Delete,
                    tint = extra.modDelete,
                    onClick = onClearChat
                )
                // Announcement
                GlassActionButton(
                    label = "Announce",
                    icon = Icons.Outlined.Notifications,
                    tint = MaterialTheme.colorScheme.tertiary,
                    isToggled = showAnnouncement,
                    onClick = { showAnnouncement = !showAnnouncement }
                )
                // Raid
                GlassActionButton(
                    label = "Raid",
                    icon = Icons.Filled.PlayArrow,
                    tint = MaterialTheme.colorScheme.secondary,
                    isToggled = showRaid,
                    onClick = { showRaid = !showRaid }
                )
                // Cancel Raid
                GlassActionButton(
                    label = "Cancel Raid",
                    icon = Icons.Filled.Close,
                    tint = extra.modBan,
                    onClick = onCancelRaid
                )
            }

            // ── Announcement Expanded ────────────────────────────────
            AnimatedVisibility(
                visible = showAnnouncement,
                enter = expandVertically(tween(200)),
                exit = shrinkVertically(tween(200))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    // Color selection
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Color:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        val colors = listOf(
                            "primary" to MaterialTheme.colorScheme.primary,
                            "blue" to Color(0xFF4A90D9),
                            "green" to Color(0xFF4CAF50),
                            "orange" to Color(0xFFFF9800),
                            "purple" to Color(0xFF9C27B0)
                        )
                        colors.forEach { (name, color) ->
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(color.copy(alpha = if (announcementColor == name) 1f else 0.3f))
                                    .then(
                                        if (announcementColor == name)
                                            Modifier.border(1.5.dp, Color.White.copy(alpha = 0.6f), CircleShape)
                                        else Modifier
                                    )
                                    .clickable { announcementColor = name }
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = announcementText,
                            onValueChange = { announcementText = it },
                            placeholder = {
                                Text(
                                    "Announcement message...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 36.dp, max = 72.dp),
                            textStyle = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                                focusedContainerColor = Color.White.copy(alpha = 0.03f),
                                unfocusedContainerColor = Color.White.copy(alpha = 0.02f)
                            ),
                            singleLine = false,
                            maxLines = 3
                        )
                        Spacer(Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (announcementText.isNotBlank())
                                        MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                                )
                                .clickable(enabled = announcementText.isNotBlank()) {
                                    onSendAnnouncement(announcementText, announcementColor)
                                    announcementText = ""
                                    showAnnouncement = false
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.Send,
                                contentDescription = "Send",
                                modifier = Modifier.size(16.dp),
                                tint = if (announcementText.isNotBlank()) Color.White
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                            )
                        }
                    }
                }
            }

            // ── Raid Expanded ────────────────────────────────────────
            AnimatedVisibility(
                visible = showRaid,
                enter = expandVertically(tween(200)),
                exit = shrinkVertically(tween(200))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = raidTarget,
                        onValueChange = { raidTarget = it },
                        placeholder = {
                            Text(
                                "Channel to raid...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp),
                        textStyle = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                            focusedContainerColor = Color.White.copy(alpha = 0.03f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.02f)
                        ),
                        singleLine = true
                    )
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (raidTarget.isNotBlank())
                                    MaterialTheme.colorScheme.secondary
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                            )
                            .clickable(enabled = raidTarget.isNotBlank()) {
                                onStartRaid(raidTarget.trim())
                                raidTarget = ""
                                showRaid = false
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.PlayArrow,
                            contentDescription = "Start Raid",
                            modifier = Modifier.size(16.dp),
                            tint = if (raidTarget.isNotBlank()) Color.White
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        )
                    }
                }
            }

            Spacer(Modifier.height(6.dp))
        }
    }
}

// ─── Section Label ──────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        letterSpacing = 0.8.sp,
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
    )
}

// ─── Glass divider ──────────────────────────────────────────────────────────

@Composable
private fun GlassDivider() {
    HorizontalDivider(
        color = Color.White.copy(alpha = 0.06f),
        thickness = 0.5.dp,
        modifier = Modifier.padding(horizontal = 8.dp)
    )
}

// ─── Glass Chip (chat modes) ────────────────────────────────────────────────

@Composable
private fun GlassChip(
    label: String,
    icon: ImageVector,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
        else Color.White.copy(alpha = 0.04f),
        animationSpec = tween(200)
    )
    val borderColor by animateColorAsState(
        if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
        else Color.White.copy(alpha = 0.08f),
        animationSpec = tween(200)
    )
    val contentColor = if (isActive) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = bgColor,
        modifier = Modifier.border(0.5.dp, borderColor, RoundedCornerShape(10.dp))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(13.dp),
                tint = contentColor
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                fontSize = 11.sp
            )
            if (isActive) {
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

// ─── Glass Mini Chip (slow mode / follower duration) ────────────────────────

@Composable
private fun GlassMiniChip(
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
        else Color.Transparent,
        animationSpec = tween(150)
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = bgColor,
        modifier = Modifier.border(
            0.5.dp,
            if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            else Color.White.copy(alpha = 0.08f),
            RoundedCornerShape(8.dp)
        )
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isActive) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            fontSize = 11.sp
        )
    }
}

// ─── Glass Action Button (quick actions) ────────────────────────────────────

@Composable
private fun GlassActionButton(
    label: String,
    icon: ImageVector,
    tint: Color,
    isToggled: Boolean = false,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        if (isToggled) tint.copy(alpha = 0.15f)
        else Color.White.copy(alpha = 0.04f),
        animationSpec = tween(200)
    )
    val borderColor by animateColorAsState(
        if (isToggled) tint.copy(alpha = 0.4f)
        else tint.copy(alpha = 0.15f),
        animationSpec = tween(200)
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = bgColor,
        modifier = Modifier.border(0.5.dp, borderColor, RoundedCornerShape(10.dp))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = tint
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = tint,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                fontSize = 11.sp
            )
        }
    }
}
