package io.rudione.chatone.data.repository

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpTimeout
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.rudione.chatone.data.remote.BestLogsClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ChannelLogBackfillTest {

    private val paths = mutableListOf<String>()

    private fun privmsg(id: String, ts: Long, login: String = "viewer") =
        "@tmi-sent-ts=$ts;id=$id;room-id=1;user-id=7;display-name=$login;badges=;color=#FF0000;emotes= " +
            ":$login!$login@$login.tmi.twitch.tv PRIVMSG #chan :hello $id"

    private val day28 = listOf(
        privmsg("a", 1_790_553_600_000L),
        privmsg("b", 1_790_600_000_000L),
        "@tmi-sent-ts=1790600001000;room-id=1;target-user-id=7;ban-duration=600 :tmi.twitch.tv CLEARCHAT #chan :viewer"
    )
    private val day20 = listOf(privmsg("old", 1_789_900_000_000L))

    private fun backfill(daysBody: String = DAYS) = ChannelLogBackfill(
        BestLogsClient(HttpClient(MockEngine { request ->
            paths += request.url.encodedPath
            when (request.url.encodedPath) {
                "/list" -> respond(daysBody, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
                "/channelid/1/2026/9/28" -> respond(day28.joinToString("\n"), HttpStatusCode.OK)
                "/channelid/1/2026/9/20" -> respond(day20.joinToString("\n"), HttpStatusCode.OK)
                else -> respond("", HttpStatusCode.NotFound)
            }
        }) { install(HttpTimeout) })
    )

    @Test
    fun olderDaysAreLoadedNewestFirstUntilEnough() = runTest {
        val result = withContext(Dispatchers.Default) {
            backfill().messagesBefore("1", beforeMs = 1_790_700_000_000L, wanted = 3)
        }!!
        assertEquals(listOf("old", "a", "b"), result.messages.map { it.id })
        assertEquals(1, result.events.size)
        assertEquals(listOf("/list", "/channelid/1/2026/9/28", "/channelid/1/2026/9/20"), paths)
    }

    @Test
    fun stopsAsSoonAsTheWantedAmountIsReached() = runTest {
        val result = withContext(Dispatchers.Default) {
            backfill().messagesBefore("1", beforeMs = 1_790_700_000_000L, wanted = 2)
        }!!
        assertEquals(listOf("a", "b"), result.messages.map { it.id })
        assertTrue("/channelid/1/2026/9/20" !in paths)
    }

    @Test
    fun messagesAtOrAfterTheCutoffAreSkipped() = runTest {
        val result = withContext(Dispatchers.Default) {
            backfill().messagesBefore("1", beforeMs = 1_790_600_000_000L, wanted = 10)
        }!!
        assertEquals(listOf("old", "a"), result.messages.map { it.id })
    }

    @Test
    fun unloggedChannelGivesNothing() = runTest {
        val result = withContext(Dispatchers.Default) {
            backfill("""{"availableLogs":[]}""").messagesBefore("1", beforeMs = 1_790_700_000_000L, wanted = 10)
        }
        assertNull(result)
    }

    private companion object {
        const val DAYS = """{"availableLogs":[{"year":"2026","month":"9","day":"20"},{"year":"2026","month":"9","day":"30"},{"year":"2026","month":"9","day":"28"}]}"""
    }
}
