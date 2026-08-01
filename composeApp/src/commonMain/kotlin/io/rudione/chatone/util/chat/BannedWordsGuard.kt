package io.rudione.chatone.util.chat

object BannedWordsGuard {

    private val LEET_MAP = mapOf(
        '0' to 'o', '1' to 'i', '3' to 'e', '4' to 'a',
        '5' to 's', '7' to 't', '@' to 'a', '$' to 's', '|' to 'i'
    )

    private val CYRILLIC_LOOKALIKE_MAP = mapOf(
        'а' to 'a', 'е' to 'e', 'ё' to 'e', 'о' to 'o', 'р' to 'p',
        'с' to 'c', 'х' to 'x', 'у' to 'y', 'к' to 'k', 'н' to 'h',
        'т' to 't', 'м' to 'm', 'в' to 'b', 'і' to 'i', 'ѕ' to 's'
    )

    private val ZERO_WIDTH_REGEX = Regex("[\\u200B-\\u200D\\uFEFF]")
    private const val SEPARATOR_PATTERN = "[\\s._*\\-]{0,2}"

    private val DEFAULT_BANNED_WORDS: Set<String> = setOf(
        "nigger", "nigga", "faggot", "chink", "spic", "kike", "retard", "tranny"
    )

    private val patternCache = mutableMapOf<String, Regex>()

    private fun normalize(text: String): String {
        val stripped = ZERO_WIDTH_REGEX.replace(text.lowercase(), "")
        val builder = StringBuilder(stripped.length)
        for (c in stripped) {
            builder.append(LEET_MAP[c] ?: CYRILLIC_LOOKALIKE_MAP[c] ?: c)
        }
        return builder.toString()
    }

    private fun spacedPattern(normalizedTerm: String): Regex =
        patternCache.getOrPut(normalizedTerm) {
            val body = normalizedTerm.map { Regex.escape(it.toString()) }.joinToString(SEPARATOR_PATTERN)
            Regex("(?<![a-z])$body(?![a-z])")
        }

    fun containsBannedWord(text: String, extraBlockedTerms: List<String> = emptyList()): String? {
        if (text.isBlank()) return null
        val normalized = normalize(text)

        val allTerms = DEFAULT_BANNED_WORDS + extraBlockedTerms.map { it.lowercase() }.filter { it.isNotBlank() }

        for (term in allTerms) {
            val normalizedTerm = normalize(term)
            if (normalizedTerm.isBlank()) continue
            if (spacedPattern(normalizedTerm).containsMatchIn(normalized)) return term
        }
        return null
    }
}
