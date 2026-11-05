package io.rudione.chatone.presentation.chat

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.rudione.chatone.presentation.chat.emote.SkiaEmoteImage

@Composable
actual fun AnimatedEmoteImage(
    url: String,
    contentDescription: String?,
    modifier: Modifier,
    isScrolling: Boolean,
    maxDimension: Int
) {
    SkiaEmoteImage(
        url = url,
        contentDescription = contentDescription,
        modifier = modifier,
        paused = isScrolling,
        maxDimension = maxDimension
    )
}
