package io.rudione.chatone.presentation.chat

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerButtons
import androidx.compose.ui.input.pointer.isBackPressed
import androidx.compose.ui.input.pointer.isForwardPressed
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.isTertiaryPressed
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import io.rudione.chatone.presentation.settings.SettingsState
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

internal fun formatTimestamp(
    timestamp: Long,
    format: SettingsState.TimestampFormat = SettingsState.TimestampFormat.H24
): String {
    val instant = Instant.fromEpochMilliseconds(timestamp)
    val dt = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    return when (format) {
        SettingsState.TimestampFormat.H24 ->
            "${dt.hour.toString().padStart(2, '0')}:${dt.minute.toString().padStart(2, '0')}"

        SettingsState.TimestampFormat.H12 -> {
            val h = if (dt.hour == 0) 12 else if (dt.hour > 12) dt.hour - 12 else dt.hour
            "$h:${dt.minute.toString().padStart(2, '0')} ${if (dt.hour < 12) "AM" else "PM"}"
        }

        SettingsState.TimestampFormat.OFF -> ""
    }
}

internal fun parseColor(hexColor: String?): Color? {
    if (hexColor == null || !hexColor.startsWith("#")) return null
    val hex = hexColor.substring(1)
    if (hex.length != 6 && hex.length != 8) return null
    return try {
        val c = hex.takeLast(6).toLong(16)
        Color(
            red = ((c shr 16) and 0xFF) / 255f,
            green = ((c shr 8) and 0xFF) / 255f,
            blue = (c and 0xFF) / 255f
        )
    } catch (_: Exception) {
        null
    }
}

internal fun argbToColor(argb: Int): Color {
    val a = ((argb shr 24) and 0xFF) / 255f
    val r = ((argb shr 16) and 0xFF) / 255f
    val g = ((argb shr 8) and 0xFF) / 255f
    val b = (argb and 0xFF) / 255f
    return Color(r, g, b, a)
}

internal fun computeEmoteDisplaySize(
    origWidth: Int,
    origHeight: Int,
    baseHeightSp: TextUnit
): Pair<TextUnit, TextUnit> {
    if (origWidth <= 0 || origHeight <= 0) return baseHeightSp to baseHeightSp
    return (baseHeightSp.value * (origWidth.toFloat() / origHeight.toFloat()).coerceIn(
        0.5f,
        4.0f
    )).sp to baseHeightSp
}

internal fun isImageUrl(url: String): Boolean {
    if (io.rudione.chatone.util.media.LinkImageResolver.hasResolver(url)) return true
    val lower = url.lowercase()
    return lower.contains("imgur.com/") && !lower.contains("/a/") && !lower.contains("/gallery/")
}

internal fun userRoleRank(
    isBroadcaster: Boolean, isGrandMod: Boolean = false,
    isModerator: Boolean, isVip: Boolean, isSubscriber: Boolean
): Int = when {
    isBroadcaster -> 10; isGrandMod -> 6; isModerator -> 4; isVip -> 2; isSubscriber -> 1; else -> 0
}

internal fun canActOnUser(
    actorIsBroadcaster: Boolean, actorIsMod: Boolean,
    targetIsBroadcaster: Boolean, targetIsMod: Boolean,
    targetIsVip: Boolean, targetIsSubscriber: Boolean,
    actorIsGrandMod: Boolean = false, targetIsGrandMod: Boolean = false
): Boolean {
    val actorRank = userRoleRank(actorIsBroadcaster, actorIsGrandMod, actorIsMod, false, false)
    val targetRank = userRoleRank(
        targetIsBroadcaster,
        targetIsGrandMod,
        targetIsMod,
        targetIsVip,
        targetIsSubscriber
    )
    return actorRank > targetRank
}

internal fun isCtrlKey(key: Key): Boolean =
    key == Key.CtrlLeft || key == Key.CtrlRight || key == Key.MetaLeft || key == Key.MetaRight

internal fun isAltKey(key: Key): Boolean = key == Key.AltLeft || key == Key.AltRight
internal fun isShiftKey(key: Key): Boolean = key == Key.ShiftLeft || key == Key.ShiftRight
@Suppress("UNUSED")
internal fun isModifierKey(key: Key): Boolean = isCtrlKey(key) || isAltKey(key) || isShiftKey(key)

internal const val MOUSE_HOTKEY_PREFIX = "mouse"
internal const val MOUSE_RIGHT = "mouse2"
internal const val MOUSE_MIDDLE = "mouse3"
internal const val MOUSE_BACK = "mouse4"
internal const val MOUSE_FORWARD = "mouse5"

