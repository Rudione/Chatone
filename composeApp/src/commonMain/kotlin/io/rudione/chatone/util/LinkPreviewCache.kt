package io.rudione.chatone.util

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private val backgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

data class LinkPreview(
    val title: String? = null,
    val description: String? = null,
    val imageUrl: String? = null,
    val siteName: String? = null,
    val channelName: String? = null,
    val viewCount: String? = null,
    val duration: String? = null,
    val isYouTube: Boolean = false,
    val isImage: Boolean = false
) {
    val isEmpty: Boolean
        get() = title.isNullOrBlank() && description.isNullOrBlank() &&
                imageUrl.isNullOrBlank() && channelName.isNullOrBlank() && !isImage
}

@Serializable
private data class YouTubeOEmbed(
    val title: String,
    val author_name: String,
    val thumbnail_url: String,
    val thumbnail_width: Int? = null,
    val thumbnail_height: Int? = null
)

object LinkPreviewCache {
    private const val TTL_MS = 60 * 60 * 1000L
    private const val MAX_BODY_BYTES = 256 * 1024

    private data class Entry(val preview: LinkPreview, val fetchedAt: Long)
    private val cache = mutableMapOf<String, Entry>()
    private val inflight = mutableSetOf<String>()
    private val mutex = Mutex()
    @OptIn(InternalCoroutinesApi::class)
    private val cacheLock = SynchronizedObject()


    @OptIn(InternalCoroutinesApi::class)
    fun cached(url: String): LinkPreview? {
        val entry = synchronized(cacheLock) {
            cache[url]
        } ?: return null

        val age = nowMs() - entry.fetchedAt
        return if (age < TTL_MS) entry.preview else null
    }

    private fun extractYouTubeVideoId(url: String): String? {
        return when {
            url.contains("youtube.com/watch?v=") ->
                url.substringAfter("watch?v=").substringBefore("&").substringBefore("#").takeIf { it.length == 11 }
            url.contains("youtu.be/") ->
                url.substringAfter("youtu.be/").substringBefore("?").substringBefore("#").takeIf { it.length == 11 }
            url.contains("youtube.com/embed/") ->
                url.substringAfter("embed/").substringBefore("?").substringBefore("#").takeIf { it.length == 11 }
            url.contains("youtube.com/shorts/") ->
                url.substringAfter("shorts/").substringBefore("?").substringBefore("#").takeIf { it.length == 11 }
            else -> null
        }
    }

    suspend fun fetch(httpClient: HttpClient, url: String): LinkPreview {
        cached(url)?.let { return it }

        val lockAcquired = mutex.withLock {
            if (url in inflight) return@withLock false
            inflight.add(url)
            true
        }

        if (!lockAcquired) {
            repeat(20) {
                kotlinx.coroutines.delay(150)
                cached(url)?.let { return it }
            }
        }

        try {
            if (isDirectImageUrl(url)) {
                val preview = LinkPreview(imageUrl = url, isImage = true)
                withContext(NonCancellable) { put(url, preview) }
                return preview
            }

            val preview = try {
                val text = httpClient.get(url) {
                    header(HttpHeaders.UserAgent, "ChatoneLinkPreview/1.0")
                    header(HttpHeaders.Accept, "text/html,*/*;q=0.8")
                }.let { resp ->
                    if (!resp.status.isSuccess()) return@let ""
                    val ct = resp.headers[HttpHeaders.ContentType] ?: ""
                    if (ct.startsWith("image/", ignoreCase = true)) {
                        val imgPreview = LinkPreview(imageUrl = url, isImage = true)
                        withContext(NonCancellable) { put(url, imgPreview) }
                        return imgPreview
                    }
                    if (!ct.contains("html", ignoreCase = true) &&
                        !ct.contains("xml", ignoreCase = true) &&
                        ct.isNotEmpty()
                    ) return@let ""
                    val body = resp.bodyAsText()
                    if (body.length > MAX_BODY_BYTES) body.take(MAX_BODY_BYTES) else body
                }

                val isYouTube = url.contains("youtube.com", ignoreCase = true) || url.contains("youtu.be", ignoreCase = true)
                if (isYouTube) {
                    val videoId = extractYouTubeVideoId(url)
                    if (videoId != null) {
                        fetchYouTubeOEmbed(httpClient, videoId)?.let { oembedPreview ->
                            val htmlPreview = parseMetaTags(text, url)
                            return oembedPreview.copy(
                                description = htmlPreview.description?.takeIf { it.isNotBlank() } ?: oembedPreview.description,
                                duration = htmlPreview.duration
                            )
                        }
                    }
                }

                parseMetaTags(text, url)

            } catch (_: Exception) {
                LinkPreview()
            }

            withContext(NonCancellable) {
                put(url, preview)
            }

            return preview

        } finally {
            mutex.withLock { inflight.remove(url) }
        }
    }

    private fun put(url: String, preview: LinkPreview) {
        synchronized(cacheLock) {
            cache[url] = Entry(preview, nowMs())
        }
    }

    private fun nowMs(): Long = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()

    private fun isDirectImageUrl(url: String): Boolean {
        val l = url.lowercase().substringBefore("?")
        return l.endsWith(".png") || l.endsWith(".jpg") || l.endsWith(".jpeg") ||
                l.endsWith(".gif") || l.endsWith(".webp") || l.endsWith(".bmp") ||
                l.endsWith(".svg")
    }

