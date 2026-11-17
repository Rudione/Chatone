package io.rudione.chatone.domain.moderation

class ForbiddenWordScanner(private val terms: List<ForbiddenTerm>) {

    fun scanChatMessage(text: String): List<ForbiddenHit> {
        val body = chatBody(text) ?: return emptyList()
        return scan(text.substring(body)).map { it.copy(start = it.start + body.first, end = it.end + body.first) }
    }

    fun scan(text: String): List<ForbiddenHit> {
        if (terms.isEmpty()) return emptyList()
        val hits = ArrayList<ForbiddenHit>()
        candidates(text).forEach { candidate ->
            if (hits.any { candidate.start < it.end && it.start < candidate.end }) return@forEach
            classify(candidate.raw)?.let { hits += ForbiddenHit(candidate.start, candidate.end, it) }
        }
        return hits.sortedBy { it.start }
    }

    private fun chatBody(text: String): IntRange? {
        val trimmed = text.trimStart()
        if (!trimmed.startsWith("/")) return text.indices
        val lead = text.length - trimmed.length
        val command = trimmed.substringBefore(' ').lowercase()
        if (command != "/me" && !command.startsWith("/announce")) return null
        val space = trimmed.indexOf(' ')
        if (space < 0) return null
        return (lead + space + 1) until text.length
    }

    private fun classify(raw: String): ForbiddenCategory? {
        val skeletons = ForbiddenNormalizer.skeletons(raw)
        return terms.firstOrNull { term ->
            skeletons.of(term.script, term.collapsesRepeats).any { term.regex.matches(it) }
        }?.category
    }

    private class Candidate(val raw: String, val start: Int, val end: Int)

    private fun candidates(text: String): List<Candidate> {
        val words = words(text)
        val out = ArrayList<Candidate>(words.size + 2)
        var run = ArrayList<Candidate>()
        fun flushRun() {
            val letters = run.takeLast(SPELLED_OUT_MAX_RUN)
            for (from in letters.indices) {
                for (to in letters.lastIndex downTo from + SPELLED_OUT_MIN - 1) {
                    val part = letters.subList(from, to + 1)
                    out += Candidate(part.joinToString("") { it.raw }, part.first().start, part.last().end)
                }
            }
            run = ArrayList()
        }
        words.forEachIndexed { index, word ->
            out += word
            val single = word.raw.count { it.isLetter() } == 1
            val adjacent = run.isNotEmpty() && onlySeparators(text, run.last().end, word.start)
            if (single && (run.isEmpty() || adjacent)) run += word
            else {
                flushRun()
                if (single) run += word
            }
            if (index == words.lastIndex) flushRun()
        }
        return out
    }

    private fun onlySeparators(text: String, from: Int, to: Int): Boolean =
        to - from in 1..SPELLED_OUT_MAX_GAP && (from until to).all { text[it].isWhitespace() || text[it] in SPELL_SEPARATORS }

    private fun words(text: String): List<Candidate> {
        val out = ArrayList<Candidate>()
        var index = 0
        while (index < text.length) {
            if (!isWordChar(text[index])) {
                index++
                continue
            }
            val start = index
            while (index < text.length && continuesWord(text, index)) index++
            word(text, start, index)?.let { out += it }
        }
        return out
    }

    private fun continuesWord(text: String, index: Int): Boolean =
        isWordChar(text[index]) || isJoiner(text, index) || ForbiddenNormalizer.isIgnorable(text[index])

    private fun word(text: String, start: Int, end: Int): Candidate? {
        var first = start
        var last = end
        while (first < last && !text[first].isLetter()) first++
        while (last > first && !text[last - 1].isLetter()) last--
        if (first >= last) return null
        if (first > start && text[start] == '@') return null
        return Candidate(text.substring(first, last), first, last)
    }

    private fun isWordChar(c: Char): Boolean = c.isLetterOrDigit() || c in SUBSTITUTES

    private fun isJoiner(text: String, index: Int): Boolean =
        text[index] in ForbiddenNormalizer.JOINERS && index > 0 && index + 1 < text.length &&
                isWordChar(text[index - 1]) && isWordChar(text[index + 1])

    private companion object {
        const val SPELLED_OUT_MIN = 3
        const val SPELLED_OUT_MAX_GAP = 3
        const val SPELLED_OUT_MAX_RUN = 24
        const val SUBSTITUTES = "@$!|"
        const val SPELL_SEPARATORS = ".-_*·,"
    }
}
