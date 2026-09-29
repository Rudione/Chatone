package io.rudione.chatone.data.repository

import io.rudione.chatone.data.remote.TwitchApiClient
import io.rudione.chatone.util.Result
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Instant

class AccountAgeRepository(
    private val apiClient: TwitchApiClient,
    private val scope: CoroutineScope
) {
    private val mutex = Mutex()
    private val known = LinkedHashMap<String, Long?>()
    private val waiting = LinkedHashMap<String, MutableList<CompletableDeferred<Long?>>>()
    private var pendingToken = ""
    private var flushJob: Job? = null

    suspend fun createdAtMs(userId: String, accessToken: String): Long? {
        if (userId.isBlank() || accessToken.isBlank()) return null
        val deferred = CompletableDeferred<Long?>()
        mutex.withLock {
            if (known.containsKey(userId)) return known[userId]
            waiting.getOrPut(userId) { mutableListOf() } += deferred
            pendingToken = accessToken
            if (flushJob?.isActive != true) {
                flushJob = scope.launch {
                    delay(BATCH_WINDOW_MS)
                    flush()
                }
            }
        }
        return deferred.await()
    }

    private suspend fun flush() {
        while (true) {
            val (ids, token) = mutex.withLock {
                if (waiting.isEmpty()) {
                    flushJob = null
                    return
                }
                waiting.keys.take(MAX_BATCH).toList() to pendingToken
            }
            val response = runCatching { apiClient.getUsers(accessToken = token, ids = ids) }.getOrNull()
            val resolved = (response as? Result.Success)?.data?.data.orEmpty()
                .mapNotNull { user -> parseInstant(user.createdAt)?.let { user.id to it } }
                .toMap()
            mutex.withLock {
                ids.forEach { id ->
                    val createdAt = resolved[id]
                    if (response is Result.Success) remember(id, createdAt)
                    waiting.remove(id)?.forEach { it.complete(createdAt) }
                }
            }
        }
    }

    private fun remember(userId: String, createdAt: Long?) {
        known[userId] = createdAt
        if (known.size > MAX_CACHED_USERS) known.remove(known.keys.first())
    }

    private fun parseInstant(value: String): Long? =
        runCatching { Instant.parse(value).toEpochMilliseconds() }.getOrNull()

    private companion object {
        const val BATCH_WINDOW_MS = 250L
        const val MAX_BATCH = 100
        const val MAX_CACHED_USERS = 5_000
    }
}
