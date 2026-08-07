package io.rudione.chatone.util.media

actual suspend fun pickAudioFile(): String? =
    AndroidFilePicker.pick(arrayOf("audio/*"), "sounds")
