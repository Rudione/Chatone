package io.rudione.chatone.util.emote

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Codec
import org.jetbrains.skia.Data
import org.jetbrains.skia.Image
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.io.encoding.Base64
import kotlin.test.AfterTest
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalEncodingApi::class)
class AnimatedEmoteDecodeTest {

    private val bytes = Base64.decode(
        "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" +
        "AAAvB4ABEA8wPGM+PPMf8BAIJFrDzR5HRP+jcQ=="
    )

    @Test
    fun fixtureHasFramesThatBuildOnEarlierOnes() {
        val codec = Codec.makeFromData(Data.makeFromBytes(bytes))
        assertTrue(codec.framesInfo.drop(1).any { it.requiredFrame >= 0 })
    }

    @Test
    fun sequentialDecodeMatchesDecodingEveryFrameOnItsOwn() {
        val decoded = assertNotNull(AnimatedEmoteLoader.decodeAnimatedFrames(bytes, 0))
        val reference = referenceFrames()
        assertEquals(reference.size, decoded.frames.size)
        reference.forEachIndexed { index, expected ->
            assertContentEquals(expected.pixels(), decoded.frames[index].pixels(), "frame $index")
        }
    }

    @AfterTest
    fun restoreFetcher() {
        AnimatedEmoteLoader.fetchBytes = { _, _, _ -> null }
        AnimatedEmoteLoader.clear()
    }

    @Test
    fun slowDownloadsDoNotHoldBackOtherEmotes() = runBlocking {
        AnimatedEmoteLoader.clear()
        val never = CompletableDeferred<ByteArray?>()
        AnimatedEmoteLoader.fetchBytes = { url, _, _ -> if (url.contains("slow")) never.await() else bytes }
        coroutineScope {
            val stuck = (1..6).map { async { AnimatedEmoteLoader.load("https://cdn.test/slow$it.webp") } }
            val fast = withTimeout(5_000) { AnimatedEmoteLoader.load("https://cdn.test/fast.webp") }
            assertEquals(24, fast?.frames?.size)
            never.complete(null)
            stuck.forEach { it.await() }
        }
    }

    @Test
    fun failedDownloadIsNotRememberedAsStatic() = runBlocking {
        AnimatedEmoteLoader.clear()
        AnimatedEmoteLoader.fetchBytes = { _, _, _ -> null }
        val url = "https://cdn.test/flaky.webp"
        assertNull(AnimatedEmoteLoader.load(url))
        assertFalse(AnimatedEmoteLoader.isKnownStatic(url))
    }

    private fun referenceFrames(): List<ImageBitmap> {
        val codec = Codec.makeFromData(Data.makeFromBytes(bytes))
        return (0 until codec.frameCount).map { index ->
            val bitmap = Bitmap()
            bitmap.allocPixels(codec.imageInfo)
            codec.readPixels(bitmap, index)
            Image.makeFromBitmap(bitmap).toComposeImageBitmap().also { bitmap.close() }
        }
    }

    private fun ImageBitmap.pixels(): IntArray = IntArray(width * height).also { readPixels(it) }
}
