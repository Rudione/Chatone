package io.rudione.chatone.util

import android.app.Application
import com.russhwolf.settings.Settings
import io.github.aakira.napier.Napier
import org.koin.mp.KoinPlatform
import java.io.File

actual object AppDataCleaner {
    actual fun clearAll() {
        try {
            Settings().clear()
        } catch (e: Exception) {
            Napier.e("Failed to clear settings: ${e.message}", e, tag = "AppDataCleaner")
        }
        try {
            val app = KoinPlatform.getKoin().get<Application>()
            listOfNotNull(
                app.filesDir,
                app.cacheDir,
                app.getExternalFilesDir(null),
                app.externalCacheDir
            ).forEach { dir ->
                runCatching {
                    dir.listFiles()?.forEach { f -> if (f.exists()) f.deleteRecursively() }
                }
            }
            val prefsDir = File(app.applicationInfo.dataDir, "shared_prefs")
            if (prefsDir.exists()) {
                prefsDir.listFiles()?.forEach { it.delete() }
            }
        } catch (e: Exception) {
            Napier.e("Failed to clear android data: ${e.message}", e, tag = "AppDataCleaner")
        }
    }
}
