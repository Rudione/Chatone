package io.rudione.chatone.presentation.settings.search

import kotlin.math.max
import kotlin.math.min

class SearchField(raw: String, val weight: Float) {
    val normalized: String = SearchText.normalize(raw)
    val tokens: List<String> = SearchText.tokens(normalized)
    val stems: List<String> = tokens.map(SearchText::stem)
}

class SearchDocument(val fields: List<SearchField>)

object SettingsSearchEngine {

    fun score(query: String, document: SearchDocument): Float {
        val normalized = SearchText.normalize(query)
        if (normalized.isEmpty()) return 0f
        val direct = scoreVariant(normalized, document)
        val swapped = SearchText.swapLayout(query)
            ?.let { scoreVariant(it, document) * LAYOUT_PENALTY }
            ?: 0f
        val transliterated = SearchText.transliterate(normalized)
            ?.let { scoreVariant(it, document) * TRANSLIT_PENALTY }
            ?: 0f
        return max(direct, max(swapped, transliterated))
    }

    private fun scoreVariant(query: String, document: SearchDocument): Float {
        val queryTokens = SearchText.tokens(query)
        if (queryTokens.isEmpty()) return 0f
        var total = 0f
        for (token in queryTokens) {
            val literal = bestMatch(token, document)
            var best = literal
            for (synonym in SearchSynonyms.of(token)) {
                best = max(best, bestMatch(synonym, document) * SYNONYM_PENALTY)
            }
            if (best == 0f) return 0f
            total += if (literal > 0f && best > literal) best + LITERAL_BONUS else best
        }
        val phraseBonus = if (queryTokens.size > 1) {
            document.fields.maxOfOrNull { if (it.normalized.contains(query)) it.weight * PHRASE_BONUS else 0f } ?: 0f
        } else 0f
        return total / queryTokens.size + phraseBonus
    }

    private fun bestMatch(token: String, document: SearchDocument): Float {
        val stem = SearchText.stem(token)
        var best = 0f
        for (field in document.fields) {
            for (index in field.tokens.indices) {
                val quality = matchQuality(token, stem, field.tokens[index], field.stems[index])
                if (quality > 0f) best = max(best, quality * field.weight)
            }
        }
        return best
    }

    internal fun matchQuality(query: String, queryStem: String, token: String, tokenStem: String): Float = when {
        token == query -> 1f
        token.startsWith(query) -> 0.92f
        query.length < MIN_FUZZY_LENGTH -> 0f
        tokenStem == queryStem -> 0.86f
        token.startsWith(queryStem) -> 0.8f
        token.contains(query) -> 0.62f
        query.length >= MIN_TYPO_LENGTH && isTypo(query, token) -> 0.48f
        else -> 0f
    }

    private fun isTypo(query: String, token: String): Boolean {
        val allowed = if (query.length >= LONG_WORD) 2 else 1
        if (token.length + allowed < query.length) return false
        if (SearchText.distance(query, token, allowed) <= allowed) return true
        if (token.length > query.length) {
            return SearchText.distance(query, token.substring(0, query.length), allowed) <= allowed
        }
        return false
    }

    private const val LAYOUT_PENALTY = 0.9f
    private const val TRANSLIT_PENALTY = 0.85f
    private const val SYNONYM_PENALTY = 0.85f
    private const val LITERAL_BONUS = 0.2f
    private const val PHRASE_BONUS = 0.6f
    private const val MIN_FUZZY_LENGTH = 3
    private const val MIN_TYPO_LENGTH = 4
    private const val LONG_WORD = 7
}

object SearchText {

    fun normalize(raw: String): String {
        val builder = StringBuilder(raw.length)
        var pendingSpace = false
        for (char in raw.lowercase()) {
            val mapped = if (char == 'ё') 'е' else char
            if (mapped.isLetterOrDigit()) {
                if (pendingSpace && builder.isNotEmpty()) builder.append(' ')
                pendingSpace = false
                builder.append(mapped)
            } else {
                pendingSpace = true
            }
        }
        return builder.toString()
    }

