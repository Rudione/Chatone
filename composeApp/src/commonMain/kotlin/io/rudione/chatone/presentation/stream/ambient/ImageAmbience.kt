package io.rudione.chatone.presentation.stream.ambient

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.LayoutDirection
import coil3.SingletonImageLoader
import coil3.compose.LocalPlatformContext
import coil3.compose.asPainter
import coil3.request.ImageRequest
import coil3.request.SuccessResult

internal expect fun ImageRequest.Builder.readablePixels(): ImageRequest.Builder

@Composable
internal fun AmbienceFromImage(url: String, ambience: StreamAmbience) {
    val context = LocalPlatformContext.current
    val density = LocalDensity.current
    LaunchedEffect(url, ambience) {
        if (url.isBlank()) return@LaunchedEffect
        val width = ambience.sampleWidth
        val height = ambience.sampleHeight
        val request = ImageRequest.Builder(context)
            .data(url)
            .size(width * SAMPLE_SCALE, height * SAMPLE_SCALE)
            .readablePixels()
            .build()
        val image = (SingletonImageLoader.get(context).execute(request) as? SuccessResult)?.image
            ?: return@LaunchedEffect
        val painter = image.asPainter(context)
        val bitmap = ImageBitmap(width, height)
        CanvasDrawScope().draw(
            density,
            LayoutDirection.Ltr,
            Canvas(bitmap),
            Size(width.toFloat(), height.toFloat())
        ) {
            with(painter) { draw(size) }
        }
        val pixels = IntArray(width * height)
        bitmap.readPixels(pixels)
        ambience.accept(pixels)
    }
}

private const val SAMPLE_SCALE = 4
