package io.rudione.chatone.presentation.chat.points

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.actions_auto_claim
import chatone.composeapp.generated.resources.actions_auto_claim_hint
import chatone.composeapp.generated.resources.event_prediction_canceled
import chatone.composeapp.generated.resources.event_prediction_locked
import chatone.composeapp.generated.resources.event_prediction_open
import chatone.composeapp.generated.resources.event_prediction_resolved
import chatone.composeapp.generated.resources.points_prediction_pool
import chatone.composeapp.generated.resources.points_prediction_show
import io.rudione.chatone.data.remote.dto.PredictionData
import io.rudione.chatone.icons.lucide.ChartNoAxesColumn
import io.rudione.chatone.icons.lucide.Lucide
import io.rudione.chatone.presentation.chat.components.OutcomeSplitBar
import io.rudione.chatone.presentation.chat.components.eventDeadline
import io.rudione.chatone.presentation.components.ChatoneSwitch
import io.rudione.chatone.presentation.components.expressive.HelpHint
import io.rudione.chatone.util.format.groupedNumber
import io.rudione.chatone.util.icons.TwitchPointsIcon
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Clock

private val PredictionBlue = Color(0xFF387AFF)
private val CardShape = RoundedCornerShape(12.dp)

@Immutable
data class PointsSheetExtras(
    val hiddenPrediction: PredictionData? = null,
    val onShowPrediction: () -> Unit = {},
    val autoClaim: Boolean = false,
    val onAutoClaimChange: (Boolean) -> Unit = {}
)

@Composable
internal fun PointsSheetExtrasBlock(extras: PointsSheetExtras, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        extras.hiddenPrediction?.let { HiddenPredictionCard(it, extras.onShowPrediction) }
        AutoClaimRow(extras.autoClaim, extras.onAutoClaimChange)
    }
}

@Composable
private fun HiddenPredictionCard(prediction: PredictionData, onShow: () -> Unit) {
    val total = prediction.outcomes.sumOf { it.channelPoints }.toLong()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .clickable(role = Role.Button, onClickLabel = stringResource(Res.string.points_prediction_show), onClick = onShow)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            Modifier.size(36.dp).clip(RoundedCornerShape(9.dp)).background(PredictionBlue),
            contentAlignment = Alignment.Center
        ) {
            Icon(Lucide.ChartNoAxesColumn, null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                prediction.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                predictionStatusText(prediction),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(TwitchPointsIcon, null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(13.dp))
                Text(
                    groupedNumber(total),
                    style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                stringResource(Res.string.points_prediction_pool),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutcomeSplitBar(prediction, Modifier.width(64.dp), height = 4.dp)
        }
    }
}

@Composable
private fun predictionStatusText(prediction: PredictionData): String {
    val deadline = eventDeadline(prediction.createdAt, prediction.predictionWindow)
    val expired = deadline != null && deadline <= Clock.System.now().toEpochMilliseconds()
    return when {
        prediction.status == "RESOLVED" -> stringResource(Res.string.event_prediction_resolved)
        prediction.status == "CANCELED" -> stringResource(Res.string.event_prediction_canceled)
        prediction.status == "LOCKED" || expired -> stringResource(Res.string.event_prediction_locked)
        else -> stringResource(Res.string.event_prediction_open)
    }
}

@Composable
private fun AutoClaimRow(checked: Boolean, onChange: (Boolean) -> Unit) {
    val title = stringResource(Res.string.actions_auto_claim)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .clickable(role = Role.Switch) { onChange(!checked) }
            .padding(start = 10.dp, end = 10.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(28.dp).clip(CircleShape).background(ClaimGreen),
            contentAlignment = Alignment.Center
        ) {
            Spacer(Modifier.size(16.dp).drawBehind { drawChest(ClaimInk, hop = 0f, open = 0f) })
        }
        Spacer(Modifier.width(10.dp))
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                modifier = Modifier.weight(1f, fill = false)
            )
            HelpHint(title = title, text = stringResource(Res.string.actions_auto_claim_hint), size = 26.dp)
        }
        ChatoneSwitch(checked = checked, onCheckedChange = onChange)
    }
}
