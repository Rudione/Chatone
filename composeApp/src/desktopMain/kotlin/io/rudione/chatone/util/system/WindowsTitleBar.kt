package io.rudione.chatone.util.system

import androidx.compose.ui.graphics.Color
import com.sun.jna.CallbackReference
import com.sun.jna.Memory
import com.sun.jna.Native
import com.sun.jna.NativeLibrary
import com.sun.jna.Pointer
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinDef
import com.sun.jna.platform.win32.WinUser
import io.github.aakira.napier.Napier
import java.awt.Window
import java.io.File
import java.util.concurrent.ConcurrentHashMap

internal fun windowsSystem32Path(name: String): String {
    val systemRoot = System.getenv("SystemRoot")?.takeIf { it.isNotBlank() } ?: "C:\\Windows"
    val resolved = File(File(systemRoot, "System32"), name)
    return if (resolved.isFile) resolved.absolutePath else name
}

object WindowsTitleBar {

    fun applyTitleBarColor(window: Window, color: Color, isDark: Boolean) {
        if (!isWindows()) return
        try {
            val hwnd = getHwnd(window) ?: return
            setDwmBool(hwnd, DWMWA_USE_IMMERSIVE_DARK_MODE, isDark)
            if (windowsBuildNumber() >= 22000) {
                setDwmColor(hwnd, DWMWA_CAPTION_COLOR, color)
                setDwmColor(hwnd, DWMWA_BORDER_COLOR, color)
            }
        } catch (_: Throwable) {}
    }

    private const val DWMWA_USE_IMMERSIVE_DARK_MODE = 20
    private const val DWMWA_CAPTION_COLOR = 35
    private const val DWMWA_BORDER_COLOR = 34
    private const val DWMWA_WINDOW_CORNER_PREFERENCE = 33
    private const val DWMWCP_ROUND = 2

    fun applyRoundedCorners(window: Window) {
        if (!isWindows() || windowsBuildNumber() < 22000) return
        runCatching {
            val hwnd = getHwnd(window) ?: return
            callDwm(hwnd, DWMWA_WINDOW_CORNER_PREFERENCE, byteArrayOf(DWMWCP_ROUND.toByte(), 0, 0, 0))
        }
    }

    private val dwmLib: NativeLibrary? by lazy {
        runCatching { NativeLibrary.getInstance(windowsSystem32Path("dwmapi.dll")) }.getOrNull()
    }

    private fun callDwm(hwnd: Long, attribute: Int, valueBytes: ByteArray) {
        val lib = dwmLib ?: return
        val mem = Memory(valueBytes.size.toLong())
        mem.write(0L, valueBytes, 0, valueBytes.size)
        lib.getFunction("DwmSetWindowAttribute")
            .invokeInt(arrayOf(Pointer(hwnd), attribute, mem, valueBytes.size))
    }

    private fun setDwmBool(hwnd: Long, attribute: Int, value: Boolean) {
        val v = if (value) 1 else 0
        callDwm(hwnd, attribute, byteArrayOf(
            (v and 0xFF).toByte(), 0, 0, 0
        ))
    }

    private fun setDwmColor(hwnd: Long, attribute: Int, color: Color) {
        val colorRef = toColorRef(color)
        callDwm(hwnd, attribute, byteArrayOf(
            (colorRef and 0xFF).toByte(),
            ((colorRef shr 8) and 0xFF).toByte(),
            ((colorRef shr 16) and 0xFF).toByte(),
            0
        ))
    }

    private fun getHwnd(window: Window): Long? =
        runCatching { Pointer.nativeValue(Native.getWindowPointer(window)) }
            .getOrNull()
            ?.takeIf { it != 0L }
            ?: getHwndViaPeer(window)

    private fun getHwndViaPeer(window: Window): Long? = runCatching {
        val peerField = java.awt.Component::class.java.getDeclaredField("peer")
        peerField.isAccessible = true
        val peer = peerField.get(window) ?: return@runCatching null
        var cls: Class<*>? = peer.javaClass
        while (cls != null) {
            runCatching {
                val f = cls!!.getDeclaredField("hwnd")
                f.isAccessible = true
                return@runCatching f.getLong(peer)
            }
            cls = cls.superclass
        }
        null
    }.getOrNull()

