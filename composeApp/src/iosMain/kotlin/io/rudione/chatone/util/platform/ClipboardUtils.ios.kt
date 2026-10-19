package io.rudione.chatone.util.platform

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.text.AnnotatedString

@OptIn(ExperimentalComposeUiApi::class)
actual fun AnnotatedString.toClipEntry(isSensitive: Boolean): ClipEntry {
    return ClipEntry.withPlainText(this.text)
}

@OptIn(ExperimentalComposeUiApi::class)
actual suspend fun ClipEntry.readText(): String? {
    return getPlainText()
}
