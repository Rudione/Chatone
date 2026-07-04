package io.rudione.chatone.util

fun relativeSimilarity(a: String, b: String): Float {
    if (a.isEmpty() || b.isEmpty()) return 0f
    val tree = Array(a.length) { IntArray(b.length) }
    var longestRun = 0
    for (i in a.indices) {
        for (j in b.indices) {
            if (a[i] == b[j]) {
                tree[i][j] = if (i == 0 || j == 0) 1 else tree[i - 1][j - 1] + 1
                if (tree[i][j] > longestRun) longestRun = tree[i][j]
            }
        }
    }
    if (longestRun == 0) return 0f
    val div = maxOf(1, a.length, b.length)
    return longestRun.toFloat() / div.toFloat()
}
