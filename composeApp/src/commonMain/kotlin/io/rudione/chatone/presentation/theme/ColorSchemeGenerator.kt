package io.rudione.chatone.presentation.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.PI
import kotlin.math.atan2

object ColorSchemeGenerator {

    private val DARK_TONES = mapOf(
        "primary" to 80,
        "onPrimary" to 20,
        "primaryContainer" to 30,
        "onPrimaryContainer" to 90,
        "secondary" to 80,
        "onSecondary" to 20,
        "secondaryContainer" to 30,
        "onSecondaryContainer" to 90,
        "tertiary" to 80,
        "onTertiary" to 20,
        "tertiaryContainer" to 30,
        "onTertiaryContainer" to 90,
        "error" to 80,
        "onError" to 20,
        "errorContainer" to 30,
        "onErrorContainer" to 90,
        "background" to 10,
        "onBackground" to 90,
        "surface" to 12,
        "onSurface" to 90,
        "surfaceVariant" to 20,
        "onSurfaceVariant" to 75,
        "outline" to 55,
        "outlineVariant" to 25,
        "inverseSurface" to 90,
        "inverseOnSurface" to 20,
        "inversePrimary" to 40,
    )

    private val LIGHT_TONES = mapOf(
        "primary" to 40,
        "onPrimary" to 100,
        "primaryContainer" to 90,
        "onPrimaryContainer" to 10,
        "secondary" to 40,
        "onSecondary" to 100,
        "secondaryContainer" to 90,
        "onSecondaryContainer" to 10,
        "tertiary" to 40,
        "onTertiary" to 100,
        "tertiaryContainer" to 90,
        "onTertiaryContainer" to 10,
        "error" to 40,
        "onError" to 100,
        "errorContainer" to 90,
        "onErrorContainer" to 10,
        "background" to 98,
        "onBackground" to 10,
        "surface" to 98,
        "onSurface" to 10,
        "surfaceVariant" to 90,
        "onSurfaceVariant" to 30,
        "outline" to 50,
        "outlineVariant" to 80,
        "inverseSurface" to 20,
        "inverseOnSurface" to 90,
        "inversePrimary" to 80,
    )

