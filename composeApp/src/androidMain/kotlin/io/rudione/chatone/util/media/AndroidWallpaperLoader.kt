package io.rudione.chatone.util.media

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import androidx.compose.ui.graphics.asImageBitmap
import io.github.aakira.napier.Napier
import io.rudione.chatone.presentation.theme.WallpaperState
import java.io.File

class AndroidWallpaperLoader(
    private val maxDimensionPx: Int = MAX_WALLPAPER_DIMENSION_PX
) : WallpaperLoader {

    override fun load(path: String): WallpaperState? {
        if (path.isBlank()) return null
        val file = File(path)
        if (!file.isFile || !file.canRead()) return null
        return runCatching { decode(file)?.asImageBitmap()?.toWallpaperState() }
            .onFailure { Napier.w("Wallpaper decode failed: ${it.message}", tag = TAG) }
            .getOrNull()
    }

    private fun decode(file: File): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        val options = BitmapFactory.Options().apply {
            inSampleSize = wallpaperSampleSize(bounds.outWidth, bounds.outHeight, maxDimensionPx)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val bitmap = BitmapFactory.decodeFile(file.absolutePath, options) ?: return null
        return bitmap.rotatedBy(exifRotationDegrees(file))
    }

    private fun exifRotationDegrees(file: File): Float = runCatching {
        when (ExifInterface(file.absolutePath).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
    }.getOrDefault(0f)

    private fun Bitmap.rotatedBy(degrees: Float): Bitmap {
        if (degrees == 0f) return this
        val rotated = Bitmap.createBitmap(this, 0, 0, width, height, Matrix().apply { postRotate(degrees) }, true)
        if (rotated !== this) recycle()
        return rotated
    }

    private companion object {
        const val TAG = "WallpaperLoader"
        const val MAX_WALLPAPER_DIMENSION_PX = 2048
    }
}

internal fun wallpaperSampleSize(width: Int, height: Int, maxDimensionPx: Int): Int {
    val longest = maxOf(width, height)
    var sample = 1
    while (longest / sample > maxDimensionPx) sample *= 2
    return sample
}
