package io.rudione.chatone.util.link

import io.rudione.chatone.presentation.settings.SettingsState

expect fun openUrl(url: String, mode: SettingsState.LinkOpenMode)

private const val MAX_URL_LENGTH = 2048

private val UNSAFE_URL_CHARS = charArrayOf(
    ' ', '\t', '\n', '\r', '"', '\'', '`', '<', '>', '|', '^',
    '{', '}', '[', ']', '\\'
)

private fun Char.isDeceptiveFormatting(): Boolean = when (code) {
    0x200B, 0x200C, 0x200D, 0x200E, 0x200F -> true
    in 0x202A..0x202E -> true
    in 0x2066..0x2069 -> true
    0xFEFF -> true
    else -> false
}

fun isSafeHttpUrl(rawUrl: String): Boolean {
    val url = rawUrl.trim()
    if (url.length !in 8..MAX_URL_LENGTH) return false

    val lower = url.lowercase()
    val schemeLen = when {
        lower.startsWith("https://") -> 8
        lower.startsWith("http://") -> 7
        else -> return false
    }
    if (url.length <= schemeLen) return false

    for (c in url) {
        if (c.code <= 0x20 || c.code == 0x7F) return false
        if (c in UNSAFE_URL_CHARS) return false
        if (c.isDeceptiveFormatting()) return false
    }

    if (lower.contains("%00")) return false

    val authorityEnd = url.indexOfFirst(schemeLen) { it == '/' || it == '?' || it == '#' }
    val authority = if (authorityEnd == -1) url.substring(schemeLen) else url.substring(schemeLen, authorityEnd)
    if (authority.isEmpty() || authority.contains('@')) return false

    return true
}

fun httpUrlHost(rawUrl: String): String? {
    val url = rawUrl.trim()
    val lower = url.lowercase()
    val schemeLen = when {
        lower.startsWith("https://") -> 8
        lower.startsWith("http://") -> 7
        else -> return null
    }
    val authorityEnd = url.indexOfFirst(schemeLen) { it == '/' || it == '?' || it == '#' }
    val authority = if (authorityEnd == -1) url.substring(schemeLen) else url.substring(schemeLen, authorityEnd)
    val host = authority.substringAfterLast('@')
    val bare = if (host.startsWith("[")) {
        host.substringAfter('[').substringBefore(']')
    } else {
        host.substringBefore(':')
    }
    return bare.lowercase().takeIf { it.isNotEmpty() }
}

private inline fun String.indexOfFirst(startIndex: Int, predicate: (Char) -> Boolean): Int {
    for (i in startIndex until length) {
        if (predicate(this[i])) return i
    }
    return -1
}
