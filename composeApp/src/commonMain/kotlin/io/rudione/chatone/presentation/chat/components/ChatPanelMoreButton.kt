package io.rudione.chatone.presentation.chat.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.rudione.chatone.domain.model.ChatPanel
import io.rudione.chatone.presentation.chat.multichat.ChatPanelManager
import io.rudione.chatone.presentation.theme.i18n.LocalStrings


@Composable
fun ChatPanelMoreButton(
    panelManager: ChatPanelManager,
    availableChannels: List<String>,
    currentChannel: String,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    var expanded by remember { mutableStateOf(false) }
    val panels by panelManager.panels.collectAsState()
    val canOpen = panels.size < ChatPanel.MAX_PANELS

    val candidates = remember(availableChannels, panels, currentChannel) {
        availableChannels
            .map { it.lowercase().removePrefix("#") }
            .filter { it.isNotBlank() && it != currentChannel.lowercase() }
            .distinct()
    }

    IconButton(
        onClick = { if (canOpen && candidates.isNotEmpty()) expanded = true },
        enabled = canOpen && candidates.isNotEmpty(),
        modifier = modifier.size(28.dp)
    ) {
        Icon(
            Icons.Outlined.Add,
            contentDescription = strings.multiChatAddPanel,
            modifier = Modifier.size(16.dp),
            tint = if (canOpen) MaterialTheme.colorScheme.onSurfaceVariant
            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
        )
    }

    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        if (candidates.isEmpty()) {
            DropdownMenuItem(
                text = { Text(strings.multiChatNoChannelsToAdd) },
                onClick = { expanded = false },
                enabled = false
            )
        } else {
            candidates.forEach { ch ->
                val alreadyOpen = panelManager.isOpen(ch)
                DropdownMenuItem(
                    text = { Text(if (alreadyOpen) "$ch · ${strings.multiChatAlreadyOpen}" else ch) },
                    enabled = !alreadyOpen,
                    onClick = {
                        panelManager.openPanel(ch)
                        expanded = false
                    }
                )
            }
        }
    }
}
