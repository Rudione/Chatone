package io.rudione.chatone.presentation.chat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import com.russhwolf.settings.Settings
import io.rudione.chatone.presentation.window.isWindowsOs
import io.rudione.chatone.util.system.WindowsTitleBar
import java.awt.Dimension
import java.awt.GraphicsEnvironment
import java.awt.MouseInfo
import java.awt.Toolkit
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent

private val PROFILE_RESIZER_THICKNESS = 12.dp
private const val POINTER_GAP = 12
private const val KEY_CARD_X = "profile_card_x"
private const val KEY_CARD_Y = "profile_card_y"

private class ScreenBox(val minX: Int, val minY: Int, val maxX: Int, val maxY: Int)

private fun screenBoxFor(width: Dp, height: Dp): ScreenBox? = runCatching {
    val pointerInfo = MouseInfo.getPointerInfo()
    val device = pointerInfo?.device
        ?: GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice
    val config = device.defaultConfiguration
    val bounds = config.bounds
    val insets = Toolkit.getDefaultToolkit().getScreenInsets(config)
    val minX = bounds.x + insets.left
    val minY = bounds.y + insets.top
    ScreenBox(
        minX = minX,
        minY = minY,
        maxX = maxOf(minX, bounds.x + bounds.width - insets.right - width.value.toInt()),
        maxY = maxOf(minY, bounds.y + bounds.height - insets.bottom - height.value.toInt())
    )
}.getOrNull()

private fun spawnPosition(width: Dp, height: Dp): WindowPosition = runCatching {
    val box = screenBoxFor(width, height) ?: return@runCatching WindowPosition.PlatformDefault
    val pointer = MouseInfo.getPointerInfo()?.location

    val targetX = pointer?.let { it.x - width.value.toInt() - POINTER_GAP }
        ?: ((box.minX + box.maxX) / 2)
    val targetY = pointer?.let { it.y - POINTER_GAP }
        ?: ((box.minY + box.maxY) / 2)

    WindowPosition(
        x = targetX.coerceIn(box.minX, box.maxX).dp,
        y = targetY.coerceIn(box.minY, box.maxY).dp
    )
}.getOrDefault(WindowPosition.PlatformDefault)

private fun restoredPosition(settings: Settings, width: Dp, height: Dp): WindowPosition? {
    val savedX = settings.getFloatOrNull(KEY_CARD_X) ?: return null
    val savedY = settings.getFloatOrNull(KEY_CARD_Y) ?: return null
    val box = screenBoxFor(width, height) ?: return WindowPosition(savedX.dp, savedY.dp)
    return WindowPosition(
        x = savedX.toInt().coerceIn(box.minX, box.maxX).dp,
        y = savedY.toInt().coerceIn(box.minY, box.maxY).dp
    )
}

@Composable
actual fun UserProfileContainer(
    isPinned: Boolean,
    width: Dp,
    height: Dp,
    onResize: (Dp, Dp) -> Unit,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    val settings = remember { Settings() }
    val initialPosition = remember {
        restoredPosition(settings, width, height) ?: spawnPosition(width, height)
    }

    val windowState = rememberWindowState(
        width = width,
        height = height,
        position = initialPosition
    )

    LaunchedEffect(windowState) {
        androidx.compose.runtime.snapshotFlow { windowState.size }
            .collect { size -> onResize(size.width, size.height) }
    }

    LaunchedEffect(windowState) {
        androidx.compose.runtime.snapshotFlow { windowState.position }
            .collect { position ->
                if (position is WindowPosition.Absolute) {
                    settings.putFloat(KEY_CARD_X, position.x.value)
                    settings.putFloat(KEY_CARD_Y, position.y.value)
                }
            }
    }

    val pinnedLatest by rememberUpdatedState(isPinned)
    val dismissLatest by rememberUpdatedState(onDismiss)

    Window(
        onCloseRequest = onDismiss,
        state = windowState,
        title = "Usercard",
        undecorated = true,
        transparent = false,
        resizable = true,
        alwaysOnTop = isPinned,
        focusable = true
    ) {
        DisposableEffect(window) {
            window.minimumSize = Dimension(
                PROFILE_CARD_MIN_WIDTH.value.toInt(),
                PROFILE_CARD_MIN_HEIGHT.value.toInt()
            )
            window.undecoratedResizerThickness = PROFILE_RESIZER_THICKNESS
            if (isWindowsOs) {
                WindowsTitleBar.enableBorderlessResize(window)
                WindowsTitleBar.applyRoundedCorners(window)
            }
            val listener = object : WindowAdapter() {
                override fun windowLostFocus(e: WindowEvent) {
                    if (!pinnedLatest) dismissLatest()
                }
            }
            window.addWindowFocusListener(listener)
            onDispose {
                window.removeWindowFocusListener(listener)
                if (isWindowsOs) WindowsTitleBar.releaseWindow(window)
            }
        }

        CompositionLocalProvider(LocalProfileWindow provides window) {
            ProfileCardSurface(
                width = windowState.size.width,
                height = windowState.size.height,
                onResize = null,
                drawBorder = true,
                cornerRadius = 0.dp
            ) { content() }
        }
    }
}
