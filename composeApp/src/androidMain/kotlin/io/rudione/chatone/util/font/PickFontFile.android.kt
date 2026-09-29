package io.rudione.chatone.util.font

import io.rudione.chatone.util.media.AndroidFilePicker

actual suspend fun pickFontFile(): String? = AndroidFilePicker.pick(
    arrayOf("font/ttf", "font/otf", "application/x-font-ttf", "application/octet-stream"),
    "fonts"
)
