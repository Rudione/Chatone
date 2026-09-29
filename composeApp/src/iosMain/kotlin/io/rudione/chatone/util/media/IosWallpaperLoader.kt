package io.rudione.chatone.util.media

import androidx.compose.ui.graphics.toComposeImageBitmap
import io.rudione.chatone.presentation.theme.WallpaperState
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import org.jetbrains.skia.Image
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.posix.memcpy

class IosWallpaperLoader : WallpaperLoader {

    override fun load(path: String): WallpaperState? {
        if (path.isBlank()) return null
        val bytes = readBytes(path) ?: return null
        return runCatching { Image.makeFromEncoded(bytes).toComposeImageBitmap().toWallpaperState() }.getOrNull()
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun readBytes(path: String): ByteArray? {
        val data: NSData = NSFileManager.defaultManager.contentsAtPath(path) ?: return null
        val length = data.length.toInt()
        if (length == 0) return null
        return ByteArray(length).also { bytes ->
            bytes.usePinned { pinned -> memcpy(pinned.addressOf(0), data.bytes, data.length) }
        }
    }
}
