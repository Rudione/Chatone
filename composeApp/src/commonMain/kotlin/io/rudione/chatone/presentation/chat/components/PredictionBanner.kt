package io.rudione.chatone.presentation.chat.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.rudione.chatone.data.remote.dto.PredictionData
import io.rudione.chatone.presentation.components.LiquidGlassSurface

@Composable
fun PredictionBanner(
    prediction: PredictionData,
    modifier: Modifier = Modifier
) {
    val totalPoints = prediction.outcomes.sumOf { it.channelPoints }
    val totalUsers = prediction.outcomes.sumOf { it.users }

    val statusLabel: String
    val accent: Color
    when (prediction.status) {
        "LOCKED" -> { statusLabel = "Locked"; accent = MaterialTheme.colorScheme.error }
        "RESOLVED" -> { statusLabel = "Resolved"; accent = MaterialTheme.colorScheme.primary }
        "CANCELED" -> { statusLabel = "Canceled"; accent = MaterialTheme.colorScheme.outline }
        else -> { statusLabel = "${prediction.predictionWindow}s window"; accent = MaterialTheme.colorScheme.tertiary }
    }

    LiquidGlassSurface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(10.dp),
        backgroundAlphaHigh = 0.55f,
        backgroundAlphaLow = 0.4f,
        borderAlphaHigh = 0.3f,
        borderAlphaLow = 0.15f
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (prediction.status == "LOCKED") {
                    Icon(
                        Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Text(
                    "Prediction · $statusLabel",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = accent
                )
                Text(
                    "· $totalUsers betters · ${formatPoints(totalPoints)} pts",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.weight(1f))
            }
            Text(
                prediction.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            prediction.outcomes.forEach { o ->
                val fraction = if (totalPoints > 0) o.channelPoints.toFloat() / totalPoints else 0f
                val isWinner = o.id == prediction.winningOutcomeId
                val outcomeColor = parseTwitchOutcomeColor(o.color)
                OutcomeRow(
                    title = o.title,
                    users = o.users,
                    points = o.channelPoints,
                    fraction = fraction,
                    accent = outcomeColor,
                    isWinner = isWinner
                )
            }
        }
    }
}

@Composable
private fun OutcomeRow(
    title: String,
    users: Int,
    points: Int,
    fraction: Float,
    accent: Color,
    isWinner: Boolean
) {
    val anim by animateFloatAsState(targetValue = fraction.coerceIn(0f, 1f), label = "pred-fraction")
    val bg by animateColorAsState(
        if (isWinner) accent.copy(alpha = 0.35f) else accent.copy(alpha = 0.12f),
        label = "pred-bg"
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(30.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.35f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(anim)
                .fillMaxHeight()
                .clip(RoundedCornerShape(8.dp))
                .background(bg)
        )
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(accent)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                title,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (isWinner) FontWeight.Bold else FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                "${(anim * 100).toInt()}% · $users · ${formatPoints(points)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun parseTwitchOutcomeColor(c: String): Color = when (c.uppercase()) {
    "BLUE" -> Color(0xFF1E69FF)
    "PINK" -> Color(0xFFFF4D9D)
    else -> Color(0xFF9146FF)
}

private fun formatPoints(p: Int): String = when {
    p >= 1_000_000 -> "${p / 100_000 / 10f}M"
    p >= 1_000 -> "${p / 100 / 10f}k"
    else -> p.toString()
}
