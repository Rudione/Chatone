package io.rudione.chatone.presentation.chat.components


import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.chat_hide_event_banner
import chatone.composeapp.generated.resources.event_poll_votes
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.pluralStringResource
import io.rudione.chatone.presentation.chat.components.pollWonText
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import io.rudione.chatone.presentation.components.layoutWidthFraction
import androidx.compose.ui.draw.drawBehind
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.rudione.chatone.data.remote.dto.PollData
import kotlin.time.Clock
import io.rudione.chatone.icons.lucide.Lucide
import io.rudione.chatone.icons.lucide.Vote

private val PollAccent = Color(0xFF9146FF)
private val PollWinnerGreen = Color(0xFF3FB950)

@Composable
fun PollBanner(
    poll: PollData,
    onVote: (choiceId: String) -> Unit = {},
    onHide: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val observedAt = remember(poll.id, poll.remainingMs, poll.receivedAtMs) {
        poll.receivedAtMs ?: Clock.System.now().toEpochMilliseconds()
    }
    val deadline = pollDeadline(poll, observedAt)
    var windowExpired by remember(poll.id, deadline) { mutableStateOf(deadline != null && deadline <= Clock.System.now().toEpochMilliseconds()) }
    val totalVotes = poll.choices.sumOf { it.votes + it.channelPointsVotes }
    val isEnded = !poll.status.equals("ACTIVE", ignoreCase = true)
    val hasVoted = poll.selfVoteChoiceId != null
    val maxChoiceVotes = poll.choices.maxOfOrNull { it.votes + it.channelPointsVotes } ?: 0
    val winner = poll.choices.firstOrNull {
        (it.votes + it.channelPointsVotes) == maxChoiceVotes && maxChoiceVotes > 0
    }
    val compact = LocalEventBannerCompact.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(eventBannerPadding(compact))
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 1f))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
    ) {
        Column {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PollAccent.copy(alpha = 0.18f))
                    .padding(horizontal = if (compact) 8.dp else 10.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Lucide.Vote,
                        contentDescription = null,
                        tint = PollAccent,
                        modifier = Modifier.size(13.dp)
                    )
                    if (compact) {
                        Spacer(Modifier.weight(1f))
                    } else {
                        PollTitle(poll.title, maxLines = 1, modifier = Modifier.weight(1f))
                    }
                    if (isEnded) {
                        Text(
                            pluralStringResource(Res.plurals.event_poll_votes, totalVotes, totalVotes),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                    if (onHide != null) {
                        EventHideButton(tooltip = stringResource(Res.string.chat_hide_event_banner), onHide = onHide)
                    }
                }
                if (compact) PollTitle(poll.title, maxLines = 2)
            }

            Column(
                modifier = Modifier.padding(horizontal = if (compact) 6.dp else 8.dp, vertical = 6.dp),
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
                        clickable = !isEnded && !windowExpired && !hasVoted,
                        compact = compact,
                        onClick = { onVote(choice.id) }
                    )
                }
            }

            if (!isEnded && deadline != null) {
                EventCountdown(deadline, poll.duration * 1000L, Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp), wavy = true) {
                    windowExpired = true
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
                        pollWonText(winner.title, winPct, totalVotes),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = PollWinnerGreen,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun PollTitle(title: String, maxLines: Int, modifier: Modifier = Modifier) {
    Text(
        title,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

@Composable
private fun PollChoiceRow(
    title: String,
    votes: Int,
    fraction: Float,
    isWinner: Boolean,
    isMine: Boolean,
    clickable: Boolean,
    compact: Boolean,
    onClick: () -> Unit
) {
    val animFraction by animateFloatAsState(targetValue = fraction.coerceIn(0f, 1f), label = "poll-fraction")
    val percent by remember { derivedStateOf { (animFraction * 100).toInt() } }
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
                .layoutWidthFraction { animFraction }
                .fillMaxHeight()
                .clip(RoundedCornerShape(6.dp))
                .drawBehind { drawRect(fillColor) }
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
            Spacer(Modifier.width(6.dp))
            Text(
                if (compact) "$percent%" else "$percent% · $votes",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}
