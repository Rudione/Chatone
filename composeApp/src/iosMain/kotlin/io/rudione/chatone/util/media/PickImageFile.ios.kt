package io.rudione.chatone.util.media

actual suspend fun pickImageFile(): String? = IosFilePicker.pickImage("images")
