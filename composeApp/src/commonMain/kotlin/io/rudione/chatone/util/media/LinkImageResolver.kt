package io.rudione.chatone.util.media

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import io.ktor.utils.io.core.readText
import io.ktor.utils.io.readRemaining
import io.rudione.chatone.util.link.OutboundUrlPolicy
import io.rudione.chatone.util.link.httpUrlHost
import io.rudione.chatone.util.link.isSafeHttpUrl
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

sealed interface ImageSource {
    data class Direct(val imageUrl: String) : ImageSource
    data object None : ImageSource
}

private interface HostImageResolver {
    fun matches(host: String, path: String): Boolean
    fun template(host: String, path: String): String?
    fun fromHtml(html: String, pageUrl: String): String?
}

private val SLUG = Regex("""^/([A-Za-z0-9_-]{4,32})/?$""")

private fun slugOf(path: String): String? = SLUG.find(path)?.groupValues?.getOrNull(1)

private object KappaLolResolver : HostImageResolver {
    override fun matches(host: String, path: String) =
        (host == "kappalol.fun" || host.endsWith(".kappalol.fun")) && slugOf(path) != null

    override fun template(host: String, path: String): String? =
        slugOf(path)?.let { "https://kappalol.fun/api/v1/files/$it/download" }

    override fun fromHtml(html: String, pageUrl: String): String? = null
}

private val EBLO_UPLOADS = Regex("""["'](/uploads/[A-Za-z0-9_-]+/[^"'?\s]+)["']""")

private object EbloResolver : HostImageResolver {
    override fun matches(host: String, path: String) =
        (host == "eblo.id" || host.endsWith(".eblo.id")) && slugOf(path) != null

    override fun template(host: String, path: String): String? = null

    override fun fromHtml(html: String, pageUrl: String): String? {
        val previewImage = htmlTags(html, "img")
            .map { htmlAttributes(it) }
            .firstOrNull { it["id"] == "preview-image" }
            ?.get("src")
        val raw = previewImage
            ?: EBLO_UPLOADS.find(html)?.groupValues?.getOrNull(1)
            ?: return null
        return absolutize(pageUrl, raw)
    }
}

private object KappaShortResolver : HostImageResolver {
    override fun matches(host: String, path: String) =
        (host == "kappa.lol" || host.endsWith(".kappa.lol")) && slugOf(path) != null

    override fun template(host: String, path: String): String? = null

    override fun fromHtml(html: String, pageUrl: String): String? =
        genericHtmlImage(html, pageUrl)
}

private val RESOLVERS = listOf(KappaLolResolver, EbloResolver, KappaShortResolver)

private val DIRECT_IMAGE_EXT = Regex(""".*\.(png|jpe?g|gif|webp|bmp|svg|avif)(\?.*)?$""")

private val DIRECT_IMAGE_HOSTS = setOf(
    "i.imgur.com", "cdn.7tv.app", "cdn.betterttv.net", "cdn.frankerfacez.com",
    "pbs.twimg.com", "media.discordapp.net", "cdn.discordapp.com",
    "i.redd.it", "preview.redd.it", "static-cdn.jtvnw.net"
)

private val AUTOLOAD_HOSTS = setOf(
    "imgur.com", "7tv.app", "betterttv.net", "frankerfacez.com", "twimg.com",
    "discordapp.com", "discordapp.net", "redd.it", "jtvnw.net", "twitch.tv",
    "ttvnw.net", "kappa.lol", "kappalol.fun", "eblo.id"
)

private val OG_IMAGE_KEYS = listOf(
    "og:image", "og:image:url", "og:image:secure_url", "twitter:image", "twitter:image:src"
)

private const val MAX_TAG_CHARS = 8192
private const val MAX_TAGS_SCANNED = 2000

internal fun htmlTags(html: String, name: String): Sequence<String> = sequence {
    val needle = "<$name"
    var index = 0
    var produced = 0
    while (index < html.length && produced < MAX_TAGS_SCANNED) {
        val start = html.indexOf(needle, index, ignoreCase = true)
        if (start < 0) return@sequence
        val after = start + needle.length
        if (after >= html.length) return@sequence
        val next = html[after]
        if (!next.isWhitespace() && next != '>' && next != '/') {
            index = after
            continue
        }
        val end = html.indexOf('>', after)
        if (end < 0) return@sequence
        if (end - start <= MAX_TAG_CHARS) {
            produced++
            yield(html.substring(after, end))
        }
        index = end + 1
    }
}

