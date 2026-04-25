package io.rudione.chatone.util

import kotlinx.coroutines.suspendCancellableCoroutine
import java.awt.Frame
import java.awt.Window
import java.io.File
import javax.swing.JFileChooser
import javax.swing.SwingUtilities
import javax.swing.filechooser.FileNameExtensionFilter
import kotlin.coroutines.resume

actual suspend fun pickImageFile(): String? = suspendCancellableCoroutine { cont ->
    SwingUtilities.invokeLater {
        val parent = findMainFrame()
        val restoredAlwaysOnTop = withAlwaysOnTopSuspended(parent)
        try {
            val chooser = JFileChooser(File(System.getProperty("user.home"))).apply {
                dialogTitle = "Select Background Image"
                fileSelectionMode = JFileChooser.FILES_ONLY
                isAcceptAllFileFilterUsed = false
                addChoosableFileFilter(
                    FileNameExtensionFilter(
                        "Images (JPG, PNG, WebP, GIF, BMP)",
                        "jpg", "jpeg", "png", "webp", "gif", "bmp"
                    )
                )
            }
            val result = chooser.showOpenDialog(parent)
            if (result == JFileChooser.APPROVE_OPTION) {
                cont.resume(chooser.selectedFile?.absolutePath)
            } else {
                cont.resume(null)
            }
        } catch (e: Exception) {
            cont.resume(null)
        } finally {
            restoredAlwaysOnTop()
        }
    }
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