internal fun hotkeyMouseButton(hotkey: String): String? {
    if (hotkey.isBlank()) return null
    val main = hotkey.lowercase().split("+").map { it.trim() }.last()
    return if (main.startsWith(MOUSE_HOTKEY_PREFIX)) main else null
}

internal fun PointerButtons.isPauseButtonPressed(button: String?): Boolean = when (button) {
    MOUSE_RIGHT -> isSecondaryPressed
    MOUSE_MIDDLE -> isTertiaryPressed
    MOUSE_BACK -> isBackPressed
    MOUSE_FORWARD -> isForwardPressed
    else -> false
}

internal fun pauseHotkeyMatches(event: KeyEvent, hotkey: String): Boolean {
    if (hotkey.isBlank()) return false
    if (hotkeyMouseButton(hotkey) != null) return false
    val parts = hotkey.lowercase().split("+").map { it.trim() }
    val mainKey = parts.last()
    val needsCtrl = parts.contains("ctrl")
    val needsAlt = parts.contains("alt")
    val needsShift = parts.contains("shift")
    val isCtrl = event.isCtrlPressed || event.isMetaPressed
    if (mainKey == "alt") {
        if (!isAltKey(event.key)) return false; return event.isAltPressed && !event.isShiftPressed && isCtrl == needsCtrl
    }
    if (mainKey == "ctrl") {
        if (!isCtrlKey(event.key)) return false; return isCtrl && !event.isAltPressed && !event.isShiftPressed
    }
    if (mainKey == "shift") {
        if (!isShiftKey(event.key)) return false; return event.isShiftPressed && !event.isAltPressed && !isCtrl
    }
    return keyNameMatches(event.key, mainKey) && isCtrl == needsCtrl &&
            event.isAltPressed == needsAlt && event.isShiftPressed == needsShift
}

internal fun pauseHotkeyMatchesRelease(event: KeyEvent, hotkey: String): Boolean {
    if (hotkey.isBlank()) return false
    if (hotkeyMouseButton(hotkey) != null) return false
    val mainKey = hotkey.lowercase().split("+").map { it.trim() }.last()
    return when (mainKey) {
        "alt" -> isAltKey(event.key) || !event.isAltPressed
        "ctrl" -> isCtrlKey(event.key) || !(event.isCtrlPressed || event.isMetaPressed)
        "shift" -> isShiftKey(event.key) || !event.isShiftPressed
        else -> keyNameMatches(event.key, mainKey)
    }
}

internal val charToKey: Map<Char, Key> = buildMap {
    put('A', Key.A); put('B', Key.B); put('C', Key.C); put('D', Key.D); put('E', Key.E)
    put('F', Key.F); put('G', Key.G); put('H', Key.H); put('I', Key.I); put('J', Key.J)
    put('K', Key.K); put('L', Key.L); put('M', Key.M); put('N', Key.N); put('O', Key.O)
    put('P', Key.P); put('Q', Key.Q); put('R', Key.R); put('S', Key.S); put('T', Key.T)
    put('U', Key.U); put('V', Key.V); put('W', Key.W); put('X', Key.X); put('Y', Key.Y)
    put('Z', Key.Z)
    put('0', Key.Zero); put('1', Key.One); put('2', Key.Two); put('3', Key.Three)
    put('4', Key.Four); put('5', Key.Five); put('6', Key.Six); put('7', Key.Seven)
    put('8', Key.Eight); put('9', Key.Nine)
}

internal val keyToChar: Map<Key, Char> = charToKey.entries.associate { (c, k) -> k to c }

internal fun keyNameMatches(key: Key, name: String): Boolean = when (name) {
    "space" -> key == Key.Spacebar
    "enter" -> key == Key.Enter
    "tab" -> key == Key.Tab
    "escape", "esc" -> key == Key.Escape
    "backspace" -> key == Key.Backspace
    "delete" -> key == Key.Delete
    "home" -> key == Key.MoveHome
    "end" -> key == Key.MoveEnd
    "pageup" -> key == Key.PageUp
    "pagedown" -> key == Key.PageDown
    "up" -> key == Key.DirectionUp
    "down" -> key == Key.DirectionDown
    "left" -> key == Key.DirectionLeft
    "right" -> key == Key.DirectionRight
    "f1" -> key == Key.F1
    "f2" -> key == Key.F2
    "f3" -> key == Key.F3
    "f4" -> key == Key.F4
    "f5" -> key == Key.F5
    "f6" -> key == Key.F6
    "f7" -> key == Key.F7
    "f8" -> key == Key.F8
    "f9" -> key == Key.F9
    "f10" -> key == Key.F10
    "f11" -> key == Key.F11
    "f12" -> key == Key.F12
    else -> if (name.length == 1) charToKey[name[0].uppercaseChar()] == key else false
}

