package io.rudione.chatone.presentation.chat.multichat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import io.rudione.chatone.presentation.theme.i18n.LocalStrings

@Composable
fun PanelDropOverlay(
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    if (!isActive) return
    val strings = LocalStrings.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
            .border(
                width = 2.dp,
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = strings.multiChatDragHint,
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.titleSmall
        )
    }
}

class DragDropState {
    var isDragOver by mutableStateOf(false)
        private set
    var lastDroppedChannel by mutableStateOf<String?>(null)
        private set

    fun enter() { isDragOver = true }
    fun exit() { isDragOver = false }
    fun consume(): String? {
        val v = lastDroppedChannel
        lastDroppedChannel = null
        return v
    }
    fun receive(channelLogin: String) {
        lastDroppedChannel = channelLogin
        isDragOver = false
    }
}

@Composable
fun rememberDragDropState(): DragDropState = remember { DragDropState() }
