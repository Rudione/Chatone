package io.rudione.chatone.util.automod

fun relativeSimilarity(a: String, b: String, atLeast: Float = 0f): Float {
    if (a.isEmpty() || b.isEmpty()) return 0f

    val div = maxOf(a.length, b.length)
    if (atLeast > 0f && minOf(a.length, b.length).toFloat() / div.toFloat() < atLeast) return 0f

    val outer = if (a.length >= b.length) a else b
    val inner = if (a.length >= b.length) b else a

    var previous = IntArray(inner.length)
    var current = IntArray(inner.length)
    var longestRun = 0

    for (i in outer.indices) {
        for (j in inner.indices) {
            current[j] = if (outer[i] == inner[j]) {
                val run = if (i == 0 || j == 0) 1 else previous[j - 1] + 1
                if (run > longestRun) longestRun = run
                run
            } else 0
        }
        val swap = previous
        previous = current
        current = swap
    }

    if (longestRun == 0) return 0f
    return longestRun.toFloat() / div.toFloat()
}
