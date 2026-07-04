package io.rudione.chatone.presentation.chat

import androidx.compose.runtime.mutableStateMapOf
import io.rudione.chatone.data.remote.TranslationClient
import io.rudione.chatone.util.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

sealed interface TranslationUiState {
    data object Loading : TranslationUiState
    data class Done(val text: String, val sourceLang: String, val targetLang: String) : TranslationUiState
    data class Error(val message: String) : TranslationUiState
}

class TranslationStore(
    private val client: TranslationClient
) {
    var targetLang: String = "en"

    val states = mutableStateMapOf<String, TranslationUiState>()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun toggle(messageId: String, text: String) {
        if (states.containsKey(messageId)) {
            states.remove(messageId)
            return
        }
        translateTo(messageId, text, targetLang)
    }

    fun translateTo(messageId: String, text: String, lang: String) {
        if (text.isBlank()) return
        states[messageId] = TranslationUiState.Loading
        scope.launch {
            when (val r = client.translate(text, lang)) {
                is Result.Success ->
                    states[messageId] = TranslationUiState.Done(r.data.text, r.data.sourceLang, lang)
                is Result.Error ->
                    states[messageId] = TranslationUiState.Error(r.exception.message ?: "error")
                Result.Loading -> Unit
            }
        }
    }
}

val TranslationLanguages = listOf(
    "en" to "English",
    "ru" to "Русский",
    "es" to "Español",
    "de" to "Deutsch",
    "fr" to "Français",
    "pt" to "Português",
    "it" to "Italiano",
    "pl" to "Polski",
    "tr" to "Türkçe",
    "uk" to "Українська",
    "ja" to "日本語",
    "ko" to "한국어",
    "zh-CN" to "中文",
    "ar" to "العربية",
)
