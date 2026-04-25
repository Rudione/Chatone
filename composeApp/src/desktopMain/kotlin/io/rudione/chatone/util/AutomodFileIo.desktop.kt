package io.rudione.chatone.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

actual suspend fun saveAutomodText(defaultName: String, content: String): String? =
    withContext(Dispatchers.IO) {
        runCatching {
            val dialog = FileDialog(null as Frame?, "Export automod rules", FileDialog.SAVE).apply {
                file = defaultName
                isVisible = true
            }
            val dir = dialog.directory ?: return@withContext null
            val name = dialog.file ?: return@withContext null
            val target = File(dir, name)
            target.writeText(content, Charsets.UTF_8)
            target.absolutePath
        }.getOrNull()
    }

actual suspend fun readAutomodText(): String? =
    withContext(Dispatchers.IO) {
        runCatching {
            val dialog = FileDialog(null as Frame?, "Import automod rules", FileDialog.LOAD).apply {
                setFilenameFilter { _, n -> n.endsWith(".json", true) || n.endsWith(".md", true) || n.endsWith(".txt", true) }
                isVisible = true
            }
            val dir = dialog.directory ?: return@withContext null
            val name = dialog.file ?: return@withContext null
            File(dir, name).readText(Charsets.UTF_8)
        }.getOrNull()
    }
