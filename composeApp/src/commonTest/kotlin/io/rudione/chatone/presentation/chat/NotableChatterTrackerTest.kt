package io.rudione.chatone.presentation.chat

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import io.rudione.chatone.data.remote.ChatterFame
import io.rudione.chatone.data.remote.TwitchChatterFameClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NotableChatterTrackerTest {

    private suspend fun TestScope.settle(done: () -> Boolean) {
        advanceTimeBy(1_300)
        runCurrent()
        withContext(Dispatchers.Default) {
            withTimeout(5_000) { while (!done()) delay(10) }
        }
        runCurrent()
    }

    private val requests = mutableListOf<String>()

    private val body = """
        {"data":{"users":[
          {"id":"1","login":"partner","displayName":"Partner","profileImageURL":"https://cdn/p.png","roles":{"isPartner":true},"followers":{"totalCount":900}},
          {"id":"2","login":"big","displayName":"Big","profileImageURL":null,"roles":{"isPartner":false},"followers":{"totalCount":50000}},
          {"id":"3","login":"small","displayName":"Small","profileImageURL":null,"roles":{"isPartner":false},"followers":{"totalCount":49999}},
          null
        ]}}
    """.trimIndent()

    private fun client() = TwitchChatterFameClient(HttpClient(MockEngine { request ->
        requests += (request.body as TextContent).text
        respond(body, headers = headersOf(HttpHeaders.ContentType, "application/json"))
    }))

    @Test
    fun partnersAndFiftyThousandFollowersAreNotable() = runTest {
        val fame = client().lookup(listOf("1", "2", "3", "4"))!!
        assertTrue(fame.getValue("1").isNotable)
        assertTrue(fame.getValue("2").isNotable)
        assertFalse(fame.getValue("3").isNotable)
        assertFalse("4" in fame)
    }

    @Test
    fun knownUsersAreNotQueriedTwiceAndBadIdsAreDropped() = runTest {
        val fameClient = client()
        fameClient.lookup(listOf("1", "2", "../x"))
        fameClient.lookup(listOf("1", "2"))
        assertEquals(1, requests.size)
        assertFalse(requests.single().contains("../x"))
    }

    @Test
    fun trackerBatchesAndAnnouncesEachStreamerOncePerChannel() = runTest {
        val announced = mutableListOf<Pair<String, ChatterFame>>()
        val tracker = NotableChatterTracker(client(), this) { channel, fame, _ ->
            announced += channel to fame
            true
        }
        listOf("1", "2", "3", "1").forEach { tracker.observe("Chan", it) }
        settle { announced.size >= 2 }
        assertEquals(listOf("1", "2"), announced.map { it.second.userId })
        assertEquals(setOf("chan"), announced.map { it.first }.toSet())
        assertEquals(1, requests.size)

        tracker.observe("chan", "1")
        advanceTimeBy(1_300)
        runCurrent()
        assertEquals(2, announced.size)
    }

    @Test
    fun streamerIsRetriedWhenTheirMessageWasNotOnScreenYet() = runTest {
        var accept = false
        val announced = mutableListOf<String>()
        var attempts = 0
        val counting = NotableChatterTracker(client(), this) { _, fame, _ ->
            attempts++
            if (accept) announced += fame.userId
            accept
        }
        counting.observe("chan", "1")
        settle { attempts >= 1 }
        accept = true
        counting.observe("chan", "1")
        settle { announced.isNotEmpty() }
        assertEquals(listOf("1"), announced)
    }

    @Test
    fun loginsAreResolvedCaseInsensitivelyAndInvalidOnesAreDropped() = runTest {
        val fame = client().lookupLogins(listOf("Partner", "big", "small", "bad login", "../x"))!!
        assertEquals(setOf("partner", "big", "small"), fame.keys)
        assertEquals(1, requests.size)
        assertTrue(requests.single().contains("\"logins\""))
        assertFalse(requests.single().contains("../x"))
        assertFalse(requests.single().contains("bad login"))
    }

    @Test
    fun joinAnnouncesNotableViewerWithoutMessagesOnce() = runTest {
        val announced = mutableListOf<Triple<String, String, Boolean>>()
        val tracker = NotableChatterTracker(client(), this) { channel, fame, joined ->
            announced += Triple(channel, fame.userId, joined)
            true
        }
        tracker.observeJoin("Chan", "Partner")
        tracker.observeJoin("chan", "small")
        settle { announced.isNotEmpty() }
        assertEquals(listOf(Triple("chan", "1", true)), announced)

        tracker.observe("chan", "1")
        tracker.observeJoin("chan", "partner")
        advanceTimeBy(1_300)
        runCurrent()
        assertEquals(1, announced.size)
    }
}
