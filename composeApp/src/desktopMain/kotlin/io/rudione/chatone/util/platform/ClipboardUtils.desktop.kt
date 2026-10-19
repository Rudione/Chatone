package io.rudione.chatone.util.platform

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.text.AnnotatedString
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.awt.datatransfer.Transferable

@OptIn(ExperimentalComposeUiApi::class)
actual fun AnnotatedString.toClipEntry(isSensitive: Boolean): ClipEntry {
    return ClipEntry(StringSelection(this.text))
}

@OptIn(ExperimentalComposeUiApi::class)
actual suspend fun ClipEntry.readText(): String? {
    val transferable = (nativeClipEntry as? Transferable) ?: return null
    if (!transferable.isDataFlavorSupported(DataFlavor.stringFlavor)) return null
    return runCatching {
        transferable.getTransferData(DataFlavor.stringFlavor) as? String
    }.getOrNull()
}
