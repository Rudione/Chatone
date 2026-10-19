package io.rudione.chatone.presentation.ai

import io.rudione.chatone.data.remote.AiFailure
import io.rudione.chatone.data.remote.AiFailureKind
import io.rudione.chatone.data.remote.AiProviders
import io.rudione.chatone.presentation.theme.i18n.StringsRu
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AiFailureTextTest {

    private val gemini = AiProviders.cloud.first { it.name.startsWith("Gemini") }

    @Test
    fun overloadedGeminiNamesGeminiAndNotOllama() {
        val text = aiFailureText(AiFailure(AiFailureKind.OVERLOADED, 503), gemini.baseUrl, gemini.defaultModel, StringsRu)

        assertEquals(StringsRu.aiAssist.errorOverloadedTitle, text.title)
        assertTrue(text.hint.contains("Gemini"))
        assertFalse(text.hint.contains("Ollama"))
    }

    @Test
    fun cloudNetworkErrorDoesNotMentionOllama() {
        val text = aiFailureText(AiFailure(AiFailureKind.NETWORK), gemini.baseUrl, gemini.defaultModel, StringsRu)

        assertTrue(text.title.contains("Gemini"))
        assertFalse(text.hint.contains("Ollama"))
    }

    @Test
    fun localNetworkErrorKeepsTheOllamaHint() {
        val text = aiFailureText(AiFailure(AiFailureKind.NETWORK), AiProviders.LOCAL_BASE_URL, "llama3.2", StringsRu)

        assertEquals(StringsRu.aiConnectErrorTitle, text.title)
        assertTrue(text.hint.contains("Ollama"))
    }

    @Test
    fun missingLocalModelSuggestsPullingIt() {
        val text = aiFailureText(AiFailure(AiFailureKind.MODEL_NOT_FOUND, 404), AiProviders.LOCAL_BASE_URL, "qwen2.5:3b", StringsRu)

        assertTrue(text.hint.contains("ollama pull qwen2.5:3b"))
    }
}
