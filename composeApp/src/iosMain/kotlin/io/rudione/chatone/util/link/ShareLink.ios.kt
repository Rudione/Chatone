package io.rudione.chatone.util.link

import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIViewController
import platform.UIKit.UIWindowScene
import platform.UIKit.popoverPresentationController

actual fun shareLink(url: String, title: String): Boolean {
    if (!isSafeHttpUrl(url)) return false
    val presenter = topViewController() ?: return false
    val items = listOfNotNull(title.takeIf { it.isNotBlank() }, url)
    val sheet = UIActivityViewController(activityItems = items, applicationActivities = null)
    sheet.popoverPresentationController?.sourceView = presenter.view
    presenter.presentViewController(sheet, animated = true, completion = null)
    return true
}

private fun topViewController(): UIViewController? {
    val scene = UIApplication.sharedApplication.connectedScenes.firstOrNull { it is UIWindowScene } as? UIWindowScene
    var controller = scene?.keyWindow?.rootViewController ?: return null
    while (true) controller = controller.presentedViewController ?: return controller
}
