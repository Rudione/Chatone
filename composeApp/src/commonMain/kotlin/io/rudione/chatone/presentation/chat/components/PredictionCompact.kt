package io.rudione.chatone.presentation.chat.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.prediction_expand
import io.rudione.chatone.data.remote.dto.PredictionData
import io.rudione.chatone.icons.lucide.ChevronDown
import io.rudione.chatone.icons.lucide.Lock
import io.rudione.chatone.icons.lucide.Lucide
import io.rudione.chatone.util.icons.TwitchPointsIcon
import org.jetbrains.compose.resources.stringResource

internal fun predictionOutcomeColor(c: String): Color = when (c.uppercase()) {
    "BLUE" -> Color(0xFF1E69FF)
    "PINK" -> Color(0xFFFF4D9D)
    else -> Color(0xFF9146FF)
}

internal fun formatPredictionPoints(p: Int): String = when {
    p >= 1_000_000 -> "${p / 100_000 / 10f}M"
    p >= 1_000 -> "${p / 100 / 10f}k"
    else -> p.toString()
}

@Composable
internal fun OutcomeSplitBar(
    prediction: PredictionData,
    modifier: Modifier = Modifier,
    height: Dp = 5.dp
) {
    val total = prediction.outcomes.sumOf { it.channelPoints }.coerceAtLeast(0)
    val track = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    val segments = remember(prediction.outcomes, total) {
        prediction.outcomes.map { o ->
            val share = if (total > 0) o.channelPoints.toFloat() / total else 1f / prediction.outcomes.size.coerceAtLeast(1)
            share to predictionOutcomeColor(o.color)
        }
    }
    val firstShare by animateFloatAsState(segments.firstOrNull()?.first ?: 0f, label = "splitShare")
    Canvas(modifier.height(height).clip(CircleShape)) {
        drawRect(track)
        if (segments.size == 2) {
            val split = size.width * firstShare
            drawRect(segments[0].second, size = Size(split, size.height))
            drawRect(segments[1].second, topLeft = Offset(split, 0f), size = Size(size.width - split, size.height))
        } else {
            var x = 0f
            segments.forEach { (share, color) ->
                val w = size.width * share
                drawRect(color, topLeft = Offset(x, 0f), size = Size(w, size.height))
                x += w
            }
        }
    }
}

@Composable
internal fun PredictionCompactRow(
    prediction: PredictionData,
    statusLabel: String,
    accent: Color,
    onExpand: () -> Unit
) {
    val total = prediction.outcomes.sumOf { it.channelPoints }
    val mine = prediction.outcomes.firstOrNull { it.id == prediction.selfOutcomeId }
    val expandLabel = stringResource(Res.string.prediction_expand)
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val showBar = maxWidth >= 250.dp
        val showPool = maxWidth >= 200.dp
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClickLabel = expandLabel, onClick = onExpand)
                .semantics { contentDescription = "$statusLabel · ${prediction.title}" }
                .padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (prediction.status == "LOCKED") {
                Icon(Lucide.Lock, null, tint = accent, modifier = Modifier.size(12.dp))
            } else {
                Box(Modifier.size(7.dp).clip(CircleShape).drawBehind { drawRect(accent) })
            }
            Text(
                prediction.title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (mine != null) {
                val mineColor = predictionOutcomeColor(mine.color)
                Text("✓", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = mineColor)
            }
            if (showBar) OutcomeSplitBar(prediction, Modifier.width(44.dp))
            if (showPool) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                Icon(TwitchPointsIcon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(11.dp))
                Text(
                    formatPredictionPoints(total),
                    style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = false
                )
            }
            Icon(Lucide.ChevronDown, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
        }
    }
}
