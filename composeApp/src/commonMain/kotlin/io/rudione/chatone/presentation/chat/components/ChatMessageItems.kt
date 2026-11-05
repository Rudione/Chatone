package io.rudione.chatone.presentation.chat

import chatone.composeapp.generated.resources.mod_row_seconds
import chatone.composeapp.generated.resources.mod_row_minutes
import chatone.composeapp.generated.resources.mod_row_hours
import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.automod_allow
import chatone.composeapp.generated.resources.automod_deny
import chatone.composeapp.generated.resources.chat_automod_panel
import chatone.composeapp.generated.resources.chat_unban_user
import chatone.composeapp.generated.resources.mod_row_automod_allowed
import chatone.composeapp.generated.resources.mod_row_automod_denied
import chatone.composeapp.generated.resources.mod_row_banned
import chatone.composeapp.generated.resources.mod_row_by
import chatone.composeapp.generated.resources.mod_row_cleared
import chatone.composeapp.generated.resources.mod_row_deleted
import chatone.composeapp.generated.resources.mod_row_modded
import chatone.composeapp.generated.resources.mod_row_timed_out
import chatone.composeapp.generated.resources.mod_row_unbanned
import chatone.composeapp.generated.resources.mod_row_unmodded
import chatone.composeapp.generated.resources.mod_row_untimedout
import chatone.composeapp.generated.resources.mod_row_unvipped
import chatone.composeapp.generated.resources.mod_row_vipped
import chatone.composeapp.generated.resources.timeline_announcement
import org.jetbrains.compose.resources.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times
import io.rudione.chatone.domain.model.DisplayMessage
import io.rudione.chatone.presentation.components.LiquidGlassSurface
import io.rudione.chatone.presentation.theme.ChatoneTheme
import io.rudione.chatone.presentation.components.ChatoneIconButton
import io.rudione.chatone.icons.lucide.Check
import io.rudione.chatone.icons.lucide.CircleCheck
import io.rudione.chatone.icons.lucide.Info
import io.rudione.chatone.icons.lucide.Lucide
import io.rudione.chatone.icons.lucide.Megaphone
import io.rudione.chatone.icons.lucide.Shield
import io.rudione.chatone.icons.lucide.X

@Composable
internal fun SystemMsgItem(message: DisplayMessage.SystemMsg) {
    Row(
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Lucide.Info,
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
        if (isAnnouncement) {
            NoticeHeader(icon = Lucide.Megaphone, text = stringResource(Res.string.timeline_announcement), color = accent)
        }
        if (message.systemText.isNotBlank()) {
            Text(
                message.systemText,
                style = MaterialTheme.typography.bodySmall,
                color = accent,
                fontWeight = FontWeight.SemiBold
            )
        }
        message.innerMessage?.let {
            if (isAnnouncement || message.systemText.isNotBlank()) Spacer(modifier = Modifier.height(2.dp))
            PrivMsgItem(message = it)
        }
    }
}

@Composable
internal fun NoticeHeader(icon: ImageVector, text: String, color: Color, modifier: Modifier = Modifier) {
    Row(modifier = modifier.padding(vertical = 1.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(6.dp))
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            color = color,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
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
    contentVerticalPadding: Dp = 9.dp,
    trailing: @Composable (RowScope.() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 1.dp)
            .clip(RoundedCornerShape(6.dp))
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
                .padding(vertical = 2.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(accent)
        )
        Spacer(Modifier.width(8.dp))
        Row(
            modifier = Modifier.weight(1f).padding(vertical = contentVerticalPadding),
            verticalAlignment = Alignment.CenterVertically
        ) { content() }
        trailing?.let { it() }
    }
}

@Composable
private fun formatModDuration(seconds: Int): String = when {
    seconds >= 3600 -> stringResource(Res.string.mod_row_hours, seconds / 3600)
    seconds >= 60 -> stringResource(Res.string.mod_row_minutes, seconds / 60)
    else -> stringResource(Res.string.mod_row_seconds, seconds)
}

