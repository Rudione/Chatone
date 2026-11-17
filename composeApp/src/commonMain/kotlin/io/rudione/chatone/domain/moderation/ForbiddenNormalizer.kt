package io.rudione.chatone.domain.moderation

internal class WordSkeletons(
    val cyrillic: List<String>,
    val cyrillicRaw: List<String>,
    val latin: String,
    val latinRaw: String
) {
    fun of(script: ForbiddenScript, collapsed: Boolean): List<String> = when (script) {
        ForbiddenScript.CYRILLIC -> if (collapsed) cyrillic else cyrillicRaw
        ForbiddenScript.LATIN -> listOf(if (collapsed) latin else latinRaw)
    }
}

internal object ForbiddenNormalizer {

    const val JOINERS = ".-_'*`·"

    fun isIgnorable(c: Char): Boolean = c.category in IGNORABLE

    fun skeletons(raw: String): WordSkeletons {
        val prepared = prepare(raw)
        val visual = mapped(prepared, ::visualCyrillic)
        val phonetic = mapped(transliterated(prepared), ::phoneticCyrillic)
        val latin = mapped(prepared, ::visualLatin)
        return WordSkeletons(
            cyrillic = listOf(collapse(visual), collapse(phonetic)),
            cyrillicRaw = listOf(visual, phonetic),
            latin = collapse(latin),
            latinRaw = latin
        )
    }

    private fun prepare(raw: String): String {
        val out = StringBuilder(raw.length)
        raw.forEach { c ->
            if (isIgnorable(c)) return@forEach
            val narrow = if (c in FULLWIDTH_FIRST..FULLWIDTH_LAST) (c.code - FULLWIDTH_SHIFT).toChar() else c
            val lower = narrow.lowercaseChar()
            out.append(ACCENTS[lower] ?: lower)
        }
        return out.toString()
    }

    private fun mapped(text: String, map: (Char) -> String): String {
        val out = StringBuilder(text.length)
        text.forEach { c -> if (c !in JOINERS) out.append(map(c)) }
        return out.toString()
    }

    private fun collapse(text: String): String {
        val out = StringBuilder(text.length)
        text.forEach { c -> if (out.isEmpty() || out.last() != c) out.append(c) }
        return out.toString()
    }

    private fun transliterated(text: String): String {
        var result = text
        DIGRAPHS.forEach { (latin, cyrillic) -> result = result.replace(latin, cyrillic) }
        return result
    }

    private fun visualCyrillic(c: Char): String = shared(c, phonetic = false) ?: VISUAL_CYRILLIC[c]?.toString() ?: c.toString()

    private fun phoneticCyrillic(c: Char): String = shared(c, phonetic = true) ?: PHONETIC_CYRILLIC[c] ?: c.toString()

    private fun visualLatin(c: Char): String = LATIN_DIGITS[c]?.toString() ?: VISUAL_LATIN[c]?.toString() ?: c.toString()

    private fun shared(c: Char, phonetic: Boolean): String? = when (c) {
        'ё' -> "е"
        'і', 'ї' -> "и"
        'є' -> "е"
        'ґ' -> "г"
        '3' -> if (phonetic) "е" else "з"
        else -> CYRILLIC_DIGITS[c]?.toString()
    }

    private const val FULLWIDTH_FIRST = '！'
    private const val FULLWIDTH_LAST = '～'
    private const val FULLWIDTH_SHIFT = 0xFEE0

    private val IGNORABLE = setOf(CharCategory.NON_SPACING_MARK, CharCategory.ENCLOSING_MARK, CharCategory.FORMAT)

    private val ACCENTS: Map<Char, Char> = listOf(
        "áàâäãåāăą" to 'a', "éèêëēęě" to 'e', "íìîïıīɪ" to 'i', "óòôöõøōő" to 'o', "úùûüūůű" to 'u',
        "ñńň" to 'n', "çčć" to 'c', "ýÿ" to 'y', "şśšș" to 's', "žźż" to 'z', "ł" to 'l', "ğ" to 'g',
        "ř" to 'r', "ťț" to 't', "ď" to 'd'
    ).flatMap { (letters, plain) -> letters.map { it to plain } }.toMap()

    private val CYRILLIC_DIGITS = mapOf(
        '0' to 'о', '1' to 'и', '4' to 'ч', '6' to 'б', '8' to 'в',
        '@' to 'а', '$' to 'с', '!' to 'и', '|' to 'и'
    )

    private val LATIN_DIGITS = mapOf(
        '0' to 'o', '1' to 'i', '3' to 'e', '4' to 'a', '5' to 's', '6' to 'g', '7' to 't', '8' to 'b', '9' to 'g',
        '@' to 'a', '$' to 's', '!' to 'i', '|' to 'i'
    )

    private val VISUAL_CYRILLIC = mapOf(
        'a' to 'а', 'b' to 'в', 'c' to 'с', 'e' to 'е', 'h' to 'н', 'i' to 'и', 'k' to 'к', 'l' to 'и', 'm' to 'м',
        'n' to 'п', 'o' to 'о', 'p' to 'р', 'r' to 'г', 't' to 'т', 'u' to 'и', 'x' to 'х', 'y' to 'у',
        'α' to 'а', 'ε' to 'е', 'ι' to 'и', 'κ' to 'к', 'ο' to 'о', 'ρ' to 'р', 'τ' to 'т', 'υ' to 'и', 'η' to 'п'
    )

    private val PHONETIC_CYRILLIC = mapOf(
        'a' to "а", 'b' to "б", 'c' to "к", 'd' to "д", 'e' to "е", 'f' to "ф", 'g' to "г", 'h' to "х",
        'i' to "и", 'j' to "ж", 'k' to "к", 'l' to "л", 'm' to "м", 'n' to "н", 'o' to "о", 'p' to "п",
        'q' to "к", 'r' to "р", 's' to "с", 't' to "т", 'u' to "у", 'v' to "в", 'w' to "в", 'x' to "кс",
        'y' to "ы", 'z' to "з"
    )

    private val DIGRAPHS = listOf(
        "shch" to "щ", "sch" to "щ", "sh" to "ш", "ch" to "ч", "zh" to "ж", "kh" to "х",
        "ts" to "ц", "ya" to "я", "yu" to "ю", "yo" to "е", "ye" to "е"
    )

    private val VISUAL_LATIN = mapOf(
        'а' to 'a', 'в' to 'b', 'е' to 'e', 'ё' to 'e', 'к' to 'k', 'м' to 'm', 'н' to 'h', 'о' to 'o',
        'р' to 'p', 'с' to 'c', 'т' to 't', 'у' to 'y', 'х' to 'x', 'і' to 'i', 'ї' to 'i',
        'α' to 'a', 'ε' to 'e', 'ι' to 'i', 'κ' to 'k', 'ο' to 'o', 'ρ' to 'p', 'τ' to 't', 'υ' to 'u',
        'ν' to 'v', 'η' to 'n', 'γ' to 'y'
    )
}
