package io.rudione.chatone.util.media

import platform.UniformTypeIdentifiers.UTTypeAudio

actual suspend fun pickAudioFile(): String? = IosFilePicker.pickDocument(listOf(UTTypeAudio), "sounds")
