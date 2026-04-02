package io.rudione.chatone.util

import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import javax.swing.JFileChooser
import javax.swing.SwingUtilities
import javax.swing.filechooser.FileNameExtensionFilter
import kotlin.coroutines.resume

actual suspend fun pickImageFile(): String? = suspendCancellableCoroutine { cont ->
    SwingUtilities.invokeLater {
        try {
            val chooser = JFileChooser(File(System.getProperty("user.home"))).apply {
                dialogTitle = "Select Background Image"
                fileSelectionMode = JFileChooser.FILES_ONLY
                isAcceptAllFileFilterUsed = false

                addChoosableFileFilter(
                    FileNameExtensionFilter(
                        "Images (JPG, PNG, WebP, GIF)",
                        "jpg", "jpeg", "png", "webp", "gif", "bmp"
                    )
                )
            }

            val result = chooser.showOpenDialog(null)

            if (result == JFileChooser.APPROVE_OPTION) {
                cont.resume(chooser.selectedFile?.absolutePath)
            } else {
                cont.resume(null)
            }
        } catch (e: Exception) {
            cont.resume(null)
        }
    }
}