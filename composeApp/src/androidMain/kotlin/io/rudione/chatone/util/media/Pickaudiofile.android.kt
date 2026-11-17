package io.rudione.chatone.util.media

actual suspend fun pickAudioFile(): String? =
    AndroidFilePicker.pick(arrayOf("audio/*", "video/mp4", "application/ogg"), "sounds")
