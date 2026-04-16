package io.rudione.chatone

import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.russhwolf.settings.Settings
import io.rudione.chatone.di.appModules
import io.rudione.chatone.presentation.settings.SettingsViewModel
import io.rudione.chatone.util.AutoUpdater
import io.rudione.chatone.util.GlobalKeyDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.core.context.startKoin

private const val WIN_X = "win_x"
private const val WIN_Y = "win_y"
private const val WIN_W = "win_w"
private const val WIN_H = "win_h"
private const val WIN_MAX = "win_maximized"

private val NATIVE_BG = java.awt.Color(0x0A, 0x0A, 0x0F)

fun main() {
   
   
    System.setProperty("apple.awt.application.appearance", "system")
   
    System.setProperty("apple.awt.application.name", "Chatone")

    startKoin {
        modules(appModules())
    }

    CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
        delay(3000)
        runCatching { AutoUpdater.checkForUpdates(showDialog = true) }
    }

    application {
        val settings = remember { Settings() }

        val savedX = settings.getFloatOrNull(WIN_X)
        val savedY = settings.getFloatOrNull(WIN_Y)
        val savedW = settings.getFloat(WIN_W, 800f)
        val savedH = settings.getFloat(WIN_H, 900f)
        val savedMaximized = settings.getBoolean(WIN_MAX, false)

        val windowState = rememberWindowState(
            width = savedW.dp,
            height = savedH.dp,
            position = if (savedX != null && savedY != null)
                WindowPosition(savedX.dp, savedY.dp)
            else
                WindowPosition.PlatformDefault,
            placement = if (savedMaximized) WindowPlacement.Maximized else WindowPlacement.Floating
        )

       
        LaunchedEffect(windowState) {
            snapshotFlow {
                WindowSnapshot(
                    width = windowState.size.width.value,
                    height = windowState.size.height.value,
                    position = windowState.position,
                    placement = windowState.placement
                )
            }.collect { snap ->
                if (snap.placement == WindowPlacement.Maximized) {
                    settings.putBoolean(WIN_MAX, true)
                } else {
                    settings.putBoolean(WIN_MAX, false)
                    settings.putFloat(WIN_W, snap.width)
                    settings.putFloat(WIN_H, snap.height)
                    (snap.position as? WindowPosition.Absolute)?.let { pos ->
                        settings.putFloat(WIN_X, pos.x.value)
                        settings.putFloat(WIN_Y, pos.y.value)
                    }
                }
            }
        }

        var alwaysOnTop by remember { mutableStateOf(SettingsViewModel.loadInitialState().alwaysOnTop) }

        Window(
            onCloseRequest = { exitApplication() },
            title = "Chatone - Twitch Chat Client",
            state = windowState,
            alwaysOnTop = alwaysOnTop,
            onPreviewKeyEvent = { GlobalKeyDispatcher.dispatch(it) }
        ) {
           
           
            LaunchedEffect(Unit) {
                window.background = NATIVE_BG
                window.contentPane.background = NATIVE_BG
                runCatching {
                    window.rootPane.putClientProperty("apple.awt.windowAppearance", "NSAppearanceNameDarkAqua")
                }
            }
            App(onAlwaysOnTopChanged = { alwaysOnTop = it })
        }
    }
}

private data class WindowSnapshot(
    val width: Float,
    val height: Float,
    val position: WindowPosition,
    val placement: WindowPlacement
)
