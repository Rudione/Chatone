package io.rudione.chatone.util.automod

import kotlin.concurrent.Volatile

object RegexCache {
    @Volatile
    private var cache: Map<String, Regex?> = emptyMap()

    private fun compile(key: String, pattern: String, ignoreCase: Boolean): Regex? {
        val snapshot = cache
        if (snapshot.containsKey(key)) return snapshot[key]
        val opts = if (ignoreCase) setOf(RegexOption.IGNORE_CASE) else emptySet()
        val compiled = try {
            Regex(pattern, opts)
        } catch (_: Exception) {
            null
        }
        cache = cache + (key to compiled)
        return compiled
    }

    fun regex(pattern: String, ignoreCase: Boolean): Regex? =
        compile("r|${if (ignoreCase) 1 else 0}|$pattern", pattern, ignoreCase)

    fun wholeWord(word: String, ignoreCase: Boolean): Regex? =
        compile(
            "w|${if (ignoreCase) 1 else 0}|$word",
            "(?<![\\p{L}\\p{N}_-])${Regex.escape(word)}(?![\\p{L}\\p{N}_-])",
            ignoreCase
        )
}
