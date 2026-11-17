package io.rudione.chatone.data.repository

import io.github.aakira.napier.Napier
import io.rudione.chatone.domain.moderation.ForbiddenCategory
import io.rudione.chatone.domain.moderation.ForbiddenScript
import io.rudione.chatone.domain.moderation.ForbiddenTerm
import io.rudione.chatone.domain.moderation.ForbiddenWordScanner
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class ForbiddenWordsRepository(scope: CoroutineScope, private val read: suspend () -> ByteArray) {

    private val loaded = MutableStateFlow<ForbiddenWordScanner?>(null)
    val scanner: StateFlow<ForbiddenWordScanner?> = loaded.asStateFlow()

    init {
        scope.launch { loaded.value = ForbiddenWordScanner(load()) }
    }

    private suspend fun load(): List<ForbiddenTerm> = try {
        ForbiddenDictionaryParser.parse(read().decodeToString())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Napier.w("Forbidden words dictionary unavailable: ${e.message}", tag = TAG)
        emptyList()
    }

    companion object {
        const val DICTIONARY_FILE = "files/moderation/forbidden_words.json"
        private const val TAG = "ForbiddenWords"
    }
}

internal object ForbiddenDictionaryParser {

    private val json = Json { ignoreUnknownKeys = true }

    fun parse(text: String): List<ForbiddenTerm> {
        val dictionary = json.decodeFromString<DictionaryDto>(text)
        return dictionary.terms.mapNotNull { term -> term.toDomain(dictionary.placeholders) }
    }

    private fun TermDto.toDomain(placeholders: Map<String, Map<String, String>>): ForbiddenTerm? {
        val parsedCategory = ForbiddenCategory.entries.firstOrNull { it.name.equals(category, ignoreCase = true) }
        val parsedScript = ForbiddenScript.entries.firstOrNull { it.name.equals(script, ignoreCase = true) }
        if (parsedCategory == null || parsedScript == null) {
            Napier.w("Skipping forbidden term with category=$category script=$script", tag = "ForbiddenWords")
            return null
        }
        val expanded = placeholders[script.lowercase()].orEmpty().entries
            .fold(pattern) { acc, (name, value) -> acc.replace("{$name}", value) }
        val regex = runCatching { Regex(expanded) }.getOrElse {
            Napier.w("Skipping invalid forbidden pattern $pattern: ${it.message}", tag = "ForbiddenWords")
            return null
        }
        return ForbiddenTerm(parsedCategory, parsedScript, regex, collapsesRepeats = collapse)
    }

    @Serializable
    private class DictionaryDto(
        val version: Int = 0,
        val placeholders: Map<String, Map<String, String>> = emptyMap(),
        val terms: List<TermDto> = emptyList()
    )

    @Serializable
    private class TermDto(
        val category: String,
        val script: String,
        val pattern: String,
        val collapse: Boolean = true
    )
}