    fun tokens(normalized: String): List<String> =
        if (normalized.isEmpty()) emptyList() else normalized.split(' ')

    fun stem(token: String): String {
        if (token.length <= MIN_STEM) return token
        val suffixes = if (token.any { it in 'а'..'я' }) RU_SUFFIXES else EN_SUFFIXES
        for (suffix in suffixes) {
            if (token.endsWith(suffix) && token.length - suffix.length >= MIN_STEM) {
                return token.dropLast(suffix.length)
            }
        }
        return token
    }

    fun swapLayout(raw: String): String? {
        val source = raw.lowercase()
        var changed = false
        val swapped = buildString(source.length) {
            for (char in source) {
                val mapped = LAYOUT[char]
                if (mapped != null) {
                    changed = true
                    append(mapped)
                } else {
                    append(char)
                }
            }
        }
        if (!changed) return null
        return normalize(swapped).takeIf { it.isNotEmpty() && it != normalize(source) }
    }

    fun transliterate(normalized: String): String? {
        if (normalized.isEmpty()) return null
        val cyrillic = normalized.any { it in 'а'..'я' }
        val latin = normalized.any { it in 'a'..'z' }
        if (cyrillic == latin) return null
        val result = if (cyrillic) toLatin(normalized) else toCyrillic(normalized)
        return result.takeIf { it != normalized }
    }

    private fun toLatin(text: String): String = buildString(text.length + 4) {
        for (char in text) append(CYRILLIC_TO_LATIN[char] ?: char.toString())
    }

    private fun toCyrillic(text: String): String = buildString(text.length) {
        var index = 0
        while (index < text.length) {
            val pair = LATIN_DIGRAPHS.firstOrNull { text.startsWith(it.first, index) }
            if (pair != null) {
                append(pair.second)
                index += pair.first.length
            } else {
                append(LATIN_TO_CYRILLIC[text[index]] ?: text[index])
                index++
            }
        }
    }

    fun highlightRanges(text: String, query: String): List<IntRange> {
        val needles = highlightNeedles(query)
        if (needles.isEmpty() || text.isEmpty()) return emptyList()
        val folded = fold(text)
        val ranges = mutableListOf<IntRange>()
        for (needle in needles) {
            var from = 0
            while (from < folded.length) {
                val index = folded.indexOf(needle, from)
                if (index < 0) break
                val startsWord = index == 0 || !folded[index - 1].isLetterOrDigit()
                if (startsWord || needle.length >= MIN_INNER_MATCH) {
                    var end = index + needle.length
                    while (end < folded.length && folded[end].isLetterOrDigit() && end - index < needle.length + MAX_SUFFIX_EXTENSION) end++
                    ranges += index until end
                }
                from = index + needle.length
            }
        }
        return merge(ranges)
    }

    private fun highlightNeedles(query: String): List<String> {
        val normalized = normalize(query)
        val variants = listOfNotNull(normalized, swapLayout(query), transliterate(normalized))
        return variants.flatMap(::tokens)
            .filter { it.length >= MIN_HIGHLIGHT }
            .map { token -> stem(token).takeIf { token.length > MIN_STEM + 1 } ?: token }
            .distinct()
    }

    private fun fold(text: String): String = buildString(text.length) {
        for (char in text) {
            val lower = char.lowercaseChar()
            append(if (lower == 'ё') 'е' else lower)
        }
    }

    private fun merge(ranges: List<IntRange>): List<IntRange> {
        if (ranges.isEmpty()) return ranges
        val sorted = ranges.sortedBy { it.first }
        val merged = mutableListOf(sorted.first())
        for (range in sorted.drop(1)) {
            val last = merged.last()
            if (range.first <= last.last + 1) {
                merged[merged.lastIndex] = last.first..maxOf(last.last, range.last)
            } else {
                merged += range
            }
        }
        return merged
    }

