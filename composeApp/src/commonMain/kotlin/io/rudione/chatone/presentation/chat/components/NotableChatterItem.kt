package io.rudione.chatone.presentation.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import io.rudione.chatone.domain.model.DisplayMessage
import io.rudione.chatone.icons.lucide.BadgeCheck
import io.rudione.chatone.icons.lucide.Lucide
import io.rudione.chatone.presentation.chat.compactCount
import io.rudione.chatone.presentation.chat.rememberNickColors
import io.rudione.chatone.presentation.theme.i18n.LocalStrings
import io.rudione.chatone.presentation.theme.i18n.format

private val PartnerPurple = Color(0xFF9146FF)
private const val ROW_SCALE = 1.2f

@Composable
internal fun NotableChatterItem(
    message: DisplayMessage.NotableChatterMsg,
    chatFontSizeSp: Float,
    onClick: () -> Unit
) {
    val s = LocalStrings.current
    val density = LocalDensity.current
    val fontSize = chatFontSizeSp.coerceIn(11f, 20f)
    val rowHeight = with(density) { ((fontSize * 1.35f).sp.toDp() + 4.dp) * ROW_SCALE }
    val avatarSize = rowHeight - 8.dp
    val nameColor = rememberNickColors().of(message.color, message.login)
    val accent = MaterialTheme.colorScheme.primary
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val description = s.format(s.chatNotableInChatA11y, message.displayName)

    Box(
        modifier = Modifier.fillMaxWidth().height(rowHeight).padding(vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxHeight()
                .clip(CircleShape)
                .background(accent.copy(alpha = if (hovered) 0.2f else 0.11f))
                .border(1.dp, accent.copy(alpha = if (hovered) 0.45f else 0.26f), CircleShape)
                .hoverable(interaction)
                .clickable(interactionSource = interaction, indication = null, onClick = onClick)
                .semantics {
                    contentDescription = description
                    role = Role.Button
                }
                .padding(start = 3.dp, end = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier.size(avatarSize).clip(CircleShape).background(nameColor.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                if (message.avatarUrl != null) {
                    AsyncImage(
                        model = message.avatarUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(avatarSize)
                    )
                } else {
                    Text(
                        message.displayName.take(1).uppercase(),
                        fontSize = (fontSize * 0.7f).sp,
                        fontWeight = FontWeight.Bold,
                        color = nameColor
                    )
                }
            }
            Text(
                message.displayName,
                fontSize = (fontSize * 0.92f).sp,
                fontWeight = FontWeight.Bold,
                color = nameColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (message.isPartner) {
                Icon(
                    Lucide.BadgeCheck,
                    contentDescription = null,
                    modifier = Modifier.size((fontSize * 0.95f).dp),
                    tint = PartnerPurple
                )
            }
            Text(
                s.chatNotableInChat,
                fontSize = (fontSize * 0.85f).sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                maxLines = 1
            )
            if (message.followers >= 1_000) {
                Text(
                    "· ${compactCount(message.followers)}",
                    fontSize = (fontSize * 0.8f).sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                    maxLines = 1
                )
            }
        }
    }
}
