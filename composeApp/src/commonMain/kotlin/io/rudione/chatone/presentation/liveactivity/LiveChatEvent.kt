package io.rudione.chatone.presentation.liveactivity

import io.rudione.chatone.data.remote.dto.PollData
import io.rudione.chatone.data.remote.dto.PredictionData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class LiveChatEvent(
    val id: String,
    val kind: String,
    val channel: String,
    val title: String,
    val phase: String,
    val deadlineMs: Long?,
    val options: List<String>,
    val totals: List<Int>,
    val updatedAtMs: Long
) {
    val key: String get() = "$kind:$channel:$id"
    val terminal: Boolean get() = phase in setOf("RESOLVED", "CANCELED", "COMPLETED", "TERMINATED", "ARCHIVED", "MODERATED", "INVALID")
}

fun PollData.liveChatEvent(channel: String, nowMs: Long, previous: LiveChatEvent? = null): LiveChatEvent {
    val start = startedAt?.let { runCatching { Instant.parse(it).toEpochMilliseconds() }.getOrNull() }
    val knownDeadline = previous?.takeIf { it.id == id && it.kind == "poll" && it.channel == channel }?.deadlineMs
    val relativeDeadline = remainingMs?.let { (receivedAtMs ?: nowMs) + it.coerceAtLeast(0) }
    val deadline = start?.let { it + duration.coerceAtLeast(0) * 1000L }
        ?: if (receivedAtMs != null) relativeDeadline ?: knownDeadline else knownDeadline ?: relativeDeadline
    return LiveChatEvent(id, "poll", channel, title.bounded(140), status.uppercase(), deadline,
        choices.take(5).map { it.title.bounded(60) }, choices.take(5).map { (it.votes.toLong() + it.channelPointsVotes).coerceIn(0, Int.MAX_VALUE.toLong()).toInt() }, nowMs)
}

fun PredictionData.liveChatEvent(channel: String, nowMs: Long): LiveChatEvent {
    val start = createdAt?.let { runCatching { Instant.parse(it).toEpochMilliseconds() }.getOrNull() }
    return LiveChatEvent(id, "prediction", channel, title.bounded(140), status.uppercase(), start?.let { it + predictionWindow.coerceAtLeast(0) * 1000L },
        outcomes.take(2).map { if (it.id == winningOutcomeId) "✓ ${it.title.bounded(58)}" else it.title.bounded(60) },
        outcomes.take(2).map { it.users.coerceAtLeast(0) }, nowMs)
}

object LiveEventActivities {
    val available = MutableStateFlow(false)
    private val _followed = MutableStateFlow<LiveChatEvent?>(null)
    val followed = _followed.asStateFlow()
    var onFollow: ((LiveChatEvent?) -> Unit)? = null
    var onObserved: ((LiveChatEvent) -> Unit)? = null
    private val skipped = LinkedHashSet<String>()
    private const val SKIPPED_MEMORY = 50

    private fun follow(event: LiveChatEvent) {
        if (event.channel.isBlank() || event.deadlineMs == null || event.terminal) return
        _followed.value = event
        onFollow?.invoke(event)
    }

    fun clearSelection() {
        _followed.value = null
    }

    fun stop() {
        clearSelection()
        onFollow?.invoke(null)
    }

    fun observed(event: LiveChatEvent) {
        if (_followed.value?.key != event.key) return
        _followed.value = event
        onObserved?.invoke(event)
    }

    fun autoFollow(event: LiveChatEvent) {
        if (!available.value || event.key in skipped) return
        val current = _followed.value
        if (current != null && !current.terminal) return
        follow(event)
    }

    fun dismissedByUser(eventKey: String) = skip(eventKey)

    fun restored(event: LiveChatEvent) { _followed.value = event }

    fun failed() {
        _followed.value?.key?.let(::skip)
        _followed.value = null
    }

    private fun skip(eventKey: String) {
        skipped += eventKey
        if (skipped.size > SKIPPED_MEMORY) skipped.remove(skipped.first())
    }
}

private fun String.bounded(limit: Int): String {
    var end = minOf(length, limit)
    if (end > 0 && end < length && this[end - 1].isHighSurrogate()) end--
    return substring(0, end)
}