internal fun htmlAttributes(tagBody: String): Map<String, String> {
    val attributes = LinkedHashMap<String, String>()
    var index = 0
    while (index < tagBody.length) {
        while (index < tagBody.length && (tagBody[index].isWhitespace() || tagBody[index] == '/')) index++
        val nameStart = index
        while (index < tagBody.length &&
            !tagBody[index].isWhitespace() && tagBody[index] != '=' && tagBody[index] != '/'
        ) index++
        if (index == nameStart) {
            index++
            continue
        }
        val name = tagBody.substring(nameStart, index).lowercase()
        while (index < tagBody.length && tagBody[index].isWhitespace()) index++
        if (index >= tagBody.length || tagBody[index] != '=') {
            if (name !in attributes) attributes[name] = ""
            continue
        }
        index++
        while (index < tagBody.length && tagBody[index].isWhitespace()) index++
        if (index >= tagBody.length) break
        val quote = tagBody[index]
        val value: String
        if (quote == '"' || quote == '\'') {
            index++
            val valueStart = index
            while (index < tagBody.length && tagBody[index] != quote) index++
            value = tagBody.substring(valueStart, index)
            if (index < tagBody.length) index++
        } else {
            val valueStart = index
            while (index < tagBody.length && !tagBody[index].isWhitespace()) index++
            val raw = tagBody.substring(valueStart, index)
            value = if (index == tagBody.length) raw.removeSuffix("/") else raw
        }
        if (name !in attributes) attributes[name] = value
    }
    return attributes
}

internal fun htmlMetaValues(html: String): Map<String, String> {
    val values = LinkedHashMap<String, String>()
    htmlTags(html, "meta").forEach { tag ->
        val attributes = htmlAttributes(tag)
        val key = (attributes["property"] ?: attributes["name"])?.lowercase() ?: return@forEach
        val content = attributes["content"] ?: return@forEach
        if (key !in values) values[key] = content
    }
    return values
}

internal fun htmlTitle(html: String): String? {
    var index = 0
    while (index < html.length) {
        val start = html.indexOf("<title", index, ignoreCase = true)
        if (start < 0) return null
        val after = start + 6
        if (after >= html.length) return null
        val next = html[after]
        if (!next.isWhitespace() && next != '>') {
            index = after
            continue
        }
        val open = html.indexOf('>', after)
        if (open < 0) return null
        val end = html.indexOf("</title", open + 1, ignoreCase = true)
        if (end < 0) return null
        return html.substring(open + 1, end)
    }
    return null
}

private fun genericHtmlImage(html: String, pageUrl: String): String? {
    val values = htmlMetaValues(html)
    val raw = OG_IMAGE_KEYS.firstNotNullOfOrNull { key -> values[key]?.takeIf { it.isNotBlank() } }
        ?: return null
    return absolutize(pageUrl, raw)
}

private fun absolutize(base: String, candidate: String): String? {
    val value = candidate.trim().replace("&amp;", "&")
    if (value.isEmpty()) return null
    if (value.startsWith("http://") || value.startsWith("https://")) return value
    return try {
        val url = io.ktor.http.Url(base)
        when {
            value.startsWith("//") -> "${url.protocol.name}:$value"
            value.startsWith("/") -> "${url.protocol.name}://${url.host}$value"
            else -> "${url.protocol.name}://${url.host}/${value.removePrefix("./")}"
        }
    } catch (_: Exception) {
        null
    }
}

private fun hostAndPath(url: String): Pair<String, String>? = try {
    val parsed = io.ktor.http.Url(url)
    parsed.host.lowercase() to parsed.encodedPath
} catch (_: Exception) {
    null
}

object LinkImageResolver {

    private const val MAX_BODY_BYTES = 192 * 1024
    private const val MAX_CACHE_ENTRIES = 512

    private val cache = LinkedHashMap<String, ImageSource>()
    private val inflight = mutableSetOf<String>()
    private val mutex = Mutex()

