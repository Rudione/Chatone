package io.rudione.chatone.util.font

import androidx.compose.ui.text.font.FontFamily
import platform.Foundation.NSFileManager
import platform.Foundation.NSString
import platform.Foundation.lastPathComponent
import platform.Foundation.pathExtension
import platform.Foundation.stringByDeletingPathExtension
import platform.UIKit.UIFont

private val FONT_EXTENSIONS = setOf("ttf", "otf")

private val builtInFonts = listOf(
    "Default" to FontFamily.Default,
    "Inter" to FontFamily.Default,
    "Serif" to FontFamily.Serif,
    "Sans Serif" to FontFamily.SansSerif,
    "Monospace" to FontFamily.Monospace,
    "Cursive" to FontFamily.Cursive,
)

private fun collectSystemFontNames(): List<String> =
    UIFont.familyNames()
        .filterIsInstance<String>()
        .filterNot { it.startsWith(".") }
        .sorted()

private fun fileExists(path: String): Boolean =
    NSFileManager.defaultManager.fileExistsAtPath(path)

private fun pathExtension(path: String): String =
    (path as NSString).pathExtension.lowercase()

private fun fileNameWithoutExtension(path: String): String =
    (path as NSString).lastPathComponent.let { last ->
        (last as NSString).stringByDeletingPathExtension
    }

actual fun resolveFontFamily(name: String, customPaths: List<String>): FontFamily {
    if (name.isBlank() || name == "Default") return FontFamily.Default

    builtInFonts.find { it.first == name }?.let { return it.second }

    for (path in customPaths) {
        if (fileExists(path) && fileNameWithoutExtension(path) == name) {
            return guessGenericFamily(name)
        }
    }

    if (collectSystemFontNames().contains(name)) {
        return guessGenericFamily(name)
    }

    return FontFamily.Default
}

private fun guessGenericFamily(name: String): FontFamily {
    val lower = name.lowercase()
    return when {
        lower.contains("mono") || lower.contains("courier") || lower.contains("menlo") ||
                lower.contains("monaco") || lower.contains("code") -> FontFamily.Monospace

        lower.contains("serif") && !lower.contains("sans") -> FontFamily.Serif

        lower.contains("sans") || lower.contains("helvetica") || lower.contains("arial") ||
                lower.contains("verdana") || lower.contains("tahoma") || lower.contains("gill") -> FontFamily.SansSerif

        lower.contains("cursive") || lower.contains("script") || lower.contains("chancery") ||
                lower.contains("brush") || lower.contains("snell") -> FontFamily.Cursive

        else -> FontFamily.Default
    }
}

actual fun listAvailableFontNames(customPaths: List<String>): List<String> {
    val result = mutableListOf<String>()
    val seen = mutableSetOf<String>()

    builtInFonts.forEach { (name, _) -> if (seen.add(name)) result += name }

    collectSystemFontNames().forEach { name ->
        if (seen.add(name)) result += name
    }

    for (path in customPaths) {
        if (fileExists(path) && pathExtension(path) in FONT_EXTENSIONS) {
            val name = fileNameWithoutExtension(path)
            if (seen.add(name)) result += name
        }
    }

    return result
}
