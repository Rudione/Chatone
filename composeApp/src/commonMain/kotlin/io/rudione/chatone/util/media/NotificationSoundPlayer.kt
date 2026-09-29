package io.rudione.chatone.util.media

expect object NotificationSoundPlayer {
    fun playMentionSound()
    fun playMentionSound(volume: Float, customSoundPath: String)
}
