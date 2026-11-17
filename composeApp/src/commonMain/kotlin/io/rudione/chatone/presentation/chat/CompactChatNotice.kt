package io.rudione.chatone.presentation.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.chat_notice_anonymous
import chatone.composeapp.generated.resources.chat_notice_gifts
import chatone.composeapp.generated.resources.chat_notice_raid
import io.rudione.chatone.domain.model.ChatNoticeUser
import io.rudione.chatone.domain.model.ChatSubscription
import io.rudione.chatone.domain.model.DisplayMessage
import io.rudione.chatone.icons.lucide.ChevronRight
import io.rudione.chatone.icons.lucide.Gift
import io.rudione.chatone.icons.lucide.Lucide
import io.rudione.chatone.icons.lucide.Rocket
import io.rudione.chatone.icons.lucide.Users
import io.rudione.chatone.icons.lucide.Zap
import io.rudione.chatone.util.format.groupedNumber
import kotlin.time.Clock
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.graphics.shadow.Shadow as DropShadow

internal data class CompactNoticeData(
    val id: String,
    val timestamp: Long,
    val author: ChatNoticeUser?,
    val raidViewers: Int?,
    val watchStreak: Int?,
    val giftCount: Int?,
    val anonymous: Boolean,
    val subscription: ChatSubscription? = null
)

internal fun DisplayMessage.UserNoticeMsg.compactNoticeData(): CompactNoticeData? {
    val details = details ?: return null
    if (details.raidViewers == null && details.watchStreak == null && details.gifts == null &&
        details.subscription == null) return null
    return CompactNoticeData(
        id = id,
        timestamp = timestamp,
        author = details.author,
        raidViewers = details.raidViewers,
        watchStreak = details.watchStreak,
        giftCount = details.gifts?.total,
        anonymous = details.gifts?.anonymous == true,
        subscription = details.subscription
    )
}

internal fun raidLevel(viewers: Int): Int = when {
    viewers < 10 -> 0
    viewers < 100 -> 1
    viewers < 500 -> 2
    viewers < 1_000 -> 3
    viewers < 2_500 -> 4
    viewers < 6_000 -> 5
    viewers < 12_500 -> 6
    viewers < 21_000 -> 7
    else -> 8
}

internal const val RaidOuterGlowViewers = 5_000

private val RaidPalette = listOf(
    Color(0xFF8E8E9A),
    Color(0xFFC4C4CE),
    Color(0xFF4F7CFF),
    Color(0xFF5CCBF5),
    Color(0xFFA66BFF),
    Color(0xFFFFC53D),
    Color(0xFFFF8A2E),
    Color(0xFFFF4747),
    Color(0xFFB3163F)
)

internal fun raidColor(tier: Int): Color = RaidPalette[tier.coerceIn(0, RaidPalette.lastIndex)]

internal val NoticeShape = RoundedCornerShape(topStart = 12.dp, topEnd = 5.dp, bottomEnd = 12.dp, bottomStart = 5.dp)

