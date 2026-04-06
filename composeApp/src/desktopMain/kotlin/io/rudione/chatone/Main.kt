package io.rudione.chatone

import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import io.github.aakira.napier.Napier
import io.rudione.chatone.di.appModules
import io.rudione.chatone.presentation.settings.SettingsViewModel
import io.rudione.chatone.util.AutoUpdater
import io.rudione.chatone.util.UpdateResult
import kotlinx.coroutines.*
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

        var alwaysOnTop by remember { mutableStateOf(SettingsViewModel.loadInitialState().alwaysOnTop) }

        // ▼▼▼ ИСПРАВЛЕНИЕ: Запускаем checkForUpdates в coroutine scope ▼▼▼
        LaunchedEffect(Unit) {
            // Не блокируем UI: запускаем проверку в фоне
            launch(Dispatchers.IO) {
                try {
                    val result = AutoUpdater.checkForUpdates(showDialog = true)
                    when (result) {
                        is UpdateResult.UpToDate -> Napier.d("App is up to date")
                        is UpdateResult.Available -> Napier.i("Update offered: ${result.version}")
                        is UpdateResult.Error -> Napier.w("Update check failed: ${result.exception.message}")
                    }
                } catch (e: Exception) {
                    Napier.e("Update check crashed: ${e.message}", e)
                }
            }
        }
        // ▲▲▲ ▲▲▲

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