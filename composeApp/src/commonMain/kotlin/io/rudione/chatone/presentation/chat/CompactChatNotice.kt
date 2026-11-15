package io.rudione.chatone.presentation.chat

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.chat_notice_anonymous
import chatone.composeapp.generated.resources.chat_notice_gifts
import chatone.composeapp.generated.resources.chat_notice_raid
import chatone.composeapp.generated.resources.chat_notice_streak
import io.rudione.chatone.domain.model.ChatNoticeUser
import io.rudione.chatone.domain.model.DisplayMessage
import io.rudione.chatone.icons.lucide.ChevronRight
import io.rudione.chatone.icons.lucide.Flame
import io.rudione.chatone.icons.lucide.Gift
import io.rudione.chatone.icons.lucide.Lucide
import io.rudione.chatone.icons.lucide.Rocket
import io.rudione.chatone.icons.lucide.Users
import io.rudione.chatone.icons.lucide.Zap
import io.rudione.chatone.util.format.groupedNumber
import kotlin.time.Clock
import org.jetbrains.compose.resources.stringResource

internal data class CompactNoticeData(
    val id: String,
    val timestamp: Long,
    val author: ChatNoticeUser?,
    val raidViewers: Int?,
    val watchStreak: Int?,
    val giftCount: Int?,
    val anonymous: Boolean
)

internal fun DisplayMessage.UserNoticeMsg.compactNoticeData(): CompactNoticeData? {
    val details = details ?: return null
    if (details.raidViewers == null && details.watchStreak == null && details.gifts == null) return null
    return CompactNoticeData(
        id = id,
        timestamp = timestamp,
        author = details.author,
        raidViewers = details.raidViewers,
        watchStreak = details.watchStreak,
        giftCount = details.gifts?.total,
        anonymous = details.gifts?.anonymous == true
    )
}

internal fun raidLevel(viewers: Int): Int = when {
    viewers < 10 -> 0
    viewers < 100 -> 1
    viewers < 500 -> 2
    viewers < 1_500 -> 3
    viewers < 5_000 -> 4
    viewers < 15_000 -> 5
    else -> 6
}

