package io.rudione.chatone.domain.live

import io.rudione.chatone.domain.model.LiveStreamSnapshot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LiveAlertPolicyTest {

    private fun stream(login: String, id: String) = LiveStreamSnapshot(
        login = login,
        displayName = login,
        streamId = id,
        startedAtMs = 0L,
        title = "",
        gameName = ""
    )

    @Test
    fun streamThatWasAlreadyLiveWhenWatchingStartedIsNotAnnounced() {
        val outcome = LiveAlertPolicy.evaluate(setOf("a"), emptyMap(), mapOf("a" to stream("a", "1")))
        assertTrue(outcome.alerts.isEmpty())
        assertEquals(LiveAlertEntry(lastStreamId = "1"), outcome.entries["a"])
    }

    @Test
    fun newStreamIsAnnouncedExactlyOnce() {
        val first = LiveAlertPolicy.evaluate(setOf("a"), emptyMap(), emptyMap())
        val live = mapOf("a" to stream("a", "7"))
        val second = LiveAlertPolicy.evaluate(setOf("a"), first.entries, live)
        assertEquals(listOf("7"), second.alerts.map { it.streamId })
        val third = LiveAlertPolicy.evaluate(setOf("a"), second.entries, live)
        assertTrue(third.alerts.isEmpty())
    }

    @Test
    fun restartedStreamIsAnnouncedAgain() {
        val entries = mapOf("a" to LiveAlertEntry(lastStreamId = "7"))
        val outcome = LiveAlertPolicy.evaluate(setOf("a"), entries, mapOf("a" to stream("a", "8")))
        assertEquals(listOf("8"), outcome.alerts.map { it.streamId })
    }

    @Test
    fun goingOfflineKeepsTheLastStreamAndDisabledChannelsAreForgotten() {
        val entries = mapOf(
            "a" to LiveAlertEntry(lastStreamId = "7"),
            "b" to LiveAlertEntry(lastStreamId = "9")
        )
        val outcome = LiveAlertPolicy.evaluate(setOf("a"), entries, emptyMap())
        assertTrue(outcome.alerts.isEmpty())
        assertEquals(mapOf("a" to LiveAlertEntry(lastStreamId = "7")), outcome.entries)
    }

    @Test
    fun liveStreamsOfChannelsNobodyWatchesAreIgnored() {
        val outcome = LiveAlertPolicy.evaluate(emptySet(), emptyMap(), mapOf("a" to stream("a", "1")))
        assertTrue(outcome.alerts.isEmpty())
        assertTrue(outcome.entries.isEmpty())
    }
}
