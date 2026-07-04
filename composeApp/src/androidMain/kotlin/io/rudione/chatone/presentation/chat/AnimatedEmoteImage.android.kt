package io.rudione.chatone.presentation.chat

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import coil3.compose.AsyncImage

@Composable
actual fun AnimatedEmoteImage(
    url: String,
    contentDescription: String?,
    modifier: Modifier,
    isScrolling: Boolean
) {
    // Coil already throttles animated-image decoding/playback on Android, so there is
    // nothing extra to pause here; isScrolling is accepted to satisfy the expect contract.
    AsyncImage(
        model = url,
        contentDescription = contentDescription,
        modifier = modifier
    )
}
