package io.rudione.chatone.presentation.chat

import androidx.compose.foundation.background
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.rudione.chatone.domain.model.DisplayMessage
import io.rudione.chatone.presentation.components.LiquidGlassSurface
import io.rudione.chatone.presentation.theme.ChatoneTheme
import io.rudione.chatone.presentation.theme.i18n.LocalStrings

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
internal fun ModerationMsgItem(
    message: DisplayMessage.ModerationMsg,
    onUsernameClick: ((String) -> Unit)? = null,
    onUnbanByLogin: ((String) -> Unit)? = null
) {
    val s = LocalStrings.current
    val (icon, color) = when (message.action) {
        DisplayMessage.ModerationMsg.ModerationAction.BAN -> Icons.Filled.Close to ChatoneTheme.extraColors.modBan
        DisplayMessage.ModerationMsg.ModerationAction.TIMEOUT -> Icons.Outlined.Refresh to ChatoneTheme.extraColors.modTimeout
        DisplayMessage.ModerationMsg.ModerationAction.DELETE -> Icons.Outlined.Delete to ChatoneTheme.extraColors.modDelete
        DisplayMessage.ModerationMsg.ModerationAction.CLEAR -> Icons.Outlined.Clear to ChatoneTheme.extraColors.modDelete
        DisplayMessage.ModerationMsg.ModerationAction.UNBAN -> Icons.Outlined.CheckCircle to ChatoneTheme.extraColors.modUnban
    }
    val showUnbanButton = message.action == DisplayMessage.ModerationMsg.ModerationAction.BAN
            && message.targetUser != null && onUnbanByLogin != null

    Row(
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showUnbanButton) {
            IconButton(
                onClick = { onUnbanByLogin!!(message.targetUser!!) },
                modifier = Modifier.size(20.dp)
            ) {
                Icon(
                    Icons.Outlined.CheckCircle,
                    contentDescription = "${s.chatUnbanUser} ${message.targetUser}",
                    modifier = Modifier.size(14.dp),
                    tint = ChatoneTheme.extraColors.modUnban.copy(alpha = 0.85f)
                )
            }
            Spacer(Modifier.width(4.dp))
        }
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = color.copy(alpha = 0.7f)
        )
        Spacer(Modifier.width(4.dp))
        val targetUser = message.targetUser
        if (targetUser != null && onUsernameClick != null && message.action != DisplayMessage.ModerationMsg.ModerationAction.CLEAR) {
            val annotated = buildAnnotatedString {
                val idx = message.text.indexOf(targetUser, ignoreCase = true)
                if (idx >= 0) {
                    append(message.text.substring(0, idx))
                    pushStringAnnotation("user", targetUser)
                    withStyle(
                        SpanStyle(
                            color = color.copy(alpha = 0.9f),
                            fontWeight = FontWeight.SemiBold,
                            textDecoration = TextDecoration.Underline
                        )
                    ) { append(message.text.substring(idx, idx + targetUser.length)) }
                    pop()
                    append(message.text.substring(idx + targetUser.length))
                } else {
                    append(message.text)
                }
            }
            var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
            Text(
                annotated,
                style = MaterialTheme.typography.bodySmall,
                color = color.copy(alpha = 0.7f),
                fontStyle = FontStyle.Italic,
                onTextLayout = { layoutResult = it },
                modifier = Modifier.pointerInput(annotated) {
                    detectTapGestures { offset ->
                        layoutResult?.let { lr ->
                            val charOffset = lr.getOffsetForPosition(offset)
                            annotated.getStringAnnotations("user", charOffset, charOffset)
                                .firstOrNull()?.let { onUsernameClick(it.item) }
                        }
                    }
                })
        } else {
            Text(
                message.text,
                style = MaterialTheme.typography.bodySmall,
                color = color.copy(alpha = 0.7f),
                fontStyle = FontStyle.Italic
            )
        }
    }
}