    private fun toColorRef(c: Color): Int {
        val r = (c.red * 255).toInt().coerceIn(0, 255)
        val g = (c.green * 255).toInt().coerceIn(0, 255)
        val b = (c.blue * 255).toInt().coerceIn(0, 255)
        return (b shl 16) or (g shl 8) or r
    }

    private fun isWindows() =
        System.getProperty("os.name")?.lowercase()?.contains("windows") == true

    private val _buildNumber: Int by lazy {
        runCatching {
            val proc = Runtime.getRuntime().exec(
                arrayOf(windowsSystem32Path("reg.exe"), "query",
                    "HKLM\\SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion",
                    "/v", "CurrentBuildNumber")
            )
            proc.inputStream.bufferedReader().readText()
                .lines()
                .firstOrNull { it.contains("CurrentBuildNumber") }
                ?.trim()?.split("\\s+".toRegex())?.lastOrNull()
                ?.toIntOrNull() ?: 0
        }.getOrDefault(0)
    }

    fun windowsBuildNumber(): Int = _buildNumber

    private const val GWL_WNDPROC = -4
    private const val GWL_STYLE = -16
    private const val GWL_EXSTYLE = -20
    private const val WS_CAPTION = 0x00C00000
    private const val WS_THICKFRAME = 0x00040000
    private const val WS_SYSMENU = 0x00080000
    private const val WS_MAXIMIZEBOX = 0x00010000
    private const val WS_MINIMIZEBOX = 0x00020000
    private const val WS_MAXIMIZE = 0x01000000
    private const val WS_EX_APPWINDOW = 0x00040000

    private const val WM_NCCALCSIZE = 0x0083
    private const val WM_NCHITTEST = 0x0084
    private const val WM_NCDESTROY = 0x0082
    private const val WM_NCMOUSEMOVE = 0x00A0
    private const val WM_NCLBUTTONDOWN = 0x00A1
    private const val WM_NCLBUTTONUP = 0x00A2
    private const val WM_MOUSELEAVE = 0x02A3

    private const val HTCLIENT = 1
    private const val HTCAPTION = 2
    private const val HTMINBUTTON = 8
    private const val HTCLOSE = 20
    private const val HTLEFT = 10
    private const val HTRIGHT = 11
    private const val HTTOP = 12
    private const val HTTOPLEFT = 13
    private const val HTTOPRIGHT = 14
    private const val HTBOTTOM = 15
    private const val HTBOTTOMLEFT = 16
    private const val HTBOTTOMRIGHT = 17
    private const val HTMAXBUTTON = 9

    private const val SM_CXSIZEFRAME = 32
    private const val SM_CYSIZEFRAME = 33
    private const val SM_CXPADDEDBORDER = 92

    private const val SWP_FRAMECHANGED = 0x0020
    private const val SWP_NOMOVE = 0x0002
    private const val SWP_NOSIZE = 0x0001
    private const val SWP_NOZORDER = 0x0004
    private const val SWP_NOACTIVATE = 0x0010

    private val subclassed = ConcurrentHashMap<Long, BorderlessWindowProc>()

    data class CaptionLayout(
        val captionHeightPx: Int,
        val maximizeButtonPx: java.awt.Rectangle?,
        val interactivePx: List<java.awt.Rectangle>,
        val draggablePx: List<java.awt.Rectangle> = emptyList()
    )

    private val captionLayouts = ConcurrentHashMap<Long, CaptionLayout>()
    private val hoverStates = ConcurrentHashMap<Long, Boolean>()
    private val maximizeCallbacks = ConcurrentHashMap<Long, () -> Unit>()
    private val maxButtonHoverListeners = ConcurrentHashMap<Long, (Boolean) -> Unit>()

    fun setCaptionLayout(window: Window, layout: CaptionLayout) {
        val hwnd = getHwnd(window) ?: return
        captionLayouts[hwnd] = layout
    }

