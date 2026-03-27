package io.rudione.chatone

import androidx.compose.ui.window.ComposeUIViewController
import io.rudione.chatone.di.appModules
import org.koin.core.context.startKoin
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController {
    startKoin {
        modules(appModules())
    }
    return ComposeUIViewController {
        App()
    }
}
