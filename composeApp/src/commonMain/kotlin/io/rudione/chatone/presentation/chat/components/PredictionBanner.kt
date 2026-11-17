package io.rudione.chatone.presentation.chat.components


import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.chat_hide_event_banner
import chatone.composeapp.generated.resources.prediction_collapse
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
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.rudione.chatone.data.remote.dto.PredictionData
import io.rudione.chatone.icons.lucide.Lock
import io.rudione.chatone.icons.lucide.Lucide
import io.rudione.chatone.icons.lucide.Users
import io.rudione.chatone.util.icons.TwitchPointsIcon
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import kotlin.time.Clock
import androidx.compose.foundation.border

private val PredictionWinnerGreen = Color(0xFF3FB950)

@Composable
fun PredictionBanner(
    prediction: PredictionData,
    pointsBalance: Long = 0L,
    onPredict: (outcomeId: String, points: Int) -> Unit = { _, _ -> },
    onHide: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    canModerate: Boolean = false,
    actionPending: Boolean = false,
    onLock: () -> Unit = {},
    onCancel: () -> Unit = {},
    onResolve: () -> Unit = {},
    collapsed: Boolean = false,
    onToggleCollapsed: (() -> Unit)? = null
) {
    val totalPoints = prediction.outcomes.sumOf { it.channelPoints }
    val totalUsers = prediction.outcomes.sumOf { it.users }
    val hasPredicted = prediction.selfOutcomeId != null
    val compact = LocalEventBannerCompact.current
    val deadline = remember(prediction.createdAt, prediction.predictionWindow) { eventDeadline(prediction.createdAt, prediction.predictionWindow) }
    var windowExpired by remember(prediction.id, deadline) { mutableStateOf(deadline != null && deadline <= Clock.System.now().toEpochMilliseconds()) }

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
            statusLabel = if (windowExpired) stringResource(Res.string.event_prediction_locked_short)
            else if (compact) stringResource(Res.string.event_prediction_open_short) else stringResource(Res.string.event_prediction_open)
            accent = MaterialTheme.colorScheme.tertiary
            canPredict = !windowExpired && prediction.status == "ACTIVE"
        }
    }
    val summary = "${pluralStringResource(Res.plurals.event_predictors, totalUsers, totalUsers)} · ${eventPoints(totalPoints, formatPredictionPoints(totalPoints))}"

    var pendingOutcomeId by remember(prediction.id) { mutableStateOf<String?>(null) }

    val toggleModifier = if (onToggleCollapsed != null) {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClickLabel = stringResource(Res.string.prediction_collapse),
            onClick = onToggleCollapsed
        )
    } else Modifier

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(eventBannerPadding(compact))
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 1f))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
    ) {
        AnimatedContent(
            targetState = collapsed && onToggleCollapsed != null,
            transitionSpec = {
                (fadeIn(tween(220, delayMillis = 70)) + scaleIn(tween(260, delayMillis = 40), initialScale = 0.96f))
                    .togetherWith(fadeOut(tween(110)))
                    .using(SizeTransform(clip = true) { _, _ -> spring(dampingRatio = 0.86f, stiffness = 420f) })
            },
            contentAlignment = Alignment.TopStart,
            label = "predictionCollapse"
        ) { isCollapsed ->
            if (isCollapsed) {
                PredictionCompactRow(
                    prediction = prediction,
                    statusLabel = statusLabel,
                    accent = accent,
                    onExpand = { onToggleCollapsed?.invoke() }
                )
                return@AnimatedContent
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(if (compact) 8.dp else 10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = toggleModifier
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
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
                        Text(
                            statusLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = accent,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        PredictionStats(
                            users = totalUsers,
                            points = totalPoints,
                            modifier = Modifier.semantics { contentDescription = summary }
                        )
                    }
                    if (onToggleCollapsed != null) {
                        EventCollapseButton(tooltip = stringResource(Res.string.prediction_collapse), onClick = onToggleCollapsed)
                    }
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
                    overflow = TextOverflow.Ellipsis,
                    modifier = toggleModifier
                )
                prediction.outcomes.forEach { o ->
                    val fraction = if (totalPoints > 0) o.channelPoints.toFloat() / totalPoints else 0f
                    val isWinner = o.id == prediction.winningOutcomeId
                    val isMine = o.id == prediction.selfOutcomeId
                    val outcomeColor = predictionOutcomeColor(o.color)
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
                if (prediction.status == "ACTIVE" && deadline != null) {
                    EventCountdown(deadline, prediction.predictionWindow * 1000L, Modifier.fillMaxWidth().padding(vertical = 4.dp), wavy = true) {
                        windowExpired = true
                    }
                }
                if (canModerate && prediction.status in setOf("ACTIVE", "LOCKED")) {
                    PredictionModerationActions(prediction.status, !actionPending, onLock, onCancel, onResolve)
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
    }

    pendingOutcomeId?.let { outcomeId ->
        val outcome = prediction.outcomes.firstOrNull { it.id == outcomeId }
        PredictionBetDialog(
            outcomeTitle = outcome?.title.orEmpty(),
            outcomeColor = predictionOutcomeColor(outcome?.color.orEmpty()),
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
                if (compact) "$percent%" else "$percent% · $users · ${formatPredictionPoints(points)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
private fun PredictionStats(users: Int, points: Int, modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    val style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum")
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(Lucide.Users, null, tint = color, modifier = Modifier.size(11.dp))
        Spacer(Modifier.width(3.dp))
        Text(users.toString(), style = style, color = color, maxLines = 1, softWrap = false)
        Spacer(Modifier.width(7.dp))
        Icon(TwitchPointsIcon, null, tint = color, modifier = Modifier.size(11.dp))
        Spacer(Modifier.width(3.dp))
        Text(formatPredictionPoints(points), style = style, color = color, maxLines = 1, softWrap = false)
    }
}
