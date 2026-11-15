package io.rudione.chatone.presentation.stream

import kotlinx.cinterop.BetaInteropApi
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSHTTPURLResponse
import platform.Foundation.NSMutableURLRequest
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.NSURLSession
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataTaskWithRequest
import kotlin.coroutines.resume

private const val FETCH_TIMEOUT_SECONDS = 10.0
private val HTTP_OK = 200..299
private const val MAP_TAG = "#EXT-X-MAP:"
private const val PREFETCH_TAG = "#EXT-X-TWITCH-PREFETCH:"
private val MAP_URI = Regex("""URI="([^"]+)"""")

internal class PlaylistFetch(val status: Int, val body: String?)

internal fun interface MediaPlaylistTransform {
    suspend fun transform(url: String, playlist: String): String
}

internal fun interface MediaPlaylistSource {
    suspend fun fetch(url: String): PlaylistFetch
}

@OptIn(BetaInteropApi::class)
internal object UrlSessionPlaylistSource : MediaPlaylistSource {
    override suspend fun fetch(url: String): PlaylistFetch {
        val target = NSURL.URLWithString(url) ?: return PlaylistFetch(0, null)
        val request = NSMutableURLRequest.requestWithURL(target).apply { setTimeoutInterval(FETCH_TIMEOUT_SECONDS) }
        return suspendCancellableCoroutine { continuation ->
            val task = NSURLSession.sharedSession.dataTaskWithRequest(request) { data, response, error ->
                val status = (response as? NSHTTPURLResponse)?.statusCode?.toInt() ?: 0
                val body = if (error == null && data != null && status in HTTP_OK) {
                    NSString.create(data = data, encoding = NSUTF8StringEncoding)?.toString()
                } else {
                    null
                }
                continuation.resume(PlaylistFetch(status, body))
            }
            continuation.invokeOnCancellation { task.cancel() }
            task.resume()
        }
    }
}

internal fun absolutizeMediaPlaylist(playlist: String, baseUrl: String): String {
    val base = NSURL.URLWithString(baseUrl) ?: return playlist
    fun resolve(uri: String): String =
        if (uri.contains("://")) uri else NSURL.URLWithString(uri, relativeToURL = base)?.absoluteString ?: uri
    return playlist.lineSequence().joinToString("\n") { line ->
        val trimmed = line.trim()
        when {
            trimmed.isEmpty() -> line
            trimmed.startsWith(MAP_TAG) -> MAP_URI.replace(trimmed) { match -> "URI=\"${resolve(match.groupValues[1])}\"" }
            trimmed.startsWith(PREFETCH_TAG) -> PREFETCH_TAG + resolve(trimmed.substring(PREFETCH_TAG.length).trim())
            trimmed.startsWith("#") -> line
            else -> resolve(trimmed)
        }
    }
}
