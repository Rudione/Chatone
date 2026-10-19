package io.rudione.chatone.data.remote

import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.rudione.chatone.util.Result
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

data class AiChatMessage(val role: String, val content: String) {
    companion object {
        const val SYSTEM = "system"
        const val USER = "user"
        const val ASSISTANT = "assistant"
    }
}

class AiAssistantClient(private val httpClient: HttpClient) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun complete(
        baseUrl: String,
        model: String,
        messages: List<AiChatMessage>,
        temperature: Double = 0.7,
        apiKey: String = ""
    ): Result<String> {
        val primary = completeWithRetries(baseUrl, model, messages, temperature, apiKey, MAX_ATTEMPTS)
        if (primary.failureKind() != AiFailureKind.OVERLOADED) return primary
        val fallback = AiProviders.overloadFallback(baseUrl, model) ?: return primary
        Napier.i("AI model $model is overloaded, asking $fallback", tag = TAG)
        val reserve = completeWithRetries(baseUrl, fallback, messages, temperature, apiKey, FALLBACK_ATTEMPTS)
        return if (reserve is Result.Success) reserve else primary
    }

    private suspend fun completeWithRetries(
        baseUrl: String,
        model: String,
        messages: List<AiChatMessage>,
        temperature: Double,
        apiKey: String,
        attempts: Int
    ): Result<String> {
        var last: Result<String> = Result.Error(AiRequestException(AiFailure(AiFailureKind.NETWORK)))
        for (attempt in 0 until attempts) {
            if (attempt > 0) delay(retryDelayMs(last, attempt))
            last = completeOnce(baseUrl, model, messages, temperature, apiKey)
            val retryable = last.failureKind()?.retryable ?: false
            if (!retryable) return last
        }
        return last
    }

    private fun retryDelayMs(previous: Result<String>, attempt: Int): Long {
        val requested = ((previous as? Result.Error)?.exception as? AiRequestException)?.retryAfterMs
        return (requested ?: (RETRY_STEP_MS * attempt)).coerceAtMost(MAX_RETRY_DELAY_MS)
    }

    private fun Result<String>.failureKind(): AiFailureKind? =
        ((this as? Result.Error)?.exception as? AiRequestException)?.failure?.kind

    private suspend fun completeOnce(
        baseUrl: String,
        model: String,
        messages: List<AiChatMessage>,
        temperature: Double,
        apiKey: String
    ): Result<String> {
        return try {
            val url = baseUrl.trimEnd('/') + "/chat/completions"
            val body = buildJsonObject {
                put("model", model)
                put("temperature", temperature)
                put("stream", false)
                putJsonArray("messages") {
                    messages.forEach { m ->
                        addJsonObject {
                            put("role", m.role)
                            put("content", m.content)
                        }
                    }
                }
            }
            val response = httpClient.post(url) {
                if (apiKey.isNotBlank()) header("Authorization", "Bearer $apiKey")
                contentType(ContentType.Application.Json)
                setBody(
                    json.encodeToString(
                        kotlinx.serialization.json.JsonObject.serializer(),
                        body
                    )
                )
            }
            val text = response.bodyAsText()
            if (!response.status.isSuccess()) {
                val failure = AiErrorParser.failureFor(response.status.value, text)
                Napier.w("AI HTTP ${failure.status} (${failure.kind}) for $model: ${failure.detail ?: text.take(300)}", tag = TAG)
                val retryAfter = AiErrorParser.retryAfterMs(response.headers[HttpHeaders.RetryAfter])
                return Result.Error(AiRequestException(failure, retryAfter))
            }
            val content = runCatching {
                json.parseToJsonElement(text).jsonObject
                    .get("choices")?.jsonArray?.firstOrNull()?.jsonObject
                    ?.get("message")?.jsonObject
                    ?.get("content")?.jsonPrimitive?.contentOrNull
            }.getOrNull()
            if (content.isNullOrBlank()) {
                Result.Error(AiRequestException(AiFailure(AiFailureKind.EMPTY_REPLY, response.status.value)))
            } else {
                Result.Success(content.trim())
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Napier.w("AI request failed: ${e.message}", tag = TAG)
            Result.Error(AiRequestException(AiFailure(AiFailureKind.NETWORK, detail = e.message), cause = e))
        }
    }

    sealed interface ServerStatus {
        data class Reachable(val models: List<String>) : ServerStatus
        data object Unreachable : ServerStatus
    }

    suspend fun probe(baseUrl: String, apiKey: String = ""): ServerStatus {
        val base = baseUrl.trimEnd('/')
        val origin = Regex("^(https?://[^/]+)").find(base)?.groupValues?.get(1)
        val urls = buildList {
            add("$base/models")
            if (origin != null && origin != base) add("$origin/models")
        }
        var reachable = false
        for (url in urls) {
            try {
                val response = httpClient.get(url) {
                    if (apiKey.isNotBlank()) header("Authorization", "Bearer $apiKey")
                }
                reachable = true
                if (response.status.isSuccess()) {
                    val models = parseModelList(response.bodyAsText())
                    if (models.isNotEmpty()) return ServerStatus.Reachable(models)
                }
            } catch (_: Exception) {

            }
        }
        return if (reachable) ServerStatus.Reachable(emptyList()) else ServerStatus.Unreachable
    }

    suspend fun listModels(baseUrl: String, apiKey: String = ""): List<String> {
        val fromBase = fetchModels(baseUrl.trimEnd('/') + "/models", apiKey)
        if (fromBase.isNotEmpty()) return fromBase

        val origin = Regex("^(https?://[^/]+)").find(baseUrl)?.groupValues?.get(1)
            ?: return emptyList()
        return fetchModels("$origin/models", apiKey)
    }

    private suspend fun fetchModels(url: String, apiKey: String): List<String> {
        return try {
            val response = httpClient.get(url) {
                if (apiKey.isNotBlank()) header("Authorization", "Bearer $apiKey")
            }
            if (!response.status.isSuccess()) return emptyList()
            parseModelList(response.bodyAsText())
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parseModelList(text: String): List<String> {
        val root = runCatching { json.parseToJsonElement(text) }.getOrNull() ?: return emptyList()
        return when {
            root is kotlinx.serialization.json.JsonObject ->
                root["data"]?.jsonArray
                    ?.mapNotNull { it.jsonObject["id"]?.jsonPrimitive?.contentOrNull }
                    .orEmpty()

            root is kotlinx.serialization.json.JsonArray ->
                root.mapNotNull { el ->
                    when (el) {
                        is kotlinx.serialization.json.JsonObject ->
                            el["name"]?.jsonPrimitive?.contentOrNull
                                ?: el["id"]?.jsonPrimitive?.contentOrNull

                        else -> el.jsonPrimitive.contentOrNull
                    }
                }

            else -> emptyList()
        }
    }

    companion object {
        private const val TAG = "AiAssistant"
        private const val MAX_ATTEMPTS = 3
        private const val FALLBACK_ATTEMPTS = 2
        private const val RETRY_STEP_MS = 2_500L
        private const val MAX_RETRY_DELAY_MS = 10_000L
        const val DEFAULT_BASE_URL = AiProviders.LOCAL_BASE_URL
        const val DEFAULT_MODEL = "llama3.2"
    }
}
