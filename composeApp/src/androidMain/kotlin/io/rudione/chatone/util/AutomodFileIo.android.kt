package io.rudione.chatone.util

import android.app.Application
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.mp.KoinPlatform
import java.io.File


actual suspend fun saveAutomodText(defaultName: String, content: String): String? =
    withContext(Dispatchers.IO) {
        runCatching {
            val app = KoinPlatform.getKoin().get<Application>()
            val dir = app.getExternalFilesDir("automod") ?: app.filesDir.resolve("automod").also { it.mkdirs() }
            dir.mkdirs()
            val target = File(dir, defaultName)
            target.writeText(content, Charsets.UTF_8)
            target.absolutePath
        }.onFailure { Napier.e("Automod save failed", it, tag = "Automod") }.getOrNull()
    }

actual suspend fun readAutomodText(): String? =
    withContext(Dispatchers.IO) {
        runCatching {
            val app = KoinPlatform.getKoin().get<Application>()
            val dir = app.getExternalFilesDir("automod") ?: app.filesDir.resolve("automod")
            val candidates = dir.listFiles { f ->
                f.isFile && (f.extension.equals("json", true) || f.extension.equals("md", true) || f.extension.equals("txt", true))
            }.orEmpty().sortedByDescending { it.lastModified() }
            candidates.firstOrNull()?.readText(Charsets.UTF_8)
        }.onFailure { Napier.e("Automod read failed", it, tag = "Automod") }.getOrNull()
    }
