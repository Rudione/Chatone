package io.rudione.chatone.presentation.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import kotlin.math.max
import kotlin.math.min

data class AccentPalette(
    val name: String,
    val accent: Color,
    val base: Color,
    val gradient: List<Color>? = null
) {
    val previewColor: Color get() = accent
}

val ChatoneBrandGradient = listOf(
    Color(0xFFC77DFF),
    Color(0xFF7C3AED),
    Color(0xFF2B7FFF)
)

val ExpressivePalettes = listOf(
    AccentPalette(
        "Chatone",
        accent = Color(0xFF8B8DE4),
        base = Color(0xFF16131E),
        gradient = ChatoneBrandGradient
    ),
    AccentPalette("Cosmic", accent = Color(0xFFB3AECB), base = Color(0xFF23212C)),
    AccentPalette("Lavender", accent = Color(0xFFD2C3F6), base = Color(0xFF191424)),
    AccentPalette("Violet", accent = ChatoneColors.Violet500, base = Color(0xFF1C132F)),
    AccentPalette("Wine Ash", accent = Color(0xFFD4AFC8), base = Color(0xFF32292F)),
    AccentPalette("Candy Blue", accent = Color(0xFFB2D5E5), base = Color(0xFF101B20)),
    AccentPalette("Turquoise", accent = Color(0xFF99E1D9), base = Color(0xFF0E1D1B)),
    AccentPalette("Lime", accent = Color(0xFFC6FF34), base = Color(0xFF16190D)),
    AccentPalette("Vanilla", accent = Color(0xFFF1FEC8), base = Color(0xFF1A1611)),
    AccentPalette("Carbon", accent = Color(0xFFE9E9E9), base = Color(0xFF171717)),
    AccentPalette("Onyx", accent = Color(0xFFC7CBD2), base = Color(0xFF020202))
)

const val DEFAULT_ACCENT_INDEX = 0

const val ACCENT_PALETTE_SCHEMA_VERSION = 4

fun accentPaletteAt(index: Int): AccentPalette =
    ExpressivePalettes.getOrElse(index) { ExpressivePalettes[DEFAULT_ACCENT_INDEX] }

private val SurfaceLevelScale = floatArrayOf(0.30f, 0.42f, 0.62f, 0.78f, 0.92f, 1.00f, 1.18f)
private val SurfaceLevelLift = floatArrayOf(0.000f, 0.012f, 0.026f, 0.038f, 0.050f, 0.058f, 0.078f)

private const val LEVEL_LOWEST = 0
private const val LEVEL_BACKGROUND = 1
private const val LEVEL_LOW = 2
private const val LEVEL_BASE = 3
private const val LEVEL_VARIANT = 4
private const val LEVEL_HIGH = 5
private const val LEVEL_HIGHEST = 6

private const val MAX_SURFACE_SATURATION = 0.30f

internal data class Hsl(val hue: Float, val saturation: Float, val lightness: Float)

internal fun Color.toHslValues(): Hsl {
    val mx = max(red, max(green, blue))
    val mn = min(red, min(green, blue))
    val l = (mx + mn) / 2f
    if (mx == mn) return Hsl(0f, 0f, l)
    val d = mx - mn
    val s = if (l > 0.5f) d / (2f - mx - mn) else d / (mx + mn)
    val h = when (mx) {
        red -> (green - blue) / d + if (green < blue) 6f else 0f
        green -> (blue - red) / d + 2f
        else -> (red - green) / d + 4f
    } / 6f * 360f
    return Hsl(h, s, l)
}

internal fun Color.relativeLuminance(): Float = 0.2126f * red + 0.7152f * green + 0.0722f * blue

internal fun Color.withLightness(lightness: Float): Color {
    val hsl = toHslValues()
    return Color.hsl(hsl.hue, hsl.saturation, lightness.coerceIn(0f, 1f))
}

internal fun Color.rotateHue(degrees: Float, saturationScale: Float, lightness: Float): Color {
    val hsl = toHslValues()
    val hue = ((hsl.hue + degrees) % 360f + 360f) % 360f
    return Color.hsl(hue, (hsl.saturation * saturationScale).coerceIn(0f, 1f), lightness.coerceIn(0f, 1f))
}

internal fun surfaceLevel(base: Color, level: Int): Color {
    val hsl = base.toHslValues()
    val lightness = (hsl.lightness * SurfaceLevelScale[level] + SurfaceLevelLift[level]).coerceIn(0f, 1f)
    val saturation = min(hsl.saturation, MAX_SURFACE_SATURATION) * (1f - 0.30f * level / 6f)
    return Color.hsl(hsl.hue, saturation.coerceIn(0f, 1f), lightness)
}

private fun normalizedAccent(accent: Color): Color {
    val hsl = accent.toHslValues()
    return if (hsl.lightness >= 0.52f) accent else accent.withLightness(0.70f)
}

fun accentColorScheme(palette: AccentPalette): ColorScheme {
    val accent = normalizedAccent(palette.accent)
    val base = palette.base

    val containerLowest = surfaceLevel(base, LEVEL_LOWEST)
    val background = surfaceLevel(base, LEVEL_BACKGROUND)
    val containerLow = surfaceLevel(base, LEVEL_LOW)
    val container = surfaceLevel(base, LEVEL_BASE)
    val variant = surfaceLevel(base, LEVEL_VARIANT)
    val containerHigh = surfaceLevel(base, LEVEL_HIGH)
    val containerHighest = surfaceLevel(base, LEVEL_HIGHEST)

    val onSurface = lerp(Color.White, accent, 0.10f)
    val onSurfaceMuted = lerp(onSurface, container, 0.42f)
    val onAccent = if (accent.relativeLuminance() > 0.45f) containerLowest else Color.White

    val secondary = accent.rotateHue(-26f, 0.60f, 0.76f)
    val tertiary = accent.rotateHue(34f, 0.80f, 0.72f)

    return darkColorScheme(
        primary = accent,
        onPrimary = onAccent,
        primaryContainer = accent.copy(alpha = 0.20f).compositeOver(containerHigh),
        onPrimaryContainer = lerp(accent, Color.White, 0.42f),
        inversePrimary = accent.withLightness(0.42f),

        secondary = secondary,
        onSecondary = onAccent,
        secondaryContainer = secondary.copy(alpha = 0.18f).compositeOver(containerHigh),
        onSecondaryContainer = lerp(secondary, Color.White, 0.42f),

        tertiary = tertiary,
        onTertiary = onAccent,
        tertiaryContainer = tertiary.copy(alpha = 0.18f).compositeOver(containerHigh),
        onTertiaryContainer = lerp(tertiary, Color.White, 0.42f),

        error = ChatoneColors.Error,
        onError = Color.White,
        errorContainer = ChatoneColors.Error.copy(alpha = 0.22f).compositeOver(containerHigh),
        onErrorContainer = lerp(ChatoneColors.Error, Color.White, 0.42f),

        background = background,
        onBackground = onSurface,
        surface = container,
        onSurface = onSurface,
        surfaceVariant = variant,
        onSurfaceVariant = onSurfaceMuted,
        surfaceTint = accent,

        surfaceBright = containerHighest,
        surfaceDim = containerLowest,
        surfaceContainerLowest = containerLowest,
        surfaceContainerLow = containerLow,
        surfaceContainer = container,
        surfaceContainerHigh = containerHigh,
        surfaceContainerHighest = containerHighest,

        outline = lerp(containerHighest, accent, 0.18f),
        outlineVariant = lerp(variant, accent, 0.06f),

        inverseSurface = onSurface,
        inverseOnSurface = container,
        scrim = Color(0xCC000000)
    )
}
