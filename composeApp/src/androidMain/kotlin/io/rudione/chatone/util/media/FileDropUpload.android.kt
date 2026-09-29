package io.rudione.chatone.util.media

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

actual suspend fun readLocalFileBytes(path: String): ByteArray? = withContext(Dispatchers.IO) {
    runCatching {
        val file = File(path)
        if (file.isFile && file.canRead()) file.readBytes() else null
    }.getOrNull()
}

@Composable
actual fun Modifier.externalFileDropTarget(
    enabled: Boolean,
    onFilesDropped: (List<String>) -> Unit,
    onDragStateChanged: (Boolean) -> Unit
): Modifier = this
