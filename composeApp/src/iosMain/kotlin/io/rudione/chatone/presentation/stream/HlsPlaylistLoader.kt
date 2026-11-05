package io.rudione.chatone.presentation.stream

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCSignatureOverride
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.AVFoundation.AVAssetResourceLoader
import platform.AVFoundation.AVAssetResourceLoaderDelegateProtocol
import platform.AVFoundation.AVAssetResourceLoadingRequest
import platform.Foundation.NSError
import platform.Foundation.NSHTTPURLResponse
import platform.Foundation.NSMutableURLRequest
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.NSURLErrorBadServerResponse
import platform.Foundation.NSURLErrorDomain
import platform.Foundation.NSURLSession
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataTaskWithRequest
import platform.Foundation.dataUsingEncoding
import platform.darwin.NSObject
import kotlin.coroutines.resume

private const val MASTER_SCHEME = "chatone-master"
private const val MEDIA_SCHEME = "chatone-media"
private const val HTTPS_PREFIX = "https://"
private const val PLAYLIST_UTI = "public.m3u-playlist"
private const val FETCH_TIMEOUT_SECONDS = 10.0
private val HTTP_OK = 200..299
private const val MAP_TAG = "#EXT-X-MAP:"
private val MAP_URI = Regex("""URI="([^"]+)"""")

internal class PlaylistFetch(val status: Int, val body: String?)

internal fun interface MediaPlaylistTransform {
    suspend fun transform(url: String, playlist: String): String
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
internal class HlsPlaylistLoader(
    private val masterPlaylist: String?,
    private val transform: MediaPlaylistTransform,
    private val onMediaStatus: (Int) -> Unit
) : NSObject(), AVAssetResourceLoaderDelegateProtocol {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    val masterUrl: NSURL = NSURL.URLWithString("$MASTER_SCHEME://chatone/master.m3u8")!!

    fun mediaUrl(remote: String): NSURL? = NSURL.URLWithString(toProxy(remote))

    fun rewriteMaster(text: String): String = text.lineSequence().joinToString("\n") { line ->
        val trimmed = line.trim()
        if (trimmed.startsWith(HTTPS_PREFIX)) toProxy(trimmed) else line
    }

    fun close() = scope.cancel()

    @ObjCSignatureOverride
    override fun resourceLoader(
        resourceLoader: AVAssetResourceLoader,
        shouldWaitForLoadingOfRequestedResource: AVAssetResourceLoadingRequest
    ): Boolean {
        val request = shouldWaitForLoadingOfRequestedResource
        val url = request.request.URL?.absoluteString ?: return false
        when {
            url.startsWith("$MASTER_SCHEME://") -> {
                val master = masterPlaylist ?: return false
                respond(request, rewriteMaster(master))
            }

            url.startsWith("$MEDIA_SCHEME://") -> scope.launch {
                val remote = fromProxy(url)
                val fetched = fetch(remote)
                onMediaStatus(fetched.status)
                val body = fetched.body
                if (body == null) {
                    request.finishLoadingWithError(NSError.errorWithDomain(NSURLErrorDomain, NSURLErrorBadServerResponse, null))
                } else {
                    val absolute = absolutizeMediaPlaylist(body, remote)
                    respond(request, runCatching { transform.transform(remote, absolute) }.getOrDefault(absolute))
                }
            }

            else -> return false
        }
        return true
    }

    private fun respond(request: AVAssetResourceLoadingRequest, text: String) {
        val data = NSString.create(string = text).dataUsingEncoding(NSUTF8StringEncoding)
        if (data == null) {
            request.finishLoadingWithError(NSError.errorWithDomain(NSURLErrorDomain, NSURLErrorBadServerResponse, null))
            return
        }
        request.contentInformationRequest?.apply {
            contentType = PLAYLIST_UTI
            contentLength = data.length.toLong()
            byteRangeAccessSupported = false
        }
        request.dataRequest?.respondWithData(data)
        request.finishLoading()
    }

    private suspend fun fetch(url: String): PlaylistFetch {
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

    private fun toProxy(remote: String): String = MEDIA_SCHEME + "://" + remote.removePrefix(HTTPS_PREFIX)

    private fun fromProxy(proxy: String): String = HTTPS_PREFIX + proxy.removePrefix("$MEDIA_SCHEME://")
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
            trimmed.startsWith("#") -> line
            else -> resolve(trimmed)
        }
    }
}
