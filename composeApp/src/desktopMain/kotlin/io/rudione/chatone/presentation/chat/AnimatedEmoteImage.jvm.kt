package io.rudione.chatone.presentation.chat

import androidx.compose.foundation.Image
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import coil3.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Codec
import org.jetbrains.skia.Data
import java.net.URI

// Simple in-memory cache for decoded animated frames
private val animatedCache = mutableMapOf<String, AnimatedFrames>()

private data class AnimatedFrames(
    val frames: List<ImageBitmap>,
    val durations: IntArray
)

@Composable
actual fun AnimatedEmoteImage(
    url: String,
    contentDescription: String?,
    modifier: Modifier
) {
    var animData by remember(url) { mutableStateOf(animatedCache[url]) }
    var isStatic by remember(url) { mutableStateOf(false) }
    var currentFrame by remember(url) { mutableIntStateOf(0) }

    LaunchedEffect(url) {
        if (animData != null) return@LaunchedEffect
        if (isStatic) return@LaunchedEffect

        withContext(Dispatchers.IO) {
            try {
                val cached = animatedCache[url]
                if (cached != null) {
                    animData = cached
                    return@withContext
                }

                val bytes = URI(url).toURL().readBytes()
                val data = Data.makeFromBytes(bytes)
                val codec = Codec.makeFromData(data)

                if (codec.frameCount > 1) {
                    val infos = codec.framesInfo
                    val durations = IntArray(codec.frameCount) { i ->
                        infos[i].duration.coerceAtLeast(16)
                    }
                    val frames = (0 until codec.frameCount).map { i ->
                        val bmp = Bitmap()
                        bmp.allocPixels(codec.imageInfo)
                        codec.readPixels(bmp, i)
                        bmp.toComposeImageBitmap()
                    }
                    val result = AnimatedFrames(frames, durations)
                    animatedCache[url] = result
                    animData = result
                } else {
                    isStatic = true
                }
            } catch (_: Exception) {
                isStatic = true
            }
        }
    }

    val data = animData
    if (data != null && data.frames.isNotEmpty()) {
        // Animate through frames
        LaunchedEffect(data) {
            while (true) {
                delay(data.durations[currentFrame].toLong())
                currentFrame = (currentFrame + 1) % data.frames.size
            }
        }
        Image(
            bitmap = data.frames[currentFrame],
            contentDescription = contentDescription,
            modifier = modifier
        )
    } else {
        // Static fallback via Coil
        AsyncImage(
            model = url,
            contentDescription = contentDescription,
            modifier = modifier
        )
    }
}

private fun Bitmap.toComposeImageBitmap(): ImageBitmap {
    val image = org.jetbrains.skia.Image.makeFromBitmap(this)
    return image.toComposeImageBitmap()
}
