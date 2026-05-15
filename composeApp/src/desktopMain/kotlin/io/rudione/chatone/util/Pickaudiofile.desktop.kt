package io.rudione.chatone.util

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
                val exts = setOf("wav", "ogg", "mp3", "aiff", "aif")
                if (System.getProperty("os.name", "").lowercase().contains("win")) {
                    file = "*.wav;*.ogg;*.mp3;*.aiff;*.aif"
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
