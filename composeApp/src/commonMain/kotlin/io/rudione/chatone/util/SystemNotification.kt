package io.rudione.chatone.util

/** OS-level notification (desktop: tray balloon). No-op where unsupported. */
expect fun showSystemNotification(title: String, body: String)
