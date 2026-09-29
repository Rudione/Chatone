package io.rudione.chatone.presentation.chat

import io.rudione.chatone.data.remote.ChatterFame
import io.rudione.chatone.data.remote.TwitchChatterFameClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

internal class NotableChatterTracker(
    private val client: TwitchChatterFameClient,
    private val scope: CoroutineScope,
    private val announce: (channel: String, fame: ChatterFame, joined: Boolean) -> Boolean
) {
    private val announced = HashMap<String, MutableSet<String>>()
    private val pendingIds = LinkedHashMap<String, String>()
    private val pendingLogins = LinkedHashMap<String, String>()
    private var flushJob: Job? = null
    private var pausedUntil: TimeSource.Monotonic.ValueTimeMark? = null

    fun observe(channel: String, userId: String) {
        if (userId.isBlank()) return
        val key = channel.lowercase()
        if (announced[key]?.contains(userId) == true) return
        pendingIds[userId] = key
        schedule()
    }

    fun observeJoin(channel: String, login: String) {
        if (login.isBlank()) return
        pendingLogins[login.lowercase()] = channel.lowercase()
        schedule()
    }

    private fun schedule() {
        if (flushJob?.isActive != true) flushJob = scope.launch { flush() }
    }

    private suspend fun flush() {
        delay(BATCH_DELAY_MS)
        pausedUntil?.let { mark -> if (mark.hasNotPassedNow()) delay((-mark.elapsedNow()).inWholeMilliseconds) }
        while (pendingIds.isNotEmpty() || pendingLogins.isNotEmpty()) {
            if (!drain(pendingIds, joined = false, lookup = client::lookup)) return
            if (!drain(pendingLogins, joined = true, lookup = client::lookupLogins)) return
        }
    }

    private suspend fun drain(
        pending: LinkedHashMap<String, String>,
        joined: Boolean,
        lookup: suspend (Collection<String>) -> Map<String, ChatterFame>?
    ): Boolean {
        if (pending.isEmpty()) return true
        val batch = LinkedHashMap(pending)
        pending.clear()
        val fame = lookup(batch.keys)
        if (fame == null) {
            batch.forEach { (key, channel) -> pending.getOrPut(key) { channel } }
            pausedUntil = TimeSource.Monotonic.markNow() + RETRY_PAUSE_MS.milliseconds
            return false
        }
        batch.forEach { (key, channel) ->
            val notable = fame[key]?.takeIf { it.isNotable } ?: return@forEach
            val seen = announced.getOrPut(channel) { HashSet() }
            if (notable.userId !in seen && announce(channel, notable, joined)) seen += notable.userId
        }
        return true
    }

    private companion object {
        const val BATCH_DELAY_MS = 1_200L
        const val RETRY_PAUSE_MS = 30_000L
    }
}
