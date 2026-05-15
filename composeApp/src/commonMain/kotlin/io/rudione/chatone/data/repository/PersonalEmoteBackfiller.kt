package io.rudione.chatone.data.repository

import io.github.aakira.napier.Napier
import io.rudione.chatone.data.remote.emote.SevenTvApiClient
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


class PersonalEmoteBackfiller(
    private val emoteRepository: EmoteRepository,
    private val sevenTvApi: SevenTvApiClient,
    private val scope: CoroutineScope
) {
    companion object {
        private const val TAG = "PersonalEmoteBackfill"
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
                    workJob = scope.launch { drainLoop() }
                }
            }
        }
    }

    private suspend fun drainLoop() {
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
            val response = try {
                sevenTvApi.getChannelEmotesWithSetId(twitchUserId)
            } catch (e: Exception) {
                Napier.w("personal-fetch failed for $twitchUserId: ${e.message}", tag = TAG)
                null
            }
            val personalSetId = extractPersonalSetId(response)
            if (personalSetId != null) {
                val emotes = emoteRepository.grantPersonalEmoteSet(twitchUserId, personalSetId)
                if (!emotes.isNullOrEmpty()) {
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

    @Suppress("UNUSED_PARAMETER")
    private fun extractPersonalSetId(result: Any?): String? {

        return null
    }
}
