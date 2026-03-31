package io.rudione.chatone.util

import java.io.File
import javax.swing.JFileChooser
import javax.swing.SwingUtilities
import javax.swing.filechooser.FileNameExtensionFilter
import java.util.concurrent.CountDownLatch

actual fun pickAudioFile(): String? {
    var result: String? = null
    val latch = CountDownLatch(1)

    SwingUtilities.invokeLater {
        val chooser = JFileChooser().apply {
            dialogTitle = "Select Mention Sound"
            fileSelectionMode = JFileChooser.FILES_ONLY
            isAcceptAllFileFilterUsed = false
            addChoosableFileFilter(
                FileNameExtensionFilter("Audio files (MP3, WAV, OGG)", "mp3", "wav", "ogg")
            )
            // Start from user home or last used dir
            currentDirectory = File(System.getProperty("user.home"))
        }

        val returnCode = chooser.showOpenDialog(null)
        if (returnCode == JFileChooser.APPROVE_OPTION) {
            result = chooser.selectedFile?.absolutePath
        }
        latch.countDown()
    }

    latch.await()
    return result
}