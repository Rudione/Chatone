package io.rudione.chatone.util.media

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.awtTransferable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.datatransfer.DataFlavor
import java.io.File

actual suspend fun readLocalFileBytes(path: String): ByteArray? = withContext(Dispatchers.IO) {
    runCatching {
        val file = File(path)
        if (file.isFile && file.canRead()) file.readBytes() else null
    }.getOrNull()
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
actual fun Modifier.externalFileDropTarget(
    enabled: Boolean,
    onFilesDropped: (List<String>) -> Unit,
    onDragStateChanged: (Boolean) -> Unit
): Modifier {
    if (!enabled) return this
    val currentOnDrop = rememberUpdatedState(onFilesDropped)
    val currentOnState = rememberUpdatedState(onDragStateChanged)
    val target = remember {
        object : DragAndDropTarget {
            override fun onEntered(event: DragAndDropEvent) {
                currentOnState.value(true)
            }

            override fun onExited(event: DragAndDropEvent) {
                currentOnState.value(false)
            }

            override fun onEnded(event: DragAndDropEvent) {
                currentOnState.value(false)
            }

            override fun onDrop(event: DragAndDropEvent): Boolean {
                currentOnState.value(false)
                val paths = runCatching {
                    val transferable = event.awtTransferable
                    if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                        @Suppress("UNCHECKED_CAST")
                        (transferable.getTransferData(DataFlavor.javaFileListFlavor) as List<File>)
                            .map { it.absolutePath }
                    } else emptyList()
                }.getOrDefault(emptyList())
                if (paths.isEmpty()) return false
                currentOnDrop.value(paths)
                return true
            }
        }
    }
    return this.dragAndDropTarget(
        shouldStartDragAndDrop = { true },
        target = target
    )
}
