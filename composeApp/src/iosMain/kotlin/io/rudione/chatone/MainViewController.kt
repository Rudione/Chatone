package io.rudione.chatone

import androidx.compose.ui.window.ComposeUIViewController
import io.rudione.chatone.di.appModules
import io.rudione.chatone.presentation.notifications.startNotificationServices
import io.rudione.chatone.util.media.AppImageLoaderInstaller
import org.koin.core.context.startKoin
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController {
    val koin = startKoin {
        modules(appModules())
    }.koin
    koin.get<AppImageLoaderInstaller>().install()
    koin.startNotificationServices()
    return ComposeUIViewController {
        App()
    }
}
