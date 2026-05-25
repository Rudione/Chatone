package io.rudione.chatone.presentation.chat.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.HowToVote
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
import io.rudione.chatone.data.remote.dto.PollData
import io.rudione.chatone.presentation.components.LiquidGlassSurface

@Composable
fun PollBanner(
    poll: PollData,
    modifier: Modifier = Modifier
) {
    val totalVotes = poll.choices.sumOf { it.votes + it.channelPointsVotes }
    val accent = MaterialTheme.colorScheme.secondary
    val isEnded = poll.status != "ACTIVE"

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
                Icon(
                    Icons.Outlined.HowToVote,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    if (isEnded) "Poll ended" else "Poll · ${poll.duration}s",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = accent
                )
                Text(
                    "· $totalVotes votes",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.weight(1f))
            }
            Text(
                poll.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            val maxChoiceVotes = poll.choices.maxOfOrNull { it.votes + it.channelPointsVotes } ?: 0
            poll.choices.forEach { choice ->
                val votes = choice.votes + choice.channelPointsVotes
                val pct = if (totalVotes > 0) votes.toFloat() / totalVotes else 0f
                val isWinner = isEnded && votes == maxChoiceVotes && votes > 0
                ChoiceRow(
                    title = choice.title,
                    votes = votes,
                    fraction = pct,
                    accent = accent,
                    isWinner = isWinner
                )
            }
        }
    }
}

@Composable
private fun ChoiceRow(
    title: String,
    votes: Int,
    fraction: Float,
    accent: Color,
    isWinner: Boolean
) {
    val animFraction by animateFloatAsState(targetValue = fraction.coerceIn(0f, 1f), label = "poll-fraction")
    val bgColor by animateColorAsState(
        if (isWinner) accent.copy(alpha = 0.25f) else accent.copy(alpha = 0.10f),
        label = "poll-bg"
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.4f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animFraction)
                .fillMaxHeight()
                .clip(RoundedCornerShape(8.dp))
                .background(bgColor)
        )
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
                "${(animFraction * 100).toInt()}% · $votes",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
