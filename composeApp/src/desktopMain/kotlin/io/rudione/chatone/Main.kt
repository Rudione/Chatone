package io.rudione.chatone

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.russhwolf.settings.Settings
import io.rudione.chatone.di.appModules
import io.rudione.chatone.presentation.settings.SettingsViewModel
import io.rudione.chatone.presentation.settings.TitleBarMode
import io.rudione.chatone.util.AutoUpdater
import io.rudione.chatone.util.GlobalKeyDispatcher
import io.rudione.chatone.util.WindowsTitleBar
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

private val DARK_CAPTION_COLOR = Color(0x0D, 0x0F, 0x1A)

fun main() {
    System.setProperty("apple.awt.application.appearance", "system")
    System.setProperty("apple.awt.application.name", "Chatone")


    System.setProperty("http.maxConnections", "64")
    System.setProperty("jdk.httpclient.connectionPoolSize", "64")

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

        val initialSettings = remember { SettingsViewModel.loadInitialState() }
        var alwaysOnTop by remember { mutableStateOf(initialSettings.alwaysOnTop) }
        var isDarkTheme by remember { mutableStateOf(initialSettings.darkTheme) }
        var dominantColor by remember { mutableStateOf<Color?>(null) }
        var titleBarMode by remember { mutableStateOf(initialSettings.titleBarMode) }

        Window(
            onCloseRequest = { exitApplication() },
            title = "Chatone - Twitch Chat Client",
            state = windowState,
            alwaysOnTop = alwaysOnTop,
            onPreviewKeyEvent = { GlobalKeyDispatcher.dispatch(it) }
        ) {
            DisposableEffect(window) {
                val initialDark = isDarkTheme
                val listener = object : java.awt.event.WindowAdapter() {
                    override fun windowOpened(e: java.awt.event.WindowEvent) {
                        window.background = NATIVE_BG
                        window.contentPane.background = NATIVE_BG
                        runCatching {
                            window.rootPane.putClientProperty(
                                "apple.awt.windowAppearance",
                                "NSAppearanceNameDarkAqua"
                            )
                        }
                        val (captionColor, useDark) = resolveTitleBar(
                            initialSettings.titleBarMode, initialDark, null
                        )
                        WindowsTitleBar.applyTitleBarColor(window, captionColor, useDark)
                    }
                }
                window.addWindowListener(listener)
                onDispose { window.removeWindowListener(listener) }
            }

            LaunchedEffect(isDarkTheme, dominantColor, titleBarMode) {
                val (captionColor, useDark) = resolveTitleBar(titleBarMode, isDarkTheme, dominantColor)
                WindowsTitleBar.applyTitleBarColor(window, captionColor, useDark)
            }

            App(
                onAlwaysOnTopChanged = { alwaysOnTop = it },
                onThemeChanged = { isDarkTheme = it },
                onDominantColorChanged = { dominantColor = it },
                onTitleBarModeChanged = { titleBarMode = it }
            )
        }
    }
}

private fun resolveTitleBar(
    mode: TitleBarMode,
    isDarkTheme: Boolean,
    dominantColor: Color?
): Pair<Color, Boolean> = when (mode) {
    TitleBarMode.DARK -> DARK_CAPTION_COLOR to true
    TitleBarMode.LIGHT -> Color(0xF0, 0xF0, 0xF5) to false
    TitleBarMode.ADAPTIVE -> {
        val blended = dominantColor?.let { blendWithDark(it, isDarkTheme) }
            ?: if (isDarkTheme) DARK_CAPTION_COLOR else Color(0xF0, 0xF0, 0xF5)
        blended to isDarkTheme
    }
    TitleBarMode.SYSTEM -> {
        val systemDark = isSystemDarkMode()
        val color = if (systemDark) DARK_CAPTION_COLOR else Color(0xF0, 0xF0, 0xF5)
        color to systemDark
    }
}

private fun isSystemDarkMode(): Boolean = runCatching {
    val osName = System.getProperty("os.name", "").lowercase()
    if (osName.contains("win")) {
        val proc = Runtime.getRuntime().exec(
            arrayOf("reg", "query",
                "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize",
                "/v", "AppsUseLightTheme")
        )
        val output = proc.inputStream.bufferedReader().readText()
        val value = output.lines()
            .firstOrNull { it.contains("AppsUseLightTheme") }
            ?.trim()?.split("\\s+".toRegex())?.lastOrNull()
            ?.let { java.lang.Long.parseLong(it.removePrefix("0x"), 16) }
        value == 0L
    } else {
        true
    }
}.getOrDefault(true)

private fun blendWithDark(dominant: Color, isDark: Boolean): Color {
    val base = if (isDark) Color(0x0A, 0x0A, 0x0F) else Color(0xF5, 0xF5, 0xFF)
    val t = 0.15f
    return Color(
        red = base.red * (1f - t) + dominant.red * t,
        green = base.green * (1f - t) + dominant.green * t,
        blue = base.blue * (1f - t) + dominant.blue * t
    )
}

private data class WindowSnapshot(
    val width: Float,
    val height: Float,
    val position: WindowPosition,
    val placement: WindowPlacement
)