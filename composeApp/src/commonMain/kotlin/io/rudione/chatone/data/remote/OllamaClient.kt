package io.rudione.chatone.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.preparePost
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.utils.io.readUTF8Line
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

data class OllamaInstalledModel(val name: String, val sizeBytes: Long)

data class OllamaPullEvent(
    val status: String,
    val total: Long,
    val completed: Long,
    val error: String?
) {
    val isSuccess: Boolean get() = status.contains("success", ignoreCase = true)
    val isVerifying: Boolean
        get() = status.contains("verif", ignoreCase = true) ||
                status.contains("writing", ignoreCase = true)
}

class OllamaClient(private val httpClient: HttpClient) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private fun originOf(baseUrl: String): String? =
        Regex("^(https?://[^/]+)").find(baseUrl.trim())?.groupValues?.get(1)

    suspend fun installedModels(baseUrl: String): List<OllamaInstalledModel> {
        val origin = originOf(baseUrl) ?: return emptyList()
        return try {
            val response = httpClient.get("$origin/api/tags")
            if (!response.status.isSuccess()) return emptyList()
            json.parseToJsonElement(response.bodyAsText()).jsonObject["models"]?.jsonArray
                ?.mapNotNull { el ->
                    val o = el.jsonObject
                    val name = o["name"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                    OllamaInstalledModel(name, o["size"]?.jsonPrimitive?.longOrNull ?: 0L)
                }.orEmpty()
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun deleteModel(baseUrl: String, name: String): Boolean {
        val origin = originOf(baseUrl) ?: return false
        return try {
            httpClient.delete("$origin/api/delete") {
                contentType(ContentType.Application.Json)
                setBody(
                    json.encodeToString(
                        JsonObject.serializer(),
                        buildJsonObject { put("name", name) })
                )
            }.status.isSuccess()
        } catch (_: Exception) {
            false
        }
    }

    fun pull(baseUrl: String, model: String): Flow<OllamaPullEvent> = flow {
        val origin = originOf(baseUrl) ?: throw IllegalArgumentException("Bad Ollama URL")
        httpClient.preparePost("$origin/api/pull") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(JsonObject.serializer(), buildJsonObject {
                put("name", model)
                put("stream", true)
            }))
        }.execute { response ->
            if (!response.status.isSuccess()) {
                throw IllegalStateException("Ollama pull HTTP ${response.status.value}")
            }
            val channel = response.bodyAsChannel()
            while (!channel.isClosedForRead) {
                val line = channel.readUTF8Line() ?: break
                if (line.isBlank()) continue
                val o =
                    runCatching { json.parseToJsonElement(line).jsonObject }.getOrNull() ?: continue
                emit(
                    OllamaPullEvent(
                        status = o["status"]?.jsonPrimitive?.contentOrNull ?: "",
                        total = o["total"]?.jsonPrimitive?.longOrNull ?: 0L,
                        completed = o["completed"]?.jsonPrimitive?.longOrNull ?: 0L,
                        error = o["error"]?.jsonPrimitive?.contentOrNull
                    )
                )
            }
        }
    }
}