    @OptIn(InternalCoroutinesApi::class)
    private val cacheLock = SynchronizedObject()

    fun isAutoLoadHost(url: String): Boolean {
        if (!OutboundUrlPolicy.isFetchAllowed(url)) return false
        val host = httpUrlHost(url) ?: return false
        if (host in AUTOLOAD_HOSTS) return true
        return AUTOLOAD_HOSTS.any { host.endsWith(".$it") }
    }

    fun isDirectImageUrl(url: String): Boolean {
        if (!OutboundUrlPolicy.isFetchAllowed(url)) return false
        val lower = url.lowercase().substringBefore('#')
        if (DIRECT_IMAGE_EXT.matches(lower)) return true
        val host = httpUrlHost(url) ?: return false
        return host in DIRECT_IMAGE_HOSTS
    }

    fun hasResolver(url: String): Boolean {
        if (!OutboundUrlPolicy.isFetchAllowed(url)) return false
        if (isDirectImageUrl(url)) return true
        val (host, path) = hostAndPath(url) ?: return false
        return RESOLVERS.any { it.matches(host, path) }
    }

    @OptIn(InternalCoroutinesApi::class)
    fun cached(url: String): ImageSource? = synchronized(cacheLock) { cache[url] }

    fun resolveImmediate(url: String): String? {
        if (!OutboundUrlPolicy.isFetchAllowed(url)) return null
        if (isDirectImageUrl(url)) return url
        val (host, path) = hostAndPath(url) ?: return null
        val resolver = RESOLVERS.firstOrNull { it.matches(host, path) } ?: return null
        return resolver.template(host, path)
    }

    suspend fun resolve(httpClient: HttpClient, url: String): ImageSource {
        cached(url)?.let { return it }

        resolveImmediate(url)?.let {
            val source = ImageSource.Direct(it)
            put(url, source)
            return source
        }

        if (!hasResolver(url) || !OutboundUrlPolicy.isFetchAllowed(url)) {
            put(url, ImageSource.None)
            return ImageSource.None
        }

        val acquired = mutex.withLock {
            if (url in inflight) false else { inflight.add(url); true }
        }
        if (!acquired) {
            repeat(20) {
                kotlinx.coroutines.delay(150)
                cached(url)?.let { return it }
            }
            return ImageSource.None
        }

        try {
            val (host, path) = hostAndPath(url) ?: return ImageSource.None
            val resolver = RESOLVERS.firstOrNull { it.matches(host, path) } ?: return ImageSource.None
            val html = fetchHtml(httpClient, url)
            val image = html?.let { resolver.fromHtml(it, url) ?: genericHtmlImage(it, url) }
            val source = image
                ?.takeIf { isSafeHttpUrl(it) && OutboundUrlPolicy.isFetchAllowed(it) }
                ?.let { ImageSource.Direct(it) }
                ?: ImageSource.None
            put(url, source)
            return source
        } finally {
            mutex.withLock { inflight.remove(url) }
        }
    }

    private suspend fun fetchHtml(httpClient: HttpClient, url: String): String? = try {
        val response = httpClient.get(url) {
            header(HttpHeaders.UserAgent, BROWSER_USER_AGENT)
            header(HttpHeaders.Accept, "text/html,application/xhtml+xml;q=0.9,*/*;q=0.8")
        }
        if (!response.status.isSuccess()) null
        else {
            val contentType = response.headers[HttpHeaders.ContentType].orEmpty()
            if (contentType.startsWith("image/", ignoreCase = true)) url
            else if (contentType.isNotEmpty() &&
                !contentType.contains("html", ignoreCase = true) &&
                !contentType.contains("xml", ignoreCase = true)
            ) null
            else response.bodyAsChannel().readRemaining(MAX_BODY_BYTES.toLong()).readText()
        }
    } catch (_: Exception) {
        null
    }

    @OptIn(InternalCoroutinesApi::class)
    private fun put(url: String, source: ImageSource) {
        synchronized(cacheLock) {
            cache.remove(url)
            cache[url] = source
            while (cache.size > MAX_CACHE_ENTRIES) {
                val oldest = cache.keys.firstOrNull() ?: break
                cache.remove(oldest)
            }
        }
    }

    internal const val BROWSER_USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/126.0.0.0 Safari/537.36"
}
