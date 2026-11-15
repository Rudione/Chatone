package io.rudione.chatone

import androidx.compose.ui.window.ComposeUIViewController
import io.rudione.chatone.presentation.feedback.IosControlFeedback
import io.rudione.chatone.di.appModules
import io.rudione.chatone.di.lowLatencyPlayerModule
import io.rudione.chatone.presentation.notifications.startNotificationServices
import io.rudione.chatone.presentation.stream.lowlatency.LowLatencyPlayerProvider
import io.rudione.chatone.presentation.system.VideoAwareHostController
import io.rudione.chatone.util.media.AppImageLoaderInstaller
import io.rudione.chatone.util.system.IosNotificationRouter
import io.rudione.chatone.util.system.observeAppForeground
import org.koin.core.context.startKoin
import platform.UIKit.UIViewController
import io.rudione.chatone.presentation.liveactivity.IosLiveEventBridge

fun MainViewController(lowLatencyPlayers: LowLatencyPlayerProvider?): UIViewController {
    val koin = startKoin {
        modules(appModules() + listOfNotNull(lowLatencyPlayers?.let(::lowLatencyPlayerModule)))
    }.koin
    koin.get<AppImageLoaderInstaller>().install()
    observeAppForeground()
    IosNotificationRouter.install()
    koin.startNotificationServices()
    IosLiveEventBridge.install(koin.get(), koin.get(), koin.get(), koin.get())
    return VideoAwareHostController(
        ComposeUIViewController {
            IosControlFeedback { App() }
        }
    )
}
