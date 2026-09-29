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
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RolesTvClientTest {

    private val requests = mutableListOf<Url>()

    private fun client(body: String, status: HttpStatusCode = HttpStatusCode.OK) = RolesTvClient(
        HttpClient(MockEngine { request ->
            requests += request.url
            respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
        }) { install(HttpTimeout) }
    )

    @Test
    fun countsUseTheUserIdAndKeepOnlyHeldRoles() = runTest {
        val counts = withContext(Dispatchers.Default) {
            client("""{"data":{"id":"814427398","roles":{"artists":2,"founders":0,"moderators":12,"vips":5,"subscribers":4}}}""")
                .roleCounts("814427398", "rudionee")
        }!!
        assertEquals(12, counts.of(ChannelRole.MODERATOR))
        assertEquals(listOf(ChannelRole.MODERATOR, ChannelRole.VIP, ChannelRole.ARTIST), counts.held)
        assertEquals("/api/user/id/814427398", requests.single().encodedPath)
    }

    @Test
    fun unknownUserHasNoRolesAndBadInputIsNotRequested() = runTest {
        val missing = withContext(Dispatchers.Default) {
            client("""{"error":"User not found"}""", HttpStatusCode.NotFound).roleCounts("", "Some_User")
        }
        assertEquals(emptyList(), missing!!.held)
        assertEquals("/api/user/login/some_user", requests.single().encodedPath)
        assertNull(client("{}").roleCounts("../1", "bad login"))
        assertEquals(1, requests.size)
    }

    @Test
    fun channelPagesParseStatusDatesAndCursor() = runTest {
        val body = """
            {"total":12,"page":1,"pages":4,"perPage":3,"cursor":"abc","data":[
              {"id":"501048449","login":"clean_killl","displayName":"clean_killl","avatar":"https://cdn/a.png","followers":77816,
               "createdAt":"2020-03-19 16:27:51.635629","grantedAt":"2026-05-03 11:52:54","isAffiliate":true,"isPartner":false},
              {"id":"x","login":"broken"}
            ]}
        """.trimIndent()
        val page = withContext(Dispatchers.Default) {
            client(body).roleChannels(ChannelRole.MODERATOR, "814427398", "rudionee", cursor = "prev")
        }!!
        assertEquals(12, page.total)
        assertEquals("abc", page.nextCursor)
        val channel = page.channels.single()
        assertTrue(channel.isAffiliate)
        assertFalse(channel.isPartner)
        assertEquals(77816, channel.followers)
        assertEquals(parseRolesTimestamp("2026-05-03T11:52:54Z"), channel.grantedAtMs)
        assertEquals("/api/stats/user/moderators/id/814427398", requests.single().encodedPath)
        assertEquals("prev", requests.single().parameters["after"])
    }

    @Test
    fun lastPageHasNoCursor() = runTest {
        val page = withContext(Dispatchers.Default) {
            client("""{"total":2,"page":1,"pages":1,"cursor":"zzz","data":[]}""")
                .roleChannels(ChannelRole.VIP, "1", "", cursor = null)
        }!!
        assertNull(page.nextCursor)
    }

    @Test
    fun rolesTimestampsAcceptSpaceSeparatedUtc() {
        assertEquals(1_590_000_000_000L, parseRolesTimestamp("2020-05-20 18:40:00"))
        assertNull(parseRolesTimestamp("not a date"))
        assertNull(parseRolesTimestamp(null))
    }
}
