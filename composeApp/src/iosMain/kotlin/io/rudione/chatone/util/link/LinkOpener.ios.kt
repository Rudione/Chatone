package io.rudione.chatone.util.link

import io.rudione.chatone.presentation.settings.SettingsState
import platform.Foundation.NSURL
import platform.UIKit.UIApplication

actual fun openUrl(url: String, mode: SettingsState.LinkOpenMode) {
    if (!isSafeHttpUrl(url)) return
    val nsUrl = NSURL.URLWithString(url) ?: return
    UIApplication.sharedApplication.openURL(nsUrl)
}
