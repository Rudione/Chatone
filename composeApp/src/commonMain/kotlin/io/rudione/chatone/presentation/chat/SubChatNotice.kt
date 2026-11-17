package io.rudione.chatone.presentation.chat

import androidx.compose.animation.core.LinearEasing
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.chat_notice_sub_months
import chatone.composeapp.generated.resources.chat_notice_sub_new
import chatone.composeapp.generated.resources.chat_notice_sub_prime
import chatone.composeapp.generated.resources.chat_notice_sub_tier
import io.rudione.chatone.domain.model.ChatNoticeUser
import io.rudione.chatone.domain.model.ChatSubTier
import io.rudione.chatone.domain.model.ChatSubscription
import io.rudione.chatone.presentation.components.ControlMotion
import io.rudione.chatone.presentation.components.LocalControlMotion
import io.rudione.chatone.presentation.components.motion.bump
import io.rudione.chatone.presentation.components.motion.easeOutCubic
import io.rudione.chatone.presentation.components.motion.window
import io.rudione.chatone.presentation.theme.ACCENT_MIN_CONTRAST
import io.rudione.chatone.presentation.theme.readableOn
import io.rudione.chatone.util.format.groupedNumber
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

private const val SweepStart = 0.4f
private const val SweepLength = 0.36f
private const val SweepGap = 0.22f

