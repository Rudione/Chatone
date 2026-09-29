package io.rudione.chatone.util.system

import androidx.compose.ui.window.Notification
import androidx.compose.ui.window.TrayState

object DesktopTrayHolder {
    var trayState: TrayState? = null
}

actual fun showSystemNotification(
    title: String,
    body: String,
    topic: NotificationTopic,
    target: NotificationTarget?
) {
    DesktopTrayHolder.trayState?.sendNotification(
        Notification(title, body, Notification.Type.Info)
    )
}

actual fun dismissSystemNotifications(channelLogin: String) = Unit
