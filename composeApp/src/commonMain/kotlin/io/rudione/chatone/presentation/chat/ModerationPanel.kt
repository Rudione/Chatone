package io.rudione.chatone.presentation.chat

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.rudione.chatone.domain.model.Macro
import io.rudione.chatone.presentation.components.LiquidGlassSurface
import io.rudione.chatone.presentation.theme.ChatoneTheme

@Composable
fun ModerationPanel(
    roomState: RoomState,
    channelLogin: String,
    isMod: Boolean,
    pinnedMacros: List<Macro> = emptyList(),
    onUpdateChatSettings: (Map<String, Any>) -> Unit,
    onClearChat: () -> Unit,
    onSendAnnouncement: (message: String, color: String) -> Unit,
    onStartRaid: (targetLogin: String) -> Unit,
    onCancelRaid: () -> Unit,
    onExecuteMacro: (Macro) -> Unit = {},
    onSendPinMessage: (message: String) -> Unit = {},
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val extra = ChatoneTheme.extraColors

    var showAnnouncement by remember { mutableStateOf(false) }
    var announcementText by remember { mutableStateOf("") }
    var announcementColor by remember { mutableStateOf("primary") }
    var showRaid by remember { mutableStateOf(false) }
    var raidTarget by remember { mutableStateOf("") }
    var showPin by remember { mutableStateOf(false) }
    var pinText by remember { mutableStateOf("") }

    // ▼▼▼ Liquid Glass контейнер для всей панели ▼▼▼
    LiquidGlassSurface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 440.dp)
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        contentPadding = PaddingValues(0.dp),
        backgroundAlphaHigh = 0.94f,
        backgroundAlphaLow = 0.86f,
        borderAlphaHigh = 0.18f,
        borderAlphaLow = 0.06f
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 8.dp)
        ) {
            // Заголовок панели
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Moderation",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Close",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Macros
            if (pinnedMacros.isNotEmpty()) {
                PanelSectionLabel("Macros")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    pinnedMacros.forEach { macro ->
                        LiquidGlassChip(
                            text = macro.name,
                            icon = macro.icon,
                            onClick = { onExecuteMacro(macro) },
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                PanelDivider()
            }

            // Chat Modes
            PanelSectionLabel("Chat Modes")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                LiquidGlassToggleChip(
                    label = "Emote",
                    icon = Icons.Outlined.Face,
                    isActive = roomState.emoteOnly
                ) { onUpdateChatSettings(mapOf("emote_mode" to !roomState.emoteOnly)) }

                LiquidGlassToggleChip(
                    label = "Subs",
                    icon = Icons.Outlined.Star,
                    isActive = roomState.subsOnly
                ) { onUpdateChatSettings(mapOf("subscriber_mode" to !roomState.subsOnly)) }

                LiquidGlassToggleChip(
                    label = "Unique",
                    icon = Icons.Outlined.Lock,
                    isActive = roomState.r9k
                ) { onUpdateChatSettings(mapOf("unique_chat_mode" to !roomState.r9k)) }

                LiquidGlassToggleChip(
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

            // Slow Mode
            PanelDivider()
            PanelSectionLabel("Slow Mode")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(0 to "Off", 3 to "3s", 5 to "5s", 10 to "10s", 30 to "30s", 60 to "1m", 120 to "2m")
                    .forEach { (secs, label) ->
                        val isActive = if (secs == 0) roomState.slowMode == 0 else roomState.slowMode == secs
                        LiquidGlassMiniChip(
                            label = label,
                            isActive = isActive
                        ) {
                            if (secs == 0) {
                                onUpdateChatSettings(mapOf("slow_mode" to false))
                            } else {
                                onUpdateChatSettings(mapOf("slow_mode" to true, "slow_mode_wait_time" to secs))
                            }
                        }
                    }
            }

            // Follower Duration
            AnimatedVisibility(
                visible = roomState.followersOnly >= 0,
                enter = expandVertically(tween(200)) + fadeIn(tween(150)),
                exit = shrinkVertically(tween(200)) + fadeOut(tween(150))
            ) {
                Column {
                    PanelDivider()
                    PanelSectionLabel("Min. Follow Time")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(0 to "Any", 10 to "10m", 30 to "30m", 60 to "1h", 1440 to "1d", 10080 to "1w")
                            .forEach { (min, label) ->
                                LiquidGlassMiniChip(
                                    label = label,
                                    isActive = roomState.followersOnly == min
                                ) {
                                    onUpdateChatSettings(mapOf("follower_mode" to true, "follower_mode_duration" to min))
                                }
                            }
                    }
                }
            }

            // Quick Actions
            PanelDivider()
            PanelSectionLabel("Quick Actions")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                LiquidGlassActionButton(
                    label = "Clear",
                    icon = Icons.Outlined.Delete,
                    tint = extra.modDelete
                ) { onClearChat() }

                LiquidGlassActionButton(
                    label = "Announce",
                    icon = Icons.Outlined.Notifications,
                    tint = MaterialTheme.colorScheme.tertiary,
                    isToggled = showAnnouncement
                ) { showAnnouncement = !showAnnouncement }

                LiquidGlassActionButton(
                    label = "Pin",
                    icon = Icons.Filled.Place,
                    tint = MaterialTheme.colorScheme.secondary,
                    isToggled = showPin
                ) { showPin = !showPin }

                LiquidGlassActionButton(
                    label = "Raid",
                    icon = Icons.Filled.PlayArrow,
                    tint = MaterialTheme.colorScheme.secondary,
                    isToggled = showRaid
                ) { showRaid = !showRaid }

                LiquidGlassActionButton(
                    label = "Cancel",
                    icon = Icons.Filled.Close,
                    tint = extra.modBan
                ) { onCancelRaid() }
            }

            // Pin expanded
            AnimatedVisibility(
                visible = showPin,
                enter = expandVertically(tween(200)) + fadeIn(tween(150)),
                exit = shrinkVertically(tween(200)) + fadeOut(tween(150))
            ) {
                LiquidGlassInputRow(
                    value = pinText,
                    onValueChange = { pinText = it },
                    placeholder = "Message to pin via /pin …",
                    onSend = {
                        if (pinText.isNotBlank()) {
                            onSendPinMessage(pinText.trim())
                            pinText = ""
                            showPin = false
                        }
                    },
                    sendColor = MaterialTheme.colorScheme.secondary,
                    onCancel = { showPin = false; pinText = "" }
                )
            }

            // Announcement expanded
            AnimatedVisibility(
                visible = showAnnouncement,
                enter = expandVertically(tween(200)) + fadeIn(tween(150)),
                exit = shrinkVertically(tween(200)) + fadeOut(tween(150))
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                    // Color picker
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
                        listOf(
                            "primary" to MaterialTheme.colorScheme.primary,
                            "blue" to Color(0xFF4A90D9),
                            "green" to Color(0xFF4CAF50),
                            "orange" to Color(0xFFFF9800),
                            "purple" to Color(0xFF9C27B0)
                        ).forEach { (name, color) ->
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
                    // Text input
                    LiquidGlassInputRow(
                        value = announcementText,
                        onValueChange = { announcementText = it },
                        placeholder = "Announcement message…",
                        onSend = {
                            if (announcementText.isNotBlank()) {
                                onSendAnnouncement(announcementText, announcementColor)
                                announcementText = ""
                                showAnnouncement = false
                            }
                        },
                        sendColor = MaterialTheme.colorScheme.primary,
                        onCancel = { showAnnouncement = false; announcementText = "" },
                        maxLines = 3
                    )
                }
            }

            // Raid expanded
            AnimatedVisibility(
                visible = showRaid,
                enter = expandVertically(tween(200)) + fadeIn(tween(150)),
                exit = shrinkVertically(tween(200)) + fadeOut(tween(150))
            ) {
                LiquidGlassInputRow(
                    value = raidTarget,
                    onValueChange = { raidTarget = it },
                    placeholder = "Channel to raid…",
                    onSend = {
                        if (raidTarget.isNotBlank()) {
                            onStartRaid(raidTarget.trim())
                            raidTarget = ""
                            showRaid = false
                        }
                    },
                    sendColor = MaterialTheme.colorScheme.secondary,
                    sendIcon = Icons.Filled.PlayArrow,
                    onCancel = { showRaid = false; raidTarget = "" }
                )
            }

            Spacer(Modifier.height(4.dp))
        }
    }
}

