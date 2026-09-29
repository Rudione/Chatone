package io.rudione.chatone.data.remote

import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.rudione.chatone.util.Result
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

data class TranslationResult(
    val text: String,
    val sourceLang: String
)

class TranslationClient(
    private val httpClient: HttpClient
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun translate(
        text: String,
        targetLang: String,
        sourceLang: String = "auto"
    ): Result<TranslationResult> {
        if (text.isBlank()) return Result.Success(TranslationResult(text, sourceLang))
        return try {
            val body = httpClient.get("https://translate.googleapis.com/translate_a/single") {
                parameter("client", "gtx")
                parameter("sl", sourceLang)
                parameter("tl", targetLang)
                parameter("dt", "t")
                parameter("q", text)
            }.bodyAsText()

            val root = json.parseToJsonElement(body).jsonArray
            val segments = root[0].jsonArray
            val translated = buildString {
                segments.forEach { seg ->
                    append(seg.jsonArray[0].jsonPrimitive.contentOrNull ?: "")
                }
            }
            val detected = root.getOrNull(2)?.jsonPrimitive?.contentOrNull ?: sourceLang
            Result.Success(TranslationResult(translated, detected))
        } catch (e: Exception) {
            Napier.w("Translation failed: ${e.message}", tag = "Translation")
            Result.Error(e)
        }
    }
}
