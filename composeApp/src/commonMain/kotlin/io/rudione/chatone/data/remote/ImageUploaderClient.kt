package io.rudione.chatone.data.remote

import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.plugins.onUpload
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.header
import io.ktor.client.request.head
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import io.rudione.chatone.domain.model.ImageUploaderConfig
import kotlin.time.Clock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

data class UploadedFile(
    val link: String,
    val deletionLink: String? = null
)

data class UploaderStatus(
    val reachable: Boolean,
    val httpCode: Int? = null,
    val latencyMs: Long? = null,
    val error: String? = null
)

class ImageUploaderClient(private val httpClient: HttpClient) {

    companion object {
        private const val TAG = "ImageUploader"
        private val json = Json { ignoreUnknownKeys = true; isLenient = true }

        fun mimeTypeFor(fileName: String): String =
            when (fileName.substringAfterLast('.', "").lowercase()) {
                "png" -> "image/png"
                "jpg", "jpeg" -> "image/jpeg"
                "gif" -> "image/gif"
                "webp" -> "image/webp"
                "bmp" -> "image/bmp"
                "avif" -> "image/avif"
                "mp4" -> "video/mp4"
                "webm" -> "video/webm"
                "mov" -> "video/quicktime"
                "mkv" -> "video/x-matroska"
                else -> "application/octet-stream"
            }

        fun isSupportedFile(fileName: String): Boolean =
            mimeTypeFor(fileName) != "application/octet-stream"

        fun parseExtraHeaders(raw: String): List<Pair<String, String>> =
            raw.split(';', '\n')
                .mapNotNull { entry ->
                    val idx = entry.indexOf(':')
                    if (idx <= 0) return@mapNotNull null
                    val name = entry.substring(0, idx).trim()
                    val value = entry.substring(idx + 1).trim()
                    if (name.isEmpty() || value.isEmpty()) null else name to value
                }

        fun resolveLink(format: String, body: String): String? {
            val trimmed = body.trim()
            if (format.isBlank()) return trimmed.takeIf { it.isNotEmpty() }
            val root: JsonElement? = runCatching { json.parseToJsonElement(trimmed) }.getOrNull()
            var missing = false
            val result = Regex("\\{([^{}]+)}").replace(format) { match ->
                val path = match.groupValues[1]
                val value = root?.let { valueAtPath(it, path) }
                when {
                    value != null -> value
                    path == "url" -> trimmed
                    else -> {
                        missing = true; ""
                    }
                }
            }
            return result.takeIf { !missing && it.isNotBlank() }
        }

        private fun valueAtPath(root: JsonElement, path: String): String? {
            var node: JsonElement = root
            for (part in path.split('.')) {
                node = when (node) {
                    is JsonObject -> node[part] ?: return null
                    is JsonArray -> part.toIntOrNull()?.let { node.getOrNull(it) } ?: return null
                    else -> return null
                }
            }
            return (node as? JsonPrimitive)?.content
        }
    }

    suspend fun upload(
        config: ImageUploaderConfig,
        fileName: String,
        bytes: ByteArray,
        onProgress: (Float) -> Unit = {}
    ): Result<UploadedFile> = runCatching {
        require(config.isUsable) { "Uploader is not configured" }
        Napier.d("Uploading $fileName (${bytes.size} bytes) to ${config.requestUrl}", tag = TAG)

        val response = httpClient.submitFormWithBinaryData(
            url = config.requestUrl,
            formData = formData {
                append(
                    config.formField.trim(),
                    bytes,
                    Headers.build {
                        append(HttpHeaders.ContentType, mimeTypeFor(fileName))
                        append(HttpHeaders.ContentDisposition, "filename=\"$fileName\"")
                    }
                )
            }
        ) {
            parseExtraHeaders(config.extraHeaders).forEach { (name, value) ->
                if (!name.equals(HttpHeaders.ContentType, ignoreCase = true) &&
                    !name.equals(HttpHeaders.ContentLength, ignoreCase = true)
                ) {
                    header(name, value)
                }
            }
            onUpload { sent, total ->
                val t = total ?: bytes.size.toLong()
                if (t > 0) onProgress((sent.toFloat() / t).coerceIn(0f, 1f))
            }
        }

        val body = response.bodyAsText()
        if (!response.status.isSuccess()) {
            Napier.w("Upload failed: HTTP ${response.status.value}: ${body.take(300)}", tag = TAG)
            throw IllegalStateException("HTTP ${response.status.value}")
        }

        val link = resolveLink(config.linkFormat, body)
            ?: throw IllegalStateException("Cannot extract link from response: ${body.take(200)}")
        val deletion = config.deletionLinkFormat
            .takeIf { it.isNotBlank() }
            ?.let { resolveLink(it, body) }

        Napier.d("Uploaded: $link", tag = TAG)
        UploadedFile(link = link, deletionLink = deletion)
    }

    suspend fun checkStatus(config: ImageUploaderConfig): UploaderStatus {
        if (config.requestUrl.isBlank()) {
            return UploaderStatus(reachable = false, error = "Request URL is empty")
        }
        val start = Clock.System.now().toEpochMilliseconds()
        return try {
            val response = httpClient.head(config.requestUrl) {
                parseExtraHeaders(config.extraHeaders).forEach { (name, value) ->
                    header(
                        name,
                        value
                    )
                }
            }
            val latency = Clock.System.now().toEpochMilliseconds() - start
            UploaderStatus(reachable = true, httpCode = response.status.value, latencyMs = latency)
        } catch (e: Exception) {
            UploaderStatus(reachable = false, error = e.message ?: e::class.simpleName)
        }
    }
}
