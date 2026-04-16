package io.rudione.chatone.presentation.loading

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.rudione.chatone.presentation.theme.ChatoneColors

@Composable
fun LoadingScreen(
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme

    val gradient = Brush.radialGradient(
        colors = listOf(
            ChatoneColors.Violet800.copy(alpha = 0.35f),
            cs.background
        ),
        center = Offset.Unspecified,
        radius = 900f
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(cs.background)
            .background(gradient),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                "Chatone",
                style = MaterialTheme.typography.titleLarge,
                color = cs.onBackground,
                fontWeight = FontWeight.SemiBold
            )
            AnimatedDots()
        }
    }
}

@Composable
private fun AnimatedDots() {
    val transition = rememberInfiniteTransition(label = "dots")
    val cs = MaterialTheme.colorScheme
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(3) { i ->
            val alpha by transition.animateFloat(
                initialValue = 0.25f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600, delayMillis = i * 180, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dot$i"
            )
            Box(
                Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(cs.primary.copy(alpha = alpha))
            )
        }
    }
}