// ─── Liquid Glass Components ──────────────────────────────────────────────

@Composable
private fun LiquidGlassChip(
    text: String,
    icon: String,
    onClick: () -> Unit,
    tint: Color
) {
    LiquidGlassSurface(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 7.dp),
        backgroundAlphaHigh = 0.12f,
        backgroundAlphaLow = 0.06f,
        borderAlphaHigh = 0.25f,
        borderAlphaLow = 0.10f
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(icon, fontSize = 13.sp, color = tint)
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun LiquidGlassToggleChip(
    label: String,
    icon: ImageVector,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val bgColor = if (isActive)
        MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
    else
        Color.Transparent

    val borderColor = if (isActive)
        MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
    else
        Color.White.copy(alpha = 0.08f)

    val contentColor = if (isActive)
        MaterialTheme.colorScheme.primary
    else
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
        modifier = Modifier
            .border(0.5.dp, borderColor, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(icon, null, modifier = Modifier.size(13.dp), tint = contentColor)
            Text(
                label,
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

@Composable
private fun LiquidGlassMiniChip(
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val bgColor = if (isActive)
        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
    else
        Color.Transparent

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = bgColor,
        modifier = Modifier
            .border(
                0.5.dp,
                if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.08f),
                RoundedCornerShape(8.dp)
            )
            .clip(RoundedCornerShape(8.dp))
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            fontSize = 11.sp
        )
    }
}

@Composable
private fun LiquidGlassActionButton(
    label: String,
    icon: ImageVector,
    tint: Color,
    isToggled: Boolean = false,
    onClick: () -> Unit
) {
    val bgColor = if (isToggled)
        tint.copy(alpha = 0.15f)
    else
        Color.Transparent

    val borderColor = if (isToggled)
        tint.copy(alpha = 0.45f)
    else
        tint.copy(alpha = 0.15f)

    LiquidGlassSurface(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 7.dp),
        backgroundAlphaHigh = if (isToggled) 0.15f else 0.04f,
        backgroundAlphaLow = if (isToggled) 0.08f else 0.02f,
        borderAlphaHigh = if (isToggled) 0.45f else 0.15f,
        borderAlphaLow = if (isToggled) 0.20f else 0.08f
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(icon, null, modifier = Modifier.size(14.dp), tint = tint)
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = tint,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun LiquidGlassInputRow(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    onSend: () -> Unit,
    onCancel: () -> Unit,
    sendColor: Color,
    sendIcon: ImageVector = Icons.Filled.Send,
    maxLines: Int = 1
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = if (maxLines == 1) Alignment.CenterVertically else Alignment.Top
    ) {
        LiquidGlassSurface(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 36.dp, max = if (maxLines > 1) 72.dp else 36.dp)
                .clip(RoundedCornerShape(12.dp)),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            backgroundAlphaHigh = 0.06f,
            backgroundAlphaLow = 0.03f,
            borderAlphaHigh = 0.12f,
            borderAlphaLow = 0.05f
        ) {
            TextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = {
                    Text(
                        placeholder,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                },
                modifier = Modifier.fillMaxSize(),
                textStyle = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                ),
                singleLine = maxLines == 1,
                maxLines = maxLines,
                minLines = 1
            )
        }
        Spacer(Modifier.width(6.dp))

        // Send button
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (value.isNotBlank()) sendColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                )
                .clickable(
                    enabled = value.isNotBlank(),
                    onClick = onSend,
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                sendIcon,
                null,
                modifier = Modifier.size(16.dp),
                tint = if (value.isNotBlank()) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
            )
        }
        Spacer(Modifier.width(4.dp))

        // Cancel button (only for expanded inputs)
        if (maxLines > 1) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .clickable(
                        onClick = onCancel,
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Close,
                    null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}

// ─── Helpers ──────────────────────────────────────────────────────────────

@Composable
private fun PanelSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        letterSpacing = 0.8.sp,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
    )
}

@Composable
private fun PanelDivider() {
    HorizontalDivider(
        color = Color.White.copy(alpha = 0.06f),
        thickness = 0.5.dp,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
    )
}