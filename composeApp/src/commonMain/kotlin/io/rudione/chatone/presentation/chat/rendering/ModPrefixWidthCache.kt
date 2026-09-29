package io.rudione.chatone.presentation.chat.rendering

private const val MAX_ENTRIES = 64

internal object ModPrefixWidthCache {

    private val widths = HashMap<String, Float>()

    operator fun get(key: String): Float? = widths[key]

    operator fun set(key: String, width: Float) {
        if (widths.size >= MAX_ENTRIES && key !in widths) widths.clear()
        widths[key] = width
    }
}
