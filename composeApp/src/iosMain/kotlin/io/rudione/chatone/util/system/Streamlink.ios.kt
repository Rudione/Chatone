package io.rudione.chatone.util.system

import platform.Foundation.NSURL
import platform.UIKit.UIApplication

actual fun openInStreamlink(channelLogin: String, quality: String) {
    val nsUrl = NSURL.URLWithString("https://twitch.tv/$channelLogin") ?: return
    UIApplication.sharedApplication.openURL(nsUrl)
}
