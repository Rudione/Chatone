package io.rudione.chatone.presentation.chat.points

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.points_claim_bonus
import chatone.composeapp.generated.resources.points_claim_bonus_amount
import io.rudione.chatone.presentation.components.ControlMotion
import io.rudione.chatone.presentation.components.LocalControlMotion
import io.rudione.chatone.presentation.components.controlClick
import io.rudione.chatone.presentation.components.motion.bump
import io.rudione.chatone.presentation.components.motion.easeOutBack
import io.rudione.chatone.presentation.components.motion.easeOutCubic
import io.rudione.chatone.presentation.components.motion.window
import io.rudione.chatone.presentation.components.motion.wobble
import io.rudione.chatone.presentation.components.tactilePress
import io.rudione.chatone.presentation.components.TactileIconButton
import io.rudione.chatone.presentation.components.motion.IconMotion
import io.rudione.chatone.presentation.components.motion.MotionIcon
import io.rudione.chatone.util.icons.TwitchPointsIcon
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

internal val ClaimGreen = Color(0xFF63D68F)
internal val ClaimInk = Color(0xFF0B1F13)
private const val IDLE_GAP_MS = 2_600L
private const val IDLE_HOP_MS = 720
private const val OPEN_MS = 520
private val ChestStroke = Stroke(width = 2f, cap = StrokeCap.Round)

