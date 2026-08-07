package io.rudione.chatone.util.media

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap

@Composable
actual fun rememberAnimatedFrame(
    url: String,
    paused: Boolean,
    maxDimension: Int
): ImageBitmap? = null
