package io.rudione.chatone.util

import android.app.Application
import android.content.Intent
import io.github.aakira.napier.Napier
import org.koin.mp.KoinPlatform
import kotlin.system.exitProcess

actual object AppRestarter {
    actual fun restart(delayMs: Long) {
        try {
            val app = KoinPlatform.getKoin().get<Application>()
            val pm = app.packageManager
            val launch = pm.getLaunchIntentForPackage(app.packageName)
            if (launch != null) {
                val intent = Intent.makeRestartActivityTask(launch.component).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
                app.startActivity(intent)
            }
        } catch (e: Exception) {
            Napier.e("Failed to restart Android app: ${e.message}", e, tag = "AppRestarter")
        } finally {
            exitProcess(0)
        }
    }
}
