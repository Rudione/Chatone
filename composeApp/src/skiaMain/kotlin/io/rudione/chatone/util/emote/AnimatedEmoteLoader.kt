package io.rudione.chatone.util.emote

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import io.rudione.chatone.util.concurrent.IoDispatcher
import io.rudione.chatone.util.link.isSafeHttpUrl
import io.rudione.chatone.util.media.scaledTo
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.jetbrains.skia.AnimationDisposalMode
import org.jetbrains.skia.AnimationFrameInfo
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Codec
import org.jetbrains.skia.Data
import org.jetbrains.skia.Image as SkiaImage
import kotlin.concurrent.Volatile
import kotlin.time.Clock

class AnimatedFrames(
    val frames: List<ImageBitmap>,
    val durations: IntArray,
    val byteSize: Long,
    @Volatile var lastAccess: Long = nowMs()
)

internal expect suspend fun fetchAnimatedImageBytes(url: String, accept: String, maxBytes: Int): ByteArray?

private fun nowMs(): Long = Clock.System.now().toEpochMilliseconds()

object AnimatedEmoteLoader {

    private const val CACHE_BUDGET_BYTES = 128L * 1024 * 1024
    private const val IDLE_TTL_MS = 5 * 60 * 1000L
    private const val MAX_FRAMES = 200
    private const val MAX_PIXELS = 4096 * 4096
    private const val PER_EMOTE_BYTE_CAP = 24L * 1024 * 1024
    private const val MAX_DOWNLOAD_BYTES = 8 * 1024 * 1024
    private const val MAX_STATIC_URLS = 20_000
    private const val SWEEP_INTERVAL_MS = 30_000L
    private const val IMAGE_ACCEPT = "image/webp,image/gif,image/apng,image/*;q=0.8"
    private const val MAX_PARALLEL_FETCHES = 12
    private const val MAX_PARALLEL_DECODES = 4
    private const val FAILURE_RETRY_MS = 20_000L

    private val lock = SynchronizedObject()
    private val cache = LinkedHashMap<String, AnimatedFrames>()
    private var cachedBytes = 0L
    private var lastSweepAt = 0L
    private val staticUrls = HashSet<String>()
    private val inflight = HashMap<String, Deferred<AnimatedFrames?>>()
    private val failures = HashMap<String, Long>()
    private val fetchGate = Semaphore(MAX_PARALLEL_FETCHES)
    private val decodeGate = Semaphore(MAX_PARALLEL_DECODES)
    private val loaderScope = CoroutineScope(SupervisorJob() + IoDispatcher)
    private var sweeper: Job? = null
    internal var fetchBytes: suspend (String, String, Int) -> ByteArray? = ::fetchAnimatedImageBytes

    fun isKnownStatic(url: String, maxDimension: Int = 0): Boolean =
        synchronized(lock) { cacheKey(url, maxDimension) in staticUrls }

    fun peek(url: String, maxDimension: Int = 0): AnimatedFrames? = synchronized(lock) {
        val key = cacheKey(url, maxDimension)
        val hit = cache.remove(key) ?: return@synchronized null
        cache[key] = hit
        hit.lastAccess = nowMs()
        hit
    }

    suspend fun load(url: String, maxDimension: Int = 0): AnimatedFrames? {
        val key = cacheKey(url, maxDimension)
        peek(url, maxDimension)?.let { return it }
        if (isKnownStatic(url, maxDimension)) return null
        if (recentlyFailed(key)) return null
        val candidate = loaderScope.async(start = CoroutineStart.LAZY) {
            peek(url, maxDimension) ?: if (isKnownStatic(url, maxDimension)) null else fetchAndDecode(url, maxDimension)
        }
        val existing = synchronized(lock) {
            inflight[key] ?: run {
                inflight[key] = candidate
                null
            }
        }
        if (existing != null) {
            candidate.cancel()
            return existing.await()
        }
        candidate.invokeOnCompletion {
            synchronized(lock) { if (inflight[key] === candidate) inflight.remove(key) }
        }
        candidate.start()
        return candidate.await()
    }

    fun clear() = synchronized(lock) {
        cache.clear()
        cachedBytes = 0
        staticUrls.clear()
        failures.clear()
    }

    private fun cacheKey(url: String, maxDimension: Int): String =
        if (maxDimension > 0) "$url#$maxDimension" else url

    private suspend fun fetchAndDecode(url: String, maxDimension: Int): AnimatedFrames? {
        val key = cacheKey(url, maxDimension)
        if (!isSafeHttpUrl(url)) return markStatic(key)
        val bytes = fetchGate.withPermit {
            try {
                fetchBytes(url, IMAGE_ACCEPT, MAX_DOWNLOAD_BYTES)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                null
            }
        } ?: return markFailed(key)
        return decodeGate.withPermit { decode(key, bytes, maxDimension) }
    }

