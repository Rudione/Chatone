package io.rudione.chatone

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.rememberTrayState
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.icon
import com.russhwolf.settings.Settings
import io.rudione.chatone.di.appModules
import io.rudione.chatone.presentation.settings.SettingsViewModel
import io.rudione.chatone.presentation.theme.ChatoneTheme
import io.rudione.chatone.presentation.window.ChatoneTitleBar
import io.rudione.chatone.presentation.window.UpdateAvailableOverlay
import io.rudione.chatone.presentation.window.NATIVE_WINDOW_BG
import io.rudione.chatone.presentation.window.applyMinimumSize
import io.rudione.chatone.presentation.window.isWindowsOs
import io.rudione.chatone.presentation.window.TitleBarState
import io.rudione.chatone.presentation.window.resolveTitleBar
import io.rudione.chatone.presentation.window.useCustomTitleBar
import io.rudione.chatone.util.system.AutoUpdater
import io.rudione.chatone.util.system.GlobalKeyDispatcher
import io.rudione.chatone.util.system.WindowsTitleBar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.koin.core.context.startKoin

private const val WIN_X = "win_x"
private const val WIN_Y = "win_y"
private const val WIN_W = "win_w"
private const val WIN_H = "win_h"
private const val WIN_MAX = "win_maximized"

@OptIn(FlowPreview::class)
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
        runCatching { AutoUpdater.checkForUpdates() }
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
            }
                .distinctUntilChanged()
                .debounce(350)
                .collect { snap ->
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
        var themeTopBarColor by remember { mutableStateOf<Color?>(null) }
        var titleBarMode by remember { mutableStateOf(initialSettings.titleBarMode) }

        val appIcon = painterResource(Res.drawable.icon)

        val trayState = rememberTrayState()
        DisposableEffect(trayState) {
            io.rudione.chatone.util.system.DesktopTrayHolder.trayState = trayState
            onDispose { io.rudione.chatone.util.system.DesktopTrayHolder.trayState = null }
        }
        Tray(
            state = trayState,
            icon = appIcon,
            tooltip = "Chatone",
            menu = {
                Item("Выход") { exitApplication() }
            }
        )

        Window(
            onCloseRequest = { exitApplication() },
            title = "Chatone",
            state = windowState,
            alwaysOnTop = alwaysOnTop,
            undecorated = useCustomTitleBar,
            icon = appIcon,
            onPreviewKeyEvent = { GlobalKeyDispatcher.dispatch(it) }
        ) {
            DisposableEffect(window) {
                val initialDark = isDarkTheme
                fun applyChrome() {
                    window.background = NATIVE_WINDOW_BG
                    window.contentPane.background = NATIVE_WINDOW_BG
                    window.applyMinimumSize()
                    loadAppIconImage()?.let { WindowsTitleBar.applyHighQualityIcons(window, it) }
                    if (isWindowsOs && useCustomTitleBar) {
                        WindowsTitleBar.enableWindowsSnapAndTaskbar(window)
                    }
                    runCatching {
                        window.rootPane.putClientProperty(
                            "apple.awt.windowAppearance",
                            "NSAppearanceNameDarkAqua"
                        )
                    }
                    if (!useCustomTitleBar) {
                        val (captionColor, useDark) = resolveTitleBar(
                            initialSettings.titleBarMode, initialDark, null
                        )
                        WindowsTitleBar.applyTitleBarColor(window, captionColor, useDark)
                    }
                }
                if (window.isDisplayable) applyChrome()
                val listener = object : java.awt.event.WindowAdapter() {
                    override fun windowOpened(e: java.awt.event.WindowEvent) {
                        applyChrome()
                    }
                }
                window.addWindowListener(listener)
                onDispose { window.removeWindowListener(listener) }
            }

            LaunchedEffect(isDarkTheme, themeTopBarColor, titleBarMode) {
                TitleBarState.mode = titleBarMode
                TitleBarState.isDarkTheme = isDarkTheme
                TitleBarState.themeTopBarColor = themeTopBarColor
                val (captionColor, useDark) = resolveTitleBar(titleBarMode, isDarkTheme, themeTopBarColor)
                WindowsTitleBar.applyTitleBarColor(window, captionColor, useDark)
            }

            Box(modifier = Modifier.fillMaxSize()) {
                if (useCustomTitleBar) {
                    val captionColor = TitleBarState.captionColor
                    Column(modifier = Modifier.fillMaxSize()) {
                        ChatoneTitleBar(
                            title = "Chatone",
                            icon = appIcon,
                            background = captionColor,
                            windowState = windowState,
                            onMinimize = { window.extendedState = java.awt.Frame.ICONIFIED },
                            onToggleMaximize = {
                                windowState.placement =
                                    if (windowState.placement == WindowPlacement.Maximized)
                                        WindowPlacement.Floating
                                    else
                                        WindowPlacement.Maximized
                            },
                            onClose = { exitApplication() }
                        )
                        App(
                            onAlwaysOnTopChanged = { alwaysOnTop = it },
                            onThemeChanged = { isDarkTheme = it },
                            onThemeTopBarColorChanged = { themeTopBarColor = it },
                            onTitleBarModeChanged = { titleBarMode = it }
                        )
                    }
                } else {
                    App(
                        onAlwaysOnTopChanged = { alwaysOnTop = it },
                        onThemeChanged = { isDarkTheme = it },
                        onThemeTopBarColorChanged = { themeTopBarColor = it },
                        onTitleBarModeChanged = { titleBarMode = it }
                    )
                }
                ChatoneTheme(darkTheme = isDarkTheme) { UpdateAvailableOverlay() }
            }
        }
    }
}

private data class WindowSnapshot(
    val width: Float,
    val height: Float,
    val position: WindowPosition,
    val placement: WindowPlacement
)

private val appIconImage: java.awt.image.BufferedImage? by lazy {
    runCatching {
        val loader = Thread.currentThread().contextClassLoader
            ?: WindowSnapshot::class.java.classLoader
        val stream = loader.getResourceAsStream("icon.png")
            ?: loader.getResourceAsStream(
                "composeResources/chatone.composeapp.generated.resources/drawable/icon.png"
            )
        stream?.use { javax.imageio.ImageIO.read(it) }
    }.getOrNull()
}

internal fun loadAppIconImage(): java.awt.image.BufferedImage? = appIconImage
