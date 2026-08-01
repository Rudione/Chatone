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
import java.util.concurrent.ConcurrentHashMap

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

    private val dwmLib: NativeLibrary? by lazy {
        runCatching { NativeLibrary.getInstance("dwmapi") }.getOrNull()
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
                arrayOf("reg", "query",
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
    private const val HTCLIENT = 1
    private const val SM_CXSIZEFRAME = 32
    private const val SM_CYSIZEFRAME = 33
    private const val SM_CXPADDEDBORDER = 92

    private const val SWP_FRAMECHANGED = 0x0020
    private const val SWP_NOMOVE = 0x0002
    private const val SWP_NOSIZE = 0x0001
    private const val SWP_NOZORDER = 0x0004
    private const val SWP_NOACTIVATE = 0x0010

    private val subclassed = ConcurrentHashMap<Long, BorderlessWindowProc>()

    private class BorderlessWindowProc(
        private val hwndValue: Long,
        @Volatile var originalProc: Pointer?
    ) : WinUser.WindowProc {
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

                        val style = User32.INSTANCE.GetWindowLong(hwnd, GWL_STYLE)
                        if (style and WS_MAXIMIZE != 0) {
                            val pad = User32.INSTANCE.GetSystemMetrics(SM_CXPADDEDBORDER)
                            val fx = User32.INSTANCE.GetSystemMetrics(SM_CXSIZEFRAME) + pad
                            val fy = User32.INSTANCE.GetSystemMetrics(SM_CYSIZEFRAME) + pad
                            val rect = Pointer(lParam.toLong())
                            rect.setInt(0L, rect.getInt(0L) + fx)
                            rect.setInt(4L, rect.getInt(4L) + fy)
                            rect.setInt(8L, rect.getInt(8L) - fx)
                            rect.setInt(12L, rect.getInt(12L) - fy)
                        }
                        WinDef.LRESULT(0)
                    } else {
                        User32.INSTANCE.CallWindowProc(original, hwnd, uMsg, wParam, lParam)
                    }
                }
                WM_NCHITTEST -> WinDef.LRESULT(HTCLIENT.toLong())
                WM_NCDESTROY -> {
                    val result = User32.INSTANCE.CallWindowProc(original, hwnd, uMsg, wParam, lParam)
                    subclassed.remove(hwndValue)
                    result
                }
                else -> User32.INSTANCE.CallWindowProc(original, hwnd, uMsg, wParam, lParam)
            }
        }
    }

    fun enableWindowsSnapAndTaskbar(window: Window) {
        if (!isWindows()) return
        val hwndValue = getHwnd(window)
        if (hwndValue == null) {
            Napier.w("enableWindowsSnapAndTaskbar: could not resolve HWND, skipping", tag = "WindowsTitleBar")
            return
        }
        if (subclassed.containsKey(hwndValue)) return
        runCatching {
            val user32 = User32.INSTANCE
            val hwnd = WinDef.HWND(Pointer(hwndValue))

            val curStyle = user32.GetWindowLong(hwnd, GWL_STYLE)
            user32.SetWindowLong(
                hwnd, GWL_STYLE,
                curStyle or WS_CAPTION or WS_THICKFRAME or WS_SYSMENU or
                        WS_MAXIMIZEBOX or WS_MINIMIZEBOX
            )
            val curEx = user32.GetWindowLong(hwnd, GWL_EXSTYLE)
            user32.SetWindowLong(hwnd, GWL_EXSTYLE, curEx or WS_EX_APPWINDOW)

            val originalProc = Pointer(user32.GetWindowLongPtr(hwnd, GWL_WNDPROC).toLong())
            val proc = BorderlessWindowProc(hwndValue, originalProc)
            subclassed[hwndValue] = proc
            user32.SetWindowLongPtr(hwnd, GWL_WNDPROC, CallbackReference.getFunctionPointer(proc))

            user32.SetWindowPos(
                hwnd, null, 0, 0, 0, 0,
                SWP_FRAMECHANGED or SWP_NOMOVE or SWP_NOSIZE or SWP_NOZORDER or SWP_NOACTIVATE
            )
            Napier.i("enableWindowsSnapAndTaskbar: snap styles applied to hwnd=$hwndValue", tag = "WindowsTitleBar")
        }.onFailure { e ->
            subclassed.remove(hwndValue)
            Napier.w("enableWindowsSnapAndTaskbar failed: ${e::class.simpleName}: ${e.message}", tag = "WindowsTitleBar")
        }
    }
}