@Composable
internal fun CompactChatNotice(
    notice: CompactNoticeData,
    fontSizeSp: Float,
    onOpenUser: (ChatNoticeUser) -> Unit,
    onUserVisible: (String) -> Unit = {},
    recipients: List<ChatNoticeUser> = emptyList()
) {
    val fontSize = (resolvedChatFontSizeOf(fontSizeSp) * 0.9f).coerceAtLeast(11f)
    val gifts = notice.giftCount
    var expanded by remember(notice.id) { mutableStateOf(false) }
    val chevron by animateFloatAsState(if (expanded) 90f else 0f, tween(180), label = "giftRecipientsChevron")
    val viewers = notice.raidViewers
    val streak = notice.watchStreak
    val tier = viewers?.let(::raidLevel) ?: 0
    val scheme = MaterialTheme.colorScheme
    val accent = when {
        gifts != null -> scheme.tertiary
        streak != null -> scheme.secondary
        else -> lerp(scheme.primary, scheme.tertiary, tier / 6f)
    }
    val icon = when {
        gifts != null -> Lucide.Gift
        streak != null -> Lucide.Flame
        tier >= 4 -> Lucide.Rocket
        tier >= 2 -> Lucide.Zap
        else -> Lucide.Users
    }
    val label = when {
        gifts != null -> stringResource(Res.string.chat_notice_gifts)
        streak != null -> stringResource(Res.string.chat_notice_streak)
        else -> stringResource(Res.string.chat_notice_raid)
    }
    val count = gifts ?: viewers ?: streak ?: 0
    val animateArrival = remember(notice.id) {
        viewers != null && Clock.System.now().toEpochMilliseconds() - notice.timestamp in 0L..10_000L
    }
    val entrance = if (viewers != null) remember(notice.id) { Animatable(if (animateArrival) 0f else 1f) } else null
    if (animateArrival && entrance != null) LaunchedEffect(entrance) {
        entrance.animateTo(1f, tween(680, easing = FastOutSlowInEasing))
    }

    Column(Modifier.fillMaxWidth().then(
        if (gifts != null) Modifier.clickable(remember { MutableInteractionSource() }, indication = null) { expanded = !expanded }
        else Modifier
    )) {
        NoticeBar(
            accent = accent,
            icon = icon,
            fontSizeSp = fontSize,
            decoration = Modifier.drawWithContent {
                drawContent()
                val progress = entrance?.value ?: 1f
                if (viewers != null && tier > 0) {
                    val gap = 4.dp.toPx()
                    repeat(tier + 1) { index ->
                        val x = size.width - 4.dp.toPx() - index * gap
                        drawLine(accent.copy(alpha = 0.32f), Offset(x, 3.dp.toPx()), Offset(x - 2.dp.toPx(), 7.dp.toPx()), 1.4.dp.toPx(), StrokeCap.Round)
                    }
                }
                if (progress < 1f) {
                    val x = size.width * progress
                    drawRect(Brush.horizontalGradient(listOf(Color.Transparent, accent.copy(alpha = 0.22f), Color.Transparent), x - size.width / 3, x + size.width / 3))
                }
            }
        ) {
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                val author = notice.author
                if (notice.anonymous || author == null) {
                    Text(stringResource(Res.string.chat_notice_anonymous), fontSize = fontSize.sp, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.weight(1f, fill = false))
                } else {
                    ChatNickname(
                        author,
                        fontSizeSp = fontSize + 1f,
                        modifier = Modifier.weight(1f, fill = false),
                        onClick = { onOpenUser(author) }
                    )
                }
                Spacer(Modifier.width(6.dp))
                if (viewers != null) {
                    NoticeCount(count, gift = false, accent = accent, fontSizeSp = fontSize)
                    Spacer(Modifier.width(6.dp))
                }
                Text(label, fontSize = fontSize.sp, color = scheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (viewers == null) {
                Spacer(Modifier.width(6.dp))
                NoticeCount(count, gift = gifts != null, accent = accent, fontSizeSp = fontSize)
            }
            if (gifts != null) {
                Spacer(Modifier.width(3.dp))
                Icon(Lucide.ChevronRight, contentDescription = null, tint = accent, modifier = Modifier.size(13.dp).graphicsLayer { rotationZ = chevron })
            }
        }
        AnimatedVisibility(
            visible = gifts != null && expanded,
            enter = expandVertically(tween(180)) + fadeIn(tween(120)),
            exit = shrinkVertically(tween(140)) + fadeOut(tween(100))
        ) {
            GiftRecipientsContent(recipients, gifts ?: 0, onOpenUser, onUserVisible)
        }
    }
}

@Composable
private fun NoticeCount(count: Int, gift: Boolean, accent: Color, fontSizeSp: Float) {
    Text(
        (if (gift) "× " else "") + groupedNumber(count.toLong()),
        fontSize = (fontSizeSp * 0.92f).sp,
        fontWeight = FontWeight.Bold,
        color = accent,
        modifier = Modifier.clip(RoundedCornerShape(8.dp))
            .background(accent.copy(alpha = 0.13f))
            .padding(horizontal = 7.dp, vertical = 1.dp),
        maxLines = 1
    )
}

@Composable
private fun NoticeBar(
    accent: Color,
    icon: ImageVector,
    fontSizeSp: Float,
    modifier: Modifier = Modifier,
    decoration: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 1.dp)
            .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 5.dp, bottomEnd = 12.dp, bottomStart = 5.dp))
            .background(accent.copy(alpha = 0.09f))
            .then(modifier).then(decoration)
            .padding(horizontal = 7.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size((fontSizeSp * 1.18f).dp))
        Spacer(Modifier.width(6.dp))
        content()
    }
}
