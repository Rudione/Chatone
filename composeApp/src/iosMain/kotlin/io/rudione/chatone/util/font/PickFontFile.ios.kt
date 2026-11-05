package io.rudione.chatone.util.font

import io.rudione.chatone.util.media.IosFilePicker
import platform.UniformTypeIdentifiers.UTTypeFont

actual suspend fun pickFontFile(): String? = IosFilePicker.pickDocument(listOf(UTTypeFont), "fonts")
