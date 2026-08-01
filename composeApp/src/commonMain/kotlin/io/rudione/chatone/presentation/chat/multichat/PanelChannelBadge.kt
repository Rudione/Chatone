package io.rudione.chatone.presentation.chat.multichat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

@Composable
fun PanelChannelBadge(
    channelLogin: String,
    isCompact: Boolean = false,
    modifier: Modifier = Modifier
) {
    Text(
        text = "#$channelLogin",
        modifier = modifier
            .clip(RoundedCornerShape(if (isCompact) 4.dp else 6.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
            .padding(horizontal = if (isCompact) 4.dp else 6.dp, vertical = 2.dp),
        style = if (isCompact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurface
    )
}
