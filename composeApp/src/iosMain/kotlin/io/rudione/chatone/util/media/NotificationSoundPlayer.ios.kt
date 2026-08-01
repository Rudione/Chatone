@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package io.rudione.chatone.util.media

import platform.AudioToolbox.AudioServicesPlaySystemSound
import platform.AVFAudio.AVAudioPlayer
import platform.Foundation.NSURL

actual object NotificationSoundPlayer {
    actual fun playMentionSound() {
        playMentionSound(volume = 0.8f, customSoundPath = "")
    }

    actual fun playMentionSound(volume: Float, customSoundPath: String) {
        if (customSoundPath.isNotBlank()) {
            try {
                val url = NSURL.fileURLWithPath(customSoundPath)
                val player = AVAudioPlayer(contentsOfURL = url, error = null)
                player.volume = volume
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
