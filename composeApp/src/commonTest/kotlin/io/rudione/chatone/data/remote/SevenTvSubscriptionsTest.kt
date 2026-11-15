package io.rudione.chatone.data.remote

import io.rudione.chatone.data.remote.emote.SevenTvSubscriptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SevenTvSubscriptionsTest {
    @Test
    fun snapshotsKeepSubscriptionsAndRemainUnchanged() {
        val subscriptions = SevenTvSubscriptions()
        assertTrue(subscriptions.add("first"))
        assertFalse(subscriptions.add("first"))
        val snapshot = subscriptions.snapshot()
        subscriptions.remove("first")
        subscriptions.add("second")
        assertEquals(listOf("first"), snapshot)
        assertEquals(listOf("second"), subscriptions.snapshot())
        subscriptions.clear()
        assertTrue(subscriptions.snapshot().isEmpty())
    }

    @Test
    fun concurrentChangesAndReconnectSnapshotsAreSafe() = runTest {
        val subscriptions = SevenTvSubscriptions()
        coroutineScope {
            repeat(4) { worker ->
                launch(Dispatchers.Default) {
                    repeat(2_000) { index ->
                        val id = "set_${worker}_$index"
                        subscriptions.add(id)
                        if (index % 2 == 0) subscriptions.remove(id)
                    }
                }
            }
            repeat(2) {
                launch(Dispatchers.Default) {
                    repeat(1_000) {
                        val snapshot = subscriptions.snapshot()
                        assertTrue(snapshot.all { id -> id.startsWith("set_") })
                        assertEquals(snapshot.size, snapshot.toSet().size)
                    }
                }
            }
        }
        assertEquals(4_000, subscriptions.snapshot().size)
    }
}
