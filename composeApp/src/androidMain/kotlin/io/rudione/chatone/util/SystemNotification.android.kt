package io.rudione.chatone.util

actual fun showSystemNotification(title: String, body: String) {
    // Android: proper push notifications need a channel + permission flow; not wired yet.
}
