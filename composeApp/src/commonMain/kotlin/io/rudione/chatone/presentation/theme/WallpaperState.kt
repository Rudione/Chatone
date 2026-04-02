package io.rudione.chatone.presentation.theme

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap

/**
 * Holds the current wallpaper state shared across the entire app.
 * Accessible via LocalWallpaper composition local.
 */
data class WallpaperState(
    val imageBitmap: ImageBitmap? = null,
    val dominantColor: Color = Color.Transparent,
    val blurRadius: Float = 12f,  // 0..40
    val isActive: Boolean = false
)

val LocalWallpaper = compositionLocalOf { WallpaperState() }

/**
 * Mutable holder — lives at the App level.
 */
class WallpaperController {
    var state by mutableStateOf(WallpaperState())
        private set

    fun update(newState: WallpaperState) {
        state = newState
    }

    fun clear() {
        state = WallpaperState()
    }
}

val LocalWallpaperController = staticCompositionLocalOf<WallpaperController> {
    error("WallpaperController not provided")
}