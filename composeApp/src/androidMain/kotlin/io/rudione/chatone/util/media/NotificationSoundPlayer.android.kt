package io.rudione.chatone.util.media

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import io.github.aakira.napier.Napier
import io.rudione.chatone.domain.sound.SoundOutput
import java.io.File

actual object NotificationSoundPlayer : SoundOutput {
    private const val TAG = "MentionSound"
    private const val MAX_POOL_BYTES = 1_000_000L
    private const val MAX_CACHED = 12
    private const val MAX_TRACKED_STREAMS = 8
    private val poolFormats = setOf("wav", "ogg")
    private val lock = Any()
    private val slots = HashMap<Boolean, PoolSlot>()
    private val players = ArrayList<MediaPlayer>()

    private class PoolSlot(val pool: SoundPool) {
        val ids = LinkedHashMap<String, Int>(16, 0.75f, true)
        val paths = HashMap<Int, String>()
        val ready = HashSet<Int>()
        val waiting = HashMap<Int, Float>()
        val streams = ArrayDeque<Int>()
    }

    actual override fun preload(paths: Collection<String>, mediaStream: Boolean) {
        val files = paths.map(::File).filter { it.isFile && fitsPool(it) }
        if (files.isEmpty()) return
        synchronized(lock) {
            val slot = slot(mediaStream)
            files.forEach { load(slot, it) }
        }
    }

    actual override fun play(path: String, volume: Float, mediaStream: Boolean) {
        val file = File(path)
        if (!file.isFile) return
        val level = volume.coerceIn(0f, 1f)
        if (!fitsPool(file)) {
            playWithMediaPlayer(file.absolutePath, level, mediaStream)
            return
        }
        synchronized(lock) {
            val slot = slot(mediaStream)
            val id = load(slot, file)
            if (id == 0) return@synchronized
            if (id in slot.ready) start(slot, id, level) else slot.waiting[id] = level
        }
    }

    actual override fun stop() {
        val active: List<MediaPlayer>
        synchronized(lock) {
            slots.values.forEach { slot ->
                slot.streams.forEach { slot.pool.stop(it) }
                slot.streams.clear()
                slot.waiting.clear()
            }
            active = players.toList()
            players.clear()
        }
        active.forEach { runCatching { it.stop() }; runCatching { it.release() } }
    }

    private fun fitsPool(file: File): Boolean =
        file.extension.lowercase() in poolFormats && file.length() in 45L..MAX_POOL_BYTES

    private fun load(slot: PoolSlot, file: File): Int {
        val key = "${file.absolutePath}#${file.lastModified()}#${file.length()}"
        slot.ids[key]?.let { return it }
        val id = runCatching { slot.pool.load(file.absolutePath, 1) }.getOrDefault(0)
        if (id == 0) return 0
        slot.ids[key] = id
        slot.paths[id] = file.absolutePath
        while (slot.ids.size > MAX_CACHED) {
            val eldest = slot.ids.entries.first()
            slot.ids.remove(eldest.key)
            forget(slot, eldest.value)
            slot.pool.unload(eldest.value)
        }
        return id
    }

    private fun forget(slot: PoolSlot, id: Int) {
        slot.paths.remove(id)
        slot.ready.remove(id)
        slot.waiting.remove(id)
    }

    private fun start(slot: PoolSlot, id: Int, volume: Float) {
        val stream = slot.pool.play(id, volume, volume, 1, 0, 1f)
        if (stream == 0) return
        slot.streams.addLast(stream)
        while (slot.streams.size > MAX_TRACKED_STREAMS) slot.streams.removeFirst()
    }

    private fun slot(mediaStream: Boolean): PoolSlot = slots.getOrPut(mediaStream) {
        val pool = SoundPool.Builder()
            .setMaxStreams(4)
            .setAudioAttributes(attributes(mediaStream))
            .build()
        val slot = PoolSlot(pool)
        pool.setOnLoadCompleteListener { _, id, status -> onLoaded(slot, id, status, mediaStream) }
        slot
    }

    private fun onLoaded(slot: PoolSlot, id: Int, status: Int, mediaStream: Boolean) {
        var fallback: Pair<String, Float>? = null
        synchronized(lock) {
            val volume = slot.waiting.remove(id)
            if (status == 0) {
                slot.ready += id
                if (volume != null) start(slot, id, volume)
            } else {
                val path = slot.paths[id]
                slot.ids.entries.removeAll { it.value == id }
                forget(slot, id)
                if (volume != null && path != null) fallback = path to volume
            }
        }
        fallback?.let { (path, volume) -> playWithMediaPlayer(path, volume, mediaStream) }
    }

    private fun playWithMediaPlayer(path: String, volume: Float, mediaStream: Boolean) {
        val player = MediaPlayer()
        try {
            player.setAudioAttributes(attributes(mediaStream))
            player.setDataSource(path)
            player.setVolume(volume, volume)
            player.setOnPreparedListener { it.start() }
            player.setOnCompletionListener { release(it) }
            player.setOnErrorListener { mp, _, _ ->
                release(mp)
                true
            }
            synchronized(lock) { players += player }
            player.prepareAsync()
        } catch (e: Exception) {
            Napier.w("Mention sound failed: ${e.message}", tag = TAG)
            release(player)
        }
    }

    private fun release(player: MediaPlayer) {
        synchronized(lock) { players.remove(player) }
        runCatching { player.release() }
    }

    private fun attributes(mediaStream: Boolean): AudioAttributes = AudioAttributes.Builder()
        .setUsage(if (mediaStream) AudioAttributes.USAGE_MEDIA else AudioAttributes.USAGE_NOTIFICATION)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()
}
