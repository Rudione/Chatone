package io.rudione.chatone.presentation.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import coil3.compose.AsyncImage
import io.rudione.chatone.domain.model.Badge
import io.rudione.chatone.domain.model.SevenTvCosmetics
import io.rudione.chatone.presentation.theme.ChatoneTheme
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Mini-profile popup shown when clicking on a username in chat.
 * Displays user info and mod actions.
 */
@Composable
fun UserProfilePopup(
    userId: String,
    username: String,
    displayName: String,
    color: String?,
    profileImageUrl: String = "",
    createdAt: String = "",
    isModerator: Boolean = false,
    isSubscriber: Boolean = false,
    isVip: Boolean = false,
    isBroadcaster: Boolean = false,
    badges: List<Badge> = emptyList(),
    sevenTvBadge: SevenTvCosmetics.Badge? = null,
    // Mod actions (only shown if viewer is mod)
    showModActions: Boolean = false,
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
    Popup(
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true)
    ) {
        Card(
            modifier = Modifier
                .width(300.dp)
                .padding(8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // ── User Info Header ────────────────────────────
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Avatar
                    if (profileImageUrl.isNotEmpty()) {
                        AsyncImage(
                            model = profileImageUrl,
                            contentDescription = displayName,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = displayName.take(2).uppercase(),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Spacer(Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        // Display name with color
                        val nameColor = parseHexColor(color)
                            ?: MaterialTheme.colorScheme.primary
                        Text(
                            text = displayName,
                            style = MaterialTheme.typography.titleMedium,
                            color = nameColor,
                            fontWeight = FontWeight.Bold
                        )
                        // Username if different
                        if (username.lowercase() != displayName.lowercase()) {
                            Text(
                                text = "@$username",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Close
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Close",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // ── Badge Images ──────────────────────────────
                if (badges.isNotEmpty() || sevenTvBadge != null) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        badges.forEach { badge ->
                            AsyncImage(
                                model = badge.imageUrl,
                                contentDescription = badge.id,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        sevenTvBadge?.let { stv ->
                            AsyncImage(
                                model = stv.url2x,
                                contentDescription = stv.tooltip,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                }

                // ── Roles ──────────────────────────────────────
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (isBroadcaster) {
                        RoleBadge("Broadcaster", MaterialTheme.colorScheme.error)
                    }
                    if (isModerator) {
                        RoleBadge("Mod", ChatoneTheme.extraColors.connected)
                    }
                    if (isVip) {
                        RoleBadge("VIP", Color(0xFFE005B9))
                    }
                    if (isSubscriber) {
                        RoleBadge("Sub", MaterialTheme.colorScheme.primary)
                    }
                }

                if (createdAt.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.DateRange,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "Joined $createdAt",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // ── Quick Actions ───────────────────────────────
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                Spacer(Modifier.height(8.dp))

                // Whisper
                TextButton(
                    onClick = { onWhisper(); onDismiss() },
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Outlined.MailOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Whisper", modifier = Modifier.weight(1f))
                }

                // ── Mod Actions ─────────────────────────────────
                if (showModActions && !isBroadcaster) {
                    Spacer(Modifier.height(4.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = "MODERATION",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )

                    // Timeout row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        TimeoutChip("1s", 1, onTimeout)
                        TimeoutChip("10s", 10, onTimeout)
                        TimeoutChip("1m", 60, onTimeout)
                        TimeoutChip("10m", 600, onTimeout)
                        TimeoutChip("1h", 3600, onTimeout)
                        TimeoutChip("1d", 86400, onTimeout)
                    }

                    Spacer(Modifier.height(8.dp))

                    // Ban / Unban
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
                            Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(14.dp))
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
                            Icon(Icons.Outlined.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Unban", style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    // Mod/VIP management
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (isModerator) {
                            OutlinedButton(
                                onClick = { onUnmod(); onDismiss() },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text("Unmod", style = MaterialTheme.typography.labelMedium)
                            }
                        } else {
                            OutlinedButton(
                                onClick = { onMod(); onDismiss() },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text("Mod", style = MaterialTheme.typography.labelMedium)
                            }
                        }

                        if (isVip) {
                            OutlinedButton(
                                onClick = { onUnvip(); onDismiss() },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text("Un-VIP", style = MaterialTheme.typography.labelMedium)
                            }
                        } else {
                            OutlinedButton(
                                onClick = { onVip(); onDismiss() },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text("VIP", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RoleBadge(label: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun TimeoutChip(
    label: String,
    seconds: Int,
    onTimeout: (Int) -> Unit
) {
    SuggestionChip(
        onClick = { onTimeout(seconds) },
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall
            )
        },
        colors = SuggestionChipDefaults.suggestionChipColors(
            containerColor = ChatoneTheme.extraColors.modTimeout.copy(alpha = 0.1f),
            labelColor = ChatoneTheme.extraColors.modTimeout
        ),
        border = null
    )
}

private fun parseHexColor(hexColor: String?): Color? {
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
