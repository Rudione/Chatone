package io.rudione.chatone.presentation.chat.multichat

import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import io.rudione.chatone.domain.model.ChatPanel
import io.rudione.chatone.presentation.main.MainViewModel
import io.rudione.chatone.presentation.theme.i18n.LocalStrings
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel


@Composable
fun ChatTopBarAddPanelButton(
    currentChannel: String,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    val panelManager: ChatPanelManager = koinInject()
    val mainViewModel: MainViewModel = koinViewModel()
    val mainState by mainViewModel.state.collectAsState()
    val panels by panelManager.panels.collectAsState()

    var expanded by remember { mutableStateOf(false) }

    val candidates = remember(mainState.openChannels, panels, currentChannel) {
        mainState.openChannels
            .map { it.login.lowercase().removePrefix("#") }
            .filter { it.isNotBlank() && it != currentChannel.lowercase() }
            .distinct()
    }

    val canOpen = panels.size < ChatPanel.MAX_PANELS && candidates.isNotEmpty()

    IconButton(
        onClick = { if (canOpen) expanded = true },
        enabled = canOpen,
        modifier = modifier.size(36.dp)
    ) {
        Icon(
            Icons.Outlined.Add,
            contentDescription = strings.multiChatAddPanel,
            modifier = Modifier.size(18.dp),
            tint = if (canOpen) MaterialTheme.colorScheme.onSurfaceVariant
            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
        )
    }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false },
        modifier = Modifier.clip(RoundedCornerShape(12.dp))
    ) {
        if (panels.size >= ChatPanel.MAX_PANELS) {
            DropdownMenuItem(
                text = { Text(strings.multiChatMaxPanels) },
                onClick = { expanded = false },
                enabled = false
            )
        } else if (candidates.isEmpty()) {
            DropdownMenuItem(
                text = { Text(strings.multiChatNoChannelsToAdd) },
                onClick = { expanded = false },
                enabled = false
            )
        } else {
            candidates.forEach { ch ->
                val alreadyOpen = panelManager.isOpen(ch)
                DropdownMenuItem(
                    text = {
                        if (alreadyOpen) {
                            Text(
                                "$ch · ${strings.multiChatAlreadyOpen}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        } else {
                            Text(ch)
                        }
                    },
                    leadingIcon = if (alreadyOpen) {
                        {
                            Icon(
                                Icons.Outlined.Check,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else null,
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
