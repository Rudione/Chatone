package io.rudione.chatone

import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import io.rudione.chatone.di.appModules
import io.rudione.chatone.presentation.settings.SettingsViewModel
import io.rudione.chatone.util.AutoUpdater
import io.rudione.chatone.util.GlobalKeyDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.context.startKoin

fun main() {
    startKoin {
        modules(appModules())
    }

    // Non-blocking update probe. Delay lets the UI render first so the
    // JOptionPane update dialog (if any) appears over an already-visible window.
    CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
        delay(3000)
        runCatching { AutoUpdater.checkForUpdates(showDialog = true) }
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
            alwaysOnTop = alwaysOnTop,
            // Global key routing: ChatScreen (and future screens) register
            // handlers via GlobalKeyDispatcher. Window-level onPreviewKeyEvent
            // fires regardless of inner focus — so pause hotkeys work whether
            // the chat input is focused or not.
            onPreviewKeyEvent = { GlobalKeyDispatcher.dispatch(it) }
        ) {
            App(onAlwaysOnTopChanged = { alwaysOnTop = it })
        }
    }
}
