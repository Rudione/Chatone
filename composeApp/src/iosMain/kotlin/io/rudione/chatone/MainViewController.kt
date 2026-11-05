package io.rudione.chatone

import androidx.compose.ui.window.ComposeUIViewController
import io.rudione.chatone.di.appModules
import io.rudione.chatone.presentation.notifications.startNotificationServices
import io.rudione.chatone.util.media.AppImageLoaderInstaller
import io.rudione.chatone.util.system.IosNotificationRouter
import io.rudione.chatone.util.system.observeAppForeground
import org.koin.core.context.startKoin
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController {
    val koin = startKoin {
        modules(appModules())
    }.koin
    koin.get<AppImageLoaderInstaller>().install()
    observeAppForeground()
    IosNotificationRouter.install()
    koin.startNotificationServices()
    return ComposeUIViewController {
        App()
    }
}
