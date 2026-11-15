package io.rudione.chatone.util.network

import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.random.Random
import kotlin.time.Clock

internal suspend fun NetworkMonitor.recoverSocket(
    onFailure: (Exception) -> Unit,
    connect: suspend () -> Unit
) = coroutineScope {
    val changes = Channel<Unit>(Channel.CONFLATED)
    val observer = launch {
        var previous = status.value
        status.collect { current ->
            if (current != previous && (previous.available != null || current.available == false || current.wakeVersion != previous.wakeVersion)) changes.trySend(Unit)
            previous = current
        }
    }
    var retryDelay = 2_000L
    try {
        while (isActive) {
            status.first { it.available != false }
            while (changes.tryReceive().isSuccess) { }
            val beganAt = Clock.System.now()
            try {
                val pathChanged = supervisorScope {
                    val connection = async { connect() }
                    try {
                        select {
                            connection.onAwait { false }
                            changes.onReceive { true }
                        }
                    } finally {
                        connection.cancelAndJoin()
                    }
                }
                if (pathChanged) {
                    retryDelay = 2_000
                    continue
                }
            } catch (e: TimeoutCancellationException) {
                currentCoroutineContext().ensureActive()
                onFailure(e)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                onFailure(e)
            }
            if ((Clock.System.now() - beganAt).inWholeSeconds >= 30) retryDelay = 2_000
            if (status.value.available != false) {
                if (withTimeoutOrNull(retryDelay + Random.nextLong(0, 500)) { changes.receive(); true } == true) retryDelay = 2_000
                else retryDelay = (retryDelay * 2).coerceAtMost(30_000)
            }
        }
    } finally {
        observer.cancel()
        changes.close()
    }
}
