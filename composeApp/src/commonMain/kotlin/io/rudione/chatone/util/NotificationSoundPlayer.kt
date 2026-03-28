package io.rudione.chatone.util

expect object NotificationSoundPlayer {
    fun playMentionSound()
    fun playMentionSound(volume: Float, customSoundPath: String)
}
