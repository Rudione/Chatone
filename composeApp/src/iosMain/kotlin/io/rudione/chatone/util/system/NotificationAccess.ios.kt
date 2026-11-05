package io.rudione.chatone.util.system

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNAuthorizationStatusEphemeral
import platform.UserNotifications.UNAuthorizationStatusProvisional
import platform.UserNotifications.UNUserNotificationCenter
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

actual object NotificationAccess {
    private val state = MutableStateFlow(false)
    actual val granted: StateFlow<Boolean> = state.asStateFlow()

    private val allowedStatuses = setOf(UNAuthorizationStatusAuthorized, UNAuthorizationStatusProvisional, UNAuthorizationStatusEphemeral)

    actual fun refresh() {
        UNUserNotificationCenter.currentNotificationCenter().getNotificationSettingsWithCompletionHandler { settings ->
            val allowed = settings?.authorizationStatus in allowedStatuses
            dispatch_async(dispatch_get_main_queue()) { state.value = allowed }
        }
    }

    actual fun request() {
        val options = UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge
        UNUserNotificationCenter.currentNotificationCenter().requestAuthorizationWithOptions(options) { allowed, _ ->
            dispatch_async(dispatch_get_main_queue()) { state.value = allowed }
        }
    }

    actual fun openSystemSettings() {
        val url = NSURL.URLWithString(UIApplicationOpenSettingsURLString) ?: return
        UIApplication.sharedApplication.openURL(url, options = emptyMap<Any?, Any?>(), completionHandler = null)
    }
}
