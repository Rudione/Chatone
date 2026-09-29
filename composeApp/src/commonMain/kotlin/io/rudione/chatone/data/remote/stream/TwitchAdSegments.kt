package io.rudione.chatone.data.remote.stream

import kotlin.math.abs
import kotlin.time.Instant

internal object TwitchAdSegments {

    private const val TAG_EXTINF = "#EXTINF:"
    private const val TAG_MEDIA_SEQUENCE = "#EXT-X-MEDIA-SEQUENCE:"
    private const val TAG_PROGRAM_DATE_TIME = "#EXT-X-PROGRAM-DATE-TIME:"
    private const val LIVE_TITLE = "live"
    private const val SAME_SEGMENT_TOLERANCE_MS = 1_000L

    fun hasAds(mediaPlaylist: String): Boolean =
        mediaPlaylist.lineSequence().any { line -> line.startsWith(TAG_EXTINF) && isAdTitle(line) }

    fun hasOnlyAbsoluteHttpsSegments(mediaPlaylist: String): Boolean {
        var segments = 0
        for (raw in mediaPlaylist.lineSequence()) {
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#")) continue
            if (!line.startsWith("https://")) return false
            segments++
        }
        return segments > 0
    }

    fun shareTimeline(first: String, second: String): Boolean {
        val firstTimes = segmentTimes(first)
        if (firstTimes.isEmpty()) return false
        val secondTimes = segmentTimes(second)
        var common = 0
        for ((sequence, time) in firstTimes) {
            val other = secondTimes[sequence] ?: continue
            if (abs(other - time) > SAME_SEGMENT_TOLERANCE_MS) return false
            common++
        }
        return common > 0
    }

    private fun segmentTimes(mediaPlaylist: String): Map<Long, Long> {
        val times = HashMap<Long, Long>()
        var sequence: Long? = null
        var pendingTime: Long? = null
        for (raw in mediaPlaylist.lineSequence()) {
            val line = raw.trim()
            when {
                line.isEmpty() -> Unit
                line.startsWith(TAG_MEDIA_SEQUENCE) ->
                    sequence = line.substring(TAG_MEDIA_SEQUENCE.length).trim().toLongOrNull()
                line.startsWith(TAG_PROGRAM_DATE_TIME) ->
                    pendingTime = runCatching {
                        Instant.parse(line.substring(TAG_PROGRAM_DATE_TIME.length).trim()).toEpochMilliseconds()
                    }.getOrNull()
                line.startsWith("#") -> Unit
                else -> {
                    val current = sequence ?: return emptyMap()
                    pendingTime?.let { times[current] = it }
                    pendingTime = null
                    sequence = current + 1
                }
            }
        }
        return times
    }

    private fun isAdTitle(extinf: String): Boolean {
        val title = extinf.substringAfter(',', "").trim()
        return title.isNotEmpty() && !title.startsWith(LIVE_TITLE)
    }
}
