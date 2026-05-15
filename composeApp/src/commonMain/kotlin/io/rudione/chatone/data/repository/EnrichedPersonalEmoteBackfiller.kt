package io.rudione.chatone.data.repository

import io.github.aakira.napier.Napier
import io.rudione.chatone.data.remote.emote.PersonalSetExtractor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock


class EnrichedPersonalEmoteBackfiller(
    private val emoteRepository: EmoteRepository,
    private val extractor: PersonalSetExtractor,
    private val scope: CoroutineScope
) {
    companion object {
        private const val TAG = "EnrichedBackfill"
        private const val DEBOUNCE_MS = 200L
        private const val NEGATIVE_TTL_MS = 5 * 60_000L
        private const val PARALLEL_LIMIT = 4
    }

    private val pending = mutableSetOf<String>()
    private val inflight = mutableSetOf<String>()
    private val negativeCache = mutableMapOf<String, Long>()
    private val lock = Mutex()
    private var workJob: Job? = null

    private val _granted = MutableSharedFlow<String>(extraBufferCapacity = 64)
    val granted: SharedFlow<String> = _granted.asSharedFlow()

    fun request(twitchUserId: String) {
        if (twitchUserId.isBlank()) return
        if (emoteRepository.hasAttemptedPersonalEmotes(twitchUserId)) return
        scope.launch {
            lock.withLock {
                val now = Clock.System.now().toEpochMilliseconds()
                val negativeEntry = negativeCache[twitchUserId]
                if (negativeEntry != null && now - negativeEntry < NEGATIVE_TTL_MS) return@withLock
                if (twitchUserId in inflight) return@withLock
                pending.add(twitchUserId)
                if (workJob?.isActive != true) {
                    workJob = scope.launch { drain() }
                }
            }
        }
    }

    private suspend fun drain() {
        while (true) {
            delay(DEBOUNCE_MS)
            val batch: List<String> = lock.withLock {
                if (pending.isEmpty()) return
                val take = pending.take(PARALLEL_LIMIT).toList()
                pending.removeAll(take.toSet())
                inflight.addAll(take)
                take
            }
            batch.forEach { userId ->
                scope.launch { fetchOne(userId) }
            }
        }
    }

    private suspend fun fetchOne(twitchUserId: String) {
        try {
            val setId = extractor.fetchPersonalSetId(twitchUserId)
            if (!setId.isNullOrBlank()) {
                val emotes = emoteRepository.grantPersonalEmoteSet(twitchUserId, setId)
                if (!emotes.isNullOrEmpty()) {
                    Napier.d("Granted personal set $setId for $twitchUserId (${emotes.size})", tag = TAG)
                    _granted.emit(twitchUserId)
                }
            } else {
                lock.withLock {
                    negativeCache[twitchUserId] = Clock.System.now().toEpochMilliseconds()
                }
            }
            emoteRepository.markPersonalEmotesAttempted(twitchUserId)
        } finally {
            lock.withLock { inflight.remove(twitchUserId) }
        }
    }

    fun reset() {
        scope.launch {
            lock.withLock {
                pending.clear()
                inflight.clear()
                negativeCache.clear()
            }
        }
    }
}
