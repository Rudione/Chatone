package io.rudione.chatone.util.media

import io.rudione.chatone.domain.sound.SoundOutput

expect object NotificationSoundPlayer : SoundOutput {
    override fun preload(paths: Collection<String>, mediaStream: Boolean)
    override fun play(path: String, volume: Float, mediaStream: Boolean)
    override fun stop()
}