@Composable
internal fun CompactChatNotice(
    notice: CompactNoticeData,
    fontSizeSp: Float,
    onOpenUser: (ChatNoticeUser) -> Unit,
    onUserVisible: (String) -> Unit = {},
    recipients: List<ChatNoticeUser> = emptyList(),
    footer: (@Composable () -> Unit)? = null
) {
    val fontSize = (resolvedChatFontSizeOf(fontSizeSp) * 0.9f).coerceAtLeast(11f)
    val gifts = notice.giftCount
    val streak = notice.watchStreak
    val subscription = notice.subscription
    if (subscription != null) {
        SubChatNotice(notice, subscription, fontSize, onOpenUser, footer)
        return
    }
    if (streak != null && gifts == null && notice.raidViewers == null) {
        StreakChatNotice(notice, streak, fontSize, onOpenUser, footer)
        return
    }
    var expanded by remember(notice.id) { mutableStateOf(false) }
    val chevron by animateFloatAsState(if (expanded) 90f else 0f, tween(180), label = "giftRecipientsChevron")
    val viewers = notice.raidViewers
    val tier = viewers?.let(::raidLevel) ?: 0
    val scheme = MaterialTheme.colorScheme
    val accent = if (gifts != null) scheme.tertiary else raidColor(tier)
    val icon = when {
        gifts != null -> Lucide.Gift
        tier >= 5 -> Lucide.Rocket
        tier >= 2 -> Lucide.Zap
        else -> Lucide.Users
    }
    val label = when {
        gifts != null -> stringResource(Res.string.chat_notice_gifts)
        else -> stringResource(Res.string.chat_notice_raid)
    }
    val count = gifts ?: viewers ?: 0
    val entrance = rememberNoticeEntrance(notice, viewers != null)
    val outerGlow = if (viewers != null && viewers >= RaidOuterGlowViewers) remember(tier, accent) {
        val step = (tier - 4).coerceAtLeast(1)
        DropShadow(radius = (4 + 2 * step).dp, color = accent, alpha = (0.3f + 0.06f * step).coerceAtMost(0.55f))
    } else null

    Column(Modifier.fillMaxWidth().then(
        if (gifts != null) Modifier.clickable(remember { MutableInteractionSource() }, indication = null) { expanded = !expanded }
        else Modifier
    )) {
        NoticeBar(
            accent = accent,
            leading = { Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size((fontSize * 1.18f).dp)) },
            glow = outerGlow,
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
                Spacer(Modifier.width(4.dp))
                if (viewers != null) {
                    GlowCount(groupedNumber(count.toLong()), glow = accent, strength = tier / 8f, fontSizeSp = fontSize)
                    Spacer(Modifier.width(4.dp))
                }
                Text(label, fontSize = fontSize.sp, color = scheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (viewers == null) {
                Spacer(Modifier.width(4.dp))
                GlowCount("× " + groupedNumber(count.toLong()), glow = accent, strength = 0.45f, fontSizeSp = fontSize)
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
        footer?.invoke()
    }
}

@Composable
internal fun rememberNoticeEntrance(
    notice: CompactNoticeData,
    enabled: Boolean,
    durationMillis: Int = 680,
    easing: Easing = FastOutSlowInEasing
): Animatable<Float, *>? {
    val fresh = remember(notice.id) {
        enabled && Clock.System.now().toEpochMilliseconds() - notice.timestamp in 0L..10_000L
    }
    val entrance = if (enabled) remember(notice.id) { Animatable(if (fresh) 0f else 1f) } else null
    if (fresh && entrance != null) LaunchedEffect(entrance) {
        entrance.animateTo(1f, tween(durationMillis, easing = easing))
    }
    return entrance
}

@Composable
internal fun GlowCount(text: String, glow: Color, strength: Float, fontSizeSp: Float) {
    val ink = MaterialTheme.colorScheme.onSurface
    val level = strength.coerceIn(0f, 1f)
    val blur = with(LocalDensity.current) { (1.5f + 3f * level).dp.toPx() }
    val style = remember(ink, glow, level, blur, fontSizeSp) {
        TextStyle(
            color = ink,
            fontSize = (fontSizeSp * 0.98f).sp,
            fontWeight = FontWeight.ExtraBold,
            shadow = if (level >= 0.5f) Shadow(glow.copy(alpha = 0.55f + 0.35f * level), blurRadius = blur) else null
        )
    }
    Text(
        text,
        style = style,
        maxLines = 1,
        modifier = Modifier
            .drawWithCache {
                val center = Offset(size.width / 2, size.height / 2)
                val radiusY = size.height / 2 + 2.dp.toPx()
                val radiusX = size.width / 2 + (5f + 7f * level).dp.toPx()
                val peak = 0.2f + 0.32f * level
                val brush = Brush.radialGradient(
                    0f to glow.copy(alpha = peak),
                    0.55f to glow.copy(alpha = peak * 0.45f),
                    1f to Color.Transparent,
                    center = center,
                    radius = radiusY
                )
                val stretch = radiusX / radiusY
                onDrawBehind {
                    scale(stretch, 1f, pivot = center) { drawCircle(brush, radiusY, center) }
                }
            }
            .padding(horizontal = 4.dp)
    )
}

@Composable
internal fun NoticeBar(
    accent: Color,
    leading: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    glow: DropShadow? = null,
    decoration: Modifier = Modifier,
    header: Modifier = Modifier,
    footer: (@Composable () -> Unit)? = null,
    content: @Composable RowScope.() -> Unit
) {
    if (glow == null) {
        NoticeCard(accent, 0.09f, leading, modifier, decoration, header, footer, content)
        return
    }
    Box(Modifier.fillMaxWidth()) {
        Spacer(
            Modifier.matchParentSize()
                .padding(horizontal = 4.dp, vertical = 1.dp)
                .drawWithCache {
                    val cutout = Path().apply { addOutline(NoticeShape.createOutline(size, layoutDirection, this@drawWithCache)) }
                    onDrawWithContent { clipPath(cutout, ClipOp.Difference) { this@onDrawWithContent.drawContent() } }
                }
                .dropShadow(NoticeShape, glow)
        )
        NoticeCard(accent, 0.13f, leading, modifier, decoration, header, footer, content)
    }
}

@Composable
private fun NoticeCard(
    accent: Color,
    tint: Float,
    leading: @Composable () -> Unit,
    modifier: Modifier,
    decoration: Modifier,
    header: Modifier,
    footer: (@Composable () -> Unit)?,
    content: @Composable RowScope.() -> Unit
) {
    val card = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 1.dp)
        .clip(NoticeShape)
        .background(accent.copy(alpha = tint))
        .then(modifier).then(decoration)
        .padding(horizontal = 7.dp, vertical = 2.dp)
    if (footer == null) {
        Row(card.then(header), verticalAlignment = Alignment.CenterVertically) {
            leading()
            Spacer(Modifier.width(6.dp))
            content()
        }
    } else {
        Column(card) {
            Row(Modifier.fillMaxWidth().then(header), verticalAlignment = Alignment.CenterVertically) {
                leading()
                Spacer(Modifier.width(6.dp))
                content()
            }
            footer()
        }
    }
}
