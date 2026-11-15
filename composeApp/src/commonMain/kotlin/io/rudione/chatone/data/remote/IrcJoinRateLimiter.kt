package io.rudione.chatone.data.remote

import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Clock

internal class IrcJoinRateLimiter(private val now: () -> Long = { Clock.System.now().toEpochMilliseconds() }) {
    private class Account {
        val mutex = Mutex()
        var nextJoinMs = 0L
    }
    private val lock = SynchronizedObject()
    private val accounts = mutableMapOf<String, Account>()

    suspend fun awaitTurn(login: String) {
        val account = synchronized(lock) { accounts.getOrPut(login) { Account() } }
        account.mutex.withLock {
            val remaining = (account.nextJoinMs - now()).coerceIn(0L, 600L)
            if (remaining > 0) delay(remaining)
            account.nextJoinMs = now() + 600L
        }
    }
}
