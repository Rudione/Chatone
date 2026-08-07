package io.rudione.chatone.util.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import io.rudione.chatone.util.emote.AnimatedEmoteLoader
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
actual fun rememberAnimatedFrame(
    url: String,
    paused: Boolean,
    maxDimension: Int
): ImageBitmap? {
    if (url.isBlank()) return null

    var frames by remember(url, maxDimension) {
        mutableStateOf(AnimatedEmoteLoader.peek(url, maxDimension))
    }
    var frameIndex by remember(url, maxDimension) { mutableIntStateOf(0) }

    LaunchedEffect(url, maxDimension) {
        if (frames != null || AnimatedEmoteLoader.isKnownStatic(url, maxDimension)) return@LaunchedEffect
        frames = AnimatedEmoteLoader.load(url, maxDimension)
    }

    val data = frames ?: return null
    if (data.frames.size < 2) return data.frames.firstOrNull()

    LaunchedEffect(data, paused) {
        if (paused) return@LaunchedEffect
        while (isActive) {
            delay(data.durations.getOrElse(frameIndex) { 100 }.toLong())
            frameIndex = (frameIndex + 1) % data.frames.size
        }
    }

    return data.frames[frameIndex.coerceIn(0, data.frames.lastIndex)]
}
