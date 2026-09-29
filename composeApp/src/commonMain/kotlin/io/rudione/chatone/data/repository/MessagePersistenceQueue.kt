package io.rudione.chatone.data.repository

import io.github.aakira.napier.Napier
import io.rudione.chatone.domain.model.ChatMessage
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

class MessagePersistenceQueue(
    private val chatRepository: ChatRepository,
    private val scope: CoroutineScope
) {
    companion object {
        private const val TAG = "MessagePersistence"
        private const val QUEUE_CAPACITY = 2048
        private const val BATCH_SIZE = 64
    }

    private val pending = Channel<ChatMessage>(
        capacity = QUEUE_CAPACITY,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    @OptIn(InternalCoroutinesApi::class)
    private val startLock = SynchronizedObject()
    private var started = false

    @OptIn(InternalCoroutinesApi::class)
    private fun ensureStarted() {
        synchronized(startLock) {
            if (started) return
            started = true
        }
        scope.launch {
            val batch = ArrayList<ChatMessage>(BATCH_SIZE)
            for (first in pending) {
                batch.clear()
                batch.add(first)
                while (batch.size < BATCH_SIZE) {
                    val next = pending.tryReceive().getOrNull() ?: break
                    batch.add(next)
                }
                try {
                    chatRepository.saveMessages(batch)
                } catch (e: Exception) {
                    Napier.w("Failed to persist ${batch.size} messages: ${e.message}", tag = TAG)
                }
            }
        }
    }

    fun enqueue(message: ChatMessage) {
        ensureStarted()
        pending.trySend(message)
    }
}
