package io.rudione.chatone.util

import androidx.compose.ui.graphics.Color
import io.github.aakira.napier.Napier
import java.awt.Window

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


    private val dwmLib: Any? by lazy {
        runCatching {
            val nlClass = Class.forName("com.sun.jna.NativeLibrary")
            nlClass.getMethod("getInstance", String::class.java).invoke(null, "dwmapi")
        }.getOrNull()
    }

    private fun callDwm(hwnd: Long, attribute: Int, valueBytes: ByteArray) {
        val lib = dwmLib ?: return
        val nlClass = Class.forName("com.sun.jna.NativeLibrary")
        val funcMethod = nlClass.getMethod("getFunction", String::class.java)
        val func = funcMethod.invoke(lib, "DwmSetWindowAttribute")
        val funcClass = Class.forName("com.sun.jna.Function")

        val memClass = Class.forName("com.sun.jna.Memory")
        val mem = memClass.getConstructor(Long::class.java).newInstance(valueBytes.size.toLong()) as Any
        val writeMethod = memClass.getMethod("write", Long::class.java, ByteArray::class.java, Int::class.java, Int::class.java)
        writeMethod.invoke(mem, 0L, valueBytes, 0, valueBytes.size)

        val ptrClass = Class.forName("com.sun.jna.Pointer")
        val ptrCtor = ptrClass.getConstructor(Long::class.java)
        val hwndPtr = ptrCtor.newInstance(hwnd)

        val invokeMethod = funcClass.getMethod("invoke", Class::class.java, Array<Any?>::class.javaObjectType)
        invokeMethod.invoke(func, Int::class.java, arrayOf(hwndPtr, attribute, mem, valueBytes.size))
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


    private fun getHwnd(window: Window): Long? = runCatching {
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


    private const val GWL_STYLE = -16
    private const val GWL_EXSTYLE = -20
    private const val WS_CAPTION = 0x00C00000.toInt()
    private const val WS_THICKFRAME = 0x00040000
    private const val WS_SYSMENU = 0x00080000
    private const val WS_MAXIMIZEBOX = 0x00010000
    private const val WS_MINIMIZEBOX = 0x00020000
    private const val WS_EX_APPWINDOW = 0x00040000

    fun enableWindowsSnapAndTaskbar(window: Window) {
        if (!isWindows()) return
        val hwnd = getHwnd(window)
        if (hwnd == null) {
            Napier.w("enableWindowsSnapAndTaskbar: could not resolve HWND, skipping", tag = "WindowsTitleBar")
            return
        }
        runCatching {
            val ptrClass   = Class.forName("com.sun.jna.Pointer")
            val ptrCtor    = ptrClass.getConstructor(Long::class.java)

            val user32Class  = Class.forName("com.sun.jna.platform.win32.User32")
            val user32       = user32Class.getField("INSTANCE").get(null)

            val hwndClass  = Class.forName("com.sun.jna.platform.win32.WinDef\$HWND")
            val hwndCtor   = hwndClass.getConstructor(ptrClass)
            val winRef     = hwndCtor.newInstance(ptrCtor.newInstance(hwnd))
            val nullRef    = hwndCtor.newInstance(ptrCtor.newInstance(0L))

            val intClass   = Int::class.java

            val getWindowLong = user32Class.getMethod("GetWindowLong", hwndClass, intClass)
            val setWindowLong = user32Class.getMethod("SetWindowLong", hwndClass, intClass, intClass)
            val setWindowPos  = user32Class.getMethod("SetWindowPos",  hwndClass, hwndClass,
                intClass, intClass, intClass, intClass, intClass)

            val curStyle  = getWindowLong.invoke(user32, winRef, GWL_STYLE) as Int
            val newStyle  = curStyle or WS_CAPTION or WS_THICKFRAME or WS_SYSMENU or
                    WS_MAXIMIZEBOX or WS_MINIMIZEBOX
            setWindowLong.invoke(user32, winRef, GWL_STYLE, newStyle)

            val curEx  = getWindowLong.invoke(user32, winRef, GWL_EXSTYLE) as Int
            setWindowLong.invoke(user32, winRef, GWL_EXSTYLE, curEx or WS_EX_APPWINDOW)

            val SWP_FLAGS = 0x0020 or 0x0002 or 0x0001 or 0x0004
            setWindowPos.invoke(user32, winRef, nullRef, 0, 0, 0, 0, SWP_FLAGS)
        }.onFailure { e ->
            Napier.w("enableWindowsSnapAndTaskbar failed: ${e::class.simpleName}: ${e.message}", tag = "WindowsTitleBar")
            runCatching { window.type = java.awt.Window.Type.NORMAL }
        }
    }
}