    fun setCaptionCallbacks(
        window: Window,
        onToggleMaximize: () -> Unit,
        onMaximizeHoverChanged: (Boolean) -> Unit
    ) {
        val hwnd = getHwnd(window) ?: return
        maximizeCallbacks[hwnd] = onToggleMaximize
        maxButtonHoverListeners[hwnd] = onMaximizeHoverChanged
    }

    private class BorderlessWindowProc(
        private val hwndValue: Long,
        @Volatile var originalProc: Pointer?
    ) : WinUser.WindowProc {

        private fun isMaximized(hwnd: WinDef.HWND): Boolean =
            User32.INSTANCE.GetWindowLong(hwnd, GWL_STYLE) and WS_MAXIMIZE != 0

        private fun frameThickness(): Pair<Int, Int> {
            val pad = User32.INSTANCE.GetSystemMetrics(SM_CXPADDEDBORDER)
            return User32.INSTANCE.GetSystemMetrics(SM_CXSIZEFRAME) + pad to
                    User32.INSTANCE.GetSystemMetrics(SM_CYSIZEFRAME) + pad
        }

        private fun hitTest(hwnd: WinDef.HWND, lParam: WinDef.LPARAM): Int {
            val screenX = (lParam.toLong() and 0xFFFF).toShort().toInt()
            val screenY = ((lParam.toLong() shr 16) and 0xFFFF).toShort().toInt()

            val rect = WinDef.RECT()
            User32.INSTANCE.GetWindowRect(hwnd, rect)
            val x = screenX - rect.left
            val y = screenY - rect.top
            val width = rect.right - rect.left
            val height = rect.bottom - rect.top

            val maximized = isMaximized(hwnd)
            if (!maximized) {
                val (fx, fy) = frameThickness()
                val onLeft = x < fx
                val onRight = x >= width - fx
                val onTop = y < fy
                val onBottom = y >= height - fy
                when {
                    onTop && onLeft -> return HTTOPLEFT
                    onTop && onRight -> return HTTOPRIGHT
                    onBottom && onLeft -> return HTBOTTOMLEFT
                    onBottom && onRight -> return HTBOTTOMRIGHT
                    onTop -> return HTTOP
                    onBottom -> return HTBOTTOM
                    onLeft -> return HTLEFT
                    onRight -> return HTRIGHT
                }
            }

            val layout = captionLayouts[hwndValue] ?: return HTCLIENT

            if (layout.draggablePx.isNotEmpty()) {
                if (layout.interactivePx.any { it.contains(x, y) }) return HTCLIENT
                if (layout.draggablePx.any { it.contains(x, y) }) return HTCAPTION
                return HTCLIENT
            }

            if (y >= layout.captionHeightPx) return HTCLIENT

            layout.maximizeButtonPx?.let { button ->
                if (button.contains(x, y)) return HTMAXBUTTON
            }
            if (layout.interactivePx.any { it.contains(x, y) }) return HTCLIENT
            return HTCAPTION
        }

        private fun notifyMaxHover(hovered: Boolean) {
            if (hoverStates.put(hwndValue, hovered) == hovered) return
            maxButtonHoverListeners[hwndValue]?.invoke(hovered)
        }

        override fun callback(
            hwnd: WinDef.HWND,
            uMsg: Int,
            wParam: WinDef.WPARAM,
            lParam: WinDef.LPARAM
        ): WinDef.LRESULT {
            val original = originalProc
                ?: return User32.INSTANCE.DefWindowProc(hwnd, uMsg, wParam, lParam)
            return when (uMsg) {
                WM_NCCALCSIZE -> {
                    if (wParam.toLong() != 0L) {
                        val rect = Pointer(lParam.toLong())
                        if (isMaximized(hwnd)) {
                            val (fx, fy) = frameThickness()
                            rect.setInt(0L, rect.getInt(0L) + fx)
                            rect.setInt(4L, rect.getInt(4L) + fy)
                            rect.setInt(8L, rect.getInt(8L) - fx)
                            rect.setInt(12L, rect.getInt(12L) - fy)
                        } else {
                            rect.setInt(4L, rect.getInt(4L) + 1)
                        }
                        WinDef.LRESULT(0)
                    } else {
                        User32.INSTANCE.CallWindowProc(original, hwnd, uMsg, wParam, lParam)
                    }
                }

                WM_NCHITTEST -> {
                    val result = hitTest(hwnd, lParam)
                    notifyMaxHover(result == HTMAXBUTTON)
                    WinDef.LRESULT(result.toLong())
                }

                WM_NCMOUSEMOVE -> {
                    if (wParam.toLong().toInt() != HTMAXBUTTON) notifyMaxHover(false)
                    User32.INSTANCE.CallWindowProc(original, hwnd, uMsg, wParam, lParam)
                }

                WM_MOUSELEAVE -> {
                    notifyMaxHover(false)
                    User32.INSTANCE.CallWindowProc(original, hwnd, uMsg, wParam, lParam)
                }

                WM_NCLBUTTONDOWN -> {
                    if (wParam.toLong().toInt() == HTMAXBUTTON) WinDef.LRESULT(0)
                    else User32.INSTANCE.CallWindowProc(original, hwnd, uMsg, wParam, lParam)
                }

                WM_NCLBUTTONUP -> {
                    if (wParam.toLong().toInt() == HTMAXBUTTON) {
                        maximizeCallbacks[hwndValue]?.let { toggle ->
                            javax.swing.SwingUtilities.invokeLater { runCatching { toggle() } }
                        }
                        notifyMaxHover(false)
                        WinDef.LRESULT(0)
                    } else {
                        User32.INSTANCE.CallWindowProc(original, hwnd, uMsg, wParam, lParam)
                    }
                }

                WM_NCDESTROY -> {
                    val result = User32.INSTANCE.CallWindowProc(original, hwnd, uMsg, wParam, lParam)
                    subclassed.remove(hwndValue)
                    captionLayouts.remove(hwndValue)
                    hoverStates.remove(hwndValue)
                    maximizeCallbacks.remove(hwndValue)
                    maxButtonHoverListeners.remove(hwndValue)
                    result
                }

                else -> User32.INSTANCE.CallWindowProc(original, hwnd, uMsg, wParam, lParam)
            }
        }
    }

