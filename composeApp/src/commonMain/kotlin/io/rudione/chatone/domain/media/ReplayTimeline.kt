package io.rudione.chatone.domain.media

class ReplayTimeline(
    private val backfillSeconds: Int = BACKFILL_SECONDS,
    private val lookaheadSeconds: Int = LOOKAHEAD_SECONDS,
    private val keepBehind: Int = KEEP_BEHIND
) {
    var lines: List<ReplayLine> = emptyList()
        private set

    var primed: Boolean = false
        private set

    private var windowStart = UNSET
    private var loadedUntil = UNSET
    private var lastNow = UNSET

    fun nextFetch(nowSeconds: Int): Int? {
        val now = nowSeconds.coerceAtLeast(0)
        val leapt = lastNow != UNSET && now - lastNow > FORWARD_LEAP_SECONDS && now > loadedUntil
        lastNow = now
        if (windowStart == UNSET || now < windowStart || leapt) reset((now - backfillSeconds).coerceAtLeast(0))
        return loadedUntil.takeIf { now + lookaheadSeconds >= it }
    }

    fun accept(from: Int, page: List<ReplayLine>): List<ReplayLine> {
        if (from != loadedUntil) return emptyList()
        val last = page.maxOfOrNull { it.offsetSeconds }
        loadedUntil = when {
            last == null -> from + EMPTY_PAGE_STEP_SECONDS
            last > from -> last
            else -> from + 1
        }
        primed = true
        val known = lines.mapTo(HashSet()) { it.id }
        val fresh = page.filter { known.add(it.id) }
        if (fresh.isEmpty()) return fresh
        lines = trimmed((lines + fresh).sortedBy { it.offsetSeconds })
        return fresh
    }

    private fun trimmed(sorted: List<ReplayLine>): List<ReplayLine> {
        val now = lastNow.coerceAtLeast(0)
        val behind = sorted.count { it.offsetSeconds <= now }
        val drop = behind - keepBehind
        if (drop <= 0) return sorted
        val kept = sorted.drop(drop)
        windowStart = maxOf(windowStart, kept.first().offsetSeconds)
        return kept
    }

    private fun reset(from: Int) {
        lines = emptyList()
        primed = false
        windowStart = from
        loadedUntil = from
    }

    companion object {
        const val BACKFILL_SECONDS = 30
        const val LOOKAHEAD_SECONDS = 20
        const val KEEP_BEHIND = 300
        const val FORWARD_LEAP_SECONDS = 8
        const val EMPTY_PAGE_STEP_SECONDS = 60
        private const val UNSET = -1
    }
}

fun List<ReplayLine>.revealTimesMs(): LongArray {
    val out = LongArray(size)
    var start = 0
    while (start < size) {
        val second = this[start].offsetSeconds
        var end = start
        while (end < size && this[end].offsetSeconds == second) end++
        val count = end - start
        for (i in start until end) out[i] = second * MS_PER_SECOND + (i - start) * MS_PER_SECOND / count
        start = end
    }
    return out
}

private const val MS_PER_SECOND = 1_000L
