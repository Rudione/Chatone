package io.rudione.chatone.presentation.chat.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.rudione.chatone.data.repository.AiAssistantController
import io.rudione.chatone.presentation.chat.multichat.ChatPanelManager
import org.koin.compose.koinInject
import io.rudione.chatone.presentation.components.ChatoneIconButton
import io.rudione.chatone.icons.lucide.Lucide
import io.rudione.chatone.icons.lucide.RefreshCw
import io.rudione.chatone.icons.lucide.Sparkles

@Composable
fun MultiChatToolbar(
    panelManager: ChatPanelManager,
    availableChannels: List<String>,
    currentChannel: String,
    canModerate: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val aiController: AiAssistantController = koinInject()
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        ChatoneIconButton(
            onClick = { aiController.open() },
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                Lucide.Sparkles,
                contentDescription = "AI Assistant",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        ChatoneIconButton(
            onClick = onRefresh,
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                Lucide.RefreshCw,
                contentDescription = "Refresh",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        ChatPanelMoreButton(
            panelManager = panelManager,
            availableChannels = availableChannels,
            currentChannel = currentChannel
        )
    }
}
