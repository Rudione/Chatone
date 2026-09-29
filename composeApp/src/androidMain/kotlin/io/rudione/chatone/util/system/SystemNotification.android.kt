package io.rudione.chatone.util.system

import android.app.Application
import io.github.aakira.napier.Napier
import org.koin.mp.KoinPlatform

actual fun showSystemNotification(
    title: String,
    body: String,
    topic: NotificationTopic,
    target: NotificationTarget?
) {
    try {
        val app = KoinPlatform.getKoin().get<Application>()
        AndroidNotifier.notify(app, title, body, topic, target)
    } catch (e: Exception) {
        Napier.w("showSystemNotification failed: ${e.message}", tag = "SystemNotification")
    }
}

actual fun dismissSystemNotifications(channelLogin: String) {
    try {
        val app = KoinPlatform.getKoin().get<Application>()
        AndroidNotifier.dismiss(app, channelLogin)
    } catch (e: Exception) {
        Napier.w("dismissSystemNotifications failed: ${e.message}", tag = "SystemNotification")
    }
}
