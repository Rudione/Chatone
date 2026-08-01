package io.rudione.chatone.util.system

expect fun showSystemNotification(title: String, body: String)

fun notifySystem(title: String, body: String) {
    val streamerMode =
        io.rudione.chatone.di.GlobalDi.tryGet<io.rudione.chatone.data.repository.StreamerModeController>()
    if (streamerMode != null && streamerMode.enabled && streamerMode.options.suppressNotifications) return
    showSystemNotification(title, body)
}
