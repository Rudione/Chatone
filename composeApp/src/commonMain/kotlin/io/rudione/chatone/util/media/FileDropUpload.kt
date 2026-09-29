package io.rudione.chatone.util.media

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

expect suspend fun readLocalFileBytes(path: String): ByteArray?

@Composable
expect fun Modifier.externalFileDropTarget(
    enabled: Boolean,
    onFilesDropped: (List<String>) -> Unit,
    onDragStateChanged: (Boolean) -> Unit = {}
): Modifier
