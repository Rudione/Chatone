package io.rudione.chatone.util.media

actual suspend fun pickImageFile(): String? =
    AndroidFilePicker.pick(arrayOf("image/*"), "images")