    private val META_TAG = Regex(
        """<meta\s+[^>]*(?:property|name)\s*=\s*["']([^"']+)["'][^>]*content\s*=\s*["']([^"']*)["'][^>]*/?>""",
        setOf(RegexOption.IGNORE_CASE)
    )
    private val META_TAG_REV = Regex(
        """<meta\s+[^>]*content\s*=\s*["']([^"']*)["'][^>]*(?:property|name)\s*=\s*["']([^"']+)["'][^>]*/?>""",
        setOf(RegexOption.IGNORE_CASE)
    )
    private val TITLE_TAG = Regex("""<title[^>]*>([\s\S]*?)</title>""", RegexOption.IGNORE_CASE)
    private val IMG_TAG = Regex(
        """<img\s+[^>]*src\s*=\s*["']([^"']+)["'][^>]*>""",
        setOf(RegexOption.IGNORE_CASE)
    )

    private fun parseMetaTags(html: String, sourceUrl: String): LinkPreview {
        if (html.isBlank()) return LinkPreview()

        val isYouTube = sourceUrl.contains("youtube.com", ignoreCase = true) ||
                sourceUrl.contains("youtu.be", ignoreCase = true)

        val tags = mutableMapOf<String, String>()
        META_TAG.findAll(html).forEach { m ->
            val key = m.groupValues[1].lowercase()
            if (key !in tags) tags[key] = decodeHtml(m.groupValues[2])
        }
        META_TAG_REV.findAll(html).forEach { m ->
            val key = m.groupValues[2].lowercase()
            if (key !in tags) tags[key] = decodeHtml(m.groupValues[1])
        }

        val title = tags["og:title"] ?: tags["twitter:title"]
        ?: TITLE_TAG.find(html)?.groupValues?.getOrNull(1)?.let { decodeHtml(it).trim() }

        val description = tags["og:description"] ?: tags["twitter:description"] ?: tags["description"]

        val image = tags["og:image"] ?: tags["og:image:url"] ?: tags["twitter:image"] ?: tags["twitter:image:src"]
        ?: IMG_TAG.find(html)?.groupValues?.getOrNull(1)
            ?.let { src ->
                if (src.startsWith("") || src.contains("pixel") || src.contains("1x1")) null
                else src
            }

        val siteName = tags["og:site_name"] ?: tags["application-name"]

        val (channelName, viewCount, duration) = if (isYouTube) {
            extractYouTubeData(html, tags)
        } else Triple(null, null, null)

        val resolvedImage = image?.let { resolveUrl(sourceUrl, it) }

        return LinkPreview(
            title = title?.take(160),
            description = description?.take(400),
            imageUrl = resolvedImage,
            siteName = siteName,
            channelName = channelName,
            viewCount = viewCount,
            duration = duration,
            isYouTube = isYouTube,
            isImage = false
        )
    }

    private fun extractYouTubeData(html: String, tags: Map<String, String>): Triple<String?, String?, String?> {

        val duration = tags["og:video:duration"]
            ?: Regex("""duration["']?\s*:\s*["']?([^"',}]+)""", RegexOption.IGNORE_CASE)
                .find(html)?.groupValues?.getOrNull(1)
                ?.let { parseISO8601Duration(it) }

        return Triple(tags["og:video:tag"] ?: tags["author"], null, duration)
    }

    private suspend fun fetchYouTubeOEmbed(httpClient: HttpClient, videoId: String): LinkPreview? {
        return try {
            val response = httpClient.get("https://www.youtube.com/oembed?url=https://www.youtube.com/watch?v=$videoId&format=json") {
                header(HttpHeaders.UserAgent, "ChatoneLinkPreview/1.0")
            }

            if (!response.status.isSuccess()) return null

            val oembed = Json { ignoreUnknownKeys = true }.decodeFromString<YouTubeOEmbed>(response.bodyAsText())

            LinkPreview(
                title = oembed.title.take(160),
                channelName = oembed.author_name.takeIf { it.isNotBlank() },
                imageUrl = oembed.thumbnail_url,
                siteName = "YouTube",
                isYouTube = true,
                isImage = false
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun parseISO8601Duration(duration: String): String? {
        val pattern = Regex("""PT(?:(\d+)H)?(?:(\d+)M)?(?:(\d+)S)?""")
        val match = pattern.find(duration) ?: return null
        val hours = match.groupValues[1].toIntOrNull() ?: 0
        val minutes = match.groupValues[2].toIntOrNull() ?: 0
        val seconds = match.groupValues[3].toIntOrNull() ?: 0
        return when {
            hours > 0 -> {
                val m = minutes.toString().padStart(2, '0')
                val s = seconds.toString().padStart(2, '0')
                "$hours:$m:$s"
            }
            else -> {
                val s = seconds.toString().padStart(2, '0')
                "$minutes:$s"
            }
        }
    }

    private fun resolveUrl(base: String, maybeRelative: String): String {
        if (maybeRelative.startsWith("http://") || maybeRelative.startsWith("https://")) return maybeRelative
        return try {
            val uri = io.ktor.http.Url(base)
            if (maybeRelative.startsWith("//")) "${uri.protocol.name}:$maybeRelative"
            else if (maybeRelative.startsWith("/")) "${uri.protocol.name}://${uri.host}$maybeRelative"
            else maybeRelative
        } catch (_: Exception) {
            maybeRelative
        }
    }

    private fun decodeHtml(s: String): String =
        s.replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&nbsp;", " ")
            .trim()
}