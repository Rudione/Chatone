package io.rudione.chatone

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import io.rudione.chatone.di.appModules
import org.koin.core.context.startKoin

fun main() {
    startKoin {
        modules(appModules())
    }

    application {
        val windowState = rememberWindowState(
            width = 1200.dp,
            height = 800.dp
        )

        Window(
            onCloseRequest = ::exitApplication,
            title = "Chatone - Twitch Chat Client",
            state = windowState
        ) {
            App()
        }
    }
}
