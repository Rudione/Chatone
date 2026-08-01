package io.rudione.chatone.util.emote

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import io.rudione.chatone.util.link.isSafeHttpUrl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Codec
import org.jetbrains.skia.Data
import java.net.URI
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class AnimatedFrames(
    val frames: List<ImageBitmap>,
    val durations: IntArray,
    val byteSize: Long,
    @Volatile var lastAccess: Long = System.currentTimeMillis()
)

object AnimatedEmoteLoader {

    private const val CACHE_BUDGET_BYTES = 128L * 1024 * 1024
    private const val IDLE_TTL_MS = 5 * 60 * 1000L
    private const val MAX_FRAMES = 200
    private const val MAX_PIXELS = 4096 * 4096
    private const val PER_EMOTE_BYTE_CAP = 24L * 1024 * 1024
    private const val MAX_DOWNLOAD_BYTES = 8 * 1024 * 1024
    private const val MAX_STATIC_URLS = 20_000

    private const val SWEEP_INTERVAL_MS = 30_000L

    private val lock = ReentrantLock()
    private val cache = object : LinkedHashMap<String, AnimatedFrames>(128, 0.75f, true) {}
    private var cachedBytes = 0L
    private var lastSweepAt = 0L

    private val staticUrls = ConcurrentHashMap.newKeySet<String>()
    private val inflight = ConcurrentHashMap<String, Deferred<AnimatedFrames?>>()
    private val decodeGate = Semaphore(4)

    private val loaderScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun isKnownStatic(url: String): Boolean = url in staticUrls

    fun peek(url: String): AnimatedFrames? = lock.withLock {
        cache[url]?.also { it.lastAccess = System.currentTimeMillis() }
    }

    suspend fun load(url: String): AnimatedFrames? {
        peek(url)?.let { return it }
        if (url in staticUrls) return null

        val candidate = loaderScope.async(start = CoroutineStart.LAZY) {
            decodeGate.withPermit {
                peek(url) ?: if (url in staticUrls) null else fetchAndDecode(url)
            }
        }
        val existing = inflight.putIfAbsent(url, candidate)
        if (existing != null) {
            candidate.cancel()
            return existing.await()
        }
        candidate.invokeOnCompletion { inflight.remove(url, candidate) }
        candidate.start()
        return candidate.await()
    }

    private fun fetchAndDecode(url: String): AnimatedFrames? {
        if (!isSafeHttpUrl(url)) {
            markStatic(url)
            return null
        }

        val bytes = try {
            val conn = URI(url).toURL().openConnection()
            conn.connectTimeout = 10_000
            conn.readTimeout = 15_000
            conn.getInputStream().use { it.readNBytes(MAX_DOWNLOAD_BYTES) }
        } catch (_: Exception) {
            markStatic(url)
            return null
        }

        val data = Data.makeFromBytes(bytes)
        val codec = try {
            Codec.makeFromData(data)
        } catch (_: Exception) {
            data.close()
            markStatic(url)
            return null
        }

        return try {
            val imageInfo = codec.imageInfo
            val frameCount = codec.frameCount
            val pixels = imageInfo.width.toLong() * imageInfo.height.toLong()
            if (frameCount <= 1 || pixels <= 0 || pixels > MAX_PIXELS) {
                markStatic(url)
                return null
            }

            val bytesPerFrame = pixels * 4L
            val affordableFrames = (PER_EMOTE_BYTE_CAP / bytesPerFrame).toInt().coerceAtLeast(1)
            val usableFrames = minOf(frameCount, MAX_FRAMES, affordableFrames)
            if (usableFrames < 2) {
                markStatic(url)
                return null
            }
            val infos = codec.framesInfo
            val durations = IntArray(usableFrames) { i -> infos[i].duration.coerceAtLeast(16) }
            val frames = (0 until usableFrames).map { i ->
                val bmp = Bitmap()
                try {
                    bmp.allocPixels(imageInfo)
                    codec.readPixels(bmp, i)
                    bmp.toComposeSnapshot()
                } finally {
                    bmp.close()
                }
            }

            val byteSize = usableFrames.toLong() * bytesPerFrame
            val result = AnimatedFrames(frames, durations, byteSize)
            store(url, result)
            result
        } catch (_: Exception) {
            markStatic(url)
            null
        } finally {
            codec.close()
            data.close()
        }
    }

    private fun markStatic(url: String) {
        if (staticUrls.size > MAX_STATIC_URLS) staticUrls.clear()
        staticUrls.add(url)
    }

    private fun store(url: String, frames: AnimatedFrames) = lock.withLock {
        cache.put(url, frames)?.let { cachedBytes -= it.byteSize }
        cachedBytes += frames.byteSize
        evictLocked()
    }

    private fun evictLocked() {
        val now = System.currentTimeMillis()

        if (now - lastSweepAt > SWEEP_INTERVAL_MS) {
            lastSweepAt = now
            val iter = cache.entries.iterator()
            while (iter.hasNext()) {
                val entry = iter.next()
                if (now - entry.value.lastAccess > IDLE_TTL_MS) {
                    cachedBytes -= entry.value.byteSize
                    iter.remove()
                }
            }
        }

        val lru = cache.entries.iterator()
        while (cachedBytes > CACHE_BUDGET_BYTES && lru.hasNext()) {
            val entry = lru.next()
            cachedBytes -= entry.value.byteSize
            lru.remove()
        }
    }

    fun clear() = lock.withLock {
        cache.clear()
        cachedBytes = 0
        staticUrls.clear()
    }

    private fun Bitmap.toComposeSnapshot(): ImageBitmap =
        org.jetbrains.skia.Image.makeFromBitmap(this).toComposeImageBitmap()
}
