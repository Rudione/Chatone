package io.rudione.chatone.presentation.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.reward_redemption_a11y
import coil3.compose.AsyncImage
import io.rudione.chatone.domain.model.DisplayMessage
import io.rudione.chatone.domain.model.HighlightRule
import io.rudione.chatone.presentation.chat.rendering.ChatterColors
import io.rudione.chatone.presentation.chat.rememberNickColors
import io.rudione.chatone.presentation.chat.resolvedChatFontSizeOf
import io.rudione.chatone.util.format.groupedNumber
import io.rudione.chatone.util.icons.TwitchPointsIcon
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun RewardRedemptionItem(
    message: DisplayMessage.RewardRedemptionMsg,
    chatFontSizeSp: Float,
    pointsIconUrl: String?,
    highlightRules: List<HighlightRule>,
    userColorByLogin: ChatterColors = ChatterColors.None,
    onUsernameClick: () -> Unit
) {
    val fontSize = resolvedChatFontSizeOf(chatFontSizeSp)
    val accent = Color(highlightRules.firstOrNull { it.id == "channel_points" }?.color ?: 0xFF9146FF)
    val nickColors = rememberNickColors()
    val nameColor = userColorByLogin[message.login.lowercase()]?.let(nickColors::adjust)
        ?: nickColors.of(message.color, message.login)
    val cost = groupedNumber(message.cost)
    val description = stringResource(Res.string.reward_redemption_a11y, message.displayName, message.rewardTitle, cost)
    val iconSize = (fontSize * 1.25f).dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(accent.copy(alpha = 0.10f))
            .drawWithContent {
                drawContent()
                val inset = 2.dp.toPx()
                val width = 3.dp.toPx()
                drawRoundRect(
                    color = accent.copy(alpha = 0.9f),
                    topLeft = Offset(1.dp.toPx(), inset),
                    size = Size(width, (size.height - inset * 2).coerceAtLeast(0f)),
                    cornerRadius = CornerRadius(width / 2f, width / 2f)
                )
            }
            .semantics(mergeDescendants = true) { contentDescription = description }
            .padding(start = 10.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RewardGlyph(message, accent, iconSize)
        Spacer(Modifier.width(7.dp))
        Text(
            message.displayName,
            fontSize = (fontSize + 1f).sp,
            fontWeight = FontWeight.Bold,
            color = nameColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onUsernameClick
            )
        )
        Text(
            "  ·  ",
            fontSize = fontSize.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
        Text(
            message.rewardTitle,
            fontSize = fontSize.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.92f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
        if (message.cost > 0) {
            Spacer(Modifier.width(8.dp))
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.16f))
                    .padding(horizontal = 7.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                CurrencyIcon("POINTS", pointsIconUrl, size = (fontSize * 0.95f).toInt(), tint = accent)
                Text(
                    cost,
                    fontSize = (fontSize * 0.85f).sp,
                    fontWeight = FontWeight.Bold,
                    color = accent,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun RewardGlyph(message: DisplayMessage.RewardRedemptionMsg, accent: Color, size: androidx.compose.ui.unit.Dp) {
    val tile = message.backgroundColor?.let(::Color) ?: accent
    val shape = RoundedCornerShape(6.dp)
    if (!message.imageUrl.isNullOrBlank()) {
        AsyncImage(
            model = message.imageUrl,
            contentDescription = null,
            modifier = Modifier
                .size(size + 4.dp)
                .clip(shape)
                .background(tile)
                .padding(2.dp)
        )
    } else {
        Icon(
            TwitchPointsIcon,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(size)
        )
    }
}
