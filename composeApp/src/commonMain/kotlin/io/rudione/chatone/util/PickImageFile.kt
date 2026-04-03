package io.rudione.chatone.util

import androidx.compose.ui.graphics.ImageBitmap

/** Opens system file picker for images. Returns absolute path or null if cancelled. */
expect suspend fun pickImageFile(): String?