    fun enableBorderlessResize(window: Window) {
        if (!isWindows()) return
        val hwndValue = getHwnd(window) ?: return
        captionLayouts[hwndValue] = CaptionLayout(
            captionHeightPx = 0,
            maximizeButtonPx = null,
            interactivePx = emptyList(),
            draggablePx = emptyList()
        )
        applySubclass(window, hwndValue, appWindowStyles = false)
    }

    fun setDraggableRegions(
        window: Window,
        draggable: List<java.awt.Rectangle>,
        interactive: List<java.awt.Rectangle>
    ) {
        val hwnd = getHwnd(window) ?: return
        val existing = captionLayouts[hwnd]
        captionLayouts[hwnd] = CaptionLayout(
            captionHeightPx = existing?.captionHeightPx ?: 0,
            maximizeButtonPx = null,
            interactivePx = interactive,
            draggablePx = draggable
        )
    }

    private const val WM_NCLBUTTONDOWN_MSG = 0x00A1
    private const val HT_CAPTION_WPARAM = 2

    fun beginWindowDrag(window: Window) {
        if (!isWindows()) return
        runCatching {
            val hwndValue = getHwnd(window) ?: return
            val hwnd = WinDef.HWND(Pointer(hwndValue))
            val user32 = NativeLibrary.getInstance(windowsSystem32Path("user32.dll"))
            user32.getFunction("ReleaseCapture").invokeInt(emptyArray())
            user32.getFunction("SendMessageW").invokeLong(
                arrayOf(hwnd, WM_NCLBUTTONDOWN_MSG, HT_CAPTION_WPARAM, 0)
            )
        }
    }

    fun releaseWindow(window: Window) {
        val hwnd = getHwnd(window) ?: return
        captionLayouts.remove(hwnd)
        hoverStates.remove(hwnd)
        maximizeCallbacks.remove(hwnd)
        maxButtonHoverListeners.remove(hwnd)
    }

