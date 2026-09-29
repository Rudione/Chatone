package io.rudione.chatone.util.emote

import io.rudione.chatone.domain.model.GenericEmote

class EmoteSearchIndex {
    private var codes: Array<String> = emptyArray()
    private var emotes: Array<GenericEmote> = emptyArray()

    fun build(source: List<GenericEmote>) {
        val seen = HashSet<String>(source.size)
        val deduped = ArrayList<GenericEmote>(source.size)
        for (emote in source) {
            if (seen.add(emote.listKey)) deduped.add(emote)
        }
        emotes = deduped.toTypedArray()
        codes = Array(deduped.size) { deduped[it].code.lowercase() }
    }

    fun search(query: String, limit: Int = Int.MAX_VALUE): List<GenericEmote> {
        if (query.isBlank() || emotes.isEmpty()) return emptyList()
        val q = query.lowercase()

        val exact = ArrayList<GenericEmote>()
        val prefix = ArrayList<GenericEmote>()
        val suffix = ArrayList<GenericEmote>()

        for (i in codes.indices) {
            val code = codes[i]
            when {
                code == q -> exact.add(emotes[i])
                code.startsWith(q) -> prefix.add(emotes[i])
                code.endsWith(q) -> suffix.add(emotes[i])
            }
        }

        if (limit == Int.MAX_VALUE) return exact + prefix + suffix

        val result = ArrayList<GenericEmote>(minOf(limit, exact.size + prefix.size + suffix.size))
        for (bucket in listOf(exact, prefix, suffix)) {
            for (emote in bucket) {
                if (result.size >= limit) return result
                result.add(emote)
            }
        }
        return result
    }

    fun clear() {
        codes = emptyArray()
        emotes = emptyArray()
    }
}
