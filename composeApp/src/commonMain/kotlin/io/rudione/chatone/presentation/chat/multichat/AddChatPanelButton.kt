package io.rudione.chatone.presentation.chat.multichat

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.rudione.chatone.domain.model.ChatPanel
import io.rudione.chatone.presentation.theme.i18n.LocalStrings
import io.rudione.chatone.presentation.components.ChatoneIconButton
import io.rudione.chatone.presentation.components.ChatoneDropdownMenu

@Composable
fun AddChatPanelButton(
    panelManager: ChatPanelManager,
    availableChannels: List<String>,
    currentChannel: String,
    modifier: Modifier = Modifier,
    iconSize: androidx.compose.ui.unit.Dp = 18.dp,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    val strings = LocalStrings.current
    var expanded by remember { mutableStateOf(false) }
    val panels by panelManager.panels.collectAsState()
    val canOpen = panels.size < ChatPanel.MAX_PANELS

    val pickable = remember(availableChannels, panels, currentChannel) {
        availableChannels
            .map { it.lowercase().removePrefix("#") }
            .filter { it.isNotBlank() && it != currentChannel.lowercase() }
            .distinct()
    }

    Box(modifier = modifier) {
        ChatoneIconButton(
            onClick = { if (canOpen) expanded = true },
            enabled = canOpen,
            modifier = Modifier.size(iconSize + 14.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = strings.multiChatAddPanel,
                modifier = Modifier.size(iconSize),
                tint = if (canOpen) tint else tint.copy(alpha = 0.4f)
            )
        }

        ChatoneDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .heightIn(max = 360.dp)
                .width(260.dp)
        ) {
            if (pickable.isEmpty()) {
                DropdownMenuItem(
                    text = { Text(strings.multiChatNoChannelsToAdd) },
                    onClick = { expanded = false },
                    enabled = false
                )
            } else {
                Text(
                    strings.multiChatChooseChannel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                pickable.forEach { channel ->
                    val alreadyOpen = panelManager.isOpen(channel)
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = channel,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f),
                                    color = if (alreadyOpen)
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    else MaterialTheme.colorScheme.onSurface
                                )
                                if (alreadyOpen) {
                                    Icon(
                                        Icons.Outlined.Check,
                                        contentDescription = strings.multiChatAlreadyOpen,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        },
                        enabled = !alreadyOpen,
                        onClick = {
                            panelManager.openPanel(channel)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
