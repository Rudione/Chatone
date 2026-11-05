package io.rudione.chatone.util.system

import platform.Foundation.NSUUID
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotification
import platform.UserNotifications.UNNotificationPresentationOptionBanner
import platform.UserNotifications.UNNotificationPresentationOptionList
import platform.UserNotifications.UNNotificationPresentationOptionSound
import platform.UserNotifications.UNNotificationPresentationOptions
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationResponse
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNUserNotificationCenter
import platform.UserNotifications.UNUserNotificationCenterDelegateProtocol
import platform.darwin.NSObject

private const val KEY_CHANNEL = "channel"
private const val KEY_MESSAGE = "message"

actual fun showSystemNotification(
    title: String,
    body: String,
    topic: NotificationTopic,
    target: NotificationTarget?
) {
    if (!NotificationAccess.granted.value) return
    val content = UNMutableNotificationContent().apply {
        setTitle(title)
        setBody(body)
        setSound(UNNotificationSound.defaultSound)
        setThreadIdentifier(target?.channelLogin ?: topic.name.lowercase())
        target?.let { setUserInfo(listOfNotNull(KEY_CHANNEL to it.channelLogin, it.messageId?.let { id -> KEY_MESSAGE to id }).toMap<Any?, Any?>()) }
    }
    val request = UNNotificationRequest.requestWithIdentifier(NSUUID().UUIDString, content, trigger = null)
    UNUserNotificationCenter.currentNotificationCenter().addNotificationRequest(request, withCompletionHandler = null)
}

actual fun dismissSystemNotifications(channelLogin: String) {
    val center = UNUserNotificationCenter.currentNotificationCenter()
    center.getDeliveredNotificationsWithCompletionHandler { delivered ->
        val ids = delivered.orEmpty().mapNotNull { it as? UNNotification }
            .filter { it.request.content.threadIdentifier == channelLogin }
            .map { it.request.identifier }
        if (ids.isNotEmpty()) center.removeDeliveredNotificationsWithIdentifiers(ids)
    }
}

object IosNotificationRouter {
    private val delegate = NotificationDelegate()

    fun install() {
        UNUserNotificationCenter.currentNotificationCenter().delegate = delegate
        NotificationAccess.refresh()
    }
}

private class NotificationDelegate : NSObject(), UNUserNotificationCenterDelegateProtocol {

    override fun userNotificationCenter(
        center: UNUserNotificationCenter,
        willPresentNotification: UNNotification,
        withCompletionHandler: (UNNotificationPresentationOptions) -> Unit
    ) {
        withCompletionHandler(UNNotificationPresentationOptionBanner or UNNotificationPresentationOptionList or UNNotificationPresentationOptionSound)
    }

    override fun userNotificationCenter(
        center: UNUserNotificationCenter,
        didReceiveNotificationResponse: UNNotificationResponse,
        withCompletionHandler: () -> Unit
    ) {
        val info = didReceiveNotificationResponse.notification.request.content.userInfo
        NotificationTarget.of(info[KEY_CHANNEL] as? String, info[KEY_MESSAGE] as? String)?.let(NotificationLaunches::publish)
        withCompletionHandler()
    }
}
