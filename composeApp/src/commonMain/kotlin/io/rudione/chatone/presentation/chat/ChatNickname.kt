package io.rudione.chatone.presentation.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.rudione.chatone.domain.model.Badge
import io.rudione.chatone.domain.model.ChatNoticeUser
import io.rudione.chatone.domain.model.DisplayMessage
import io.rudione.chatone.domain.model.SevenTvCosmetics
import io.rudione.chatone.presentation.chat.components.BadgeIcon
import io.rudione.chatone.presentation.chat.components.collectBadgeVisuals

private class NicknameLayout { var value: TextLayoutResult? = null }

internal data class ChatNicknameVisual(val name: String, val color: Color, val paint: SevenTvCosmetics.Paint?)

@Composable
internal fun rememberChatNickname(
    userId: String,
    login: String,
    displayName: String,
    color: String?,
    paint: SevenTvCosmetics.Paint? = null
): ChatNicknameVisual {
    val paints = LocalSevenTvPaints.current
    val published by remember(paints, userId) { derivedStateOf { paints.value[userId] } }
    val name = LocalNicknames.current[userId] ?: displayName.ifBlank { login }
    return ChatNicknameVisual(
        name,
        rememberNickColors().of(color, login.ifBlank { displayName }),
        (paint ?: published)?.takeIf { it.hasRenderableGradient() }
    )
}

@Composable
internal fun ChatNickname(
    user: ChatNoticeUser,
    modifier: Modifier = Modifier,
    fontSizeSp: Float = 14f,
    badges: List<Badge> = emptyList(),
    showBadges: Boolean = false,
    colorOverride: Color? = null,
    onClick: (() -> Unit)? = null
) {
    val cachedVisual = rememberChatNickname(user.userId, user.login, user.displayName, user.color)
    val visual = if (colorOverride != null) cachedVisual.copy(color = colorOverride) else cachedVisual
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        if (showBadges) {
            val cosmetics = LocalSevenTvCosmetics.current
            val badge by remember(cosmetics, user.userId) { derivedStateOf { cosmetics.value[user.userId]?.badge } }
            collectBadgeVisuals(user.login, user.userId, badges, badge).forEach { BadgeIcon(it, fontSizeSp.dp) }
        }
        val paint = visual.paint
        val paintModifier = if (paint != null) {
            val holder = remember { NicknameLayout() }
            val brush = rememberSevenTvPaintBrushProvider(paint, visual.color)
            val fallback = remember(visual.color) { SolidColor(visual.color) }
            val density = LocalDensity.current
            val shadow = remember(paint, density) { sevenTvPaintShadow(paint, density.density) }
            Modifier.drawWithContent {
                drawContent()
                val layout = holder.value
                val currentBrush = brush() ?: fallback
                if (layout != null) drawText(layout, brush = currentBrush, alpha = 1f, shadow = shadow)
            } to holder
        } else Modifier to null
        Text(
            text = visual.name,
            color = if (paint != null) Color.Transparent else visual.color,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = fontSizeSp.sp, fontWeight = FontWeight.Bold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { paintModifier.second?.value = it },
            modifier = paintModifier.first
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
        )
    }
}

internal fun ChatNoticeUser.profileMessage(channel: String, timestamp: Long): DisplayMessage.PrivMsg = DisplayMessage.PrivMsg(
    id = "notice_user_${userId.ifBlank { login }}",
    timestamp = timestamp,
    channel = channel,
    userId = userId,
    username = login,
    displayName = displayName.ifBlank { login },
    color = color,
    tokens = emptyList(),
    badges = emptyList(),
    isModerator = false,
    isSubscriber = false,
    isVip = false,
    isBroadcaster = false,
    isMention = false,
    isAction = false
)