    fun generateFromSeed(
        seedColor: Int,
        isDark: Boolean,
        contrastLevel: Float = 0f
    ): ColorScheme {
        val baseHct = rgbToHct(Color(seedColor))
        val tones = if (isDark) DARK_TONES else LIGHT_TONES

        val primary = generateTone(baseHct, tones["primary"]!!, contrastLevel)
        val onPrimary = generateTone(baseHct, tones["onPrimary"]!!, contrastLevel)
        val primaryContainer = generateTone(baseHct, tones["primaryContainer"]!!, contrastLevel)
        val onPrimaryContainer = generateTone(baseHct, tones["onPrimaryContainer"]!!, contrastLevel)

        val secondaryHct = baseHct.copy(hue = (baseHct.hue + 60f) % 360f)
        val secondary = generateTone(secondaryHct, tones["secondary"]!!, contrastLevel)
        val onSecondary = generateTone(secondaryHct, tones["onSecondary"]!!, contrastLevel)
        val secondaryContainer = generateTone(secondaryHct, tones["secondaryContainer"]!!, contrastLevel)
        val onSecondaryContainer = generateTone(secondaryHct, tones["onSecondaryContainer"]!!, contrastLevel)

        val tertiaryHct = baseHct.copy(hue = (baseHct.hue - 60f + 360f) % 360f)
        val tertiary = generateTone(tertiaryHct, tones["tertiary"]!!, contrastLevel)
        val onTertiary = generateTone(tertiaryHct, tones["onTertiary"]!!, contrastLevel)
        val tertiaryContainer = generateTone(tertiaryHct, tones["tertiaryContainer"]!!, contrastLevel)
        val onTertiaryContainer = generateTone(tertiaryHct, tones["onTertiaryContainer"]!!, contrastLevel)

        val neutralHct = baseHct.copy(chroma = min(baseHct.chroma * 0.15f, 4f))
        val background = generateTone(neutralHct, tones["background"]!!, contrastLevel)
        val onBackground = generateTone(neutralHct, tones["onBackground"]!!, contrastLevel)
        val surface = generateTone(neutralHct, tones["surface"]!!, contrastLevel)
        val onSurface = generateTone(neutralHct, tones["onSurface"]!!, contrastLevel)
        val surfaceVariant = generateTone(neutralHct, tones["surfaceVariant"]!!, contrastLevel * 0.5f)
        val onSurfaceVariant = generateTone(neutralHct, tones["onSurfaceVariant"]!!, contrastLevel * 0.5f)
        val outline = generateTone(neutralHct, tones["outline"]!!, 0f)
        val outlineVariant = generateTone(neutralHct, tones["outlineVariant"]!!, 0f)

        val errorBase = Hct(25f, 45f, if (isDark) 80f else 40f)
        val error = generateTone(errorBase, tones["error"]!!, contrastLevel)
        val onError = generateTone(errorBase, tones["onError"]!!, contrastLevel)
        val errorContainer = generateTone(errorBase, tones["errorContainer"]!!, contrastLevel)
        val onErrorContainer = generateTone(errorBase, tones["onErrorContainer"]!!, contrastLevel)

        return if (isDark) {
            darkColorScheme(
                primary = primary, onPrimary = onPrimary,
                primaryContainer = primaryContainer, onPrimaryContainer = onPrimaryContainer,
                secondary = secondary, onSecondary = onSecondary,
                secondaryContainer = secondaryContainer, onSecondaryContainer = onSecondaryContainer,
                tertiary = tertiary, onTertiary = onTertiary,
                tertiaryContainer = tertiaryContainer, onTertiaryContainer = onTertiaryContainer,
                error = error, onError = onError,
                errorContainer = errorContainer, onErrorContainer = onErrorContainer,
                background = background, onBackground = onBackground,
                surface = surface, onSurface = onSurface,
                surfaceVariant = surfaceVariant, onSurfaceVariant = onSurfaceVariant,
                outline = outline, outlineVariant = outlineVariant,
                inverseSurface = Color(0xFFE0E0E0), inverseOnSurface = Color(0xFF202020),
                inversePrimary = primary.copy(alpha = 0.9f),
                scrim = Color(0xCC000000)
            )
        } else {
            lightColorScheme(
                primary = primary, onPrimary = onPrimary,
                primaryContainer = primaryContainer, onPrimaryContainer = onPrimaryContainer,
                secondary = secondary, onSecondary = onSecondary,
                secondaryContainer = secondaryContainer, onSecondaryContainer = onSecondaryContainer,
                tertiary = tertiary, onTertiary = onTertiary,
                tertiaryContainer = tertiaryContainer, onTertiaryContainer = onTertiaryContainer,
                error = error, onError = onError,
                errorContainer = errorContainer, onErrorContainer = onErrorContainer,
                background = background, onBackground = onBackground,
                surface = surface, onSurface = onSurface,
                surfaceVariant = surfaceVariant, onSurfaceVariant = onSurfaceVariant,
                outline = outline, outlineVariant = outlineVariant,
                inverseSurface = Color(0xFF202020), inverseOnSurface = Color(0xFFE0E0E0),
                inversePrimary = primary.copy(alpha = 0.9f),
                scrim = Color(0x66000000)
            )
        }
    }

    fun applyOverrides(base: ColorScheme, overrides: Map<String, Int>): ColorScheme {
        return base.copy(
            primary = overrides["primary"]?.let { Color(it) } ?: base.primary,
            onPrimary = overrides["onPrimary"]?.let { Color(it) } ?: base.onPrimary,
            primaryContainer = overrides["primaryContainer"]?.let { Color(it) } ?: base.primaryContainer,
            secondary = overrides["secondary"]?.let { Color(it) } ?: base.secondary,
            tertiary = overrides["tertiary"]?.let { Color(it) } ?: base.tertiary,
            error = overrides["error"]?.let { Color(it) } ?: base.error,
            background = overrides["background"]?.let { Color(it) } ?: base.background,
            surface = overrides["surface"]?.let { Color(it) } ?: base.surface,
            outline = overrides["outline"]?.let { Color(it) } ?: base.outline,
        )
    }

    private data class Hct(val hue: Float, val chroma: Float, val tone: Float)

