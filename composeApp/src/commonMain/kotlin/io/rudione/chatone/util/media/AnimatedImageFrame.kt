package io.rudione.chatone.util.media

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap

@Composable
expect fun rememberAnimatedFrame(
    url: String,
    paused: Boolean = false,
    maxDimension: Int = 0
): ImageBitmap?
