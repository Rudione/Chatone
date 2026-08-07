package io.rudione.chatone.presentation.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.rudione.chatone.domain.model.DisplayMessage
import io.rudione.chatone.presentation.components.LiquidGlassSurface
import io.rudione.chatone.presentation.theme.ChatoneTheme
import io.rudione.chatone.presentation.theme.i18n.LocalStrings
import io.rudione.chatone.presentation.theme.i18n.format
import io.rudione.chatone.presentation.components.ChatoneIconButton

@Composable
internal fun SystemMsgItem(message: DisplayMessage.SystemMsg) {
    Row(
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Outlined.Info,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = ChatoneTheme.extraColors.systemMessage
        )
        Spacer(Modifier.width(4.dp))
        Text(
            message.text,
            style = MaterialTheme.typography.bodySmall,
            color = ChatoneTheme.extraColors.systemMessage
        )
    }
}

@Composable
internal fun UserNoticeMsgItem(message: DisplayMessage.UserNoticeMsg) {
    val isAnnouncement = message.noticeType.equals("announcement", ignoreCase = true)
    val themePrimary = MaterialTheme.colorScheme.primary
    val accent = if (isAnnouncement) announceAccentColor(
        message.announceColor,
        themePrimary
    ) else themePrimary
    Column(
        modifier = Modifier.fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(accent.copy(alpha = 0.12f))
            .drawWithContent {
                drawContent()
                if (isAnnouncement) drawRect(
                    color = accent.copy(alpha = 0.9f),
                    size = Size(4.dp.toPx(), size.height)
                )
            }
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            message.systemText,
            style = MaterialTheme.typography.bodySmall,
            color = accent,
            fontWeight = FontWeight.SemiBold
        )
        message.innerMessage?.let {
            Spacer(modifier = Modifier.height(2.dp));
            PrivMsgItem(message = it)
        }
    }
}

internal fun announceAccentColor(name: String?, fallback: Color): Color = when (name?.lowercase()) {
    "blue" -> Color(0xFF1E88E5); "green" -> Color(0xFF43A047)
    "orange" -> Color(0xFFFB8C00); "purple" -> Color(0xFF8E24AA)
    else -> fallback
}

