package io.rudione.chatone.util

import platform.AudioToolbox.AudioServicesPlaySystemSound

actual object NotificationSoundPlayer {
    actual fun playMentionSound() {
        // System sound 1007 = "Tink" (short notification sound)
        AudioServicesPlaySystemSound(1007u)
    }
}
