package io.rudione.chatone.presentation.chat.emote

import android.content.Context
import android.graphics.drawable.Animatable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.util.LruCache
import coil3.SingletonImageLoader
import coil3.asDrawable
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async

private const val MIN_CACHE_BYTES = 8L * 1024 * 1024
private const val MAX_CACHE_BYTES = 48L * 1024 * 1024
private const val MEMORY_SHARE = 12
private const val BYTES_PER_PIXEL = 4

internal object SharedEmoteDrawables {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val inFlight = HashMap<String, Deferred<Drawable?>>()
    private val listeners = HashMap<Drawable, MutableSet<() -> Unit>>()

    private val cache = object : LruCache<String, Drawable>(cacheBudget()) {
        override fun sizeOf(key: String, value: Drawable): Int = value.estimatedBytes()
    }

    private val fanOut = object : Drawable.Callback {
        override fun invalidateDrawable(who: Drawable) {
            listeners[who]?.forEach { it() }
        }

        override fun scheduleDrawable(who: Drawable, what: Runnable, `when`: Long) {
            mainHandler.postAtTime(what, who, `when`)
        }

        override fun unscheduleDrawable(who: Drawable, what: Runnable) {
            mainHandler.removeCallbacks(what, who)
        }
    }

    fun keyOf(url: String, maxDimension: Int): String =
        if (maxDimension > 0) "$url#$maxDimension" else url

    fun peek(key: String): Drawable? = cache.get(key)

    fun evictAll() = cache.evictAll()

    suspend fun load(context: Context, url: String, maxDimension: Int): Drawable? {
        val key = keyOf(url, maxDimension)
        cache.get(key)?.let { return it }
        val pending = inFlight.getOrPut(key) {
            scope.async {
                try {
                    fetch(context, url, maxDimension)?.also { cache.put(key, it) }
                } finally {
                    inFlight.remove(key)
                }
            }
        }
        return pending.await()
    }

    fun attach(drawable: Drawable, listener: () -> Unit) {
        val registered = listeners.getOrPut(drawable) { LinkedHashSet() }
        registered += listener
        if (registered.size == 1) {
            drawable.callback = fanOut
            (drawable as? Animatable)?.start()
        }
    }

    fun detach(drawable: Drawable, listener: () -> Unit) {
        val registered = listeners[drawable] ?: return
        registered -= listener
        if (registered.isNotEmpty()) return
        listeners.remove(drawable)
        (drawable as? Animatable)?.stop()
        drawable.callback = null
    }

    private suspend fun fetch(context: Context, url: String, maxDimension: Int): Drawable? = runCatching {
        val request = ImageRequest.Builder(context)
            .data(url)
            .apply { if (maxDimension > 0) size(maxDimension) }
            .build()
        val result = SingletonImageLoader.get(context).execute(request) as? SuccessResult
        result?.image?.asDrawable(context.resources)
    }.getOrNull()

    private fun cacheBudget(): Int =
        (Runtime.getRuntime().maxMemory() / MEMORY_SHARE).coerceIn(MIN_CACHE_BYTES, MAX_CACHE_BYTES).toInt()
}

private fun Drawable.estimatedBytes(): Int {
    (this as? BitmapDrawable)?.bitmap?.let { return it.allocationByteCount }
    val pixels = intrinsicWidth.coerceAtLeast(1).toLong() * intrinsicHeight.coerceAtLeast(1)
    val frames = if (this is Animatable) 2 else 1
    return (pixels * BYTES_PER_PIXEL * frames).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
}
