package io.rudione.chatone.presentation.chat.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.HowToVote
import androidx.compose.material.icons.outlined.PushPin
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.rudione.chatone.data.remote.dto.PollData
import io.rudione.chatone.presentation.components.ChatoneIconButton
import io.rudione.chatone.presentation.components.chatoneGlassPanel
import io.rudione.chatone.presentation.theme.i18n.LocalStrings
import kotlinx.coroutines.delay
import kotlin.time.Clock

private val PollAccent = Color(0xFF9146FF)
private val PollWinnerGreen = Color(0xFF3FB950)

@Composable
fun PollBanner(
    poll: PollData,
    onVote: (choiceId: String) -> Unit = {},
    onHide: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val totalVotes = poll.choices.sumOf { it.votes + it.channelPointsVotes }
    val isEnded = !poll.status.equals("ACTIVE", ignoreCase = true)
    val hasVoted = poll.selfVoteChoiceId != null
    val maxChoiceVotes = poll.choices.maxOfOrNull { it.votes + it.channelPointsVotes } ?: 0
    val winner = poll.choices.firstOrNull {
        (it.votes + it.channelPointsVotes) == maxChoiceVotes && maxChoiceVotes > 0
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 18.dp, top = 4.dp, bottom = 4.dp)
            .chatoneGlassPanel(RoundedCornerShape(12.dp))
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PollAccent.copy(alpha = 0.18f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    Icons.Outlined.HowToVote,
                    contentDescription = null,
                    tint = PollAccent,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    poll.title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (!isEnded) {
                    PollCountdownChip(poll = poll)
                } else {
                    Text(
                        "$totalVotes votes",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (onHide != null) {
                    LiquidGlassTooltipBox(tooltip = LocalStrings.current.chatHideEventBanner) {
                        ChatoneIconButton(onClick = onHide, modifier = Modifier.size(22.dp)) {
                            Icon(
                                Icons.Outlined.PushPin,
                                contentDescription = LocalStrings.current.chatHideEventBanner,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                poll.choices.forEach { choice ->
                    val votes = choice.votes + choice.channelPointsVotes
                    val pct = if (totalVotes > 0) votes.toFloat() / totalVotes else 0f
                    val isWinnerRow = isEnded && choice.id == winner?.id
                    val isMine = choice.id == poll.selfVoteChoiceId
                    PollChoiceRow(
                        title = choice.title,
                        votes = votes,
                        fraction = pct,
                        isWinner = isWinnerRow,
                        isMine = isMine,
                        clickable = !isEnded && !hasVoted,
                        onClick = { onVote(choice.id) }
                    )
                }
            }

            if (isEnded && winner != null) {
                val winPct = if (totalVotes > 0) (winner.votes + winner.channelPointsVotes) * 100 / totalVotes else 0
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(PollWinnerGreen.copy(alpha = 0.16f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        "Poll ended — “${winner.title}” won: $winPct%, $totalVotes votes",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = PollWinnerGreen
                    )
                }
            }
        }
    }
}

@Composable
private fun PollCountdownChip(poll: PollData) {
    val endsAtMs = remember(poll.id) {
        val now = Clock.System.now().toEpochMilliseconds()
        val remaining = poll.remainingMs ?: (poll.duration * 1000L)
        now + remaining
    }
    var now by remember(poll.id) { mutableStateOf(Clock.System.now().toEpochMilliseconds()) }
    LaunchedEffect(poll.id) {
        while (true) {
            now = Clock.System.now().toEpochMilliseconds()
            delay(1000)
        }
    }
    val remainingSec = ((endsAtMs - now) / 1000L).coerceAtLeast(0L)
    val m = remainingSec / 60
    val s = remainingSec % 60
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(5.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.7f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            "$m:${s.toString().padStart(2, '0')}",
            style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
            fontWeight = FontWeight.Bold,
            color = if (remainingSec <= 10) MaterialTheme.colorScheme.error else PollAccent
        )
    }
}

@Composable
private fun PollChoiceRow(
    title: String,
    votes: Int,
    fraction: Float,
    isWinner: Boolean,
    isMine: Boolean,
    clickable: Boolean,
    onClick: () -> Unit
) {
    val animFraction by animateFloatAsState(targetValue = fraction.coerceIn(0f, 1f), label = "poll-fraction")
    val fillColor by animateColorAsState(
        when {
            isWinner -> PollWinnerGreen.copy(alpha = 0.35f)
            isMine -> PollAccent.copy(alpha = 0.30f)
            else -> PollAccent.copy(alpha = 0.14f)
        },
        label = "poll-bg"
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(22.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.4f))
            .then(
                if (clickable) Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                ) else Modifier
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animFraction)
                .fillMaxHeight()
                .clip(RoundedCornerShape(6.dp))
                .background(fillColor)
        )
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isWinner || isMine) FontWeight.Bold else FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (isMine) {
                Text(
                    "✓",
                    style = MaterialTheme.typography.labelSmall,
                    color = PollAccent,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(4.dp))
            }
            Text(
                "${(animFraction * 100).toInt()}% · $votes",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
