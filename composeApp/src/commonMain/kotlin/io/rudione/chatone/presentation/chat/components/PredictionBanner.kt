package io.rudione.chatone.presentation.chat.components

import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.chat_hide_event_banner
import chatone.composeapp.generated.resources.event_label_prediction
import chatone.composeapp.generated.resources.event_prediction_canceled
import chatone.composeapp.generated.resources.event_prediction_locked
import chatone.composeapp.generated.resources.event_prediction_locked_short
import chatone.composeapp.generated.resources.event_prediction_open
import chatone.composeapp.generated.resources.event_prediction_open_short
import chatone.composeapp.generated.resources.event_prediction_resolved
import chatone.composeapp.generated.resources.event_prediction_won
import chatone.composeapp.generated.resources.event_predictors
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.pluralStringResource
import io.rudione.chatone.presentation.chat.components.eventPoints
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import io.rudione.chatone.presentation.components.layoutWidthFraction
import androidx.compose.ui.draw.drawBehind
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.rudione.chatone.data.remote.dto.PredictionData
import io.rudione.chatone.presentation.components.chatoneGlassPanel
import io.rudione.chatone.icons.lucide.Lock
import io.rudione.chatone.icons.lucide.Lucide

private val PredictionWinnerGreen = Color(0xFF3FB950)

@Composable
fun PredictionBanner(
    prediction: PredictionData,
    pointsBalance: Long = 0L,
    onPredict: (outcomeId: String, points: Int) -> Unit = { _, _ -> },
    onHide: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val totalPoints = prediction.outcomes.sumOf { it.channelPoints }
    val totalUsers = prediction.outcomes.sumOf { it.users }
    val hasPredicted = prediction.selfOutcomeId != null
    val compact = LocalEventBannerCompact.current

    val statusLabel: String
    val accent: Color
    val canPredict: Boolean
    when (prediction.status) {
        "LOCKED" -> {
            statusLabel = if (compact) stringResource(Res.string.event_prediction_locked_short) else stringResource(Res.string.event_prediction_locked); accent = MaterialTheme.colorScheme.error; canPredict = false
        }

        "RESOLVED" -> {
            statusLabel = stringResource(Res.string.event_prediction_resolved); accent = MaterialTheme.colorScheme.primary; canPredict = false
        }

        "CANCELED" -> {
            statusLabel = stringResource(Res.string.event_prediction_canceled); accent = MaterialTheme.colorScheme.outline; canPredict = false
        }

        else -> {
            statusLabel = if (compact) stringResource(Res.string.event_prediction_open_short) else stringResource(Res.string.event_prediction_open)
            accent = MaterialTheme.colorScheme.tertiary
            canPredict = true
        }
    }
    val summary = "${pluralStringResource(Res.plurals.event_predictors, totalUsers, totalUsers)} · ${eventPoints(totalPoints, formatPoints(totalPoints))}"

    var pendingOutcomeId by remember(prediction.id) { mutableStateOf<String?>(null) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(eventBannerPadding(compact))
            .chatoneGlassPanel(RoundedCornerShape(12.dp))
            .padding(if (compact) 8.dp else 10.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (prediction.status == "LOCKED") {
                    Icon(
                        Lucide.Lock,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(13.dp)
                    )
                }
                val summaryColor = MaterialTheme.colorScheme.onSurfaceVariant
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(color = accent, fontWeight = FontWeight.SemiBold)) {
                            append(if (compact) statusLabel else "${stringResource(Res.string.event_label_prediction)} · $statusLabel")
                        }
                        if (!compact) withStyle(SpanStyle(color = summaryColor)) { append(" · $summary") }
                    },
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (onHide != null) {
                    EventHideButton(tooltip = stringResource(Res.string.chat_hide_event_banner), onHide = onHide)
                }
            }
            Text(
                prediction.title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (compact) {
                Text(
                    summary,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            prediction.outcomes.forEach { o ->
                val fraction = if (totalPoints > 0) o.channelPoints.toFloat() / totalPoints else 0f
                val isWinner = o.id == prediction.winningOutcomeId
                val isMine = o.id == prediction.selfOutcomeId
                val outcomeColor = parseTwitchOutcomeColor(o.color)
                OutcomeRow(
                    title = o.title,
                    users = o.users,
                    points = o.channelPoints,
                    fraction = fraction,
                    accent = outcomeColor,
                    isWinner = isWinner,
                    isMine = isMine,
                    clickable = canPredict && !hasPredicted,
                    compact = compact,
                    onClick = { pendingOutcomeId = o.id }
                )
            }
            if (prediction.status == "RESOLVED" && prediction.winningOutcomeId != null) {
                val winner = prediction.outcomes.firstOrNull { it.id == prediction.winningOutcomeId }
                if (winner != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(PredictionWinnerGreen.copy(alpha = 0.16f))
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                    ) {
                        Text(
                            stringResource(Res.string.event_prediction_won, winner.title),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = PredictionWinnerGreen,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }

    pendingOutcomeId?.let { outcomeId ->
        val outcome = prediction.outcomes.firstOrNull { it.id == outcomeId }
        PredictionBetDialog(
            outcomeTitle = outcome?.title.orEmpty(),
            outcomeColor = parseTwitchOutcomeColor(outcome?.color.orEmpty()),
            pointsBalance = pointsBalance,
            outcomePoints = (outcome?.channelPoints ?: 0).toLong(),
            totalPoints = totalPoints.toLong(),
            onConfirm = { points ->
                onPredict(outcomeId, points)
                pendingOutcomeId = null
            },
            onDismiss = { pendingOutcomeId = null }
        )
    }
}

@Composable
private fun OutcomeRow(
    title: String,
    users: Int,
    points: Int,
    fraction: Float,
    accent: Color,
    isWinner: Boolean,
    isMine: Boolean,
    clickable: Boolean,
    compact: Boolean,
    onClick: () -> Unit
) {
    val anim by animateFloatAsState(targetValue = fraction.coerceIn(0f, 1f), label = "pred-fraction")
    val percent by remember { derivedStateOf { (anim * 100).toInt() } }
    val bg by animateColorAsState(
        when {
            isWinner -> PredictionWinnerGreen.copy(alpha = 0.35f)
            isMine -> accent.copy(alpha = 0.30f)
            else -> accent.copy(alpha = 0.12f)
        },
        label = "pred-bg"
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(22.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.35f))
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
                .layoutWidthFraction { anim }
                .fillMaxHeight()
                .clip(RoundedCornerShape(6.dp))
                .drawBehind { drawRect(bg) }
        )
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(accent)
            )
            Spacer(Modifier.width(6.dp))
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
                Text("✓", style = MaterialTheme.typography.labelSmall, color = accent, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(4.dp))
            }
            Spacer(Modifier.width(6.dp))
            Text(
                if (compact) "$percent%" else "$percent% · $users · ${formatPoints(points)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                softWrap = false
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
