package io.rudione.chatone.util

import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

/**
 * Opens JFileChooser synchronously.
 * Compose Desktop runs on AWT/EDT, so we call showOpenDialog directly
 * without invokeLater — no deadlock, no latch needed.
 */
actual fun pickAudioFile(): String? {
    val chooser = JFileChooser(File(System.getProperty("user.home"))).apply {
        dialogTitle = "Select Mention Sound"
        fileSelectionMode = JFileChooser.FILES_ONLY
        isAcceptAllFileFilterUsed = false
        addChoosableFileFilter(
            FileNameExtensionFilter("Audio files (WAV, OGG)", "wav", "ogg")
        )
    }
    return if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
        chooser.selectedFile?.absolutePath
    } else null
}