@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)

package io.rudione.chatone.util.media

import platform.AudioToolbox.AudioServicesPlaySystemSound
import platform.AVFAudio.AVAudioPlayer
import platform.Foundation.NSURL
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi

actual object NotificationSoundPlayer {
    private var activePlayer: AVAudioPlayer? = null
    actual fun playMentionSound() {
        playMentionSound(volume = 0.8f, customSoundPath = "")
    }

    actual fun playMentionSound(volume: Float, customSoundPath: String) {
        activePlayer?.stop()
        activePlayer = null
        if (volume <= 0f) return
        if (customSoundPath.isNotBlank()) {
            try {
                val url = NSURL.fileURLWithPath(customSoundPath)
                val player = AVAudioPlayer(contentsOfURL = url, error = null)
                activePlayer = player
                player.volume = volume.coerceIn(0f, 1f)
                player.prepareToPlay()
                player.play()
            } catch (_: Exception) {
                AudioServicesPlaySystemSound(1007u)
            }
        } else {
            AudioServicesPlaySystemSound(1007u)
        }
    }
}
