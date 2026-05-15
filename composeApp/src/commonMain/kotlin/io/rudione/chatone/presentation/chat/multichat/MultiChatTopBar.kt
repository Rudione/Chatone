package io.rudione.chatone.presentation.chat.multichat

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp


@Composable
fun MultiChatTopBar(
    panelManager: ChatPanelManager,
    leadingContent: @Composable () -> Unit = {},
    trailingContent: @Composable () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val panels by panelManager.panels.collectAsState()
    if (panels.size <= 1) return

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(34.dp)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        leadingContent()
        Spacer(Modifier.width(8.dp))
        Box(modifier = Modifier.weight(1f)) {
            PanelHeaderTabs(panelManager = panelManager)
        }
        Spacer(Modifier.width(8.dp))
        PanelCloseAllButton(panelManager = panelManager)
        trailingContent()
    }
}