@Composable
internal fun ModerationMsgItem(
    message: DisplayMessage.ModerationMsg,
    chatFontSizeSp: Float = 15f,
    onUsernameClick: ((String) -> Unit)? = null,
    onUnbanByLogin: ((String) -> Unit)? = null
) {
    val resolvedFontSize = chatFontSizeSp.coerceIn(11f, 16f)
    val rowLineHeight = (resolvedFontSize * 1.35f).sp
    val rowFontSize = (resolvedFontSize * 0.92f).sp
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
        DisplayMessage.ModerationMsg.ModerationAction.AUTOMOD_ALLOWED -> ChatoneTheme.extraColors.modUnban
        DisplayMessage.ModerationMsg.ModerationAction.AUTOMOD_DENIED -> ChatoneTheme.extraColors.modBan
    }
    val isAutoMod = message.action == DisplayMessage.ModerationMsg.ModerationAction.AUTOMOD_ALLOWED ||
            message.action == DisplayMessage.ModerationMsg.ModerationAction.AUTOMOD_DENIED
    val targetUser = message.targetUser
    val predicate = when (message.action) {
        DisplayMessage.ModerationMsg.ModerationAction.BAN -> stringResource(Res.string.mod_row_banned)
        DisplayMessage.ModerationMsg.ModerationAction.TIMEOUT ->
            stringResource(Res.string.mod_row_timed_out, formatModDuration(message.duration ?: 0))
        DisplayMessage.ModerationMsg.ModerationAction.DELETE -> stringResource(Res.string.mod_row_deleted)
        DisplayMessage.ModerationMsg.ModerationAction.UNBAN -> stringResource(Res.string.mod_row_unbanned)
        DisplayMessage.ModerationMsg.ModerationAction.UNTIMEOUT -> stringResource(Res.string.mod_row_untimedout)
        DisplayMessage.ModerationMsg.ModerationAction.MOD -> stringResource(Res.string.mod_row_modded)
        DisplayMessage.ModerationMsg.ModerationAction.UNMOD -> stringResource(Res.string.mod_row_unmodded)
        DisplayMessage.ModerationMsg.ModerationAction.VIP -> stringResource(Res.string.mod_row_vipped)
        DisplayMessage.ModerationMsg.ModerationAction.UNVIP -> stringResource(Res.string.mod_row_unvipped)
        DisplayMessage.ModerationMsg.ModerationAction.CLEAR -> stringResource(Res.string.mod_row_cleared)
        DisplayMessage.ModerationMsg.ModerationAction.AUTOMOD_ALLOWED -> stringResource(Res.string.mod_row_automod_allowed)
        DisplayMessage.ModerationMsg.ModerationAction.AUTOMOD_DENIED -> stringResource(Res.string.mod_row_automod_denied)
    }
    val showUnbanButton = message.action == DisplayMessage.ModerationMsg.ModerationAction.BAN
            && targetUser != null && onUnbanByLogin != null

    ChatEventRow(
        accent = color,
        contentVerticalPadding = 1.dp,
        trailing = {
            if (showUnbanButton) {
                ChatoneIconButton(
                    onClick = { onUnbanByLogin!!(targetUser!!) },
                    modifier = Modifier.size(16.dp)
                ) {
                    Icon(
                        Lucide.CircleCheck,
                        contentDescription = "${stringResource(Res.string.chat_unban_user)} $targetUser",
                        modifier = Modifier.size(13.dp),
                        tint = ChatoneTheme.extraColors.modUnban.copy(alpha = 0.85f)
                    )
                }
                Spacer(Modifier.width(6.dp))
            }
            message.moderatorLogin?.takeIf { it.isNotBlank() }?.let { mod ->
                Text(
                    stringResource(Res.string.mod_row_by, mod),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = rowFontSize * 0.9f,
                        lineHeight = rowLineHeight
                    ),
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.42f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    ) {
        if (isAutoMod) {
            AutoModResolutionContent(
                predicate = predicate,
                accent = color,
                author = targetUser,
                fontSize = rowFontSize,
                lineHeight = rowLineHeight,
                onAuthorClick = onUsernameClick
            )
        } else ModerationTargetContent(
            message = message,
            predicate = predicate,
            fontSize = rowFontSize,
            lineHeight = rowLineHeight,
            onUsernameClick = onUsernameClick
        )
    }
}

@Composable
private fun RowScope.ModerationTargetContent(
    message: DisplayMessage.ModerationMsg,
    predicate: String,
    fontSize: TextUnit,
    lineHeight: TextUnit,
    onUsernameClick: ((String) -> Unit)?
) {
    val targetUser = message.targetUser
    if (targetUser != null && message.action != DisplayMessage.ModerationMsg.ModerationAction.CLEAR) {
        Text(
            targetUser,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = fontSize,
                lineHeight = lineHeight
            ),
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
        Spacer(Modifier.width(6.dp))
    }
    Text(
        predicate,
        style = MaterialTheme.typography.bodySmall.copy(
            fontSize = fontSize,
            lineHeight = lineHeight
        ),
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.66f),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.weight(1f, fill = false)
    )
}

@Composable
private fun RowScope.AutoModResolutionContent(
    predicate: String,
    accent: Color,
    author: String?,
    fontSize: TextUnit,
    lineHeight: TextUnit,
    onAuthorClick: ((String) -> Unit)?
) {
    Icon(Lucide.Shield, contentDescription = null, tint = accent, modifier = Modifier.size(12.dp))
    Spacer(Modifier.width(5.dp))
    Text(
        predicate,
        style = MaterialTheme.typography.bodySmall.copy(fontSize = fontSize, lineHeight = lineHeight),
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.66f),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.weight(1f, fill = false)
    )
    if (author != null) {
        Text(
            "  ·  ",
            style = MaterialTheme.typography.bodySmall.copy(fontSize = fontSize, lineHeight = lineHeight),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        )
        Text(
            author,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = fontSize, lineHeight = lineHeight),
            fontWeight = FontWeight.Bold,
            color = rememberNickColors().of(null, author),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = if (onAuthorClick != null) {
                Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable(onClick = { onAuthorClick(author) })
                    .pointerHoverIcon(PointerIcon.Hand)
                    .padding(horizontal = 2.dp)
            } else Modifier
        )
    }
}
