package io.rudione.chatone.util.system

actual fun showSystemNotification(
    title: String,
    body: String,
    topic: NotificationTopic,
    target: NotificationTarget?
) = Unit

actual fun dismissSystemNotifications(channelLogin: String) = Unit