private val TWITCH_USERNAME_COLORS = longArrayOf(
    0xFFFF0000L,
    0xFF0000FFL,
    0xFF008000L,
    0xFFB22222L,
    0xFFFF7F50L,
    0xFF9ACD32L,
    0xFFFF4500L,
    0xFF2E8B57L,
    0xFFDAA520L,
    0xFFD2691EL,
    0xFF5F9EA0L,
    0xFF1E90FFL,
    0xFFFF69B4L,
    0xFF8A2BE2L,
    0xFF00FF7FL
)

internal fun stableUserColor(login: String): Color {
    val key = login.lowercase()
    if (key.isEmpty()) return Color(TWITCH_USERNAME_COLORS[0])
    val idx = (key.first().code + key.last().code) % TWITCH_USERNAME_COLORS.size
    return Color(TWITCH_USERNAME_COLORS[idx])
}

internal fun twitchNickColor(hexColor: String?, login: String): Color =
    parseColor(hexColor) ?: stableUserColor(login)

private const val NICK_MIN_CONTRAST = 4.5f
private const val NICK_LIGHTNESS_STEP = 0.1f
private const val NICK_NEAR_BLACK = 36f / 255f
private val NICK_NEAR_BLACK_REPLACEMENT = Color(0xFF7A7A7A)

private fun srgbToLinear(channel: Float): Float =
    if (channel <= 0.03928f) channel / 12.92f else ((channel + 0.055f) / 1.055f).pow(2.4f)

private fun relativeLuminance(color: Color): Float =
    0.2126f * srgbToLinear(color.red) +
        0.7152f * srgbToLinear(color.green) +
        0.0722f * srgbToLinear(color.blue)

private fun contrastRatio(a: Color, b: Color): Float {
    val la = relativeLuminance(a)
    val lb = relativeLuminance(b)
    return (max(la, lb) + 0.05f) / (min(la, lb) + 0.05f)
}

private fun hslToColor(hue: Float, saturation: Float, lightness: Float): Color {
    if (saturation <= 0f) return Color(lightness, lightness, lightness)
    val q = if (lightness < 0.5f) lightness * (1f + saturation)
    else lightness + saturation - lightness * saturation
    val p = 2f * lightness - q
    fun channel(offset: Float): Float {
        var t = offset
        if (t < 0f) t += 1f
        if (t > 1f) t -= 1f
        return when {
            t < 1f / 6f -> p + (q - p) * 6f * t
            t < 1f / 2f -> q
            t < 2f / 3f -> p + (q - p) * (2f / 3f - t) * 6f
            else -> p
        }
    }
    return Color(channel(hue + 1f / 3f), channel(hue), channel(hue - 1f / 3f))
}

private fun shiftLightness(color: Color, delta: Float): Color {
    val r = color.red
    val g = color.green
    val b = color.blue
    val mx = maxOf(r, g, b)
    val mn = minOf(r, g, b)
    val l = (mx + mn) / 2f
    val d = mx - mn
    var h = 0f
    var s = 0f
    if (d > 0.0001f) {
        s = if (l > 0.5f) d / (2f - mx - mn) else d / (mx + mn)
        h = when (mx) {
            r -> (g - b) / d + if (g < b) 6f else 0f
            g -> (b - r) / d + 2f
            else -> (r - g) / d + 4f
        } / 6f
    }
    return hslToColor(h, s, (l + delta).coerceIn(0f, 1f)).copy(alpha = color.alpha)
}

internal fun readableNickColor(color: Color, background: Color): Color {
    if (contrastRatio(color, background) >= NICK_MIN_CONTRAST) return color
    val darkBackground = relativeLuminance(background) < 0.18f
    if (darkBackground &&
        color.red < NICK_NEAR_BLACK && color.green < NICK_NEAR_BLACK && color.blue < NICK_NEAR_BLACK
    ) return NICK_NEAR_BLACK_REPLACEMENT
    val step = if (darkBackground) NICK_LIGHTNESS_STEP else -NICK_LIGHTNESS_STEP
    var adjusted = color
    var iterations = 0
    while (contrastRatio(adjusted, background) < NICK_MIN_CONTRAST && iterations < 50) {
        adjusted = shiftLightness(adjusted, step)
        iterations++
    }
    return adjusted
}

@Immutable
internal data class NickColors(val background: Color, val readable: Boolean) {
    fun of(hexColor: String?, login: String): Color = adjust(twitchNickColor(hexColor, login))
    fun adjust(color: Color): Color =
        if (readable) readableNickColor(color, background) else color
}

@Composable
internal fun rememberNickColors(): NickColors {
    val readable = LocalReadableNickColors.current
    val background = MaterialTheme.colorScheme.background
    return remember(readable, background) { NickColors(background, readable) }
}
