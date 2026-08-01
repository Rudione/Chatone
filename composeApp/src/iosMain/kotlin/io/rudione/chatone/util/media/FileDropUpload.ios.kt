package io.rudione.chatone.util.media

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.posix.memcpy

@OptIn(ExperimentalForeignApi::class)
actual suspend fun readLocalFileBytes(path: String): ByteArray? {
    val data: NSData = NSFileManager.defaultManager.contentsAtPath(path) ?: return null
    val length = data.length.toInt()
    if (length == 0) return ByteArray(0)
    val bytes = ByteArray(length)
    bytes.usePinned { pinned ->
        memcpy(pinned.addressOf(0), data.bytes, data.length)
    }
    return bytes
}

@Composable
actual fun Modifier.externalFileDropTarget(
    enabled: Boolean,
    onFilesDropped: (List<String>) -> Unit,
    onDragStateChanged: (Boolean) -> Unit
): Modifier = this
