package io.rudione.chatone.presentation.chat

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import io.rudione.chatone.domain.model.ChatSubTier
import io.rudione.chatone.icons.lucide.Gift
import io.rudione.chatone.icons.lucide.Lucide
import io.rudione.chatone.presentation.components.motion.GlyphCache
import io.rudione.chatone.presentation.components.motion.bump
import io.rudione.chatone.presentation.components.motion.easeOutCubic
import io.rudione.chatone.presentation.components.motion.window
import io.rudione.chatone.presentation.components.motion.wobble
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import androidx.compose.ui.graphics.shadow.Shadow as DropShadow

internal const val SubArrivalMillis = 2_800

internal const val SubPulseEnd = 0.42f
internal const val SubLidStart = 0.4f
internal const val SubLidEnd = 0.86f

internal class SubTierLook(
    val glow: Color,
    boxTop: Color,
    val boxBottom: Color,
    val ribbon: Color,
    washStart: Color,
    washEnd: Color,
    val rim: Color?,
    val chipInk: Color,
    chipStart: Color,
    chipEnd: Color,
    val sparkles: Int,
    val sweep: Float,
    val sweeps: Int,
    val numberGlow: Float,
    val haloRest: Float,
    val outerGlow: DropShadow?
) {
    val body: Brush = Brush.verticalGradient(listOf(boxTop, boxBottom), startY = 12f, endY = 21f)
    val lid: Brush = Brush.verticalGradient(listOf(lerp(boxTop, Color.White, 0.35f), boxTop), startY = 8f, endY = 12f)
    val outline: Color = lerp(boxTop, Color.White, 0.55f)
    val halo: Brush = Brush.radialGradient(listOf(glow, glow.copy(alpha = 0f)), center = GiftCenter, radius = HaloRadius)
    val chip: Brush = Brush.horizontalGradient(listOf(chipStart, chipEnd))
    val wash: Brush = Brush.horizontalGradient(listOf(washStart, washEnd))
}

