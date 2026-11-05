package io.rudione.chatone.presentation.ai

import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.ai_assist_error_model_local_hint
import chatone.composeapp.generated.resources.ai_assist_error_network_title
import chatone.composeapp.generated.resources.ai_assist_error_overloaded_title
import chatone.composeapp.generated.resources.ai_connect_error_title
import io.rudione.chatone.data.remote.AiFailure
import io.rudione.chatone.data.remote.AiFailureKind
import io.rudione.chatone.data.remote.AiProviders
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AiFailureTextTest {

    private val gemini = AiProviders.cloud.first { it.name.startsWith("Gemini") }

    @Test
    fun overloadedGeminiNamesGemini() {
        val message = aiFailureMessage(AiFailure(AiFailureKind.OVERLOADED, 503), gemini.baseUrl, gemini.defaultModel)

        assertEquals(Res.string.ai_assist_error_overloaded_title, message.title)
        assertTrue(message.hintArgs.any { it.contains("Gemini") })
    }

    @Test
    fun cloudNetworkErrorUsesTheCloudText() {
        val message = aiFailureMessage(AiFailure(AiFailureKind.NETWORK), gemini.baseUrl, gemini.defaultModel)

        assertEquals(Res.string.ai_assist_error_network_title, message.title)
        assertTrue(message.titleArgs.any { it.contains("Gemini") })
    }

    @Test
    fun localNetworkErrorKeepsTheOllamaHint() {
        val message = aiFailureMessage(AiFailure(AiFailureKind.NETWORK), AiProviders.LOCAL_BASE_URL, "llama3.2")

        assertEquals(Res.string.ai_connect_error_title, message.title)
        assertEquals(listOf(AiProviders.LOCAL_BASE_URL), message.hintArgs)
    }

    @Test
    fun missingLocalModelSuggestsPullingIt() {
        val message = aiFailureMessage(AiFailure(AiFailureKind.MODEL_NOT_FOUND, 404), AiProviders.LOCAL_BASE_URL, "qwen2.5:3b")

        assertEquals(Res.string.ai_assist_error_model_local_hint, message.hint)
        assertTrue(message.hintArgs.contains("qwen2.5:3b"))
    }
}
