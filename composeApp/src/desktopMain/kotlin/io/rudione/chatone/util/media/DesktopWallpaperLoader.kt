package io.rudione.chatone.util.media

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import io.rudione.chatone.presentation.theme.WallpaperState
import org.jetbrains.skia.Image
import java.io.File

class DesktopWallpaperLoader : WallpaperLoader {

    override fun load(path: String, blurRadius: Float): WallpaperState? {
        if (path.isBlank()) return null
        val file = File(path)
        if (!file.exists() || !file.canRead()) return null

        return try {
            val bytes = file.readBytes()
            val skiaImage = Image.makeFromEncoded(bytes)
            val bitmap = skiaImage.toComposeImageBitmap()
            val dominant = extractDominantColor(bitmap)
            WallpaperState(
                imageBitmap = bitmap,
                dominantColor = dominant,

                isActive = true
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun extractDominantColor(bitmap: ImageBitmap): Color {
        val width = bitmap.width
        val height = bitmap.height
        if (width == 0 || height == 0) return Color(0xFF1A1A2E)

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
                if (a > 10) {
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
}
