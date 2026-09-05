package io.rudione.chatone.presentation.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.rudione.chatone.presentation.chat.AnimatedEmoteImage
import io.rudione.chatone.presentation.settings.SettingsState
import io.rudione.chatone.util.chat.MessageToken
import io.rudione.chatone.util.link.openChatUrl

private val GIF_MAX_WIDTH = 320.dp

@Composable
internal fun ChatGifBlock(
    gif: MessageToken.GifToken,
    maxHeight: Dp,
    linkOpenMode: SettingsState.LinkOpenMode,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .padding(start = 4.dp, top = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.4f))
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
                RoundedCornerShape(8.dp)
            )
            .clickable {
                try {
                    openChatUrl(gif.url, linkOpenMode)
                } catch (_: Exception) {
                }
            },
        contentAlignment = Alignment.BottomEnd
    ) {
        AnimatedEmoteImage(
            url = gif.url,
            contentDescription = gif.title,
            modifier = Modifier
                .heightIn(max = maxHeight)
                .widthIn(max = GIF_MAX_WIDTH)
        )
        Text(
            text = "GIF",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            modifier = Modifier
                .padding(4.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.Black.copy(alpha = 0.55f))
                .padding(horizontal = 4.dp, vertical = 1.dp)
        )
    }
}
