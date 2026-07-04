package io.rudione.chatone.util

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import java.io.File

private val SYSTEM_FONT_DIRS = listOf(
    "/system/fonts",
    "/system/font",
    "/data/fonts"
)

private val FONT_EXTENSIONS = setOf("ttf", "otf")

private data class AndroidFont(val name: String, val path: String)

private val builtInFonts = listOf(
    "Default" to FontFamily.Default,
    "Serif" to FontFamily.Serif,
    "Sans Serif" to FontFamily.SansSerif,
    "Monospace" to FontFamily.Monospace,
    "Cursive" to FontFamily.Cursive,
)

private fun loadFontFamily(file: File): FontFamily? = runCatching {
    if (!file.exists() || !file.isFile) return null
    FontFamily(
        Font(
            file = file,
            weight = FontWeight.Normal,
            style = FontStyle.Normal
        )
    )
}.getOrNull()

private fun collectSystemFonts(): List<AndroidFont> {
    val seen = mutableSetOf<String>()
    val result = mutableListOf<AndroidFont>()
    for (dir in SYSTEM_FONT_DIRS) {
        val folder = File(dir)
        if (!folder.exists() || !folder.isDirectory) continue
        folder.listFiles()
            ?.filter { it.isFile && it.extension.lowercase() in FONT_EXTENSIONS }
            ?.forEach { file ->
                val name = file.nameWithoutExtension
                    .replace("-", " ")
                    .replace("_", " ")
                if (seen.add(name)) result += AndroidFont(name = name, path = file.absolutePath)
            }
    }
    return result
}

actual fun resolveFontFamily(name: String, customPaths: List<String>): FontFamily {
    if (name.isBlank() || name == "Default") return FontFamily.Default

    builtInFonts.find { it.first == name }?.let { return it.second }

    for (path in customPaths) {
        val file = File(path)
        if (file.exists() && file.nameWithoutExtension == name) {
            loadFontFamily(file)?.let { return it }
        }
    }

    collectSystemFonts()
        .find { it.name == name }
        ?.let { loadFontFamily(File(it.path))?.let { ff -> return ff } }

    return FontFamily.Default
}

actual fun listAvailableFontNames(customPaths: List<String>): List<String> {
    val result = mutableListOf<String>()
    val seen = mutableSetOf<String>()

    builtInFonts.forEach { (name, _) -> if (seen.add(name)) result += name }

    collectSystemFonts().forEach { font ->
        if (seen.add(font.name)) result += font.name
    }

    for (path in customPaths) {
        val file = File(path)
        if (file.exists() && file.isFile && file.extension.lowercase() in FONT_EXTENSIONS) {
            val name = file.nameWithoutExtension
            if (seen.add(name)) result += name
        }
    }

    return result
}