package io.rudione.chatone.util.media

import androidx.compose.ui.graphics.toComposeImageBitmap
import io.rudione.chatone.presentation.theme.WallpaperState
import org.jetbrains.skia.Image
import java.io.File

class DesktopWallpaperLoader : WallpaperLoader {

    override fun load(path: String): WallpaperState? {
        if (path.isBlank()) return null
        val file = File(path)
        if (!file.exists() || !file.canRead()) return null

        return try {
            Image.makeFromEncoded(file.readBytes()).toComposeImageBitmap().toWallpaperState()
        } catch (_: Exception) {
            null
        }
    }
}
