@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)

package io.rudione.chatone.util.media

import io.rudione.chatone.domain.sound.SoundOutput
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFAudio.AVAudioPlayer
import platform.AudioToolbox.AudioServicesPlaySystemSound
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

actual object NotificationSoundPlayer : SoundOutput {
    private const val MAX_CACHED = 12
    private const val FALLBACK_SOUND = 1007u
    private val players = LinkedHashMap<String, AVAudioPlayer>()

    actual override fun preload(paths: Collection<String>, mediaStream: Boolean) {
        val list = paths.toList()
        dispatch_async(dispatch_get_main_queue()) { list.forEach { player(it) } }
    }

    actual override fun play(path: String, volume: Float, mediaStream: Boolean) {
        val level = volume.coerceIn(0f, 1f)
        dispatch_async(dispatch_get_main_queue()) {
            val player = player(path)
            if (player == null) {
                AudioServicesPlaySystemSound(FALLBACK_SOUND)
                return@dispatch_async
            }
            player.stop()
            player.currentTime = 0.0
            player.volume = level
            if (!player.play()) AudioServicesPlaySystemSound(FALLBACK_SOUND)
        }
    }

    actual override fun stop() {
        dispatch_async(dispatch_get_main_queue()) { players.values.forEach { it.stop() } }
    }

    private fun player(path: String): AVAudioPlayer? {
        players.remove(path)?.let { cached ->
            players[path] = cached
            return cached
        }
        if (!NSFileManager.defaultManager.fileExistsAtPath(path)) return null
        val created = runCatching { AVAudioPlayer(contentsOfURL = NSURL.fileURLWithPath(path), error = null) }.getOrNull()
            ?: return null
        created.prepareToPlay()
        players[path] = created
        while (players.size > MAX_CACHED) {
            val eldest = players.keys.first()
            players.remove(eldest)?.stop()
        }
        return created
    }
}
