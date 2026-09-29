package io.rudione.chatone

import androidx.compose.ui.window.ComposeUIViewController
import io.rudione.chatone.di.appModules
import io.rudione.chatone.presentation.notifications.startNotificationServices
import org.koin.core.context.startKoin
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController {
    startKoin {
        modules(appModules())
    }.koin.startNotificationServices()
    return ComposeUIViewController {
        App()
    }
}
