package io.rudione.chatone.presentation.chat.components

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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.rudione.chatone.presentation.components.ChatoneSlider
import io.rudione.chatone.presentation.theme.ChatoneTheme
import io.rudione.chatone.presentation.components.chatoneGlassPanel
import io.rudione.chatone.presentation.theme.i18n.LocalStrings
import io.rudione.chatone.util.icons.TwitchPointsIcon

private const val TWITCH_MAX_BET = 250_000L

private fun groupedPoints(value: Long): String {
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
internal fun PredictionBetDialog(
    outcomeTitle: String,
    outcomeColor: Color,
    pointsBalance: Long,
    outcomePoints: Long,
    totalPoints: Long,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val s = LocalStrings.current
    val maxBet = minOf(pointsBalance, TWITCH_MAX_BET).coerceAtLeast(0L)
    var amount by remember { mutableStateOf(0L) }
    val clamped = amount.coerceIn(0L, maxBet)
    val fraction = if (maxBet > 0L) clamped.toFloat() / maxBet else 0f
    val percent = (fraction * 100f).toInt()

    val outcomeAfter = outcomePoints + clamped
    val totalAfter = totalPoints + clamped
    val ratio = if (outcomeAfter > 0L) totalAfter.toDouble() / outcomeAfter.toDouble() else 1.0
    val payout = (clamped.toDouble() * ratio).toLong()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .width(340.dp)
                .chatoneGlassPanel(RoundedCornerShape(18.dp), elevation = 22.dp)
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(outcomeColor)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    s.predictionBetTitle.replace("{0}", outcomeTitle),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    s.predictionBetBalance,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(6.dp))
                Icon(
                    TwitchPointsIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(Modifier.width(3.dp))
                Text(
                    groupedPoints(pointsBalance),
                    style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "$percent%",
                    style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                    fontWeight = FontWeight.Bold,
                    color = outcomeColor
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(outcomeColor.copy(alpha = 0.12f))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    TwitchPointsIcon,
                    contentDescription = null,
                    tint = outcomeColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                BasicTextField(
                    value = if (clamped == 0L) "" else clamped.toString(),
                    onValueChange = { raw ->
                        val digits = raw.filter { it.isDigit() }.take(9)
                        amount = (digits.toLongOrNull() ?: 0L).coerceIn(0L, maxBet)
                    },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.headlineSmall.copy(
                        fontFeatureSettings = "tnum",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    cursorBrush = SolidColor(outcomeColor),
                    modifier = Modifier.weight(1f),
                    decorationBox = { inner ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (clamped == 0L) {
                                Text(
                                    "0",
                                    style = MaterialTheme.typography.headlineSmall.copy(
                                        fontFeatureSettings = "tnum"
                                    ),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                                )
                            }
                            inner()
                        }
                    }
                )
                Text(
                    "/ ${groupedPoints(maxBet)}",
                    style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            ChatoneSlider(
                value = fraction.coerceIn(0f, 1f),
                onValueChange = { f -> amount = (maxBet * f).toLong().coerceIn(0L, maxBet) },
                valueRange = 0f..1f,
                trackHeight = 5.dp,
                knobSize = 15.dp,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(10, 25, 50, 75).forEach { p ->
                    BankChip(
                        label = "$p%",
                        selected = percent == p,
                        accent = outcomeColor,
                        modifier = Modifier.weight(1f)
                    ) { amount = (maxBet * p / 100L).coerceIn(0L, maxBet) }
                }
                BankChip(
                    label = s.predictionBetAllIn,
                    selected = clamped == maxBet && maxBet > 0L,
                    accent = outcomeColor,
                    modifier = Modifier.weight(1f)
                ) { amount = maxBet }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    s.predictionBetPotentialReturn,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(6.dp))
                Icon(
                    TwitchPointsIcon,
                    contentDescription = null,
                    tint = PredictionPayoutGreen,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(Modifier.width(3.dp))
                Text(
                    groupedPoints(payout),
                    style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                    fontWeight = FontWeight.Bold,
                    color = PredictionPayoutGreen
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "×${((ratio * 100).toInt() / 100.0)}",
                    style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DialogActionButton(
                    label = s.predictionBetCancel,
                    filled = false,
                    accent = outcomeColor,
                    enabled = true,
                    modifier = Modifier.weight(1f),
                    onClick = onDismiss
                )
                DialogActionButton(
                    label = s.predictionBetPlace,
                    filled = true,
                    accent = outcomeColor,
                    enabled = clamped > 0L,
                    modifier = Modifier.weight(1.4f),
                    onClick = { if (clamped > 0L) onConfirm(clamped.toInt()) }
                )
            }
        }
    }
}

private val PredictionPayoutGreen = Color(0xFF3FB950)


@Composable
private fun BankChip(
    label: String,
    selected: Boolean,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.45f)
            )
            .border(
                1.dp,
                if (selected) Color.Transparent
                else ChatoneTheme.extraColors.cardBorder.copy(alpha = 0.55f),
                shape
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

@Composable
private fun DialogActionButton(
    label: String,
    filled: Boolean,
    accent: Color,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bg = when {
        !enabled -> MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.4f)
        filled -> accent
        else -> MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f)
    }
    val fg = when {
        !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        filled -> Color.White
        else -> MaterialTheme.colorScheme.onSurface
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = fg,
            maxLines = 1
        )
    }
}
