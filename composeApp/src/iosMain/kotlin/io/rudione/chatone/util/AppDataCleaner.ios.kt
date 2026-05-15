package io.rudione.chatone.util

import com.russhwolf.settings.Settings

actual object AppDataCleaner {
    actual fun clearAll() {
        runCatching { Settings().clear() }
    }
}
