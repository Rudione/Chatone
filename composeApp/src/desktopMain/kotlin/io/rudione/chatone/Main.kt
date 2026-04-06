package io.rudione.chatone

import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import io.rudione.chatone.di.appModules
import io.rudione.chatone.presentation.settings.SettingsViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.context.startKoin

fun main() {
    startKoin {
        modules(appModules())
    }

    application {
        val windowState = rememberWindowState(
            width = 800.dp,
            height = 900.dp
        )

        // Reactive alwaysOnTop — reads initial value, App() provides SettingsViewModel reactively
        var alwaysOnTop by remember { mutableStateOf(SettingsViewModel.loadInitialState().alwaysOnTop) }

        Window(
            onCloseRequest = ::exitApplication,
            title = "Chatone - Twitch Chat Client",
            state = windowState,
            alwaysOnTop = alwaysOnTop
        ) {
            App(onAlwaysOnTopChanged = { alwaysOnTop = it })
        }
    }
}
