package io.rudione.chatone.util.system

import com.russhwolf.settings.Settings

actual object AppDataCleaner {
    actual fun clearAll() {
        runCatching { Settings().clear() }
    }
}