private val GiftCenter = Offset(12f, 13f)
private const val HaloRadius = 13f
private const val HaloPeak = 0.7f
private val BodyPivot = Offset(12f, 18f)
private val LidPivot = Offset(12f, 10f)
private val GiftStroke = Stroke(width = 1.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
private val BowStroke = Stroke(width = 1.7f, cap = StrokeCap.Round, join = StrokeJoin.Round)
private val GiftLayers by lazy { GlyphCache.layers(Lucide.Gift) }

private val PrimeLook = SubTierLook(
    glow = Color(0xFF5BB4FF),
    boxTop = Color(0xFF8CCBFF),
    boxBottom = Color(0xFF2F6FE0),
    ribbon = Color(0xFFF2F8FF),
    washStart = Color(0xFF3D8BFF).copy(alpha = 0.13f),
    washEnd = Color(0xFF3D8BFF).copy(alpha = 0.03f),
    rim = null,
    chipInk = Color(0xFF8CCBFF),
    chipStart = Color(0xFF5BB4FF).copy(alpha = 0.2f),
    chipEnd = Color(0xFF5BB4FF).copy(alpha = 0.12f),
    sparkles = 3,
    sweep = 0.16f,
    sweeps = 1,
    numberGlow = 0.35f,
    haloRest = 0.2f,
    outerGlow = null
)

private val Tier1Look = SubTierLook(
    glow = Color(0xFFC4ADFF),
    boxTop = Color(0xFFDCCFFF),
    boxBottom = Color(0xFF9A7BFF),
    ribbon = Color.White,
    washStart = Color(0xFFB69CFF).copy(alpha = 0.13f),
    washEnd = Color(0xFFB69CFF).copy(alpha = 0.03f),
    rim = null,
    chipInk = Color(0xFFD2C1FF),
    chipStart = Color(0xFFC4ADFF).copy(alpha = 0.2f),
    chipEnd = Color(0xFFC4ADFF).copy(alpha = 0.12f),
    sparkles = 3,
    sweep = 0.16f,
    sweeps = 1,
    numberGlow = 0.35f,
    haloRest = 0.2f,
    outerGlow = null
)

private val Tier2Look = SubTierLook(
    glow = Color(0xFFA463FF),
    boxTop = Color(0xFFBE93FF),
    boxBottom = Color(0xFF5E1FD0),
    ribbon = Color(0xFFFFC2EC),
    washStart = Color(0xFF5B1FC2).copy(alpha = 0.32f),
    washEnd = Color(0xFF8F4DFF).copy(alpha = 0.07f),
    rim = Color(0xFFB57BFF).copy(alpha = 0.4f),
    chipInk = Color(0xFFD9BCFF),
    chipStart = Color(0xFF8F4DFF).copy(alpha = 0.34f),
    chipEnd = Color(0xFFFF7AD9).copy(alpha = 0.18f),
    sparkles = 5,
    sweep = 0.22f,
    sweeps = 1,
    numberGlow = 0.55f,
    haloRest = 0.28f,
    outerGlow = null
)

private val Tier3Look = SubTierLook(
    glow = Color(0xFFFFC857),
    boxTop = Color(0xFFA56BFF),
    boxBottom = Color(0xFF3A0F8F),
    ribbon = Color(0xFFFFD36E),
    washStart = Color(0xFF1E0750).copy(alpha = 0.62f),
    washEnd = Color(0xFF4A17A8).copy(alpha = 0.16f),
    rim = Color(0xFFFFC857).copy(alpha = 0.6f),
    chipInk = Color(0xFFFFD36E),
    chipStart = Color(0xFFFFC857).copy(alpha = 0.3f),
    chipEnd = Color(0xFFFF8A3D).copy(alpha = 0.14f),
    sparkles = 7,
    sweep = 0.3f,
    sweeps = 2,
    numberGlow = 0.8f,
    haloRest = 0.36f,
    outerGlow = DropShadow(radius = 9.dp, color = Color(0xFF8A3DFF), alpha = 0.42f)
)

internal fun subTierLook(tier: ChatSubTier): SubTierLook = when (tier) {
    ChatSubTier.Prime -> PrimeLook
    ChatSubTier.Tier1 -> Tier1Look
    ChatSubTier.Tier2 -> Tier2Look
    ChatSubTier.Tier3 -> Tier3Look
}

internal fun subPulse(arrival: Float): Float {
    if (arrival <= 0f || arrival >= SubPulseEnd) return 0f
    val beats = arrival / SubPulseEnd * 3f
    val index = floor(beats)
    return bump(beats - index) * (0.75f + 0.125f * index)
}

internal fun subLidOpen(arrival: Float): Float = bump(window(arrival, SubLidStart, SubLidEnd))

internal fun DrawScope.drawSubGift(look: SubTierLook, arrival: Float) {
    val glyph = GiftLayers
    val lidPath = glyph[0]?.path ?: return
    val bodyPath = glyph[2]?.path ?: return
    val bowPath = glyph[3]?.path ?: return
    val edge = minOf(size.width, size.height)
    val unit = edge / glyph.viewportWidth
    val pulse = subPulse(arrival)
    val open = subLidOpen(arrival)
    val lift = 2.4f * open
    val spin = 18f * wobble(window(arrival, SubLidStart, 0.94f), 2f, 1.1f)
    val tilt = -6f * wobble(window(arrival, SubLidStart, 1f), 1.5f, 1.6f)
    val pop = 0.07f * bump(window(arrival, SubLidStart, SubLidStart + 0.14f))
    val grow = 1f + 0.16f * pulse + pop
    val shine = maxOf(pulse, open * 0.9f)
    val halo = look.haloRest + (HaloPeak - look.haloRest) * shine
    withTransform({
        translate((size.width - edge) / 2f, (size.height - edge) / 2f)
        scale(unit, unit, Offset.Zero)
    }) {
        val haloScale = 1f + 0.3f * shine
        withTransform({ scale(haloScale, haloScale, GiftCenter) }) {
            drawCircle(look.halo, HaloRadius, GiftCenter, alpha = halo)
        }
        withTransform({
            rotate(tilt, BodyPivot)
            scale(grow, grow, BodyPivot)
        }) {
            drawPath(bodyPath, look.body)
            drawPath(bodyPath, look.outline, style = GiftStroke)
            drawLine(look.ribbon, Offset(12f, 12.5f), Offset(12f, 20.5f), strokeWidth = 1.8f, cap = StrokeCap.Round)
            if (open > 0f) drawSparkles(look, arrival, lift)
            withTransform({
                translate(0f, -lift)
                rotate(spin, LidPivot)
            }) {
                drawPath(lidPath, look.lid)
                drawPath(lidPath, look.outline, style = GiftStroke)
                drawLine(look.ribbon, Offset(12f, 8.5f), Offset(12f, 11.5f), strokeWidth = 1.8f, cap = StrokeCap.Round)
                drawPath(bowPath, look.ribbon, style = BowStroke)
            }
        }
    }
}

private fun DrawScope.drawSparkles(look: SubTierLook, arrival: Float, lift: Float) {
    val count = look.sparkles
    val originY = 11f - lift * 0.5f
    for (i in 0 until count) {
        val start = SubLidStart + 0.03f + i * 0.028f
        val phase = window(arrival, start, start + 0.36f)
        if (phase <= 0f || phase >= 1f) continue
        val spread = if (count == 1) 0.5f else i / (count - 1f)
        val angle = (-150.0 + 120.0 * spread) * PI / 180.0
        val reach = 3f + (7f + (i % 3) * 1.6f) * easeOutCubic(phase)
        val center = Offset(12f + (cos(angle) * reach).toFloat(), originY + (sin(angle) * reach).toFloat())
        val alpha = bump(phase)
        val color = if (i % 2 == 0) look.glow else look.ribbon
        val arm = 0.9f + 0.7f * alpha
        drawLine(color, center.copy(x = center.x - arm), center.copy(x = center.x + arm), 0.7f, StrokeCap.Round, alpha = alpha)
        drawLine(color, center.copy(y = center.y - arm), center.copy(y = center.y + arm), 0.7f, StrokeCap.Round, alpha = alpha)
    }
}