@Composable
internal fun ChatEventRow(
    accent: Color,
    modifier: Modifier = Modifier,
    trailing: @Composable (RowScope.() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(accent.copy(alpha = 0.10f))
            .height(IntrinsicSize.Min)
            .padding(end = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(5.dp))
        Box(
            Modifier
                .width(3.dp)
                .fillMaxHeight()
                .padding(vertical = 5.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(accent)
        )
        Spacer(Modifier.width(11.dp))
        Row(
            modifier = Modifier.weight(1f).padding(vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) { content() }
        trailing?.let { it() }
    }
}

private fun formatModDuration(seconds: Int, s: io.rudione.chatone.presentation.theme.i18n.AppStrings): String = when {
    seconds >= 3600 -> s.format(s.modRowHours, (seconds / 3600).toString())
    seconds >= 60 -> s.format(s.modRowMinutes, (seconds / 60).toString())
    else -> s.format(s.modRowSeconds, seconds.toString())
}

@Composable
internal fun ModerationMsgItem(
    message: DisplayMessage.ModerationMsg,
    onUsernameClick: ((String) -> Unit)? = null,
    onUnbanByLogin: ((String) -> Unit)? = null
) {
    val s = LocalStrings.current
    val color = when (message.action) {
        DisplayMessage.ModerationMsg.ModerationAction.BAN -> ChatoneTheme.extraColors.modBan
        DisplayMessage.ModerationMsg.ModerationAction.TIMEOUT -> ChatoneTheme.extraColors.modTimeout
        DisplayMessage.ModerationMsg.ModerationAction.DELETE -> ChatoneTheme.extraColors.modDelete
        DisplayMessage.ModerationMsg.ModerationAction.CLEAR -> ChatoneTheme.extraColors.modDelete
        DisplayMessage.ModerationMsg.ModerationAction.UNBAN,
        DisplayMessage.ModerationMsg.ModerationAction.UNTIMEOUT -> ChatoneTheme.extraColors.modUnban
        DisplayMessage.ModerationMsg.ModerationAction.MOD,
        DisplayMessage.ModerationMsg.ModerationAction.VIP -> MaterialTheme.colorScheme.primary
        DisplayMessage.ModerationMsg.ModerationAction.UNMOD,
        DisplayMessage.ModerationMsg.ModerationAction.UNVIP -> ChatoneTheme.extraColors.modTimeout
    }
    val targetUser = message.targetUser
    val predicate = when (message.action) {
        DisplayMessage.ModerationMsg.ModerationAction.BAN -> s.modRowBanned
        DisplayMessage.ModerationMsg.ModerationAction.TIMEOUT ->
            s.format(s.modRowTimedOut, formatModDuration(message.duration ?: 0, s))
        DisplayMessage.ModerationMsg.ModerationAction.DELETE -> s.modRowDeleted
        DisplayMessage.ModerationMsg.ModerationAction.UNBAN -> s.modRowUnbanned
        DisplayMessage.ModerationMsg.ModerationAction.UNTIMEOUT -> s.modRowUntimedout
        DisplayMessage.ModerationMsg.ModerationAction.MOD -> s.modRowModded
        DisplayMessage.ModerationMsg.ModerationAction.UNMOD -> s.modRowUnmodded
        DisplayMessage.ModerationMsg.ModerationAction.VIP -> s.modRowVipped
        DisplayMessage.ModerationMsg.ModerationAction.UNVIP -> s.modRowUnvipped
        DisplayMessage.ModerationMsg.ModerationAction.CLEAR -> s.modRowCleared
    }
    val showUnbanButton = message.action == DisplayMessage.ModerationMsg.ModerationAction.BAN
            && targetUser != null && onUnbanByLogin != null

    ChatEventRow(
        accent = color,
        trailing = {
            if (showUnbanButton) {
                ChatoneIconButton(
                    onClick = { onUnbanByLogin!!(targetUser!!) },
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        Icons.Outlined.CheckCircle,
                        contentDescription = "${s.chatUnbanUser} $targetUser",
                        modifier = Modifier.size(14.dp),
                        tint = ChatoneTheme.extraColors.modUnban.copy(alpha = 0.85f)
                    )
                }
                Spacer(Modifier.width(6.dp))
            }
            message.moderatorLogin?.takeIf { it.isNotBlank() }?.let { mod ->
                Text(
                    s.format(s.modRowBy, mod),
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.42f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    ) {
        if (targetUser != null && message.action != DisplayMessage.ModerationMsg.ModerationAction.CLEAR) {
            Text(
                targetUser,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.92f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = if (onUsernameClick != null) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onUsernameClick(targetUser) }
                } else Modifier
            )
            Spacer(Modifier.width(7.dp))
        }
        Text(
            predicate,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.66f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
    }
}

@Composable
private fun AutomodActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(9.dp)
    Row(
        modifier = modifier
            .clip(shape)
            .background(accent.copy(alpha = 0.16f))
            .border(1.dp, accent.copy(alpha = 0.40f), shape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(13.dp))
        Spacer(Modifier.width(5.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = accent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
internal fun AutoModMsgItem(
    message: DisplayMessage.AutoModMsg,
    onAllow: () -> Unit,
    onDeny: () -> Unit,
    onUsernameClick: () -> Unit = {}
) {
    val s = LocalStrings.current
    val pending = message.status == DisplayMessage.AutoModMsg.AutoModStatus.PENDING
    val accent = when (message.status) {
        DisplayMessage.AutoModMsg.AutoModStatus.PENDING -> ChatoneTheme.extraColors.modUnban
        DisplayMessage.AutoModMsg.AutoModStatus.ALLOWED -> MaterialTheme.colorScheme.primary
        DisplayMessage.AutoModMsg.AutoModStatus.DENIED -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val caughtColor = ChatoneTheme.extraColors.modBan

    fun categoryLabel(raw: String?): String? = when (raw?.lowercase()) {
        "aggression" -> "Aggression"; "bullying" -> "Bullying"; "disability" -> "Disability"
        "misogyny" -> "Misogyny"; "race_ethnicity_or_religion", "race" -> "Discrimination"
        "sex_based_terms", "sexuality" -> "Sexual content"; "sexuality_sex_or_gender", "gender" -> "Identity"
        "swearing" -> "Profanity"; null, "" -> null
        else -> raw.replaceFirstChar { it.uppercase() }.replace('_', ' ')
    }

    val nameColor = message.color?.let { hex ->
        try {
            val v = hex.removePrefix("#").toLong(16)
            Color(
                red = ((v shr 16) and 0xFF) / 255f,
                green = ((v shr 8) and 0xFF) / 255f,
                blue = (v and 0xFF) / 255f
            )
        } catch (_: Exception) {
            null
        }
    } ?: MaterialTheme.colorScheme.primary

    val msgAlpha = if (message.status == DisplayMessage.AutoModMsg.AutoModStatus.DENIED) 0.45f else 0.92f

    val body = remember(message.text, message.flaggedFragments, caughtColor, msgAlpha) {
        buildAnnotatedString {
            val base = message.text
            if (message.flaggedFragments.isEmpty()) {
                append(base)
                return@buildAnnotatedString
            }
            var cursor = 0
            while (cursor < base.length) {
                val hit = message.flaggedFragments
                    .filter { it.isNotBlank() }
                    .mapNotNull { frag ->
                        val idx = base.indexOf(frag, cursor, ignoreCase = true)
                        if (idx >= 0) idx to frag else null
                    }
                    .minByOrNull { it.first }
                if (hit == null) {
                    append(base.substring(cursor))
                    break
                }
                append(base.substring(cursor, hit.first))
                withStyle(SpanStyle(color = caughtColor, fontWeight = FontWeight.SemiBold)) {
                    append(base.substring(hit.first, hit.first + hit.second.length))
                }
                cursor = hit.first + hit.second.length
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 3.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(accent.copy(alpha = if (pending) 0.10f else 0.06f))
            .drawWithContent {
                drawContent()
                val inset = 6.dp.toPx()
                val width = 3.dp.toPx()
                drawRoundRect(
                    color = accent,
                    topLeft = Offset(5.dp.toPx(), inset),
                    size = Size(width, (size.height - inset * 2).coerceAtLeast(0f)),
                    cornerRadius = CornerRadius(width / 2f, width / 2f)
                )
            }
            .padding(start = 16.dp, end = 12.dp, top = 9.dp, bottom = 9.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Outlined.Shield,
                contentDescription = null,
                modifier = Modifier.size(13.dp),
                tint = accent
            )
            Spacer(Modifier.width(5.dp))
            Text(
                s.chatAutomodPanel,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = accent,
                maxLines = 1
            )
            categoryLabel(message.reasonCategory)?.let { label ->
                Spacer(Modifier.width(7.dp))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(accent.copy(alpha = 0.16f))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = accent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    message.reasonLevel?.let { lvl ->
                        Spacer(Modifier.width(5.dp))
                        repeat(4) { i ->
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 0.5.dp)
                                    .size(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(
                                        if (i < lvl.coerceIn(0, 4)) accent
                                        else accent.copy(alpha = 0.25f)
                                    )
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        Row(verticalAlignment = Alignment.Top) {
            Text(
                text = message.displayName,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = nameColor.copy(alpha = msgAlpha),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onUsernameClick
                )
            )
            Text(
                text = ": ",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = msgAlpha * 0.6f)
            )
            SelectionContainer(modifier = Modifier.weight(1f)) {
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = msgAlpha)
                )
            }
        }

        if (pending) {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                AutomodActionButton(
                    label = s.automodAllow,
                    icon = Icons.Filled.Check,
                    accent = ChatoneTheme.extraColors.modUnban,
                    modifier = Modifier.weight(1f),
                    onClick = onAllow
                )
                AutomodActionButton(
                    label = s.automodDeny,
                    icon = Icons.Filled.Close,
                    accent = ChatoneTheme.extraColors.modBan,
                    modifier = Modifier.weight(1f),
                    onClick = onDeny
                )
            }
        }
    }
}
