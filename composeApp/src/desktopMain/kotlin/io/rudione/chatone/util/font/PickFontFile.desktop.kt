package io.rudione.chatone.util.font

import io.rudione.chatone.util.media.withAlwaysOnTopSuspended
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

actual suspend fun pickFontFile(): String? = withContext(Dispatchers.IO) {
    runCatching {
        val parent = Frame.getFrames().firstOrNull { it.isVisible && it.isDisplayable }
        val restore = withAlwaysOnTopSuspended(parent)
        try {
            val dialog = FileDialog(parent, "Select Font File", FileDialog.LOAD).apply {
                val exts = setOf("ttf", "otf")
                if (System.getProperty("os.name", "").lowercase().contains("win")) {
                    file = "*.ttf;*.otf"
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
