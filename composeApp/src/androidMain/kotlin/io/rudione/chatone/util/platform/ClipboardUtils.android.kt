package io.rudione.chatone.util.platform

import android.content.ClipData
import android.content.ClipDescription
import android.os.Build
import android.os.PersistableBundle
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.text.AnnotatedString

actual fun AnnotatedString.toClipEntry(isSensitive: Boolean): ClipEntry {
    val clipData = ClipData.newPlainText("text", this.text)
    if (isSensitive && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        clipData.description.extras = PersistableBundle().apply {
            putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
        }
    }
    return ClipEntry(clipData)
}

actual suspend fun ClipEntry.readText(): String? {
    if (clipData.itemCount == 0) return null
    return clipData.getItemAt(0)?.text?.toString()
}
