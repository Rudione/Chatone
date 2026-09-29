package io.rudione.chatone.presentation.browse

import io.rudione.chatone.domain.browse.BrowseStream
import io.rudione.chatone.presentation.theme.i18n.BrowseStrings

internal object BrowseFormatting {

    private const val MAX_PROMPT_TITLE = 200
    private const val MAX_PROMPT_TAGS = 8
    private const val UNKNOWN = "—"

    fun aiPrompt(
        stream: BrowseStream,
        category: String,
        strings: BrowseStrings,
        nowMs: Long
    ): String =
        strings.aiStreamPrompt(
            streamer = stream.displayName.ifBlank { stream.login },
            title = stream.title.replace('\n', ' ').trim().take(MAX_PROMPT_TITLE)
                .ifBlank { UNKNOWN },
            category = category.ifBlank { UNKNOWN },
            tags = stream.tags.take(MAX_PROMPT_TAGS).joinToString(", ").ifBlank { UNKNOWN },
            language = stream.language.uppercase().ifBlank { UNKNOWN },
            viewers = viewers(stream.viewers, strings),
            uptime = uptime(stream.startedAtMs, nowMs) ?: UNKNOWN
        )

    fun compact(value: Int, strings: BrowseStrings): String = when {
        value >= 1_000_000 -> tenths(
            value / 100_000,
            strings.decimalSeparator
        ) + strings.millionSuffix

        value >= 10_000 -> (value / 1_000).toString() + strings.thousandSuffix
        value >= 1_000 -> tenths(value / 100, strings.decimalSeparator) + strings.thousandSuffix
        else -> value.coerceAtLeast(0).toString()
    }

    fun viewers(value: Int, strings: BrowseStrings): String =
        strings.viewers(value, compact(value, strings))

    fun channels(value: Int, strings: BrowseStrings): String =
        strings.channels(value, compact(value, strings))

    fun uptime(startedAtMs: Long?, nowMs: Long): String? {
        val elapsedMinutes =
            ((nowMs - (startedAtMs ?: return null)) / 60_000L).takeIf { it >= 0 } ?: return null
        val hours = elapsedMinutes / 60
        val minutes = elapsedMinutes % 60
        return if (hours > 0) "${hours}h ${
            minutes.toString().padStart(2, '0')
        } min" else "$minutes min"
    }

    private fun tenths(scaledByTen: Int, separator: String): String {
        val whole = scaledByTen / 10
        val fraction = scaledByTen % 10
        return if (fraction == 0) whole.toString() else "$whole$separator$fraction"
    }
}