@Composable
fun ClaimChestButton(
    claim: PointsClaim,
    claiming: Boolean,
    size: Dp,
    iconSize: Dp,
    onClaim: () -> Unit,
    modifier: Modifier = Modifier
) {
    val still = LocalControlMotion.current == ControlMotion.Reduced
    val source = remember { MutableInteractionSource() }
    val hop = remember { Animatable(0f) }
    val open = remember { Animatable(0f) }
    val label = claimLabel(claim)
    val click = controlClick({ if (!claiming) onClaim() })

    if (!still) {
        LaunchedEffect(claim.id, claiming) {
            if (claiming) return@LaunchedEffect
            delay(400L)
            while (true) {
                hop.snapTo(0f)
                hop.animateTo(1f, tween(IDLE_HOP_MS, easing = LinearEasing))
                hop.snapTo(0f)
                delay(IDLE_GAP_MS)
            }
        }
    }
    LaunchedEffect(claiming) {
        if (still) {
            open.snapTo(if (claiming) 1f else 0f)
        } else {
            open.animateTo(if (claiming) 1f else 0f, if (claiming) tween(OPEN_MS) else spring(Spring.DampingRatioMediumBouncy))
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .tactilePress(source, pressedScale = 0.86f)
            .clip(CircleShape)
            .drawBehind { drawRect(ClaimGreen) }
            .clickable(interactionSource = source, indication = null, role = Role.Button, onClick = click)
            .pointerHoverIcon(PointerIcon.Hand)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        Spacer(
            Modifier
                .size(iconSize)
                .drawBehind { drawChest(ClaimInk, hop.value, open.value) }
        )
    }
}

internal fun DrawScope.drawChest(ink: Color, hop: Float, open: Float) {
    val unit = size.minDimension / 24f
    val lift = -2.2f * bump(window(hop, 0f, 0.45f))
    val rock = 9f * wobble(window(hop, 0.15f, 1f), 2f, 1.4f)
    withTransform({
        scale(unit, unit, Offset.Zero)
        translate(0f, lift)
        rotate(rock, Offset(12f, 20f))
    }) {
        drawRoundRect(ink, Offset(3f, 10.5f), Size(18f, 9.5f), CornerRadius(1.8f), style = ChestStroke)
        withTransform({
            translate(0f, -3.2f * open)
            rotate(-16f * open, Offset(3f, 10.5f))
        }) {
            drawRoundRect(ink, Offset(3f, 4.5f), Size(18f, 6f), CornerRadius(2.6f), style = ChestStroke)
            drawRoundRect(ink, Offset(10.6f, 8.6f), Size(2.8f, 3.6f), CornerRadius(0.8f))
        }
        val rays = maxOf(bump(window(hop, 0.25f, 0.85f)), open)
        if (rays > 0f) {
            for (i in -1..1) {
                val angle = (-90.0 + i * 38.0) * PI / 180.0
                val from = 4.4f + 1.4f * open
                val to = from + 2.2f * rays
                val origin = Offset(12f, 6.5f - 3.2f * open)
                drawLine(
                    ink.copy(alpha = rays.coerceIn(0f, 1f)),
                    origin + Offset((cos(angle) * from).toFloat(), (sin(angle) * from).toFloat()),
                    origin + Offset((cos(angle) * to).toFloat(), (sin(angle) * to).toFloat()),
                    strokeWidth = 1.6f,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

private const val BURST_MS = 1_700
private const val BUBBLES = 8

@Composable
fun Modifier.pointsGainBurst(
    gain: PointsGain?,
    color: Color,
    textStyle: TextStyle
): Modifier {
    val still = LocalControlMotion.current == ControlMotion.Reduced
    val progress = remember { Animatable(1f) }
    val measurer = rememberTextMeasurer(cacheSize = 4)
    val layerPaint = remember { Paint() }
    val seen = remember { longArrayOf(gain?.seq ?: 0L) }
    val current by rememberUpdatedState(gain)
    val currentColor by rememberUpdatedState(color)
    val style by rememberUpdatedState(textStyle)

    LaunchedEffect(gain?.seq) {
        val seq = gain?.seq ?: return@LaunchedEffect
        if (seq <= seen[0] || still) {
            seen[0] = maxOf(seen[0], seq)
            return@LaunchedEffect
        }
        seen[0] = seq
        progress.snapTo(0f)
        progress.animateTo(1f, tween(BURST_MS, easing = LinearEasing))
    }

    return drawWithContent {
        val t = progress.value
        val g = current
        if (g == null || t >= 1f) {
            drawContent()
            return@drawWithContent
        }
        val show = easeOutCubic(window(t, 0f, 0.12f)) * (1f - window(t, 0.74f, 0.9f))
        val iconScale = 1f - 0.45f * show + 0.12f * bump(window(t, 0.86f, 1f))
        val center = Offset(size.width / 2f, size.height / 2f)
        if (show < 1f) {
            drawIntoCanvas { it.saveLayer(Rect(Offset.Zero, size), layerPaint.apply { alpha = 1f - show }) }
            withTransform({ scale(iconScale, iconScale, center) }) { this@drawWithContent.drawContent() }
            drawIntoCanvas { it.restore() }
        }
        drawBubbles(g.seq, t, currentColor)
        if (show <= 0f) return@drawWithContent
        val layout = measurer.measure("+${g.amount}", style.copy(color = currentColor))
        val pop = 0.6f + 0.4f * easeOutBack(window(t, 0f, 0.28f))
        val lift = -size.height * 0.16f * window(t, 0.6f, 0.9f)
        val topLeft = Offset(center.x - layout.size.width / 2f, center.y - layout.size.height / 2f + lift)
        withTransform({ scale(pop, pop, center) }) {
            drawText(layout, topLeft = topLeft, alpha = show.coerceIn(0f, 1f))
        }
    }
}

private fun DrawScope.drawBubbles(seed: Long, t: Float, color: Color) {
    val random = Random(seed)
    val base = size.minDimension
    val center = Offset(size.width / 2f, size.height / 2f)
    repeat(BUBBLES) { i ->
        val delayFrac = random.nextFloat() * 0.25f
        val life = window(t, delayFrac, delayFrac + 0.6f + random.nextFloat() * 0.15f)
        if (life <= 0f || life >= 1f) return@repeat
        val drift = (random.nextFloat() - 0.5f) * base * 0.9f
        val sway = sin((life * 6f + i).toDouble()).toFloat() * base * 0.06f
        val climb = base * (0.55f + random.nextFloat() * 0.6f)
        val radius = base * (0.065f + random.nextFloat() * 0.075f) * (0.6f + 0.4f * easeOutCubic(life))
        val pos = center + Offset(drift * easeOutCubic(life) + sway, -climb * easeOutCubic(life))
        val alpha = (1f - life) * window(life, 0f, 0.15f)
        if (i % 3 == 0) {
            drawCircle(color, radius, pos, alpha = alpha * 0.9f)
        } else {
            drawCircle(color, radius, pos, alpha = alpha, style = Stroke(width = radius * 0.45f))
        }
    }
}

@Composable
fun PointsButton(
    claim: PointsClaim?,
    claiming: Boolean,
    gainSeq: Int,
    size: Dp,
    iconSize: Dp,
    contentDescription: String,
    onOpen: () -> Unit,
    onClaim: () -> Unit
) {
    AnimatedContent(
        targetState = claim,
        contentKey = { it != null },
        transitionSpec = {
            (scaleIn(spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium), 0.4f) + fadeIn(tween(140)))
                .togetherWith(scaleOut(tween(200), 0.4f) + fadeOut(tween(160)))
                .using(SizeTransform(clip = false))
        },
        contentAlignment = Alignment.Center,
        label = "pointsButton"
    ) { shown ->
        if (shown != null) {
            ClaimChestButton(
                claim = shown,
                claiming = claiming || claim == null,
                size = size,
                iconSize = iconSize,
                onClaim = onClaim
            )
        } else {
            TactileIconButton(onClick = onOpen, size = size) { tint ->
                MotionIcon(
                    imageVector = TwitchPointsIcon,
                    motion = IconMotion.Points,
                    contentDescription = contentDescription,
                    tint = tint,
                    pulse = gainSeq,
                    modifier = Modifier.size(iconSize)
                )
            }
        }
    }
}

@Composable
fun claimLabel(claim: PointsClaim): String = if (claim.points > 0) {
    stringResource(Res.string.points_claim_bonus_amount, claim.points)
} else {
    stringResource(Res.string.points_claim_bonus)
}
