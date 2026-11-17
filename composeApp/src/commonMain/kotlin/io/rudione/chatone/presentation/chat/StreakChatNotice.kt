package io.rudione.chatone.presentation.chat

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.chat_notice_anonymous
import chatone.composeapp.generated.resources.chat_notice_streak_line
import io.rudione.chatone.domain.model.ChatNoticeUser
import io.rudione.chatone.presentation.points.FlameCore
import io.rudione.chatone.presentation.points.FlameEdge
import io.rudione.chatone.presentation.points.FlameHot
import io.rudione.chatone.presentation.points.StreakFlame
import io.rudione.chatone.util.format.groupedNumber
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

internal fun streakLevel(streak: Int): Int = when {
    streak < 5 -> 0
    streak < 10 -> 1
    streak < 25 -> 2
    streak < 50 -> 3
    streak < 100 -> 4
    else -> 5
}

internal const val NoticeNumberSlot = "\uE000"

@Composable
internal fun StreakChatNotice(
    notice: CompactNoticeData,
    streak: Int,
    fontSize: Float,
    onOpenUser: (ChatNoticeUser) -> Unit,
    footer: (@Composable () -> Unit)?
) {
    val level = streakLevel(streak)
    val scheme = MaterialTheme.colorScheme
    val accent = lerp(scheme.secondary, FlameCore, level * 0.17f)
    val numberGlow = lerp(FlameHot, FlameEdge, level / 5f)
    val line = pluralStringResource(Res.plurals.chat_notice_streak_line, streak, NoticeNumberSlot)
    val before = line.substringBefore(NoticeNumberSlot).trim()
    val after = line.substringAfter(NoticeNumberSlot, "").trim()
    val entrance = rememberNoticeEntrance(notice, level > 0)
    val iconSize = (fontSize * 1.18f).dp
    NoticeBar(
        accent = accent,
        leading = { StreakFlame(active = true, size = iconSize, animated = false) },
        decoration = Modifier.drawWithCache {
            val wash = if (level > 0) Brush.horizontalGradient(
                listOf(FlameCore.copy(alpha = 0.035f * level), Color.Transparent),
                endX = size.width * (0.3f + 0.1f * level)
            ) else null
            val ember = 1.2.dp.toPx()
            val emberGap = 5.dp.toPx()
            val emberRight = 6.dp.toPx()
            val emberHigh = 4.dp.toPx()
            val emberLow = 6.5.dp.toPx()
            val sweepColors = listOf(Color.Transparent, FlameHot.copy(alpha = 0.2f), Color.Transparent)
            onDrawWithContent {
                wash?.let { drawRect(it) }
                drawContent()
                if (level >= 3) repeat(level - 1) { index ->
                    drawCircle(
                        FlameHot.copy(alpha = 0.55f - index * 0.08f),
                        radius = ember,
                        center = Offset(size.width - emberRight - index * emberGap, if (index % 2 == 0) emberHigh else emberLow)
                    )
                }
                val progress = entrance?.value ?: 1f
                if (progress < 1f) {
                    val x = size.width * progress
                    drawRect(Brush.horizontalGradient(sweepColors, x - size.width / 3, x + size.width / 3))
                }
            }
        },
        header = Modifier.drawWithCache {
            val center = Offset(iconSize.toPx() / 2, size.height / 2)
            val radius = (16 + 9 * level).dp.toPx()
            val settled = 0.12f + 0.06f * level
            val peak = (settled * 1.6f).coerceAtMost(0.75f)
            val glow = Brush.radialGradient(listOf(FlameCore.copy(alpha = peak), Color.Transparent), center, radius)
            onDrawBehind {
                val progress = entrance?.value ?: 1f
                drawCircle(glow, radius, center, alpha = (settled + (peak - settled) * (1f - progress)) / peak)
            }
        },
        footer = footer
    ) {
        StreakSentenceRow(Modifier.weight(1f)) {
            val author = notice.author
            if (author == null) {
                Text(stringResource(Res.string.chat_notice_anonymous), fontSize = fontSize.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            } else {
                ChatNickname(author, fontSizeSp = fontSize + 1f, onClick = { onOpenUser(author) })
            }
            Text(before, fontSize = fontSize.sp, color = scheme.onSurfaceVariant, maxLines = 1)
            GlowCount(groupedNumber(streak.toLong()), glow = numberGlow, strength = 0.3f + 0.14f * level, fontSizeSp = fontSize)
            Text(after, fontSize = fontSize.sp, color = scheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun StreakSentenceRow(modifier: Modifier, content: @Composable () -> Unit) {
    Layout(content, modifier) { measurables, constraints ->
        val (nick, before, count, after) = measurables
        val gap = 3.dp.roundToPx()
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val beforeP = before.measure(loose)
        val countP = count.measure(loose)
        val lineHeight = maxOf(beforeP.height, countP.height)
        val nickWant = nick.maxIntrinsicWidth(lineHeight)
        val afterWant = after.maxIntrinsicWidth(lineHeight)
        val gaps = gap * (1 + (if (beforeP.width > 0) 1 else 0) + (if (afterWant > 0) 1 else 0))
        val rest = if (constraints.hasBoundedWidth) {
            (constraints.maxWidth - beforeP.width - countP.width - gaps).coerceAtLeast(0)
        } else nickWant + afterWant
        val nickWidth = if (nickWant + afterWant <= rest) nickWant
        else minOf(nickWant, maxOf((rest * 0.45f).toInt(), rest - afterWant))
        val nickP = nick.measure(loose.copy(maxWidth = nickWidth))
        val afterP = after.measure(loose.copy(maxWidth = (rest - nickP.width).coerceAtLeast(0)))
        val parts = listOf(nickP, beforeP, countP, afterP)
        val height = parts.maxOf { it.height }
        val used = parts.sumOf { it.width } + gaps
        val width = if (constraints.hasBoundedWidth) used.coerceIn(constraints.minWidth, constraints.maxWidth) else used
        layout(width, height) {
            var x = 0
            parts.forEach { part ->
                if (part.width > 0) {
                    part.placeRelative(x, (height - part.height) / 2)
                    x += part.width + gap
                }
            }
        }
    }
}