    private fun decode(key: String, bytes: ByteArray, maxDimension: Int): AnimatedFrames? =
        decodeAnimatedFrames(bytes, maxDimension)?.also { store(key, it) } ?: markStatic(key)

    private fun recentlyFailed(key: String): Boolean = synchronized(lock) {
        val failedAt = failures[key] ?: return@synchronized false
        if (nowMs() - failedAt < FAILURE_RETRY_MS) return@synchronized true
        failures.remove(key)
        false
    }

    private fun markFailed(key: String): AnimatedFrames? = synchronized(lock) {
        if (failures.size > MAX_STATIC_URLS) failures.clear()
        failures[key] = nowMs()
        null
    }

    internal fun decodeAnimatedFrames(bytes: ByteArray, maxDimension: Int): AnimatedFrames? {
        val data = Data.makeFromBytes(bytes)
        val codec = try {
            Codec.makeFromData(data)
        } catch (_: Exception) {
            data.close()
            return null
        }
        return try {
            val imageInfo = codec.imageInfo
            val frameCount = codec.frameCount
            val pixels = imageInfo.width.toLong() * imageInfo.height.toLong()
            if (frameCount <= 1 || pixels <= 0 || pixels > MAX_PIXELS) return null
            val longestSide = maxOf(imageInfo.width, imageInfo.height)
            val scale = if (maxDimension in 1 until longestSide) maxDimension.toFloat() / longestSide else 1f
            val targetWidth = (imageInfo.width * scale).toInt().coerceAtLeast(1)
            val targetHeight = (imageInfo.height * scale).toInt().coerceAtLeast(1)
            val bytesPerFrame = targetWidth.toLong() * targetHeight.toLong() * 4L
            val affordableFrames = (PER_EMOTE_BYTE_CAP / bytesPerFrame).toInt().coerceAtLeast(1)
            val usableFrames = minOf(frameCount, MAX_FRAMES, affordableFrames)
            if (usableFrames < 2) return null
            val infos = codec.framesInfo
            val durations = IntArray(usableFrames) { i -> infos[i].duration.coerceAtLeast(16) }
            val canvas = Bitmap()
            val frames = try {
                canvas.allocPixels(imageInfo)
                (0 until usableFrames).map { i ->
                    codec.composeFrame(canvas, infos, i)
                    SkiaImage.makeFromBitmap(canvas).toComposeImageBitmap().scaledTo(targetWidth, targetHeight)
                }
            } finally {
                canvas.close()
            }
            AnimatedFrames(frames, durations, usableFrames.toLong() * bytesPerFrame)
        } catch (_: Exception) {
            null
        } finally {
            codec.close()
            data.close()
        }
    }

    private fun Codec.composeFrame(canvas: Bitmap, infos: Array<AnimationFrameInfo>, index: Int) {
        val required = infos[index].requiredFrame
        val prior = index - 1
        if (required >= 0 && prior >= required && infos[prior].disposalMethod != AnimationDisposalMode.RESTORE_PREVIOUS) {
            readPixels(canvas, index, prior)
        } else {
            canvas.erase(0)
            readPixels(canvas, index)
        }
    }

    private fun markStatic(key: String): AnimatedFrames? = synchronized(lock) {
        if (staticUrls.size > MAX_STATIC_URLS) staticUrls.clear()
        staticUrls.add(key)
        null
    }

    private fun store(key: String, frames: AnimatedFrames) = synchronized(lock) {
        cache.remove(key)?.let { cachedBytes -= it.byteSize }
        cache[key] = frames
        cachedBytes += frames.byteSize
        evictLocked()
        if (sweeper?.isActive != true) sweeper = loaderScope.launch { sweepPeriodically() }
    }

    private suspend fun sweepPeriodically() {
        while (true) {
            delay(SWEEP_INTERVAL_MS)
            val empty = synchronized(lock) {
                sweepIdleLocked(nowMs())
                cache.isEmpty()
            }
            if (empty) return
        }
    }

    private fun sweepIdleLocked(now: Long) {
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

    private fun evictLocked() {
        val now = nowMs()
        if (now - lastSweepAt > SWEEP_INTERVAL_MS) sweepIdleLocked(now)
        val lru = cache.entries.iterator()
        while (cachedBytes > CACHE_BUDGET_BYTES && lru.hasNext()) {
            val entry = lru.next()
            cachedBytes -= entry.value.byteSize
            lru.remove()
        }
    }
}
