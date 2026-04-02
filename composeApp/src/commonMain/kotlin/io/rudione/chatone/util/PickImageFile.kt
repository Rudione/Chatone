package io.rudione.chatone.util

/** Opens system file picker for images. Returns absolute path or null if cancelled. */
expect suspend fun pickImageFile(): String?