@Composable
internal fun AutoModMsgItem(
    message: DisplayMessage.AutoModMsg,
    onAllow: () -> Unit,
    onDeny: () -> Unit,
    onUsernameClick: () -> Unit = {}
) {
    val pendingAccent = Color(0xFFE0A12F)
    val accent = when (message.status) {
        DisplayMessage.AutoModMsg.AutoModStatus.PENDING -> pendingAccent
        DisplayMessage.AutoModMsg.AutoModStatus.ALLOWED -> MaterialTheme.colorScheme.primary
        DisplayMessage.AutoModMsg.AutoModStatus.DENIED -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val bgColor = accent.copy(
        alpha = if (message.status == DisplayMessage.AutoModMsg.AutoModStatus.PENDING) 0.10f else 0.06f
    )

    fun categoryLabel(raw: String?): String? = when (raw?.lowercase()) {
        "aggression" -> "Aggression"; "bullying" -> "Bullying"; "disability" -> "Disability"
        "misogyny" -> "Misogyny"; "race_ethnicity_or_religion", "race" -> "Discrimination"
        "sex_based_terms", "sexuality" -> "Sexual content"; "sexuality_sex_or_gender", "gender" -> "Identity"
        "swearing" -> "Profanity"; null, "" -> null
        else -> raw.replaceFirstChar { it.uppercase() }.replace('_', ' ')
    }

    LiquidGlassSurface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(0.dp),
        backgroundAlphaHigh = 0.85f,
        backgroundAlphaLow = 0.75f,
        borderAlphaHigh = 0.25f,
        borderAlphaLow = 0.10f
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val compactActions = maxWidth < 320.dp
            Column(
                modifier = Modifier.fillMaxWidth()
                    .drawWithContent {
                        drawContent()
                        drawRect(
                            color = accent.copy(alpha = 0.9f),
                            size = Size(3.dp.toPx(), size.height)
                        )
                    }
                    .background(bgColor)
                    .padding(start = 12.dp, end = 10.dp, top = 8.dp, bottom = 8.dp)
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
                        LocalStrings.current.chatAutomodPanel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = accent
                    )
                    val label = categoryLabel(message.reasonCategory)
                    if (label != null) {
                        Spacer(Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = accent.copy(alpha = 0.16f),
                            tonalElevation = 0.dp
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = accent
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
                    Spacer(Modifier.weight(1f))
                    if (message.status == DisplayMessage.AutoModMsg.AutoModStatus.PENDING && !compactActions) {
                        TextButton(
                            onClick = onAllow,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            modifier = Modifier.height(26.dp),
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text(
                                LocalStrings.current.automodAllow,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.width(2.dp))
                        TextButton(
                            onClick = onDeny,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            modifier = Modifier.height(26.dp),
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text(
                                LocalStrings.current.automodDeny,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                if (message.status == DisplayMessage.AutoModMsg.AutoModStatus.PENDING && compactActions) {
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val allowBg = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        val denyBg = MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(allowBg)
                                .clickable(onClick = onAllow)
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = LocalStrings.current.automodAllow,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                LocalStrings.current.automodAllow,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(denyBg)
                                .clickable(onClick = onDeny)
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = LocalStrings.current.automodDeny,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                LocalStrings.current.automodDeny,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                Spacer(Modifier.height(5.dp))
                val automodBg = MaterialTheme.colorScheme.background
                val nameColor = message.color?.let { hex ->
                    try {
                        val v = hex.removePrefix("#").toLong(16)
                        adjustReadableColor(
                            Color(
                                red = ((v shr 16) and 0xFF) / 255f,
                                green = ((v shr 8) and 0xFF) / 255f,
                                blue = (v and 0xFF) / 255f
                            ),
                            automodBg
                        )
                    } catch (_: Exception) {
                        null
                    }
                } ?: MaterialTheme.colorScheme.primary
                val msgAlpha =
                    if (message.status == DisplayMessage.AutoModMsg.AutoModStatus.DENIED) 0.4f else 0.9f
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        text = message.displayName,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = nameColor.copy(alpha = msgAlpha),
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
                    SelectionContainer {
                        Text(
                            text = message.text,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = msgAlpha)
                        )
                    }
                }
            }
        }
    }
}