package io.rudione.chatone.util.media

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import io.rudione.chatone.presentation.theme.WallpaperState

interface WallpaperLoader {
    fun load(path: String): WallpaperState?
}

private const val DOMINANT_SAMPLES = 20
private const val MIN_SAMPLE_ALPHA = 10
private val FallbackWallpaperColor = Color(0xFF1A1A2E)

internal fun ImageBitmap.toWallpaperState(): WallpaperState =
    WallpaperState(imageBitmap = this, dominantColor = dominantColor(), isActive = true)

internal fun ImageBitmap.dominantColor(): Color {
    if (width == 0 || height == 0) return FallbackWallpaperColor
    val xStep = (width / DOMINANT_SAMPLES).coerceAtLeast(1)
    val yStep = (height / DOMINANT_SAMPLES).coerceAtLeast(1)
    val row = IntArray(width)
    var rSum = 0L
    var gSum = 0L
    var bSum = 0L
    var count = 0
    var y = 0
    while (y < height) {
        readPixels(row, startX = 0, startY = y, width = width, height = 1)
        var x = 0
        while (x < width) {
            val pixel = row[x]
            if ((pixel ushr 24) and 0xFF > MIN_SAMPLE_ALPHA) {
                rSum += (pixel shr 16) and 0xFF
                gSum += (pixel shr 8) and 0xFF
                bSum += pixel and 0xFF
                count++
            }
            x += xStep
        }
        y += yStep
    }
    if (count == 0) return FallbackWallpaperColor
    return Color(
        red = (rSum / count).toInt() / 255f,
        green = (gSum / count).toInt() / 255f,
        blue = (bSum / count).toInt() / 255f,
        alpha = 1f
    )
}
