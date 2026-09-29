package io.rudione.chatone.util.chat

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.rudione.chatone.domain.model.GenericEmote

@Composable
expect fun EmoteImageWithTooltip(
    emote: GenericEmote,
    modifier: Modifier,
    onShowContextMenu: (() -> Unit)?
)