    fun distance(a: String, b: String, limit: Int): Int {
        if (a == b) return 0
        if (kotlin.math.abs(a.length - b.length) > limit) return limit + 1
        val rows = a.length + 1
        val cols = b.length + 1
        val grid = Array(rows) { IntArray(cols) }
        for (i in 0 until rows) grid[i][0] = i
        for (j in 0 until cols) grid[0][j] = j
        for (i in 1 until rows) {
            var rowBest = Int.MAX_VALUE
            for (j in 1 until cols) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                var value = min(min(grid[i - 1][j] + 1, grid[i][j - 1] + 1), grid[i - 1][j - 1] + cost)
                if (i > 1 && j > 1 && a[i - 1] == b[j - 2] && a[i - 2] == b[j - 1]) {
                    value = min(value, grid[i - 2][j - 2] + 1)
                }
                grid[i][j] = value
                rowBest = min(rowBest, value)
            }
            if (rowBest > limit) return limit + 1
        }
        return grid[a.length][b.length]
    }

    private const val MIN_STEM = 3
    private const val MIN_HIGHLIGHT = 2
    private const val MIN_INNER_MATCH = 4
    private const val MAX_SUFFIX_EXTENSION = 4

    private val RU_SUFFIXES = listOf(
        "иями", "ями", "ами", "его", "ого", "ему", "ому", "ыми", "ими", "ией", "иях", "ах", "ях",
        "ой", "ей", "ую", "юю", "ая", "яя", "ое", "ее", "ые", "ие", "ый", "ий", "ом", "ем", "ам", "ям",
        "ов", "ев", "ть", "ти", "ет", "ит", "ут", "ют", "ат", "ят", "ия", "ие", "ию", "ии",
        "а", "я", "о", "е", "ы", "и", "у", "ю", "й", "ь"
    )

    private val EN_SUFFIXES = listOf("ings", "ing", "ies", "ed", "es", "s")

    private val CYRILLIC_TO_LATIN: Map<Char, String> = mapOf(
        'а' to "a", 'б' to "b", 'в' to "v", 'г' to "g", 'д' to "d", 'е' to "e", 'ж' to "zh", 'з' to "z",
        'и' to "i", 'й' to "y", 'к' to "k", 'л' to "l", 'м' to "m", 'н' to "n", 'о' to "o", 'п' to "p",
        'р' to "r", 'с' to "s", 'т' to "t", 'у' to "u", 'ф' to "f", 'х' to "kh", 'ц' to "ts", 'ч' to "ch",
        'ш' to "sh", 'щ' to "sch", 'ъ' to "", 'ы' to "y", 'ь' to "", 'э' to "e", 'ю' to "yu", 'я' to "ya"
    )

    private val LATIN_DIGRAPHS: List<Pair<String, Char>> = listOf(
        "sch" to 'щ', "shch" to 'щ', "zh" to 'ж', "kh" to 'х', "ts" to 'ц', "ch" to 'ч', "sh" to 'ш',
        "yu" to 'ю', "ya" to 'я', "yo" to 'е', "ye" to 'е'
    ).sortedByDescending { it.first.length }

    private val LATIN_TO_CYRILLIC: Map<Char, Char> = mapOf(
        'a' to 'а', 'b' to 'б', 'c' to 'к', 'd' to 'д', 'e' to 'е', 'f' to 'ф', 'g' to 'г', 'h' to 'х',
        'i' to 'и', 'j' to 'ж', 'k' to 'к', 'l' to 'л', 'm' to 'м', 'n' to 'н', 'o' to 'о', 'p' to 'п',
        'q' to 'к', 'r' to 'р', 's' to 'с', 't' to 'т', 'u' to 'у', 'v' to 'в', 'w' to 'в', 'x' to 'к',
        'y' to 'ы', 'z' to 'з'
    )

    private const val EN_KEYS = "qwertyuiop[]asdfghjkl;'zxcvbnm,.`"
    private const val RU_KEYS = "йцукенгшщзхъфывапролджэячсмитьбюё"

    private val LAYOUT: Map<Char, Char> = buildMap {
        EN_KEYS.forEachIndexed { index, char -> put(char, RU_KEYS[index]) }
        RU_KEYS.forEachIndexed { index, char -> put(char, EN_KEYS[index]) }
    }
}