    private fun rgbToHct(color: Color): Hct {
        val (r, g, b) = listOf(color.red, color.green, color.blue).map { it.coerceIn(0f, 1f) }

        val linearR = if (r <= 0.04045f) r / 12.92f else ((r + 0.055f) / 1.055f).pow(2.4f)
        val linearG = if (g <= 0.04045f) g / 12.92f else ((g + 0.055f) / 1.055f).pow(2.4f)
        val linearB = if (b <= 0.04045f) b / 12.92f else ((b + 0.055f) / 1.055f).pow(2.4f)

        val x = linearR * 0.4124f + linearG * 0.3576f + linearB * 0.1805f
        val y = linearR * 0.2126f + linearG * 0.7152f + linearB * 0.0722f
        val z = linearR * 0.0193f + linearG * 0.1192f + linearB * 0.9505f

        val xyzToLab = { v: Float ->
            if (v > 0.008856f) v.pow(1f / 3f) else (7.787f * v) + (16f / 116f)
        }
        val fx = xyzToLab(x / 0.95047f)
        val fy = xyzToLab(y / 1.00000f)
        val fz = xyzToLab(z / 1.08883f)

        val l = (116f * fy) - 16f
        val a = 500f * (fx - fy)
        val bVal = 200f * (fy - fz)

        val chroma = sqrt(a * a + bVal * bVal)

        val hue = (atan2(bVal.toDouble(), a.toDouble()) * 180 / PI).toFloat()
        val normalizedHue = if (hue < 0f) hue + 360f else hue

        return Hct(normalizedHue, chroma, l)
    }

    private fun generateTone(hct: Hct, targetTone: Int, contrastAdjust: Float): Color {
        val adjustedChroma = when {
            targetTone < 20 -> hct.chroma * 0.3f
            targetTone < 40 -> hct.chroma * 0.6f
            targetTone < 70 -> hct.chroma * 0.9f
            else -> hct.chroma * 0.7f
        } * (1f + contrastAdjust * 0.3f)

        val l = targetTone.toFloat()
        val fy = (l + 16f) / 116f

        val hueRad = hct.hue * (PI / 180f).toFloat()
        val fx = fy + (cos(hueRad.toDouble()).toFloat() * adjustedChroma) / 500f
        val fz = fy - (sin(hueRad.toDouble()).toFloat() * adjustedChroma) / 200f

        val labToXyz = { v: Float ->
            val cube = v.pow(3f)
            if (cube > 0.008856f) cube else (v - 16f / 116f) / 7.787f
        }
        val x = labToXyz(fx) * 0.95047f
        val y = labToXyz(fy) * 1.00000f
        val z = labToXyz(fz) * 1.08883f

        val xyzToRgb = { r: Float, g: Float, b: Float ->
            val linearR = r * 3.2406f + g * -1.5372f + b * -0.4986f
            val linearG = r * -0.9689f + g * 1.8758f + b * 0.0415f
            val linearB = r * 0.0557f + g * -0.2040f + b * 1.0570f

            val gamma = { c: Float ->
                if (c <= 0.0031308f) 12.92f * c else 1.055f * c.pow(1f / 2.4f) - 0.055f
            }
            Color(
                red = gamma(linearR).coerceIn(0f, 1f),
                green = gamma(linearG).coerceIn(0f, 1f),
                blue = gamma(linearB).coerceIn(0f, 1f)
            )
        }

        return xyzToRgb(x, y, z)
    }

    fun getAvailableRoles(): List<String> = listOf(
        "primary", "secondary", "tertiary", "error",
        "background", "surface", "surfaceContainer", "surfaceContainerHigh",
        "onPrimary", "onSurface", "onSurfaceVariant", "outline",
        "primaryContainer", "onPrimaryContainer",
        "inverseSurface", "inverseOnSurface"
    )

    fun roleToDisplayName(role: String): String = when (role) {
        "primary" -> "Main Accent"
        "secondary" -> "Secondary Accent"
        "tertiary" -> "Highlight Accent"
        "error" -> "Error / Alert"
        "background" -> "App Background"
        "surface" -> "Chat Background"
        "surfaceContainer" -> "Panel Background"
        "surfaceContainerHigh" -> "Header / Footer Background"
        "onPrimary" -> "Text on Main Accent"
        "onSurface" -> "Main Text Color"
        "onSurfaceVariant" -> "Secondary Text"
        "outline" -> "Borders / Dividers"
        "primaryContainer" -> "Button Background"
        "onPrimaryContainer" -> "Button Text"
        "inverseSurface" -> "Overlay Background"
        "inverseOnSurface" -> "Overlay Text"
        else -> role.replaceFirstChar { it.uppercase() }
    }
}
