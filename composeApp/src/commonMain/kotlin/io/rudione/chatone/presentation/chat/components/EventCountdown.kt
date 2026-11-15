package io.rudione.chatone.presentation.chat.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.rudione.chatone.data.remote.dto.PollData
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin
import kotlin.time.Clock
import kotlin.time.Instant

internal fun eventDeadline(startedAt: String?, durationSeconds: Int): Long? =
    startedAt?.takeIf { durationSeconds > 0 }?.let {
        runCatching { Instant.parse(it).toEpochMilliseconds() + durationSeconds * 1000L }.getOrNull()
    }

internal fun pollDeadline(poll: PollData, observedAtMs: Long): Long? = poll.remainingMs?.let {
    (poll.receivedAtMs ?: observedAtMs) + it.coerceAtLeast(0L)
} ?: eventDeadline(poll.startedAt, poll.duration)

@Composable
internal fun EventCountdown(
    endsAtMs: Long,
    totalDurationMs: Long?,
    modifier: Modifier = Modifier,
    wavy: Boolean = false,
    onExpired: () -> Unit = {}
) {
    var remaining by remember(endsAtMs) { mutableLongStateOf((endsAtMs - Clock.System.now().toEpochMilliseconds()).coerceAtLeast(0L)) }
    val total = remember(endsAtMs, totalDurationMs) { (totalDurationMs ?: remaining).coerceAtLeast(1L) }
    val expire by rememberUpdatedState(onExpired)
    LaunchedEffect(endsAtMs) {
        while (remaining > 0L) {
            delay(minOf(remaining, 1_000L))
            remaining = (endsAtMs - Clock.System.now().toEpochMilliseconds()).coerceAtLeast(0L)
        }
        expire()
    }
    val fraction by animateFloatAsState((remaining.toFloat() / total).coerceIn(0f, 1f), tween(800), label = "eventTime")
    val accent = if (remaining <= 10_000L) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.outlineVariant
    val seconds = (remaining + 999L) / 1_000L
    val timestamp = "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.weight(1f).height(if (wavy) 8.dp else 2.dp)
            .semantics { progressBarRangeInfo = ProgressBarRangeInfo(fraction, 0f..1f) }
            .drawWithCache {
                val path = Path()
                val amplitude = if (wavy) 2.dp.toPx() else 0f
                val wavelength = 20.dp.toPx()
                val stroke = Stroke(2.dp.toPx(), cap = StrokeCap.Round)
                var x = 0f
                while (x <= size.width) {
                    val y = size.height / 2 + sin(x / wavelength * 2 * PI).toFloat() * amplitude
                    if (x == 0f) path.moveTo(x, y) else path.lineTo(x, y)
                    x += 1.dp.toPx()
                }
                onDrawBehind {
                    drawPath(path, track, style = stroke)
                    clipRect(right = size.width * fraction) { drawPath(path, accent, style = stroke) }
                }
            })
        Text(timestamp, style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontFeatureSettings = "tnum"), color = accent)
    }
}