    fun enableWindowsSnapAndTaskbar(window: Window) {
        if (!isWindows()) return
        val hwndValue = getHwnd(window)
        if (hwndValue == null) {
            Napier.w("enableWindowsSnapAndTaskbar: could not resolve HWND, skipping", tag = "WindowsTitleBar")
            return
        }
        applySubclass(window, hwndValue, appWindowStyles = true)
    }

    private fun applySubclass(window: Window, hwndValue: Long, appWindowStyles: Boolean) {
        if (!isWindows()) return
        if (subclassed.containsKey(hwndValue)) return
        runCatching {
            val user32 = User32.INSTANCE
            val hwnd = WinDef.HWND(Pointer(hwndValue))

            val curStyle = user32.GetWindowLong(hwnd, GWL_STYLE)
            val newStyle = if (appWindowStyles) {
                curStyle or WS_CAPTION or WS_THICKFRAME or WS_SYSMENU or
                        WS_MAXIMIZEBOX or WS_MINIMIZEBOX
            } else {
                curStyle or WS_THICKFRAME or WS_SYSMENU
            }
            user32.SetWindowLong(hwnd, GWL_STYLE, newStyle)

            if (appWindowStyles) {
                val curEx = user32.GetWindowLong(hwnd, GWL_EXSTYLE)
                user32.SetWindowLong(hwnd, GWL_EXSTYLE, curEx or WS_EX_APPWINDOW)
            }

            val originalProc = Pointer(user32.GetWindowLongPtr(hwnd, GWL_WNDPROC).toLong())
            val proc = BorderlessWindowProc(hwndValue, originalProc)
            subclassed[hwndValue] = proc
            user32.SetWindowLongPtr(hwnd, GWL_WNDPROC, CallbackReference.getFunctionPointer(proc))

            user32.SetWindowPos(
                hwnd, null, 0, 0, 0, 0,
                SWP_FRAMECHANGED or SWP_NOMOVE or SWP_NOSIZE or SWP_NOZORDER or SWP_NOACTIVATE
            )
            Napier.i(
                "applySubclass: styles applied to hwnd=$hwndValue appWindow=$appWindowStyles",
                tag = "WindowsTitleBar"
            )
        }.onFailure { e ->
            subclassed.remove(hwndValue)
            Napier.w("applySubclass failed: ${e::class.simpleName}: ${e.message}", tag = "WindowsTitleBar")
        }
    }

    fun applyHighQualityIcons(window: Window, source: java.awt.image.BufferedImage) {
        runCatching {
            val sizes = intArrayOf(16, 20, 24, 32, 40, 48, 64, 128, 256)
            window.iconImages = sizes.map { size -> scaleIcon(source, size) }
        }.onFailure { e ->
            Napier.w("applyHighQualityIcons failed: ${e.message}", tag = "WindowsTitleBar")
        }
    }

    private fun scaleIcon(source: java.awt.image.BufferedImage, size: Int): java.awt.image.BufferedImage {
        var current = source
        var currentSize = maxOf(source.width, source.height)
        while (currentSize / 2 > size) {
            currentSize /= 2
            current = redraw(current, currentSize)
        }
        return redraw(current, size)
    }

    private fun redraw(source: java.awt.image.BufferedImage, size: Int): java.awt.image.BufferedImage {
        val target = java.awt.image.BufferedImage(size, size, java.awt.image.BufferedImage.TYPE_INT_ARGB)
        val g = target.createGraphics()
        g.setRenderingHint(
            java.awt.RenderingHints.KEY_INTERPOLATION,
            java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR
        )
        g.setRenderingHint(
            java.awt.RenderingHints.KEY_RENDERING,
            java.awt.RenderingHints.VALUE_RENDER_QUALITY
        )
        g.setRenderingHint(
            java.awt.RenderingHints.KEY_ANTIALIASING,
            java.awt.RenderingHints.VALUE_ANTIALIAS_ON
        )
        g.drawImage(source, 0, 0, size, size, null)
        g.dispose()
        return target
    }
}
