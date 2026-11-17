package io.rudione.chatone.util.media

import io.rudione.chatone.domain.sound.SoundLimits
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

actual suspend fun pickAudioFile(): String? = withContext(Dispatchers.IO) {
    runCatching {
        val parent = Frame.getFrames().firstOrNull { it.isVisible && it.isDisplayable }
        val restore = withAlwaysOnTopSuspended(parent)
        try {
            val dialog = FileDialog(parent, "Select Mention Sound", FileDialog.LOAD).apply {
                val exts = SoundLimits.supportedExtensions
                if (System.getProperty("os.name", "").lowercase().contains("win")) {
                    file = exts.joinToString(";") { "*.$it" }
                } else {
                    setFilenameFilter { _, name -> name.substringAfterLast('.').lowercase() in exts }
                }
                isVisible = true
            }
            val dir = dialog.directory ?: return@runCatching null
            val name = dialog.file ?: return@runCatching null
            File(dir, name).takeIf { it.isFile && it.canRead() }?.absolutePath
        } finally {
            restore()
        }
    }.getOrNull()
}
