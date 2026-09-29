package io.rudione.chatone.presentation.chat

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import kotlin.math.abs

private val CHEER_GRAY = Color(0xFF979797)
private val CHEER_PURPLE = Color(0xFF9C3EE8)
private val CHEER_GREEN = Color(0xFF1DB2A5)
private val CHEER_BLUE = Color(0xFF0099FE)
private val CHEER_RED = Color(0xFFF43021)

fun cheerTierColor(amount: Int): Color = when {
    amount >= 10000 -> CHEER_RED
    amount >= 5000 -> CHEER_BLUE
    amount >= 1000 -> CHEER_GREEN
    amount >= 100 -> CHEER_PURPLE
    else -> CHEER_GRAY
}

private fun cheerSpinMillis(amount: Int): Int = when {
    amount >= 10000 -> 1100
    amount >= 5000 -> 1400
    amount >= 1000 -> 1800
    amount >= 100 -> 2300
    else -> 3000
}

@Composable
fun CheerToken(
    amount: Int,
    fontSizeSp: Float,
    modifier: Modifier = Modifier
) {
    val tint = cheerTierColor(amount)
    val transition = rememberInfiniteTransition()
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(cheerSpinMillis(amount), easing = LinearEasing)
        )
    )

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        CheerGem(
            angle = angle,
            tint = tint,
            size = (fontSizeSp * 1.35f).dp
        )
        Text(
            text = amount.toString(),
            style = TextStyle(
                fontSize = fontSizeSp.sp,
                fontWeight = FontWeight.Bold,
                color = tint
            )
        )
    }
}

@Composable
private fun CheerGem(angle: Float, tint: Color, size: Dp) {
    val radians = angle * kotlin.math.PI.toFloat() / 180f
    val facing = kotlin.math.cos(radians)
    val edgeShade = remember(tint) { tint.copy(alpha = 0.55f) }

    Canvas(
        modifier = Modifier
            .size(size)
            .graphicsLayer {
                rotationY = angle
                cameraDistance = 12f * density
            }
    ) {
        val w = this.size.width
        val h = this.size.height
        val midY = h * 0.42f

        val body = Path().apply {
            moveTo(w * 0.5f, 0f)
            lineTo(w, midY)
            lineTo(w * 0.5f, h)
            lineTo(0f, midY)
            close()
        }

        val front = abs(facing)
        val top = Color.White.copy(alpha = 0.35f + 0.25f * front)

        drawPath(
            path = body,
            brush = Brush.linearGradient(
                colors = listOf(
                    tint.copy(alpha = 0.95f),
                    edgeShade
                ),
                start = Offset(0f, 0f),
                end = Offset(w, h)
            )
        )

        val facet = Path().apply {
            moveTo(w * 0.5f, 0f)
            lineTo(w * 0.78f, midY)
            lineTo(w * 0.5f, midY * 1.15f)
            lineTo(w * 0.22f, midY)
            close()
        }
        drawPath(path = facet, color = top)
    }
}
