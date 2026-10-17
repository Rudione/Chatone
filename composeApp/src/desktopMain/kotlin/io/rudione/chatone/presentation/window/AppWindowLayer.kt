package io.rudione.chatone.presentation.window

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.awt.ComposeWindow
import io.rudione.chatone.presentation.settings.AlwaysOnTopMode
import io.rudione.chatone.util.system.ForegroundApp
import io.rudione.chatone.util.system.ForegroundAppProbe
import io.rudione.chatone.util.system.MacSpaces
import io.rudione.chatone.util.system.WindowsTitleBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.CopyOnWriteArrayList

private enum class WindowStack { NORMAL, ON_TOP, ABOVE_SYSTEM }

private data class AppliedStack(val revision: Int, val stack: WindowStack, val foreground: ForegroundApp)

internal object AppWindowLayer {
    var alwaysOnTop by mutableStateOf(false)
    var mode by mutableStateOf(AlwaysOnTopMode.EXCEPT_SYSTEM)

    private val windows = CopyOnWriteArrayList<ComposeWindow>()
    private val revision = MutableStateFlow(0)
    private var customized = false

    fun keepsOnTop(pinned: Boolean): Boolean = pinned || alwaysOnTop

    fun attach(window: ComposeWindow, primary: Boolean) {
        if (primary) windows.add(0, window) else windows.add(window)
        revision.update { it + 1 }
    }

    fun detach(window: ComposeWindow) {
        windows.remove(window)
    }

    suspend fun maintain(enabled: Boolean, mode: AlwaysOnTopMode) {
        if (customized) {
            delay(SETTLE_MS)
            applyStack(null)
            customized = false
        }
        if (!enabled || mode == AlwaysOnTopMode.EXCEPT_SYSTEM || !ForegroundAppProbe.isSupported) return
        customized = true
        var stack = WindowStack.ON_TOP
        var applied: AppliedStack? = null
        while (true) {
            val foreground = withContext(Dispatchers.IO) { ForegroundAppProbe.current() }
            stack = targetStack(mode, foreground, stack)
            val current = AppliedStack(revision.value, stack, foreground)
            val reassert = mode == AlwaysOnTopMode.ALL_WINDOWS && isWindowsOs && foreground != ForegroundApp.SELF
            if (current != applied || reassert) {
                applyStack(stack)
                if (current.revision != applied?.revision) {
                    delay(SETTLE_MS)
                    applyStack(stack)
                }
                applied = current
            }
            withTimeoutOrNull(POLL_MS) { revision.first { it != current.revision } }
        }
    }

    private fun targetStack(mode: AlwaysOnTopMode, foreground: ForegroundApp, previous: WindowStack): WindowStack =
        when (mode) {
            AlwaysOnTopMode.EXCEPT_SYSTEM -> WindowStack.ON_TOP
            AlwaysOnTopMode.ALL_WINDOWS -> WindowStack.ABOVE_SYSTEM
            AlwaysOnTopMode.BROWSER -> when (foreground) {
                ForegroundApp.BROWSER -> WindowStack.ON_TOP
                ForegroundApp.OTHER -> WindowStack.NORMAL
                ForegroundApp.SELF, ForegroundApp.UNKNOWN -> previous
            }
        }

    private suspend fun applyStack(stack: WindowStack?) {
        val targets = windows.filter { it.isDisplayable && it.isVisible }
        when {
            isWindowsOs -> {
                val handles = targets.mapNotNull { window ->
                    WindowsTitleBar.nativeHandle(window)?.let { it to (window.isAlwaysOnTop && stack != WindowStack.NORMAL) }
                }
                withContext(Dispatchers.IO) {
                    handles.forEach { (hwnd, topmost) -> WindowsTitleBar.setTopmost(hwnd, topmost) }
                }
            }
            isMacOs -> targets.forEach { window ->
                MacSpaces.applyStacking(window.windowHandle, window.title, macLevel(stack, window.isAlwaysOnTop))
            }
        }
    }

    private fun macLevel(stack: WindowStack?, onTop: Boolean): MacSpaces.StackLevel = when {
        !onTop || stack == WindowStack.NORMAL -> MacSpaces.StackLevel.NORMAL
        stack == WindowStack.ABOVE_SYSTEM -> MacSpaces.StackLevel.ABOVE_SYSTEM
        else -> MacSpaces.StackLevel.FLOATING
    }

    private const val SETTLE_MS = 300L
    private const val POLL_MS = 500L
}

@Composable
internal fun AppWindowLayerEffect() {
    val enabled = AppWindowLayer.alwaysOnTop
    val mode = AppWindowLayer.mode
    LaunchedEffect(enabled, mode) { AppWindowLayer.maintain(enabled, mode) }
}

@Composable
internal fun RegisterAppWindow(window: ComposeWindow, primary: Boolean = false) {
    DisposableEffect(window) {
        AppWindowLayer.attach(window, primary)
        onDispose { AppWindowLayer.detach(window) }
    }
}
