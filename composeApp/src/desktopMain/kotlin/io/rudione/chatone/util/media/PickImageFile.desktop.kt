package io.rudione.chatone.util.media

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.FileDialog
import java.awt.Frame
import java.awt.Window
import java.io.File

actual suspend fun pickImageFile(): String? = withContext(Dispatchers.IO) {
    runCatching {
        val parent = findMainFrame()
        val restore = withAlwaysOnTopSuspended(parent)
        try {
            val dialog = FileDialog(parent, "Select Background Image", FileDialog.LOAD).apply {
                val exts = setOf("jpg", "jpeg", "png", "webp", "gif", "bmp")
                if (System.getProperty("os.name", "").lowercase().contains("win")) {
                    file = "*.jpg;*.jpeg;*.png;*.webp;*.gif;*.bmp"
                } else {
                    setFilenameFilter { _, name -> name.substringAfterLast('.').lowercase() in exts }
                }
                isVisible = true
            }
            val dir = dialog.directory ?: return@runCatching null
            val name = dialog.file ?: return@runCatching null
            File(dir, name).absolutePath
        } finally {
            restore()
        }
    }.getOrNull()
}

private fun findMainFrame(): Frame? =
    Frame.getFrames().firstOrNull { it.isVisible && it.isDisplayable }

internal fun withAlwaysOnTopSuspended(vararg focusHint: Window?): () -> Unit {
    val affected = Window.getWindows()
        .filter { it.isVisible && it.isAlwaysOnTop }
        .onEach { it.isAlwaysOnTop = false }
    return {
        affected.forEach {
            try { it.isAlwaysOnTop = true } catch (_: Exception) {}
        }
    }
}
