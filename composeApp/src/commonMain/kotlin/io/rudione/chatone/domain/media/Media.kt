package io.rudione.chatone.domain.media

import androidx.compose.runtime.Immutable

@Immutable
data class MediaPerson(val id: String, val login: String, val displayName: String, val avatarUrl: String)

@Immutable
data class MediaSource(val label: String, val url: String, val isStream: Boolean)

enum class MediaKind { CLIP, VIDEO }

@Immutable
data class MediaDetails(
    val kind: MediaKind,
    val id: String,
    val title: String,
    val views: Int,
    val createdAtMs: Long?,
    val durationSeconds: Int,
    val thumbnailUrl: String,
    val pageUrl: String,
    val broadcaster: MediaPerson,
    val curator: MediaPerson?,
    val category: String,
    val replayVideoId: String?,
    val replayOffsetSeconds: Int,
    val sources: List<MediaSource>
)

@Immutable
data class ReplayLine(
    val id: String,
    val offsetSeconds: Int,
    val userId: String,
    val login: String,
    val displayName: String,
    val colorHex: String?,
    val text: String,
    val badges: List<ReplayBadge> = emptyList(),
    val emotes: List<ReplayEmote> = emptyList()
)

@Immutable
data class ReplayBadge(val setId: String, val version: String)

@Immutable
data class ReplayEmote(val id: String, val start: Int, val end: Int)

@Immutable
data class ClipDraft(
    val slug: String,
    val durationSeconds: Float,
    val sources: List<MediaSource>,
    val thumbnailUrl: String,
    val fallbackSources: List<MediaSource> = emptyList(),
    val ready: Boolean = true
) {
    val previewCandidates: List<MediaSource>
        get() = listOfNotNull(sources.previewQuality(), fallbackSources.previewQuality())

    private fun List<MediaSource>.previewQuality(): MediaSource? =
        firstOrNull { it.height in 1..PREVIEW_MAX_HEIGHT } ?: firstOrNull()

    private val MediaSource.height: Int
        get() = label.removeSuffix("p").toIntOrNull() ?: 0

    private companion object {
        const val PREVIEW_MAX_HEIGHT = 720
    }
}

enum class ClipFailure {
    SIGNED_OUT, NOT_LIVE, BLOCKED, FORBIDDEN, BANNED, RESTRICTED, THROTTLED, NEEDS_RIGHTS, TWITCH_ERROR, FAILED;

    companion object {
        private val FORBIDDEN_MARKERS = listOf("DISABLED", "FORBIDDEN", "NOT_ALLOWED")

        fun fromTwitchCode(code: String): ClipFailure = when (code.uppercase()) {
            "CHANNEL_NOT_LIVE" -> NOT_LIVE
            "CHANNEL_NOT_CLIPPABLE", "CHANNEL_BANNED" -> FORBIDDEN
            "USER_BANNED", "USER_TIMED_OUT" -> BANNED
            "USER_RESTRICTED" -> RESTRICTED
            "REQUEST_THROTTLED" -> THROTTLED
            else -> if (FORBIDDEN_MARKERS.any { code.contains(it, ignoreCase = true) }) FORBIDDEN else FAILED
        }
    }
}

sealed interface ClipOutcome<out T> {
    data class Done<T>(val value: T) : ClipOutcome<T>
    data class Failed(val reason: ClipFailure) : ClipOutcome<Nothing>
}

interface MediaCatalog {
    suspend fun clip(slug: String): MediaDetails?
    suspend fun video(id: String): MediaDetails?
    suspend fun replay(videoId: String, fromSeconds: Int): List<ReplayLine>?
}

sealed interface ClipOrigin {
    data class Live(val delaySeconds: Float?) : ClipOrigin
    data class Video(val videoId: String, val broadcasterId: String, val offsetSeconds: Float) : ClipOrigin
}

