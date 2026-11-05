package io.rudione.chatone.util.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import io.rudione.chatone.util.emote.AnimatedEmoteLoader
import io.rudione.chatone.util.emote.rememberSharedEmoteFrame

@Composable
actual fun rememberAnimatedFrame(
    url: String,
    paused: Boolean,
    maxDimension: Int
): State<ImageBitmap?>? {
    if (url.isBlank()) return null

    var frames by remember(url, maxDimension) {
        mutableStateOf(AnimatedEmoteLoader.peek(url, maxDimension))
    }

    LaunchedEffect(url, maxDimension) {
        if (frames != null || AnimatedEmoteLoader.isKnownStatic(url, maxDimension)) return@LaunchedEffect
        frames = AnimatedEmoteLoader.load(url, maxDimension)
    }

    val data = frames ?: return null
    return rememberSharedEmoteFrame(data, paused)
}
