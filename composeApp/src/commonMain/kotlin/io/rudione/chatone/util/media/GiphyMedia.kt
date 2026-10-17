package io.rudione.chatone.util.media

import io.rudione.chatone.util.link.httpUrlHost

internal expect val animatedWebpSupported: Boolean

object GiphyMedia {

    private val ID_PATTERN = Regex("^[A-Za-z0-9]{1,64}$")
    private const val MEDIA_BASE = "https://media.giphy.com/media"

    fun chatUrl(id: String, url: String): String {
        if (!ID_PATTERN.matches(id) || !isGiphyHost(url)) return url
        val file = if (animatedWebpSupported) "giphy.webp" else "giphy.gif"
        return "$MEDIA_BASE/$id/$file"
    }

    fun preferWebp(gifUrl: String, webpUrl: String?): String =
        if (animatedWebpSupported && !webpUrl.isNullOrBlank()) webpUrl else gifUrl

    private fun isGiphyHost(url: String): Boolean {
        val host = httpUrlHost(url)?.lowercase() ?: return false
        return host == "giphy.com" || host.endsWith(".giphy.com")
    }
}