interface ClipStudio {
    suspend fun createDraft(broadcasterId: String, broadcastId: String?, offsetSeconds: Float): ClipOutcome<String>
    suspend fun createVideoDraft(broadcasterId: String, videoId: String, offsetSeconds: Float): ClipOutcome<String>
    suspend fun draft(slug: String): ClipDraft?
    suspend fun publish(slug: String, startSeconds: Float, lengthSeconds: Float, title: String): ClipOutcome<String>
    suspend fun delete(slug: String): ClipOutcome<Unit>
}

enum class ClipTrimGrip { START, END, BODY }

object ClipTrim {
    const val MIN_SECONDS = 5f
    const val MAX_SECONDS = 60f
    const val DEFAULT_SECONDS = 30f
    private const val SEEK_SLACK_SECONDS = 0.5f
    private const val DURATION_TOLERANCE_SECONDS = 0.75f

    fun defaultRange(total: Float): ClosedFloatingPointRange<Float> {
        val length = DEFAULT_SECONDS.coerceAtMost(total)
        return (total - length).coerceAtLeast(0f)..total
    }

    fun clamp(start: Float, end: Float, total: Float, movingStart: Boolean): ClosedFloatingPointRange<Float> {
        val maxLength = MAX_SECONDS.coerceAtMost(total)
        val minLength = MIN_SECONDS.coerceAtMost(total)
        var s = start.coerceIn(0f, total)
        var e = end.coerceIn(0f, total)
        if (movingStart) {
            s = s.coerceIn((e - maxLength).coerceAtLeast(0f), (e - minLength).coerceAtLeast(0f))
        } else {
            e = e.coerceIn((s + minLength).coerceAtMost(total), (s + maxLength).coerceAtMost(total))
        }
        return s..e
    }

    fun shift(range: ClosedFloatingPointRange<Float>, delta: Float, total: Float): ClosedFloatingPointRange<Float> {
        val length = range.endInclusive - range.start
        val start = (range.start + delta).coerceIn(0f, (total - length).coerceAtLeast(0f))
        return start..(start + length)
    }

    fun move(
        grip: ClipTrimGrip,
        range: ClosedFloatingPointRange<Float>,
        delta: Float,
        total: Float
    ): ClosedFloatingPointRange<Float> = when (grip) {
        ClipTrimGrip.START -> clamp(range.start + delta, range.endInclusive, total, movingStart = true)
        ClipTrimGrip.END -> clamp(range.start, range.endInclusive + delta, total, movingStart = false)
        ClipTrimGrip.BODY -> shift(range, delta, total)
    }

    fun previewAnchor(grip: ClipTrimGrip, range: ClosedFloatingPointRange<Float>): Float =
        if (grip == ClipTrimGrip.END) range.endInclusive else range.start

    fun withLength(range: ClosedFloatingPointRange<Float>, seconds: Float, total: Float): ClosedFloatingPointRange<Float> {
        val length = seconds.coerceIn(MIN_SECONDS, MAX_SECONDS).coerceAtMost(total)
        val end = range.endInclusive.coerceIn(length, total)
        return (end - length)..end
    }

    fun fit(range: ClosedFloatingPointRange<Float>, total: Float): ClosedFloatingPointRange<Float> {
        val length = (range.endInclusive - range.start)
            .coerceIn(MIN_SECONDS.coerceAtMost(total), MAX_SECONDS.coerceAtMost(total))
        val start = range.start.coerceIn(0f, (total - length).coerceAtLeast(0f))
        return start..(start + length)
    }

    fun leavesRange(positionSeconds: Float, range: ClosedFloatingPointRange<Float>): Boolean =
        positionSeconds >= range.endInclusive || positionSeconds < range.start - SEEK_SLACK_SECONDS

    fun playhead(positionSeconds: Float, range: ClosedFloatingPointRange<Float>): Float =
        positionSeconds.coerceIn(range.start, range.endInclusive)

    fun needsWindowUpdate(windowSeconds: Float, mediaSeconds: Float): Boolean =
        mediaSeconds >= MIN_SECONDS && kotlin.math.abs(windowSeconds - mediaSeconds) > DURATION_TOLERANCE_SECONDS
}

interface MediaModeration {
    suspend fun canModerate(broadcasterId: String): Boolean
    suspend fun ban(broadcasterId: String, userId: String, reason: String): Boolean
}
