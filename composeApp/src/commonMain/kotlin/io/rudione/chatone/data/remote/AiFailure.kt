package io.rudione.chatone.data.remote

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

enum class AiFailureKind(val retryable: Boolean) {
    OVERLOADED(true),
    RATE_LIMITED(true),
    SERVER(true),
    NETWORK(true),
    UNAUTHORIZED(false),
    MODEL_NOT_FOUND(false),
    BAD_REQUEST(false),
    EMPTY_REPLY(true)
}

data class AiFailure(
    val kind: AiFailureKind,
    val status: Int? = null,
    val detail: String? = null
)

class AiRequestException(
    val failure: AiFailure,
    val retryAfterMs: Long? = null,
    cause: Throwable? = null
) : Exception("AI request failed: ${failure.kind} ${failure.status ?: ""}".trim(), cause)

internal object AiErrorParser {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val invalidKeyMarkers = listOf("API_KEY_INVALID", "API key not valid", "invalid_api_key", "Incorrect API key")
    private const val MAX_DETAIL_LENGTH = 240

    fun failureFor(status: Int, body: String): AiFailure {
        val detail = providerMessage(body)
        val kind = when {
            status == 401 || status == 403 -> AiFailureKind.UNAUTHORIZED
            invalidKeyMarkers.any { body.contains(it, ignoreCase = true) } -> AiFailureKind.UNAUTHORIZED
            status == 404 -> AiFailureKind.MODEL_NOT_FOUND
            status == 408 -> AiFailureKind.NETWORK
            status == 429 -> AiFailureKind.RATE_LIMITED
            status == 503 || status == 529 -> AiFailureKind.OVERLOADED
            status in 500..599 -> AiFailureKind.SERVER
            else -> AiFailureKind.BAD_REQUEST
        }
        return AiFailure(kind, status, detail)
    }

    fun retryAfterMs(header: String?): Long? =
        header?.trim()?.toLongOrNull()?.takeIf { it > 0 }?.times(1_000)

    private fun providerMessage(body: String): String? {
        val root = runCatching { json.parseToJsonElement(body) }.getOrNull() ?: return null
        val node = (root as? JsonArray)?.firstOrNull() ?: root
        val error = (node as? JsonObject)?.get("error") ?: return null
        val message = when (error) {
            is JsonObject -> error["message"].text()
            is JsonPrimitive -> error.contentOrNull
            else -> null
        }
        return message?.trim()?.takeIf { it.isNotEmpty() }?.take(MAX_DETAIL_LENGTH)
    }

    private fun JsonElement?.text(): String? = (this as? JsonPrimitive)?.contentOrNull
}
