package io.rudione.chatone.presentation.chat.multichat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.rudione.chatone.domain.model.ChatPanel
import io.rudione.chatone.domain.model.PanelLayoutMode
import io.rudione.chatone.domain.model.layoutMode
import io.rudione.chatone.presentation.theme.i18n.LocalStrings


@Composable
fun MultiChatHost(
    panelManager: ChatPanelManager,
    panelContent: @Composable (ChatPanel, isCompact: Boolean, modifier: Modifier) -> Unit,
    modifier: Modifier = Modifier
) {
    val panels by panelManager.panels.collectAsState()
    val mode = panels.layoutMode()
    val isCompact = mode == PanelLayoutMode.COMPACT
    val strings = LocalStrings.current

    if (panels.isEmpty()) {
        Box(modifier = modifier)
        return
    }

    Row(
        modifier = modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        panels.forEachIndexed { _, panel ->
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(horizontal = 1.dp)
            ) {
                if (panels.size > 1) {
                    PanelHeader(
                        title = panel.channelLogin,
                        onClose = { panelManager.closePanel(panel.panelId) },
                        compact = isCompact,
                        closeLabel = strings.multiChatClosePanel
                    )
                    Spacer(Modifier.height(2.dp))
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(if (isCompact) 6.dp else 10.dp))
                ) {
                    panelContent(panel, isCompact, Modifier.fillMaxSize())
                }
            }
        }
    }
}

@Composable
private fun PanelHeader(
    title: String,
    onClose: () -> Unit,
    compact: Boolean,
    closeLabel: String
) {
    val height = if (compact) 22.dp else 28.dp
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = if (compact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        IconButton(
            onClick = onClose,
            modifier = Modifier.size(if (compact) 16.dp else 20.dp)
        ) {
            Icon(
                Icons.Outlined.Close,
                contentDescription = closeLabel,
                modifier = Modifier.size(if (compact) 12.dp else 14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
