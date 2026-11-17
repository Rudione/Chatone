package io.rudione.chatone.util.media

import io.rudione.chatone.data.sound.decodePlatformAudio
import io.rudione.chatone.domain.sound.SoundLimits
import io.rudione.chatone.domain.sound.SoundOutput
import io.rudione.chatone.domain.sound.SoundSynth
import io.rudione.chatone.domain.sound.WavCodec
import kotlinx.coroutines.runBlocking
import java.io.ByteArrayInputStream
import java.io.File
import java.util.Collections
import java.util.concurrent.Executors
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import javax.sound.sampled.FloatControl
import javax.sound.sampled.LineEvent
import kotlin.math.log10

actual object NotificationSoundPlayer : SoundOutput {
    private const val MAX_CACHED = 12
    private const val MAX_CACHED_BYTES = 4_000_000L
    private val executor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "mention-sound").apply { isDaemon = true }
    }
    private val cache = Collections.synchronizedMap(object : LinkedHashMap<String, ByteArray>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ByteArray>?): Boolean = size > MAX_CACHED
    })
    private val clips = Collections.synchronizedSet(LinkedHashSet<Clip>())
    private val javaSoundFormats = setOf("wav", "aif", "aiff", "au")

    actual override fun preload(paths: Collection<String>, mediaStream: Boolean) {
        val files = paths.map(::File).filter { it.isFile }
        if (files.isEmpty()) return
        executor.execute { files.forEach { runCatching { bytesFor(it) } } }
    }

    actual override fun play(path: String, volume: Float, mediaStream: Boolean) {
        val file = File(path)
        if (!file.isFile) return
        val level = volume.coerceIn(0f, 1f)
        executor.execute {
            val bytes = runCatching { bytesFor(file) }.getOrNull()
            if (bytes == null || !playBytes(bytes, level)) playTone(level)
        }
    }

    actual override fun stop() {
        val active = synchronized(clips) { clips.toList().also { clips.clear() } }
        active.forEach { runCatching { it.stop() }; runCatching { it.close() } }
    }

    private fun bytesFor(file: File): ByteArray? {
        val key = "${file.absolutePath}#${file.lastModified()}#${file.length()}"
        cache[key]?.let { return it }
        val bytes = if (file.extension.lowercase() in javaSoundFormats && file.length() <= MAX_CACHED_BYTES) {
            file.readBytes()
        } else {
            runBlocking { decodePlatformAudio(file.absolutePath, SoundLimits.MAX_CLIP_MS * 2) }?.let(WavCodec::encode)
        } ?: return null
        if (bytes.size <= MAX_CACHED_BYTES) cache[key] = bytes
        return bytes
    }

    private fun playBytes(bytes: ByteArray, volume: Float): Boolean = runCatching {
        val stream = AudioSystem.getAudioInputStream(ByteArrayInputStream(bytes))
        val clip = AudioSystem.getClip()
        clip.open(stream)
        stream.close()
        applyVolume(clip, volume)
        clip.addLineListener { event ->
            if (event.type == LineEvent.Type.STOP) {
                clips.remove(clip)
                clip.close()
            }
        }
        clips.add(clip)
        clip.start()
        true
    }.getOrDefault(false)

    private fun applyVolume(clip: Clip, volume: Float) {
        runCatching {
            val gain = clip.getControl(FloatControl.Type.MASTER_GAIN) as FloatControl
            val db = (20.0 * log10(volume.coerceAtLeast(0.0001f).toDouble())).toFloat()
            gain.value = db.coerceIn(gain.minimum, gain.maximum)
        }.recoverCatching {
            val control = clip.getControl(FloatControl.Type.VOLUME) as FloatControl
            control.value = volume.coerceIn(control.minimum, control.maximum)
        }
    }

    private fun playTone(volume: Float) {
        runCatching { playBytes(WavCodec.encode(SoundSynth.beep()), volume) }
    }
}
