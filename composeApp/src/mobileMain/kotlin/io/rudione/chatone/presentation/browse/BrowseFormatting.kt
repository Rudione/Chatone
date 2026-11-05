package io.rudione.chatone.presentation.browse

import androidx.compose.runtime.Composable
import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.browse_ai_stream_prompt
import chatone.composeapp.generated.resources.browse_decimal_separator
import chatone.composeapp.generated.resources.browse_million_suffix
import chatone.composeapp.generated.resources.browse_thousand_suffix
import chatone.composeapp.generated.resources.browse_uptime_hours
import chatone.composeapp.generated.resources.browse_uptime_minutes
import chatone.composeapp.generated.resources.browse_viewers
import io.rudione.chatone.domain.browse.BrowseStream
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

enum class CompactUnit { NONE, THOUSAND, MILLION }

data class CompactCount(val whole: Int, val tenths: Int, val unit: CompactUnit)

data class Uptime(val hours: Long, val minutes: Long)

object BrowseFormatting {

    private const val MAX_PROMPT_TITLE = 200
    private const val MAX_PROMPT_TAGS = 8
    private const val UNKNOWN = "—"

    fun compactCount(value: Int): CompactCount = when {
        value >= 1_000_000 -> (value / 100_000).let { CompactCount(it / 10, it % 10, CompactUnit.MILLION) }
        value >= 10_000 -> CompactCount(value / 1_000, 0, CompactUnit.THOUSAND)
        value >= 1_000 -> (value / 100).let { CompactCount(it / 10, it % 10, CompactUnit.THOUSAND) }
        else -> CompactCount(value.coerceAtLeast(0), 0, CompactUnit.NONE)
    }

    fun uptime(startedAtMs: Long?, nowMs: Long): Uptime? {
        val elapsedMinutes = ((nowMs - (startedAtMs ?: return null)) / 60_000L).takeIf { it >= 0 } ?: return null
        return Uptime(elapsedMinutes / 60, elapsedMinutes % 60)
    }

    @Composable
    fun compact(value: Int): String = render(
        compactCount(value),
        stringResource(Res.string.browse_decimal_separator),
        stringResource(Res.string.browse_thousand_suffix),
        stringResource(Res.string.browse_million_suffix)
    )

    @Composable
    fun viewers(value: Int): String = browseViewers(value, compact(value))

    @Composable
    fun channels(value: Int): String = browseChannels(value, compact(value))

    @Composable
    fun uptimeText(startedAtMs: Long?, nowMs: Long): String? = uptime(startedAtMs, nowMs)?.let {
        if (it.hours > 0) {
            stringResource(Res.string.browse_uptime_hours, it.hours.toInt(), it.minutes.toString().padStart(2, '0'))
        } else {
            stringResource(Res.string.browse_uptime_minutes, it.minutes.toInt())
        }
    }

    suspend fun aiPrompt(stream: BrowseStream, category: String, nowMs: Long): String {
        val compact = render(
            compactCount(stream.viewers),
            getString(Res.string.browse_decimal_separator),
            getString(Res.string.browse_thousand_suffix),
            getString(Res.string.browse_million_suffix)
        )
        val uptime = uptime(stream.startedAtMs, nowMs)?.let {
            if (it.hours > 0) {
                getString(Res.string.browse_uptime_hours, it.hours.toInt(), it.minutes.toString().padStart(2, '0'))
            } else {
                getString(Res.string.browse_uptime_minutes, it.minutes.toInt())
            }
        } ?: UNKNOWN
        return getString(
            Res.string.browse_ai_stream_prompt,
            stream.displayName.ifBlank { stream.login },
            stream.title.replace('\n', ' ').trim().take(MAX_PROMPT_TITLE).ifBlank { UNKNOWN },
            category.ifBlank { UNKNOWN },
            stream.tags.take(MAX_PROMPT_TAGS).joinToString(", ").ifBlank { UNKNOWN },
            stream.language.uppercase().ifBlank { UNKNOWN },
            getPluralString(Res.plurals.browse_viewers, compactQuantity(stream.viewers), compact),
            uptime
        )
    }

    fun render(count: CompactCount, separator: String, thousand: String, million: String): String {
        val number = if (count.tenths == 0) count.whole.toString() else "${count.whole}$separator${count.tenths}"
        return when (count.unit) {
            CompactUnit.NONE -> number
            CompactUnit.THOUSAND -> number + thousand
            CompactUnit.MILLION -> number + million
        }
    }
}
