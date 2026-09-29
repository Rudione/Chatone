package io.rudione.chatone.presentation.chat

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.rudione.chatone.presentation.chat.emote.SharedEmoteImage

@Composable
actual fun AnimatedEmoteImage(
    url: String,
    contentDescription: String?,
    modifier: Modifier,
    isScrolling: Boolean,
    maxDimension: Int
) {
    SharedEmoteImage(
        url = url,
        contentDescription = contentDescription,
        modifier = modifier,
        maxDimension = maxDimension
    )
}
