package io.rudione.chatone.presentation.chat

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import coil3.compose.AsyncImage

@Composable
actual fun AnimatedEmoteImage(
    url: String,
    contentDescription: String?,
    modifier: Modifier
) {
    AsyncImage(
        model = url,
        contentDescription = contentDescription,
        modifier = modifier
    )
}