@Composable
internal fun SubChatNotice(
    notice: CompactNoticeData,
    subscription: ChatSubscription,
    fontSize: Float,
    onOpenUser: (ChatNoticeUser) -> Unit,
    footer: (@Composable () -> Unit)?
) {
    val scheme = MaterialTheme.colorScheme
    val look = subTierLook(subscription.tier)
    val still = LocalControlMotion.current == ControlMotion.Reduced
    val arrival = rememberNoticeEntrance(notice, !still, SubArrivalMillis, LinearEasing)
    val progress = { arrival?.value ?: 1f }
    val iconSize = (fontSize * 2.1f).dp
    val months = subscription.months
    val line = if (months > 1) {
        pluralStringResource(Res.plurals.chat_notice_sub_months, months, NoticeNumberSlot)
    } else stringResource(Res.string.chat_notice_sub_new)
    val before = line.substringBefore(NoticeNumberSlot).trim()
    val after = line.substringAfter(NoticeNumberSlot, "").trim()
    NoticeBar(
        accent = look.boxBottom,
        glow = look.outerGlow,
        leading = { Spacer(Modifier.size(iconSize).drawBehind { drawSubGift(look, progress()) }) },
        decoration = Modifier.subCardDecoration(look, progress),
        header = Modifier.subIconGlow(look, iconSize, progress),
        footer = footer
    ) {
        Column(Modifier.weight(1f).padding(vertical = 2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) {
                    notice.author?.let { author ->
                        ChatNickname(author, fontSizeSp = fontSize + 1f, onClick = { onOpenUser(author) })
                    }
                }
                Spacer(Modifier.width(6.dp))
                SubTierChip(subscription.tier, look, fontSize)
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                itemVerticalAlignment = Alignment.CenterVertically
            ) {
                if (before.isNotEmpty()) Text(before, fontSize = fontSize.sp, color = scheme.onSurfaceVariant)
                if (months > 1) {
                    GlowCount(groupedNumber(months.toLong()), glow = look.glow, strength = look.numberGlow, fontSizeSp = fontSize)
                }
                if (after.isNotEmpty()) Text(after, fontSize = fontSize.sp, color = scheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SubTierChip(tier: ChatSubTier, look: SubTierLook, fontSize: Float) {
    val label = when (tier) {
        ChatSubTier.Prime -> stringResource(Res.string.chat_notice_sub_prime)
        ChatSubTier.Tier1 -> stringResource(Res.string.chat_notice_sub_tier, 1)
        ChatSubTier.Tier2 -> stringResource(Res.string.chat_notice_sub_tier, 2)
        ChatSubTier.Tier3 -> stringResource(Res.string.chat_notice_sub_tier, 3)
    }
    val surface = MaterialTheme.colorScheme.surface
    val ink = remember(look, surface) { readableOn(look.chipInk, surface, ACCENT_MIN_CONTRAST) }
    Text(
        label,
        fontSize = (fontSize * 0.78f).sp,
        fontWeight = FontWeight.Bold,
        color = ink,
        maxLines = 1,
        modifier = Modifier
            .clip(CircleShape)
            .background(look.chip)
            .padding(horizontal = 6.dp, vertical = 1.dp)
    )
}

private fun Modifier.subIconGlow(look: SubTierLook, iconSize: Dp, arrival: () -> Float): Modifier = drawWithCache {
    val center = Offset(iconSize.toPx() / 2, size.height / 2)
    val radius = iconSize.toPx() * 0.9f + (6 * look.sweeps).dp.toPx()
    val glow = Brush.radialGradient(listOf(look.glow, Color.Transparent), center, radius)
    onDrawBehind {
        val t = arrival()
        val shine = maxOf(subPulse(t), subLidOpen(t))
        drawCircle(glow, radius * (1f + 0.25f * shine), center, alpha = look.haloRest * 0.6f + 0.4f * shine)
    }
}

private fun Modifier.subCardDecoration(look: SubTierLook, arrival: () -> Float): Modifier = drawWithCache {
    val rimPath = look.rim?.let { Path().apply { addOutline(NoticeShape.createOutline(size, layoutDirection, this@drawWithCache)) } }
    val rimBrush = look.rim?.let { color ->
        Brush.linearGradient(
            listOf(color, color.copy(alpha = 0f), color.copy(alpha = color.alpha * 0.55f)),
            start = Offset.Zero,
            end = Offset(size.width, size.height)
        )
    }
    val rimStroke = Stroke(1.5.dp.toPx())
    val band = size.width / 3
    val sweep = Brush.horizontalGradient(
        listOf(Color.Transparent, look.glow.copy(alpha = look.sweep), Color.Transparent),
        startX = -band,
        endX = band
    )
    val sweepSize = Size(band * 2, size.height)
    val star = 1.6.dp.toPx()
    val starStroke = 0.9.dp.toPx()
    val starGap = 7.dp.toPx()
    val starRight = 8.dp.toPx()
    val starTop = 4.dp.toPx()
    val starLow = 7.dp.toPx()
    val stars = (look.sparkles - 3).coerceAtLeast(0)
    onDrawWithContent {
        drawRect(look.wash)
        drawContent()
        if (rimPath != null && rimBrush != null) drawPath(rimPath, rimBrush, style = rimStroke)
        val t = arrival()
        for (index in 0 until stars) {
            val twinkle = bump(window(t, SubLidStart + index * 0.07f, SubLidStart + index * 0.07f + 0.3f))
            val alpha = 0.3f + 0.6f * twinkle
            val arm = star * (1f + 0.6f * twinkle)
            val center = Offset(size.width - starRight - index * starGap, if (index % 2 == 0) starTop else starLow)
            drawLine(look.glow, center.copy(x = center.x - arm), center.copy(x = center.x + arm), starStroke, StrokeCap.Round, alpha = alpha)
            drawLine(look.glow, center.copy(y = center.y - arm), center.copy(y = center.y + arm), starStroke, StrokeCap.Round, alpha = alpha)
        }
        if (t < 1f) for (pass in 0 until look.sweeps) {
            val start = SweepStart + pass * SweepGap
            val phase = window(t, start, start + SweepLength)
            if (phase <= 0f || phase >= 1f) continue
            val x = -band + (size.width + band * 2) * easeOutCubic(phase)
            translate(left = x) { drawRect(sweep, topLeft = Offset(-band, 0f), size = sweepSize) }
        }
    }
}
