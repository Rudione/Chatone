package io.rudione.chatone.util

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import java.awt.GraphicsEnvironment
import java.io.File

data class AvailableFont(
    val name: String,
    val fontFamily: FontFamily,
    val path: String? = null
)

private val builtInFonts: List<AvailableFont> by lazy {
    listOf(
        AvailableFont("Default", FontFamily.Default),
        AvailableFont("Serif", FontFamily.Serif),
        AvailableFont("Sans Serif", FontFamily.SansSerif),
        AvailableFont("Monospace", FontFamily.Monospace),
        AvailableFont("Cursive", FontFamily.Cursive),
    )
}


fun listAvailableFonts(customPaths: List<String> = emptyList()): List<AvailableFont> {
    val result = mutableListOf<AvailableFont>()
    result += builtInFonts

    val ge = GraphicsEnvironment.getLocalGraphicsEnvironment()

    val seen = mutableSetOf<String>()
    ge.availableFontFamilyNames.forEach { familyName ->
        if (familyName.startsWith(".") || familyName.startsWith("System")) return@forEach

        if (!seen.add(familyName)) return@forEach

        runCatching {
            val awtFont = java.awt.Font(familyName, java.awt.Font.PLAIN, 12)
            val resolvedFamily = awtFont.family
            if (resolvedFamily.equals("Dialog", ignoreCase = true) ||
                resolvedFamily.equals("DialogInput", ignoreCase = true) ||
                resolvedFamily.equals("Monospaced", ignoreCase = true) ||
                resolvedFamily.equals("SansSerif", ignoreCase = true) ||
                resolvedFamily.equals("Serif", ignoreCase = true)
            ) return@runCatching

            val fontFile = runCatching {
                val awtF = java.awt.Font(familyName, java.awt.Font.PLAIN, 12)
                val font2DMethod = awtF.javaClass.getMethod("getFont2D")
                val font2D = font2DMethod.invoke(awtF)
                val fileNameField = runCatching {
                    font2D.javaClass.getField("platName")
                }.getOrNull() ?: run {
                    var cls: Class<*>? = font2D.javaClass
                    var f: java.lang.reflect.Field? = null
                    while (cls != null && f == null) {
                        f = runCatching { cls!!.getDeclaredField("platName") }.getOrNull()
                        cls = cls.superclass
                    }
                    f
                }
                fileNameField?.also { it.isAccessible = true }?.get(font2D) as? String
            }.getOrNull()

            if (fontFile == null || !File(fontFile).exists()) return@runCatching

            val ff = runCatching {
                FontFamily(
                    androidx.compose.ui.text.platform.Font(
                        fontFile,
                        weight = FontWeight.Normal,
                        style = FontStyle.Normal
                    )
                )
            }.getOrNull() ?: return@runCatching

            result += AvailableFont(name = familyName, fontFamily = ff)
        }
    }

    customPaths.forEach { path ->
        val file = File(path)
        if (file.exists() && file.isFile) {
            val fontName = file.nameWithoutExtension
            if (seen.add(fontName)) {
                runCatching {
                    result += AvailableFont(
                        name = fontName,
                        fontFamily = FontFamily(
                            androidx.compose.ui.text.platform.Font(
                                file.absolutePath,
                                weight = FontWeight.Normal,
                                style = FontStyle.Normal
                            )
                        ),
                        path = file.absolutePath
                    )
                }
            }
        }
    }

    return result
}

actual fun resolveFontFamily(name: String, customPaths: List<String>): FontFamily {
    if (name == "Default" || name.isBlank()) return FontFamily.Default
    return runCatching {
        listAvailableFonts(customPaths).find { it.name == name }?.fontFamily
    }.getOrNull() ?: FontFamily.Default
}

actual fun listAvailableFontNames(customPaths: List<String>): List<String> {
    return listAvailableFonts(customPaths).map { it.name }
}