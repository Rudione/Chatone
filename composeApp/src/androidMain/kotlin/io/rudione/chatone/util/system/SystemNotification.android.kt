package io.rudione.chatone.util.system

import android.app.Application
import io.github.aakira.napier.Napier
import org.koin.mp.KoinPlatform

actual fun showSystemNotification(title: String, body: String) {
    try {
        val app = KoinPlatform.getKoin().get<Application>()
        AndroidNotifier.notify(app, title, body)
    } catch (e: Exception) {
        Napier.w("showSystemNotification failed: ${e.message}", tag = "SystemNotification")
    }
}
