package io.rudione.chatone.util.media

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.PorterDuff
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.readRawBytes
import io.rudione.chatone.util.link.isSafeHttpUrl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject
import java.nio.ByteBuffer

private const val FRAME_INTERVAL_MS = 40L
private const val MAX_DOWNLOAD_BYTES = 8 * 1024 * 1024
private const val MAX_ANIMATORS = 24

@Composable
actual fun rememberAnimatedFrame(
    url: String,
    paused: Boolean,
    maxDimension: Int
): ImageBitmap? {
    if (url.isBlank() || Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return null

    val httpClient: HttpClient = koinInject()
    var animated by remember(url) { mutableStateOf(AnimatedImageStore.peek(url)) }

    LaunchedEffect(url, maxDimension) {
        if (animated != null || AnimatedImageStore.isKnown(url)) return@LaunchedEffect
        animated = AnimatedImageStore.load(url, maxDimension) {
            if (!isSafeHttpUrl(url)) return@load null
            runCatching { httpClient.get(url).readRawBytes() }
                .getOrNull()
                ?.takeIf { it.isNotEmpty() && it.size <= MAX_DOWNLOAD_BYTES }
        }
    }

    val holder = animated ?: return null

    DisposableEffect(holder, paused) {
        if (!paused) holder.retain()
        onDispose { if (!paused) holder.release() }
    }

    return holder.frame
}

private object AnimatedImageStore {

    private val cache = mutableMapOf<String, AnimatedImageHolder?>()
    private val mutex = Mutex()

    fun peek(url: String): AnimatedImageHolder? = cache[url]

    fun isKnown(url: String): Boolean = cache.containsKey(url)

    suspend fun load(
        url: String,
        maxDimension: Int,
        fetch: suspend () -> ByteArray?
    ): AnimatedImageHolder? = mutex.withLock {
        cache[url]?.let { return@withLock it }
        if (cache.containsKey(url) || cache.size >= MAX_ANIMATORS) {
            cache[url] = null
            return@withLock null
        }
        val bytes = fetch()
        val holder = bytes?.let { withContext(Dispatchers.Default) { decode(it, maxDimension) } }
        cache[url] = holder
        holder
    }

    private fun decode(bytes: ByteArray, maxDimension: Int): AnimatedImageHolder? = runCatching {
        val source = ImageDecoder.createSource(ByteBuffer.wrap(bytes))
        val drawable = ImageDecoder.decodeDrawable(source) { decoder, info, _ ->
            val longestSide = maxOf(info.size.width, info.size.height)
            if (maxDimension in 1 until longestSide) {
                val scale = maxDimension.toFloat() / longestSide
                decoder.setTargetSize(
                    (info.size.width * scale).toInt().coerceAtLeast(1),
                    (info.size.height * scale).toInt().coerceAtLeast(1)
                )
            }
        }
        (drawable as? AnimatedImageDrawable)?.let { AnimatedImageHolder(it) }
    }.getOrNull()
}

private class AnimatedImageHolder(private val drawable: AnimatedImageDrawable) {

    private val width = drawable.intrinsicWidth.coerceAtLeast(1)
    private val height = drawable.intrinsicHeight.coerceAtLeast(1)
    private val buffers = Array(2) { Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888) }
    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val callback = object : Drawable.Callback {
        override fun invalidateDrawable(who: Drawable) = Unit

        override fun scheduleDrawable(who: Drawable, what: Runnable, time: Long) {
            handler.postAtTime(what, who, time)
        }

        override fun unscheduleDrawable(who: Drawable, what: Runnable) {
            handler.removeCallbacks(what, who)
        }
    }

    private var consumers = 0
    private var job: Job? = null

    var frame: ImageBitmap? by mutableStateOf(null)
        private set

    fun retain() {
        if (consumers++ > 0) return
        drawable.callback = callback
        drawable.setBounds(0, 0, width, height)
        drawable.repeatCount = AnimatedImageDrawable.REPEAT_INFINITE
        drawable.start()
        job = scope.launch {
            var index = 0
            while (isActive) {
                val target = buffers[index % buffers.size]
                index++
                val canvas = Canvas(target)
                canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
                drawable.draw(canvas)
                frame = target.asImageBitmap()
                delay(FRAME_INTERVAL_MS)
            }
        }
    }

    fun release() {
        if (--consumers > 0) return
        consumers = 0
        job?.cancel()
        job = null
        drawable.stop()
        drawable.callback = null
    }
}
