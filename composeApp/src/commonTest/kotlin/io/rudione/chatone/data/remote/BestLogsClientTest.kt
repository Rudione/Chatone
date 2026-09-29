package io.rudione.chatone.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpTimeout
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.headersOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BestLogsClientTest {

    private val requests = mutableListOf<Url>()

    private fun client(body: String, status: HttpStatusCode = HttpStatusCode.OK): BestLogsClient {
        val engine = MockEngine { request ->
            requests += request.url
            respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        return BestLogsClient(HttpClient(engine) { install(HttpTimeout) })
    }

    private val monthBody = """
        {"messages":[
          {"text":"later","timestamp":"2026-09-02T10:00:00Z","id":"m2","tags":{"user-id":"7"}},
          {"text":"hello","timestamp":"2026-09-01T10:00:00Z","id":"m1","tags":{"user-id":"7"}},
          {"text":"","timestamp":"2026-09-01T10:00:05Z","id":"x","tags":{"target-msg-id":"m1"}},
          {"text":"u has been timed out for 600 seconds","timestamp":"2026-09-02T11:00:00Z","id":"t1","tags":{"target-user-id":"7","ban-duration":"600"}},
          {"text":"u has been banned","timestamp":"2026-09-03T11:00:00Z","id":"b1","tags":{"target-user-id":"7"}},
          {"text":"u subscribed","timestamp":"2026-09-03T12:00:00Z","id":"n1","tags":{"system-msg":"u subscribed"}}
        ]}
    """.trimIndent()

    @Test
    fun monthLinesAreSortedAndClassified() = runTest {
        val lines = withContext(Dispatchers.Default) {
            client(monthBody).monthLines("1", "7", LogMonth(2026, 9))
        }.orEmpty()
        assertEquals(listOf("m1", "m2", "t1", "b1", "n1"), lines.map { it.id })
        assertTrue(lines.first { it.id == "m1" }.isDeleted)
        assertEquals(ArchivedChatLine.Kind.TIMEOUT, lines.first { it.id == "t1" }.kind)
        assertEquals(600, lines.first { it.id == "t1" }.durationSeconds)
        assertEquals(ArchivedChatLine.Kind.BAN, lines.first { it.id == "b1" }.kind)
        assertEquals(ArchivedChatLine.Kind.NOTICE, lines.first { it.id == "n1" }.kind)
        assertEquals("/channelid/1/userid/7/2026/9", requests.single().encodedPath)
    }

    @Test
    fun monthsAreNewestFirst() = runTest {
        val result = withContext(Dispatchers.Default) {
            client("""{"availableLogs":[{"year":"2025","month":"12"},{"year":"2026","month":"2"},{"year":"2026","month":"9"}]}""")
                .availableMonths("1", "7")
        }
        assertIs<LogMonthsResult.Available>(result)
        assertEquals(listOf(LogMonth(2026, 9), LogMonth(2026, 2), LogMonth(2025, 12)), result.months)
    }

    @Test
    fun missingChannelIsNotLogged() = runTest {
        val result = withContext(Dispatchers.Default) {
            client("channel not logged", HttpStatusCode.NotFound).availableMonths("1", "7")
        }
        assertIs<LogMonthsResult.NotLogged>(result)
    }

    @Test
    fun nonNumericIdsAreNeverSent() = runTest {
        val result = withContext(Dispatchers.Default) {
            client(monthBody).monthLines("../1", "7", LogMonth(2026, 9))
        }
        assertNull(result)
        assertTrue(requests.isEmpty())
    }

    @Test
    fun searchSendsTheQuery() = runTest {
        withContext(Dispatchers.Default) { client(monthBody).search("1", "7", " hello world ") }
        val url = requests.single()
        assertEquals("/channelid/1/userid/7/search", url.encodedPath)
        assertEquals("hello world", url.parameters["q"])
        assertEquals("1", url.parameters["jsonBasic"])
    }

    @Test
    fun nameHistoryDropsBlankLoginsAndSortsNewestFirst() = runTest {
        val names = withContext(Dispatchers.Default) {
            client("""[{"user_login":"","last_timestamp":"2026-09-04T22:22:06Z","first_timestamp":"2026-09-04T22:22:06Z"},
                {"user_login":"OldNick","last_timestamp":"2022-01-01T00:00:00Z","first_timestamp":"2020-01-01T00:00:00Z"},
                {"user_login":"rudionee","last_timestamp":"2026-09-28T21:08:30Z","first_timestamp":"2022-12-22T09:34:15Z"}]""")
                .nameHistory("814427398")
        }!!
        assertEquals(listOf("rudionee", "oldnick"), names.map { it.login })
        assertEquals("/namehistory/814427398", requests.single().encodedPath)
    }

    @Test
    fun channelDaysAreNewestFirst() = runTest {
        val days = withContext(Dispatchers.Default) {
            client("""{"availableLogs":[{"year":"2025","month":"12","day":"31"},{"year":"2026","month":"9","day":"2"},{"year":"2026","month":"9","day":"28"}]}""")
                .channelDays("1")
        }!!
        assertEquals(listOf(LogDay(2026, 9, 28), LogDay(2026, 9, 2), LogDay(2025, 12, 31)), days)
        assertNull(client("{}").channelDays("../1"))
    }
}
