package io.rudione.chatone.util.platform

import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.text.AnnotatedString
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CancellationException

expect fun AnnotatedString.toClipEntry(isSensitive: Boolean = false): ClipEntry
expect suspend fun ClipEntry.readText(): String?

suspend fun Clipboard.setText(text: String, isSensitive: Boolean = false) {
    guarded("write") { setClipEntry(AnnotatedString(text).toClipEntry(isSensitive)) }
}

suspend fun Clipboard.readText(): String? = guarded("read") { getClipEntry()?.readText() }

suspend fun Clipboard.clear() {
    guarded("clear") { setClipEntry(null) }
}

private inline fun <T> guarded(operation: String, block: () -> T): T? = try {
    block()
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    Napier.w("Clipboard $operation failed: ${e.message}", tag = "Clipboard")
    null
}
