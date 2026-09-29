package io.rudione.chatone.presentation.stream

import io.rudione.chatone.domain.stream.StreamErrorKind
import io.rudione.chatone.presentation.theme.i18n.StreamPlayerStrings
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.time.Clock
import kotlin.time.Instant

internal object StreamFormatting {

    fun decimal(value: Float, fractionDigits: Int, separator: Char): String {
        if (!value.isFinite()) return "—"
        val scale = when (fractionDigits) {
            0 -> 1
            1 -> 10
            else -> 100
        }
        val scaled = (abs(value) * scale).roundToLong()
        val whole = scaled / scale
        val sign = if (value < 0f && scaled != 0L) "-" else ""
        if (fractionDigits <= 0) return "$sign$whole"
        val fraction = (scaled % scale).toString().padStart(if (scale == 10) 1 else 2, '0')
        return "$sign$whole$separator$fraction"
    }

    fun compactCount(count: Int, separator: Char): String = when {
        count >= 1_000_000 -> decimal(count / 1_000_000f, 1, separator).removeSuffix("${separator}0") + "M"
        count >= 10_000 -> "${(count / 1_000f).roundToInt()}K"
        count >= 1_000 -> decimal(count / 1_000f, 1, separator).removeSuffix("${separator}0") + "K"
        else -> count.coerceAtLeast(0).toString()
    }

    fun uptime(startedAtIso: String, nowMs: Long = Clock.System.now().toEpochMilliseconds()): String? {
        val startMs = runCatching { Instant.parse(startedAtIso).toEpochMilliseconds() }.getOrNull() ?: return null
        return uptime(startMs, nowMs)
    }

    fun uptime(startMs: Long, nowMs: Long = Clock.System.now().toEpochMilliseconds()): String {
        val totalSeconds = ((nowMs - startMs) / 1000L).coerceAtLeast(0L)
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        val mm = minutes.toString().padStart(2, '0')
        val ss = seconds.toString().padStart(2, '0')
        return if (hours > 0) "$hours:$mm:$ss" else "$minutes:$ss"
    }

    fun errorMessage(kind: StreamErrorKind?, strings: StreamPlayerStrings): String = when (kind) {
        StreamErrorKind.NETWORK -> strings.errorNetwork
        StreamErrorKind.FORBIDDEN -> strings.errorForbidden
        StreamErrorKind.GEOBLOCKED -> strings.errorGeoblocked
        StreamErrorKind.SUBSCRIBERS_ONLY -> strings.errorSubscribersOnly
        StreamErrorKind.UNSUPPORTED_CODEC -> strings.errorCodec
        StreamErrorKind.NOT_FOUND -> strings.errorNotFound
        StreamErrorKind.UNKNOWN, null -> strings.errorUnknown
    }
}
