package io.rudione.chatone.util.concurrent

import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlin.time.Clock

class SingleFlightCache<T : Any>(
    private val capacity: Int,
    private val retryFailedAfterMs: Long = DEFAULT_RETRY_FAILED_AFTER_MS,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    private val nowMs: () -> Long = { Clock.System.now().toEpochMilliseconds() }
) {
    private class Entry<T>(val value: T?, val storedAtMs: Long)

    private val lock = SynchronizedObject()
    private val entries = LinkedHashMap<String, Entry<T>>()
    private val pending = HashMap<String, Deferred<T?>>()

    fun peek(key: String): T? = synchronized(lock) { settled(key)?.value }

    suspend fun load(key: String, produce: suspend () -> T?): T? {
        val flight = synchronized(lock) {
            settled(key)?.let { return it.value }
            pending.getOrPut(key) { scope.async(start = CoroutineStart.LAZY) { land(key, produce) } }
        }
        flight.start()
        return flight.await()
    }

    fun clear() = synchronized(lock) { entries.clear() }

    private suspend fun land(key: String, produce: suspend () -> T?): T? {
        val value = try {
            produce()
        } catch (cancelled: CancellationException) {
            synchronized(lock) { pending.remove(key) }
            throw cancelled
        } catch (failure: Exception) {
            null
        }
        synchronized(lock) {
            pending.remove(key)
            entries.remove(key)
            entries[key] = Entry(value, nowMs())
            while (entries.size > capacity) entries.remove(entries.keys.first())
        }
        return value
    }

    private fun settled(key: String): Entry<T>? {
        val entry = entries.remove(key) ?: return null
        if (entry.value == null && nowMs() - entry.storedAtMs >= retryFailedAfterMs) return null
        entries[key] = entry
        return entry
    }

    private companion object {
        const val DEFAULT_RETRY_FAILED_AFTER_MS = 30_000L
    }
}
