package io.rudione.chatone.util.system

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationDidBecomeActiveNotification
import platform.UIKit.UIApplicationDidEnterBackgroundNotification
import platform.UIKit.UIApplicationState

private var observing = false
private val foreground = MutableStateFlow(true)

actual val appForeground: StateFlow<Boolean> = foreground.asStateFlow()

fun observeAppForeground() {
    if (observing) return
    observing = true
    foreground.value = UIApplication.sharedApplication.applicationState != UIApplicationState.UIApplicationStateBackground
    val center = NSNotificationCenter.defaultCenter
    center.addObserverForName(UIApplicationDidBecomeActiveNotification, null, NSOperationQueue.mainQueue) { _ ->
        NotificationAccess.refresh()
        foreground.value = true
    }
    center.addObserverForName(UIApplicationDidEnterBackgroundNotification, null, NSOperationQueue.mainQueue) { _ ->
        foreground.value = false
    }
}
