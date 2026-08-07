package io.rudione.chatone.presentation.window

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.icon
import com.russhwolf.settings.Settings
import io.rudione.chatone.presentation.settings.TitleBarMode
import io.rudione.chatone.presentation.settings.SettingsViewModel
import io.rudione.chatone.util.system.WindowsTitleBar
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import org.jetbrains.compose.resources.painterResource

private val osName: String = System.getProperty("os.name", "").lowercase()
internal val isMacOs: Boolean = osName.contains("mac")
internal val isWindowsOs: Boolean = osName.contains("win")
internal val useCustomTitleBar: Boolean = !isMacOs

internal val NATIVE_WINDOW_BG = java.awt.Color(0x0A, 0x0A, 0x0F)

val MIN_WINDOW_WIDTH: Dp = 100.dp
val MIN_WINDOW_HEIGHT: Dp = 300.dp

val MIN_TOOL_WINDOW_WIDTH: Dp = 400.dp
val MIN_TOOL_WINDOW_HEIGHT: Dp = 400.dp

internal fun java.awt.Window.applyMinimumSize(width: Dp = MIN_WINDOW_WIDTH, height: Dp = MIN_WINDOW_HEIGHT) {
    val scale = runCatching {
        graphicsConfiguration?.defaultTransform?.scaleX?.takeIf { it > 0.0 } ?: 1.0
    }.getOrDefault(1.0)
    minimumSize = java.awt.Dimension(
        (width.value * scale).toInt(),
        (height.value * scale).toInt()
    )
}

internal val DARK_CAPTION_COLOR = Color(0x0D, 0x0F, 0x1A)
internal val LIGHT_CAPTION_COLOR = Color(0xF0, 0xF0, 0xF5)

internal fun resolveTitleBar(
    mode: TitleBarMode,
    isDarkTheme: Boolean,
    themeTopBarColor: Color?
): Pair<Color, Boolean> = when (mode) {
    TitleBarMode.DARK -> DARK_CAPTION_COLOR to true
    TitleBarMode.LIGHT -> LIGHT_CAPTION_COLOR to false
    TitleBarMode.ADAPTIVE -> {
        val resolved = themeTopBarColor?.takeIf { it.alpha > 0.05f }?.compositeOverBase(isDarkTheme)
            ?: if (isDarkTheme) DARK_CAPTION_COLOR else LIGHT_CAPTION_COLOR
        resolved to (resolved.perceivedLuminance() < 0.5f)
    }
    TitleBarMode.SYSTEM -> {
        val systemDark = isSystemDarkMode()
        val color = if (systemDark) DARK_CAPTION_COLOR else LIGHT_CAPTION_COLOR
        color to systemDark
    }
}

private fun Color.compositeOverBase(isDarkTheme: Boolean): Color {
    if (alpha >= 0.999f) return this
    val base = if (isDarkTheme) DARK_CAPTION_COLOR else LIGHT_CAPTION_COLOR
    return Color(
        red = red * alpha + base.red * (1f - alpha),
        green = green * alpha + base.green * (1f - alpha),
        blue = blue * alpha + base.blue * (1f - alpha)
    )
}

private fun Color.perceivedLuminance(): Float = 0.2126f * red + 0.7152f * green + 0.0722f * blue

internal fun isSystemDarkMode(): Boolean = runCatching {
    if (isWindowsOs) {
        val proc = Runtime.getRuntime().exec(
            arrayOf(
                io.rudione.chatone.util.system.windowsSystem32Path("reg.exe"), "query",
                "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize",
                "/v", "AppsUseLightTheme"
            )
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

private data class DetachedWindowSnapshot(
    val width: Float,
    val height: Float,
    val position: WindowPosition,
    val placement: WindowPlacement
)

@OptIn(FlowPreview::class)
@Composable
fun ChatoneDetachedWindow(
    windowId: String,
    title: String,
    defaultWidth: Dp,
    defaultHeight: Dp,
    minWidth: Dp = MIN_WINDOW_WIDTH,
    minHeight: Dp = MIN_WINDOW_HEIGHT,
    alwaysOnTop: Boolean = false,
    resizable: Boolean = true,
    onCloseRequest: () -> Unit,
    content: @Composable () -> Unit
) {
    val settings = remember { Settings() }
    val keyX = "win_${windowId}_x"
    val keyY = "win_${windowId}_y"
    val keyW = "win_${windowId}_w"
    val keyH = "win_${windowId}_h"
    val keyMax = "win_${windowId}_maximized"

    val savedX = remember { settings.getFloatOrNull(keyX) }
    val savedY = remember { settings.getFloatOrNull(keyY) }
    val savedW = remember { settings.getFloat(keyW, defaultWidth.value) }
    val savedH = remember { settings.getFloat(keyH, defaultHeight.value) }
    val savedMaximized = remember { settings.getBoolean(keyMax, false) }

    val windowState = rememberWindowState(
        width = savedW.dp.coerceAtLeast(minWidth),
        height = savedH.dp.coerceAtLeast(minHeight),
        position = if (savedX != null && savedY != null)
            WindowPosition(savedX.dp, savedY.dp)
        else
            WindowPosition.PlatformDefault,
        placement = if (savedMaximized) WindowPlacement.Maximized else WindowPlacement.Floating
    )

    LaunchedEffect(windowState) {
        snapshotFlow {
            DetachedWindowSnapshot(
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
                    settings.putBoolean(keyMax, true)
                } else {
                    settings.putBoolean(keyMax, false)
                    settings.putFloat(keyW, snap.width)
                    settings.putFloat(keyH, snap.height)
                    (snap.position as? WindowPosition.Absolute)?.let { pos ->
                        settings.putFloat(keyX, pos.x.value)
                        settings.putFloat(keyY, pos.y.value)
                    }
                }
            }
    }

    val initialSettings = remember { SettingsViewModel.loadInitialState() }
    val appIcon = painterResource(Res.drawable.icon)

    Window(
        onCloseRequest = onCloseRequest,
        title = title,
        state = windowState,
        alwaysOnTop = alwaysOnTop,
        resizable = resizable,
        undecorated = useCustomTitleBar,
        icon = appIcon
    ) {
        DisposableEffect(window) {
            fun applyChrome() {
                window.background = NATIVE_WINDOW_BG
                window.contentPane.background = NATIVE_WINDOW_BG
                window.applyMinimumSize(minWidth, minHeight)
                if (isWindowsOs && useCustomTitleBar) {
                    WindowsTitleBar.enableWindowsSnapAndTaskbar(window)
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

        if (useCustomTitleBar) {
            val captionColor = TitleBarState.captionColor
            LaunchedEffect(captionColor) {
                WindowsTitleBar.applyTitleBarColor(
                    window, captionColor, TitleBarState.captionIsDark
                )
            }
            Column(modifier = Modifier.fillMaxSize()) {
                ChatoneTitleBar(
                    title = title,
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
                    onClose = onCloseRequest
                )
                Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    content()
                }
            }
        } else {
            content()
        }
    }
}
