package io.rudione.chatone.presentation.stream

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.rudione.chatone.domain.stream.StreamLatencyLevel
import kotlinx.coroutines.flow.StateFlow

private val LowLatencyColor = Color(0xFF3DDC84)
private val MediumLatencyColor = Color(0xFFFFC107)
private val HighLatencyColor = Color(0xFFFF5252)
private const val BADGE_ALPHA = 0.55f

internal fun StreamLatencyLevel.color(): Color = when (this) {
    StreamLatencyLevel.LOW -> LowLatencyColor
    StreamLatencyLevel.MEDIUM -> MediumLatencyColor
    StreamLatencyLevel.HIGH -> HighLatencyColor
}

@Composable
fun StreamLatencyBadge(
    latency: StateFlow<Float?>,
    modifier: Modifier = Modifier,
    textSize: TextUnit = 11.sp,
    dotSize: Dp = 6.dp
) {
    val seconds by latency.collectAsState()
    val current = seconds ?: return
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(Color.Black.copy(alpha = BADGE_ALPHA))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            Modifier
                .size(dotSize)
                .clip(CircleShape)
                .background(StreamLatencyLevel.of(current).color())
        )
        Text(
            text = streamLatencyText(current, compact = true),
            color = Color.White,
            fontSize = textSize,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}
