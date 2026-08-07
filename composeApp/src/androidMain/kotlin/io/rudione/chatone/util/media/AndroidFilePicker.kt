package io.rudione.chatone.util.media

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CompletableDeferred
import java.io.File

object AndroidFilePicker {

    private const val TAG = "AndroidFilePicker"

    private var launcher: ((Array<String>) -> Unit)? = null
    private var pending: CompletableDeferred<String?>? = null
    private var pendingFolder: String = "picked"

    fun attach(launch: (Array<String>) -> Unit) {
        launcher = launch
    }

    fun detach() {
        launcher = null
        pending?.complete(null)
        pending = null
    }

    suspend fun pick(mimeTypes: Array<String>, folder: String): String? {
        val launch = launcher
        if (launch == null) {
            Napier.w("File picker requested before the activity was ready", tag = TAG)
            return null
        }
        pending?.complete(null)
        val deferred = CompletableDeferred<String?>()
        pending = deferred
        pendingFolder = folder
        return try {
            launch(mimeTypes)
            deferred.await()
        } catch (e: Exception) {
            Napier.e("File picker failed: ${e.message}", e, tag = TAG)
            pending = null
            null
        }
    }

    fun deliver(context: Context, uri: Uri?) {
        val deferred = pending ?: return
        pending = null
        if (uri == null) {
            deferred.complete(null)
            return
        }
        deferred.complete(copyToCache(context, uri, pendingFolder))
    }

    private fun copyToCache(context: Context, uri: Uri, folder: String): String? = runCatching {
        val name = displayName(context, uri) ?: "file_${System.currentTimeMillis()}"
        val dir = File(context.cacheDir, folder).apply { mkdirs() }
        val target = File(dir, sanitize(name))
        context.contentResolver.openInputStream(uri)?.use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        } ?: return null
        target.absolutePath
    }.onFailure { Napier.e("Copying picked file failed: ${it.message}", it, tag = TAG) }.getOrNull()

    private fun displayName(context: Context, uri: Uri): String? = runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            } ?: uri.lastPathSegment?.substringAfterLast('/')
    }.getOrNull()

    private fun sanitize(name: String): String =
        name.replace(Regex("[^A-Za-z0-9._-]"), "_").take(120).ifBlank { "file" }
}
