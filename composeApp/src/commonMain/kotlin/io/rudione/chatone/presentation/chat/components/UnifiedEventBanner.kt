package io.rudione.chatone.presentation.chat.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.material3.Icon
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.rudione.chatone.presentation.components.ChatoneIconButton
import io.rudione.chatone.presentation.components.chatoneGlassPanel
import io.rudione.chatone.util.system.isDesktopPlatform
import kotlinx.coroutines.delay
import kotlin.time.Clock
import io.rudione.chatone.icons.lucide.EyeOff
import io.rudione.chatone.icons.lucide.Lucide

private val StripShape = RoundedCornerShape(50)

data class EventBannerItem(
    val key: String,
    val label: String,
    val endsAtMs: Long?,
    val totalDurationMs: Long?,
    val content: @Composable () -> Unit
)

@Composable
fun UnifiedEventBanner(
    items: List<EventBannerItem>,
    horizontalInset: Dp = 26.dp,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return
    var index by remember(items.size) { mutableStateOf(0) }
    val safeIndex = index.coerceIn(0, items.lastIndex)
    val current = items[safeIndex]

    val showStrip = items.size > 1 || current.endsAtMs != null
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalInset)
            .pointerInput(items.size) {
                var drag = 0f
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (drag > 55f) index = (safeIndex - 1 + items.size) % items.size
                        else if (drag < -55f) index = (safeIndex + 1) % items.size
                        drag = 0f
                    },
                    onDragCancel = { drag = 0f }
                ) { change, dragAmount -> change.consume(); drag += dragAmount }
            }
    ) {
        if (showStrip) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 18.dp, top = 2.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .chatoneGlassPanel(StripShape, elevation = 4.dp)
                        .padding(start = if (current.endsAtMs != null) 3.dp else 10.dp, end = 10.dp, top = 3.dp, bottom = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    current.endsAtMs?.let { ends ->
                        CountdownRing(endsAtMs = ends, totalDurationMs = current.totalDurationMs)
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(
                        current.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(Modifier.weight(1f))
                if (items.size > 1) {
                    Row(
                        modifier = Modifier
                            .chatoneGlassPanel(StripShape, elevation = 4.dp)
                            .padding(horizontal = 3.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items.indices.forEach { i ->
                            val selected = i == safeIndex
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) { index = i },
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(if (selected) 7.dp else 6.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (selected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                                        )
                                )
                            }
                        }
                    }
                }
            }
        }
        current.content()
    }
}

@Composable
private fun CountdownRing(endsAtMs: Long, totalDurationMs: Long?) {
    var now by remember { mutableStateOf(Clock.System.now().toEpochMilliseconds()) }
    LaunchedEffect(endsAtMs) {
        while (true) {
            now = Clock.System.now().toEpochMilliseconds()
            delay(1000)
        }
    }
    val remaining = (endsAtMs - now).coerceAtLeast(0L)
    val total = (totalDurationMs ?: remaining).coerceAtLeast(1L)
    val fraction by animateFloatAsState(
        (remaining.toFloat() / total).coerceIn(0f, 1f),
        label = "countdown"
    )
    val secs = (remaining / 1000L)
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(22.dp)) {
        CircularProgressIndicator(
            progress = { fraction },
            modifier = Modifier.size(22.dp),
            strokeWidth = 2.dp,
            color = if (secs <= 10) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.18f)
        )
        Text(
            if (secs >= 60) "${secs / 60}m" else "$secs",
            style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
            fontSize = androidx.compose.ui.unit.TextUnit(8f, androidx.compose.ui.unit.TextUnitType.Sp),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
internal fun EventHideButton(tooltip: String, onHide: () -> Unit) {
    val touch = !isDesktopPlatform
    LiquidGlassTooltipBox(tooltip = tooltip) {
        ChatoneIconButton(
            onClick = onHide,
            modifier = Modifier
                .size(if (touch) 28.dp else 24.dp)
                .then(
                    if (touch) Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    else Modifier
                )
        ) {
            Icon(
                Lucide.EyeOff,
                contentDescription = tooltip,
                modifier = Modifier.size(if (touch) 17.dp else 14.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = if (touch) 0.85f else 0.7f)
            )
        }
    }
}

