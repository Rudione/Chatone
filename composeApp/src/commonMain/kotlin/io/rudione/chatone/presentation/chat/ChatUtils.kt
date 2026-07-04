package io.rudione.chatone.presentation.chat

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
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import io.rudione.chatone.presentation.settings.SettingsState
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime


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
    return try {
        val c = hexColor.substring(1).toLong(16)
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
    val lower = url.lowercase()
    if (lower.matches(Regex(""".*\.(png|jpg|jpeg|gif|webp|bmp|svg)(\?.*)?$"""))) return true
    return lower.contains("i.imgur.com/") ||
            (lower.contains("imgur.com/") && !lower.contains("/a/") && !lower.contains("/gallery/")) ||
            lower.contains("kappa.lol/") || lower.contains("kappalol.com/") ||
            lower.contains("cdn.7tv.app/") || lower.contains("cdn.betterttv.net/") ||
            lower.contains("cdn.frankerfacez.com/") || lower.contains("pbs.twimg.com/") ||
            lower.contains("media.discordapp.net/") || lower.contains("cdn.discordapp.com/attachments/") ||
            lower.contains("i.redd.it/") || lower.contains("preview.redd.it/")
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

internal fun pauseHotkeyMatches(event: KeyEvent, hotkey: String): Boolean {
    if (hotkey.isBlank()) return false
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
    else -> if (name.length == 1) charToKey[name[0].uppercaseChar()] == key else false
}


internal fun adjustReadableColor(color: Color, background: Color): Color {
    val bgLum = 0.299f * background.red + 0.587f * background.green + 0.114f * background.blue
    val isDarkBg = bgLum < 0.5f

    val r = color.red; val g = color.green; val b = color.blue
    val max = maxOf(r, g, b); val min = minOf(r, g, b)
    val l = (max + min) / 2f
    val d = max - min

    var h = 0f
    var s = 0f
    if (d > 0.0001f) {
        s = if (l > 0.5f) d / (2f - max - min) else d / (max + min)
        h = when (max) {
            r -> (g - b) / d + if (g < b) 6f else 0f
            g -> (b - r) / d + 2f
            else -> (r - g) / d + 4f
        } / 6f
    }

    val targetL = if (isDarkBg) l.coerceIn(0.50f, 0.78f) else l.coerceIn(0.24f, 0.60f)
    val targetS = s.coerceAtMost(0.90f)

    return hslToColor(h, targetS, targetL).copy(alpha = color.alpha)
}

private fun hslToColor(h: Float, s: Float, l: Float): Color {
    if (s <= 0.0001f) return Color(l, l, l)
    val q = if (l < 0.5f) l * (1f + s) else l + s - l * s
    val p = 2f * l - q
    fun hue(t: Float): Float {
        var tt = t
        if (tt < 0f) tt += 1f
        if (tt > 1f) tt -= 1f
        return when {
            tt < 1f / 6f -> p + (q - p) * 6f * tt
            tt < 1f / 2f -> q
            tt < 2f / 3f -> p + (q - p) * (2f / 3f - tt) * 6f
            else -> p
        }
    }
    return Color(hue(h + 1f / 3f), hue(h), hue(h - 1f / 3f))
}

internal fun stableUserColor(login: String): Color {
    val palette = longArrayOf(
        0xFFFF4A80L, 0xFF1E90FFL, 0xFF2ECC71L, 0xFFFF7F50L,
        0xFF9B59B6L, 0xFFF1C40F, 0xFF00CED1L, 0xFFFF6B6BL,
        0xFF8A2BE2L, 0xFF20C997L, 0xFFFFA500L, 0xFFD9534FL
    )
    val key = login.lowercase()
    var h = 0
    for (c in key) h = (h * 31 + c.code)
    val idx = ((h.toLong() and 0x7FFFFFFFL) % palette.size).toInt()
    return Color(palette[idx] or 0xFF000000L)
}
