package io.rudione.chatone.util.network

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SocketRecoveryTest {
    @Test fun networkLossPausesAttemptsAndPathRecoveryInterruptsBackoff() = runTest {
        val path = MutableStateFlow(NetworkStatus(true, "wifi"))
        val monitor = NetworkMonitor(backgroundScope, path)
        var attempts = 0
        var failures = 0
        val job = backgroundScope.launch {
            monitor.recoverSocket(onFailure = { failures++ }) {
                attempts++
                throw IllegalStateException("unavailable")
            }
        }
        runCurrent()
        assertEquals(1, attempts)
        path.value = NetworkStatus(false)
        runCurrent()
        val beforeOffline = attempts
        advanceTimeBy(300_000); runCurrent()
        assertEquals(beforeOffline, attempts)
        path.value = NetworkStatus(true, "ethernet")
        runCurrent()
        assertEquals(beforeOffline + 1, attempts)
        assertTrue(failures >= 2)
        job.cancelAndJoin()
        advanceTimeBy(300_000); runCurrent()
        assertEquals(beforeOffline + 1, attempts)
    }

    @Test fun handshakeTimeoutRetriesButExternalCancellationDoesNot() = runTest {
        val monitor = NetworkMonitor(backgroundScope, MutableStateFlow(NetworkStatus(true, "wifi")))
        var attempts = 0
        var failures = 0
        val job = backgroundScope.launch {
            monitor.recoverSocket(onFailure = { failures++ }) {
                attempts++
                withTimeout(1_000) { awaitCancellation() }
            }
        }
        runCurrent()
        advanceTimeBy(4_000); runCurrent()
        assertTrue(attempts >= 2)
        assertTrue(failures >= 1)
        job.cancelAndJoin()
        val stoppedAt = attempts
        advanceTimeBy(120_000); runCurrent()
        assertEquals(stoppedAt, attempts)
    }

    @Test fun pathChangeAndWakeCancelOldSessionBeforeStartingNext() = runTest {
        val path = MutableStateFlow(NetworkStatus(true, "wifi"))
        val monitor = NetworkMonitor(backgroundScope, path)
        var active = 0
        var maxActive = 0
        var attempts = 0
        val job = backgroundScope.launch {
            monitor.recoverSocket(onFailure = { throw it }) {
                attempts++
                active++
                maxActive = maxOf(maxActive, active)
                try { awaitCancellation() } finally { active-- }
            }
        }
        runCurrent()
        path.value = NetworkStatus(true, "ethernet")
        runCurrent()
        path.value = NetworkStatus(true, "ethernet", 1)
        runCurrent()
        assertEquals(3, attempts)
        assertEquals(1, maxActive)
        job.cancelAndJoin()
        assertEquals(0, active)
    }
}
