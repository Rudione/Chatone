package io.rudione.chatone.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import io.rudione.chatone.util.Result
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class AiAssistantClientTest {

    private val json = headersOf(HttpHeaders.ContentType, "application/json")
    private val requestedModels = mutableListOf<String>()
    private val messages = listOf(AiChatMessage(AiChatMessage.USER, "hi"))

    private fun client(answer: (model: String) -> Pair<HttpStatusCode, String>) =
        AiAssistantClient(HttpClient(MockEngine { request ->
            val body = (request.body as TextContent).text
            val model = Regex("\"model\":\"([^\"]+)\"").find(body)!!.groupValues[1]
            requestedModels += model
            val (status, text) = answer(model)
            respond(text, status, json)
        }))

    private fun reply(text: String) = HttpStatusCode.OK to """{"choices":[{"message":{"content":"$text"}}]}"""

    @Test
    fun overloadedGeminiFallsBackToTheLighterModel() = runTest {
        val gemini = AiProviders.cloud.first { it.name.startsWith("Gemini") }
        val ai = client { model ->
            if (model == gemini.defaultModel) HttpStatusCode.ServiceUnavailable to """[{"error":{"code":503,"message":"busy"}}]"""
            else reply("fallback answer")
        }

        val result = ai.complete(gemini.baseUrl, gemini.defaultModel, messages, apiKey = "key")

        assertEquals(Result.Success("fallback answer"), result)
        assertEquals(List(3) { gemini.defaultModel } + gemini.overloadFallbackModel!!, requestedModels)
    }

    @Test
    fun rejectedKeyIsNotRetried() = runTest {
        val ai = client { HttpStatusCode.Unauthorized to """{"error":{"message":"bad key"}}""" }

        val result = ai.complete("https://api.groq.com/openai/v1", "llama", messages, apiKey = "key")

        val failure = assertIs<AiRequestException>(assertIs<Result.Error>(result).exception).failure
        assertEquals(AiFailureKind.UNAUTHORIZED, failure.kind)
        assertEquals(1, requestedModels.size)
    }

    @Test
    fun overloadedProviderWithoutFallbackReportsOverload() = runTest {
        val ai = client { HttpStatusCode.ServiceUnavailable to "" }

        val result = ai.complete("https://api.groq.com/openai/v1", "llama", messages, apiKey = "key")

        val failure = assertIs<AiRequestException>(assertIs<Result.Error>(result).exception).failure
        assertEquals(AiFailureKind.OVERLOADED, failure.kind)
        assertTrue(requestedModels.all { it == "llama" })
        assertEquals(3, requestedModels.size)
    }

    @Test
    fun unreachableServerIsANetworkFailure() = runTest {
        val ai = AiAssistantClient(HttpClient(MockEngine { error("connection refused") }))

        val result = ai.complete(AiProviders.LOCAL_BASE_URL, "llama3.2", messages)

        val failure = assertIs<AiRequestException>(assertIs<Result.Error>(result).exception).failure
        assertEquals(AiFailureKind.NETWORK, failure.kind)
    }
}
