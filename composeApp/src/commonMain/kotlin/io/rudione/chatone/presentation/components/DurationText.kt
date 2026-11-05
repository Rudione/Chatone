package io.rudione.chatone.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.duration_short_days
import chatone.composeapp.generated.resources.duration_short_hours
import chatone.composeapp.generated.resources.duration_short_minutes
import chatone.composeapp.generated.resources.duration_short_seconds
import io.rudione.chatone.domain.moderation.ModSliderDurations
import org.jetbrains.compose.resources.stringResource

private const val COUNT_SLOT = "\u0000"

@Composable
fun rememberDurationFormatter(): (Int) -> String {
    val days = stringResource(Res.string.duration_short_days, COUNT_SLOT)
    val hours = stringResource(Res.string.duration_short_hours, COUNT_SLOT)
    val minutes = stringResource(Res.string.duration_short_minutes, COUNT_SLOT)
    val seconds = stringResource(Res.string.duration_short_seconds, COUNT_SLOT)
    return remember(days, hours, minutes, seconds) {
        { total ->
            val (template, amount) = when {
                total >= ModSliderDurations.DAY && total % ModSliderDurations.DAY == 0 -> days to total / ModSliderDurations.DAY
                total >= ModSliderDurations.HOUR && total % ModSliderDurations.HOUR == 0 -> hours to total / ModSliderDurations.HOUR
                total >= ModSliderDurations.MINUTE && total % ModSliderDurations.MINUTE == 0 -> minutes to total / ModSliderDurations.MINUTE
                else -> seconds to total
            }
            template.replace(COUNT_SLOT, amount.toString())
        }
    }
}

@Composable
fun durationText(seconds: Int): String = rememberDurationFormatter()(seconds)
