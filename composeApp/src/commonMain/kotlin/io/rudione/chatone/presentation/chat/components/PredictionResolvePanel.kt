package io.rudione.chatone.presentation.chat

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.rudione.chatone.data.remote.dto.PredictionData
import io.rudione.chatone.presentation.components.ChatoneIconButton
import io.rudione.chatone.presentation.components.chatoneGlassPanel
import io.rudione.chatone.presentation.theme.i18n.LocalStrings

private fun outcomeAccent(color: String): Color = when (color.uppercase()) {
    "BLUE" -> Color(0xFF1E69FF)
    "PINK" -> Color(0xFFFF4D9D)
    else -> Color(0xFF9146FF)
}

private fun groupPoints(value: Long): String {
    val raw = value.toString()
    if (raw.length <= 3) return raw
    val sb = StringBuilder()
    raw.forEachIndexed { i, c ->
        if (i > 0 && (raw.length - i) % 3 == 0) sb.append(' ')
        sb.append(c)
    }
    return sb.toString()
}

@Composable
internal fun PredictionResolvePanel(
    prediction: PredictionData,
    canDetach: Boolean,
    onDetach: () -> Unit,
    onLock: () -> Unit,
    onResolve: (outcomeId: String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val s = LocalStrings.current
    var selected by remember(prediction.id) { mutableStateOf<String?>(null) }
    val totalPoints = prediction.outcomes.sumOf { it.channelPoints }.toLong()
    val isLocked = prediction.status == "LOCKED"

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 18.dp, top = 4.dp, bottom = 4.dp)
            .chatoneGlassPanel(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {}
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    s.predictionResolveTitle,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    s.predictionResolveSubtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (canDetach) {
                ChatoneIconButton(onClick = onDetach, modifier = Modifier.size(24.dp)) {
                    Icon(
                        Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = s.predictionResolveDetach,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            ChatoneIconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = s.cancel,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
        Text(
            groupPoints(totalPoints),
            style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        prediction.outcomes.forEachIndexed { index, outcome ->
            OutcomeChoiceRow(
                index = index + 1,
                title = outcome.title,
                users = outcome.users,
                points = outcome.channelPoints,
                accent = outcomeAccent(outcome.color),
                selected = selected == outcome.id,
                onClick = { selected = outcome.id }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (!isLocked) {
                ResolveActionButton(
                    label = s.predictionResolveLock,
                    filled = false,
                    accent = MaterialTheme.colorScheme.primary,
                    enabled = true,
                    leadingIcon = true,
                    modifier = Modifier.weight(1f),
                    onClick = onLock
                )
            }
            ResolveActionButton(
                label = s.predictionBetCancel,
                filled = false,
                accent = MaterialTheme.colorScheme.primary,
                enabled = true,
                modifier = Modifier.weight(1f),
                onClick = onClose
            )
            ResolveActionButton(
                label = s.predictionResolveComplete,
                filled = true,
                accent = MaterialTheme.colorScheme.primary,
                enabled = selected != null,
                modifier = Modifier.weight(1.5f),
                onClick = { selected?.let(onResolve) }
            )
        }
    }
}

@Composable
private fun OutcomeChoiceRow(
    index: Int,
    title: String,
    users: Int,
    points: Int,
    accent: Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    val borderColor by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.onSurface
        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
        animationSpec = tween(160),
        label = "outcomeBorder"
    )
    val borderWidth by animateDpAsState(
        if (selected) 1.6.dp else 1.dp,
        animationSpec = tween(160),
        label = "outcomeBorderWidth"
    )
    val bg by animateColorAsState(
        if (selected) accent.copy(alpha = 0.16f)
        else MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.55f),
        animationSpec = tween(160),
        label = "outcomeBg"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(borderWidth, borderColor, RoundedCornerShape(8.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 8.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(17.dp)
                .clip(CircleShape)
                .background(accent),
            contentAlignment = Alignment.Center
        ) {
            Text(
                index.toString(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Text(
            "$users · ${groupPoints(points.toLong())}",
            style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ResolveActionButton(
    label: String,
    filled: Boolean,
    accent: Color,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    leadingIcon: Boolean = false,
    onClick: () -> Unit
) {
    val bg by animateColorAsState(
        when {
            !enabled -> MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.4f)
            filled -> accent
            else -> MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.65f)
        },
        animationSpec = tween(160),
        label = "resolveBtn"
    )
    val fg = when {
        !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        filled -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onSurface
    }
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(9.dp))
            .background(bg)
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leadingIcon) {
            Icon(
                Icons.Outlined.Lock,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = fg
            )
            Spacer(Modifier.width(4.dp))
        }
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = fg,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
