package io.rudione.chatone.presentation.chat.emote

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import coil3.compose.AsyncImage
import io.rudione.chatone.presentation.chat.rendering.LocalScrollActivity
import io.rudione.chatone.util.emote.AnimatedEmoteLoader
import io.rudione.chatone.util.emote.rememberSharedEmotePainter

@Composable
internal fun SkiaEmoteImage(
    url: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    paused: Boolean = false,
    maxDimension: Int = 0
) {
    var frames by remember(url, maxDimension) { mutableStateOf(AnimatedEmoteLoader.peek(url, maxDimension)) }
    LaunchedEffect(url, maxDimension) {
        if (frames != null || AnimatedEmoteLoader.isKnownStatic(url, maxDimension)) return@LaunchedEffect
        frames = AnimatedEmoteLoader.load(url, maxDimension)
    }
    val animated = frames
    if (animated == null) {
        AsyncImage(model = url, contentDescription = contentDescription, modifier = modifier)
        return
    }
    val listScrolling = LocalScrollActivity.current.isScrolling
    Image(
        painter = rememberSharedEmotePainter(animated, paused = paused || listScrolling),
        contentDescription = contentDescription,
        modifier = modifier
    )
}
