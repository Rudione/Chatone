package io.rudione.chatone.util

import androidx.compose.ui.graphics.Color
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
}