package io.rudione.chatone.presentation.stream

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCSignatureOverride
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import platform.AVFoundation.AVAssetResourceLoader
import platform.AVFoundation.AVAssetResourceLoaderDelegateProtocol
import platform.AVFoundation.AVAssetResourceLoadingRequest
import platform.Foundation.NSError
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.NSURLErrorBadServerResponse
import platform.Foundation.NSURLErrorDomain
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.darwin.NSObject

private const val MASTER_SCHEME = "chatone-master"
private const val MEDIA_SCHEME = "chatone-media"
private const val HTTPS_PREFIX = "https://"
private const val PLAYLIST_UTI = "public.m3u-playlist"

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
internal class HlsPlaylistLoader(
    private val masterPlaylist: String?,
    private val transform: MediaPlaylistTransform,
    private val onMediaStatus: (Int) -> Unit,
    private val source: MediaPlaylistSource = UrlSessionPlaylistSource
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
                val fetched = source.fetch(remote)
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

    private fun toProxy(remote: String): String = MEDIA_SCHEME + "://" + remote.removePrefix(HTTPS_PREFIX)

    private fun fromProxy(proxy: String): String = HTTPS_PREFIX + proxy.removePrefix("$MEDIA_SCHEME://")
}
