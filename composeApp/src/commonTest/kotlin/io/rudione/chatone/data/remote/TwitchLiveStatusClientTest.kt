package io.rudione.chatone.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TwitchLiveStatusClientTest {

    private val response = """
        {"data":{"users":[
          {"login":"caedrel","displayName":"Caedrel","stream":{"id":"317789871844","createdAt":"2026-09-19T08:31:13Z","title":"Worlds","game":{"displayName":"League of Legends"}}},
          {"login":"xqc","displayName":"xQc","stream":null},
          null
        ]}}
    """.trimIndent()

    private val bodies = mutableListOf<String>()

    private fun client(status: HttpStatusCode = HttpStatusCode.OK): TwitchLiveStatusClient {
        val engine = MockEngine { request ->
            bodies += (request.body as TextContent).text
            respond(response, status, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        return TwitchLiveStatusClient(HttpClient(engine))
    }

    @Test
    fun onlyLiveChannelsAreReturned() = runTest {
        val live = withContext(Dispatchers.Default) { client().liveStreams(listOf("Caedrel", "xqc", "gone")) }
        val stream = live?.get("caedrel")
        assertEquals(setOf("caedrel"), live?.keys)
        assertEquals("317789871844", stream?.streamId)
        assertEquals("Caedrel", stream?.displayName)
        assertEquals("League of Legends", stream?.gameName)
        assertEquals(1_789_806_673_000L, stream?.startedAtMs)
    }

    @Test
    fun invalidLoginsAreNeverSent() = runTest {
        withContext(Dispatchers.Default) { client().liveStreams(listOf("ok_login", "../x", "a b")) }
        assertTrue(bodies.single().contains("ok_login"))
        assertTrue(bodies.none { it.contains("../x") || it.contains("a b") })
    }

    @Test
    fun failedRequestIsReportedAsUnknown() = runTest {
        assertNull(withContext(Dispatchers.Default) { client(HttpStatusCode.BadGateway).liveStreams(listOf("xqc")) })
    }
}
