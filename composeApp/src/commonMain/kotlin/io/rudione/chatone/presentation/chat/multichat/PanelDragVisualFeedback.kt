package io.rudione.chatone.presentation.chat.multichat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.rudione.chatone.presentation.theme.i18n.LocalStrings


@Composable
fun PanelDragVisualFeedback(
    visible: Boolean,
    panelCount: Int,
    maxPanels: Int,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    val canAccept = panelCount < maxPanels
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(16.dp))
                .background(
                    if (canAccept)
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    else
                        MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                )
                .border(
                    width = 3.dp,
                    color = if (canAccept) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error,
                    shape = RoundedCornerShape(16.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (canAccept) strings.multiChatDragHint else strings.multiChatMaxPanels,
                style = MaterialTheme.typography.titleMedium,
                color = if (canAccept) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.error
            )
        }
    }
}
