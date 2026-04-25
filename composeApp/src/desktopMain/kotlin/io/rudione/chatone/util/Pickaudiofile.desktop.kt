
package io.rudione.chatone.util

import java.awt.Frame
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

actual fun pickAudioFile(): String? {
    val parent = Frame.getFrames().firstOrNull { it.isVisible && it.isDisplayable }
    val restore = withAlwaysOnTopSuspended(parent)
    try {
        val chooser = JFileChooser(File(System.getProperty("user.home"))).apply {
            dialogTitle = "Select Mention Sound"
            fileSelectionMode = JFileChooser.FILES_ONLY
            isAcceptAllFileFilterUsed = false
            addChoosableFileFilter(
                FileNameExtensionFilter("Audio files (WAV, OGG)", "wav", "ogg")
            )
        }
        return if (chooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
            chooser.selectedFile?.absolutePath
        } else null
    } finally {
        restore()
    }
}
