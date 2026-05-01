package io.rudione.chatone.util

import com.russhwolf.settings.Settings
import io.github.aakira.napier.Napier
import java.io.File
import java.util.prefs.Preferences

actual object AppDataCleaner {
    actual fun clearAll() {
        try {
            Settings().clear()
        } catch (e: Exception) {
            Napier.e("Failed to clear settings: ${e.message}", e, tag = "AppDataCleaner")
        }
        try {
            Preferences.userRoot().removeNode()
        } catch (_: Exception) {}
        try {
            val dataDir = File(System.getProperty("user.home"), ".chatone")
            if (dataDir.exists()) {
                dataDir.deleteRecursively()
            }
        } catch (e: Exception) {
            Napier.e("Failed to delete data dir: ${e.message}", e, tag = "AppDataCleaner")
        }
    }
}
