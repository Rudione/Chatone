package io.rudione.chatone.util.emote

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSHTTPURLResponse
import platform.Foundation.NSMutableURLRequest
import platform.Foundation.NSURL
import platform.Foundation.NSURLSession
import platform.Foundation.dataTaskWithRequest
import platform.Foundation.setValue
import platform.posix.memcpy
import kotlin.coroutines.resume

private const val REQUEST_TIMEOUT_SECONDS = 15.0
private const val HTTP_OK_MIN = 200
private const val HTTP_OK_MAX = 299

@OptIn(ExperimentalForeignApi::class)
internal actual suspend fun fetchAnimatedImageBytes(url: String, accept: String, maxBytes: Int): ByteArray? {
    val target = NSURL.URLWithString(url) ?: return null
    val request = NSMutableURLRequest.requestWithURL(target).apply {
        setValue(accept, forHTTPHeaderField = "Accept")
        setTimeoutInterval(REQUEST_TIMEOUT_SECONDS)
    }
    return suspendCancellableCoroutine { continuation ->
        val task = NSURLSession.sharedSession.dataTaskWithRequest(request) { data, response, error ->
            val status = (response as? NSHTTPURLResponse)?.statusCode?.toInt() ?: 0
            val length = data?.length?.toInt() ?: 0
            val bytes = if (error != null || data == null || status !in HTTP_OK_MIN..HTTP_OK_MAX || length == 0 || length > maxBytes) {
                null
            } else {
                ByteArray(length).also { out -> out.usePinned { memcpy(it.addressOf(0), data.bytes, data.length) } }
            }
            continuation.resume(bytes)
        }
        continuation.invokeOnCancellation { task.cancel() }
        task.resume()
    }
}
