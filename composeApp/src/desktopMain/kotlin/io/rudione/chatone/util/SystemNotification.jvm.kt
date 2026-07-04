package io.rudione.chatone.util

import androidx.compose.ui.window.Notification
import androidx.compose.ui.window.TrayState

/** Set once by Main.kt when the Tray composable is up; notifications route through it. */
object DesktopTrayHolder {
    var trayState: TrayState? = null
}

actual fun showSystemNotification(title: String, body: String) {
    DesktopTrayHolder.trayState?.sendNotification(
        Notification(title, body, Notification.Type.Info)
    )
}
