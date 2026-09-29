package io.rudione.chatone.presentation.chat.multichat

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun Modifier.panelActiveHighlight(
    panelManager: ChatPanelManager,
    panelId: String,
    enabled: Boolean = true
): Modifier {
    if (!enabled) return this
    val isActive = rememberIsPanelActive(panelManager, panelId)
    val targetColor = if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
    else Color.Transparent
    val borderColor by animateColorAsState(targetValue = targetColor, label = "panelHighlightColor")
    return this.border(
        width = if (isActive) 1.dp else 0.dp,
        color = borderColor,
        shape = RoundedCornerShape(10.dp)
    )
}
