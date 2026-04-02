package io.rudione.chatone.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import io.rudione.chatone.presentation.theme.WallpaperState

/**
 * Platform-agnostic interface for loading wallpaper images.
 * Actual implementations live in platform source sets.
 */
interface WallpaperLoader {
    fun load(path: String, blurRadius: Float): WallpaperState?
}


/**
 * Samples a grid of pixels from the bitmap and returns the average color.
 * Uses a 20x20 grid to keep it fast even on large images.
 */
private fun extractDominantColor(bitmap: ImageBitmap): Color {
    val width = bitmap.width
    val height = bitmap.height
    if (width == 0 || height == 0) return Color.Transparent

    val pixels = IntArray(width * height)
    bitmap.readPixels(pixels, 0, 0, width, height)

    val steps = 20
    val xStep = (width / steps).coerceAtLeast(1)
    val yStep = (height / steps).coerceAtLeast(1)

    var rSum = 0L; var gSum = 0L; var bSum = 0L; var count = 0

    var y = 0
    while (y < height) {
        var x = 0
        while (x < width) {
            val pixel = pixels[y * width + x]
            val a = (pixel shr 24) and 0xFF
            if (a > 10) { // Skip near-transparent pixels
                rSum += (pixel shr 16) and 0xFF
                gSum += (pixel shr 8) and 0xFF
                bSum += pixel and 0xFF
                count++
            }
            x += xStep
        }
        y += yStep
    }

    if (count == 0) return Color(0xFF1A1A2E)

    return Color(
        red = (rSum / count).toInt() / 255f,
        green = (gSum / count).toInt() / 255f,
        blue = (bSum / count).toInt() / 255f,
        alpha = 1f
    )
}