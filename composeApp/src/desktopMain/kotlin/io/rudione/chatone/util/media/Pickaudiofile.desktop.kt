package io.rudione.chatone.util.media

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
            val source = File(dir, name)
            persistMentionSound(source) ?: source.absolutePath
        } finally {
            restore()
        }
    }.getOrNull()
}

private fun persistMentionSound(source: File): String? = runCatching {
    if (!source.exists() || !source.canRead()) return@runCatching null
    val soundsDir = File(System.getProperty("user.home"), ".chatone/sounds")
    soundsDir.mkdirs()
    val ext = source.extension.ifBlank { "wav" }
    val dest = File(soundsDir, "mention_sound_${System.currentTimeMillis()}.$ext")
    source.copyTo(dest, overwrite = true)
    soundsDir.listFiles()?.forEach { f ->
        if (f != dest && f.name.startsWith("mention_sound_")) runCatching { f.delete() }
    }
    dest.absolutePath
}.getOrNull()
