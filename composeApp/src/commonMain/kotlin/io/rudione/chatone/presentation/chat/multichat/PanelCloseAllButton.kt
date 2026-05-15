package io.rudione.chatone.presentation.chat.multichat

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloseFullscreen
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp


@Composable
fun PanelCloseAllButton(
    panelManager: ChatPanelManager,
    modifier: Modifier = Modifier
) {
    val panels by panelManager.panels.collectAsState()
    if (panels.size <= 1) return

    IconButton(
        onClick = {

            val primary = panels.firstOrNull() ?: return@IconButton
            panels.drop(1).forEach { panelManager.closePanel(it.panelId) }
        },
        modifier = modifier.size(28.dp)
    ) {
        Icon(
            Icons.Outlined.CloseFullscreen,
            contentDescription = "Close extra panels",
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
